package cronozx.cullinggames.database;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.tasks.StartBattleRoyal;
import cronozx.cullinggames.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class CoreDatabase {
    private static final CullingGames plugin = CullingGames.getInstance();
    private static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();
    private static final String dbPassword = configManager.getDbServerPass();
    private static final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), configManager.getDbServerIp(), configManager.getDbServerPort(), 2000, dbPassword);
    private  static final Logger logger = CullingGames.getInstance().getLogger();

    public void closeConnection() {
        jedisPool.close();
    }

    //Queue Methods
    public ArrayList<OfflinePlayer> getQueue() {
        ArrayList<OfflinePlayer> queue = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> playerUUIDs = jedis.lrange("playerQueue", 0, -1);

            for (String uuid : playerUUIDs) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                queue.add(player);
            }
        }
        return queue;
    }

    public void clearQueue() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("playerQueue");
        }
    }

    //Points Methods
    public void initPointsPlayers(ArrayList<OfflinePlayer> list) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (OfflinePlayer player : list) {
                jedis.hset("playerPoints", player.getUniqueId().toString(), "0");
            }
        }
    }

    public void setPoints(Player player, int points) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset("playerPoints", player.getUniqueId().toString(), String.valueOf(points));
        }
    }

    public int getPlayerPoints(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            String points = jedis.hget("playerPoints", player.getUniqueId().toString());
            return points != null ? Integer.parseInt(points) : 0;
        }
    }

    public ArrayList<OfflinePlayer> getAllPlayersInGame() {
        ArrayList<OfflinePlayer> players = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hkeys("playerPoints").forEach(playerUUID -> players.add(Bukkit.getOfflinePlayer(UUID.fromString(playerUUID))));
        }

        return players;
    }

    public void clearPlayersInGame() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel("playerPoints");
        }
    }

    //Util Methods
    public boolean inCullingGames(OfflinePlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hexists("playerPoints", player.getUniqueId().toString());
        }
    }

    public void removePlayerFromGame(OfflinePlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel("playerPoints", player.getUniqueId().toString());
        }
    }

    public int playersLeft() {
        try (Jedis jedis = jedisPool.getResource()) {
            return (int) jedis.hlen("playerPoints");
        }
    }


    //Messaging Methods
    public void sendMessageToRedis(String server, String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(server, message);
        }
    }

    public void onMessageFromRedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    logger.info("Message: " + message);
                    if (message.equals("start") && configManager.isBattleRoyalServer()) {
                        Bukkit.getScheduler().runTask(plugin, new StartBattleRoyal());
                    }
                }
            }, "cullinggames:bukkit");
        }
    }
}
