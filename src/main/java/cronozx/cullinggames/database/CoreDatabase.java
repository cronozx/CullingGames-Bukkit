package cronozx.cullinggames.database;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CoreDatabase {

    private static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();
    private static String dbPassword = configManager.getDbServerPass();
    private static final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), configManager.getDbServerIp(), configManager.getDbServerPort(), 2000, dbPassword);

    //Elo methods
    public void closeConnection() {
        jedisPool.close();
    }

    public void addPlayer(OfflinePlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset("playerElo:" + player.getUniqueId(), "username", player.getName());
            jedis.hset("playerElo:" + player.getUniqueId(), "elo", "100");
        }
    }

    public boolean playerExists(OfflinePlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists("playerElo:" + player.getUniqueId());
        }
    }

    public int getPlayerElo(OfflinePlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!playerExists(player)) {
                addPlayer(player);
            }
            return Integer.parseInt(jedis.hget("playerElo:" + player.getUniqueId(), "elo"));
        }
    }

    public void setPlayerElo(OfflinePlayer player, int elo) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!playerExists(player)) {
                addPlayer(player);
            }
            jedis.hset("playerElo:" + player.getUniqueId(), "elo", String.valueOf(elo));
        }
    }

    //Queue Methods
    public void queuePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.rpush("playerQueue", player.getUniqueId().toString());
        }
    }

    public ArrayList<OfflinePlayer> getQueue() {
        ArrayList<OfflinePlayer> queue = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> playerUUIDs = jedis.lrange("playerQueue", 0, -1);
            System.out.println("Retrieved player UUIDs from queue: " + playerUUIDs);
            for (String uuid : playerUUIDs) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                queue.add(player);
                System.out.println("Player added to queue list: " + player.getName());
            }
        }
        return queue;
    }

    public void clearQueue() {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "playerQueue";
            String keyType = jedis.type(key);

            if ("list".equals(keyType)) {
                jedis.del(key);
            } else {
                System.err.println("Expected 'playerQueue' to be a string but found: " + keyType);
            }
        }
    }

    public boolean playerInQueue(OfflinePlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.lrange("playerQueue", 0, -1).contains(player.getUniqueId().toString());
        }
    }

    public void unQueuePlayer(OfflinePlayer player) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lrem("playerQueue", 0, player.getUniqueId().toString());
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
        long players;
        try (Jedis jedis = jedisPool.getResource()) {
            players = jedis.llen("playerPoints");
        }

        return (int) players;
    }

    public void sendMessageToDB(String server, String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(server, message);
        }
    }
}