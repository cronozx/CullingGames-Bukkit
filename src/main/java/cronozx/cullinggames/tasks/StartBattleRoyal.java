package cronozx.cullinggames.tasks;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.ConfigManager;
import cronozx.cullinggames.util.ItemManager;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import static cronozx.cullinggames.util.TeleportUtil.*;

public class StartBattleRoyal implements Runnable {

    private static final CullingGames plugin = CullingGames.getInstance();
    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();
    private static final ItemManager itemManager = CullingGames.getInstance().getItemManager();
    public static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();
    private static final Random random = CullingGames.getInstance().getRandom();
    private static final Logger logger = Logger.getLogger(StartBattleRoyal.class.getName());

    public StartBattleRoyal() {}

    @Override
    public void run() {
        logger.info("StartBattleRoyal task started.");
        if (plugin.getServerInfo().getAddress().getAddress().getHostAddress().equals(configManager.getServerIp()) && plugin.getServerInfo().getAddress().getPort() == configManager.getServerPort()) {
            logger.info("Server IP matches the configured IP.");
            database.initPointsPlayers(database.getQueue());
            logger.info("Initialized points for players in the queue.");

            teleportPlayers();
        } else {
            logger.warning("Server IP does not match the configured IP." + "IP: " + plugin.getServerInfo().getAddress().getAddress().getHostAddress() + " Port: " + plugin.getServerInfo().getAddress().getPort());
        }
    }

    private ArrayList<ArrayList<OfflinePlayer>> createLobbies() {
        logger.info("Creating lobbies.");
        ArrayList<ArrayList<OfflinePlayer>> lobbies = new ArrayList<>();
        ArrayList<OfflinePlayer> queue = database.getQueue();
        logger.info("Queue: " + database.getQueue());

        queue.sort((p1, p2) -> Integer.compare(database.getPlayerElo(p2), database.getPlayerElo(p1)));
        logger.info("Sorted players by Elo rating.");

        int numberOfLobbies = (int) Math.ceil(queue.size() / 20.0);
        logger.info("Number of lobbies: " + numberOfLobbies);
        for (int i = 0; i < numberOfLobbies; i++) {
            lobbies.add(new ArrayList<>());
        }

        for (int i = 0; i < queue.size(); i++) {
            lobbies.get(i % numberOfLobbies).add(queue.get(i));
        }
        logger.info("Distributed players into lobbies.");
        logger.info("Lobbies: " + lobbies);

        return lobbies;
    }

    private void teleportPlayers() {
        logger.info("Teleporting players.");
        ArrayList<ArrayList<OfflinePlayer>> lobbies = createLobbies();
        for (ArrayList<OfflinePlayer> lobby : lobbies) {
            World world = createWorld(lobby.getFirst().getUniqueId());
            generateChests();
            for (OfflinePlayer offlinePlayer : lobby) {
                teleportPlayerVelocity(configManager.getServerName(), offlinePlayer.getName());
               new BukkitRunnable() {
                    @Override
                    public void run() {
                        Player player = offlinePlayer.getPlayer();
                        if (player != null) {
                            randomTP(world, player);
                        } else {
                            logger.warning("Player " + offlinePlayer.getName() + " is not online.");
                            lobby.remove(offlinePlayer);
                        }
                    }
                }.runTaskLater(plugin, 40L);
            }
        }
    }

    public World createWorld(UUID lobbyUUID) {
        return new WorldCreator("lobby_" + lobbyUUID).copy(WorldCreator.name("lobby_map")).createWorld();
    }

    private ArrayList<ItemStack> getChestLoot() {
        logger.info("Generating chest loot.");
        ArrayList<ItemStack> items = new ArrayList<>();

        for (int i = 0; i < random.nextInt(10); i++) {
            items.add(itemManager.getRandomItem());
        }
        logger.info("Generated " + items.size() + " items for chest loot.");

        return items;
    }

    private void generateChests() {
        logger.info("Generating chests.");
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof Chest chest) {
                        ItemStack[] items = getChestLoot().toArray(new ItemStack[0]);
                        chest.getInventory().setContents(items);
                        logger.info("Generated loot for chest at: " + blockState.getLocation());
                    }
                }
            }
        }
    }
}