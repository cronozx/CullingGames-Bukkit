package cronozx.cullinggames.tasks;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.ConfigManager;
import cronozx.cullinggames.util.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static cronozx.cullinggames.util.TeleportUtil.*;

public class StartBattleRoyal implements Runnable {

    private static final CullingGames plugin = CullingGames.getInstance();
    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();
    private static final ItemManager itemManager = CullingGames.getInstance().getItemManager();
    private static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();
    private static final Random random = CullingGames.getInstance().getRandom();
    private static final Logger logger = Logger.getLogger(StartBattleRoyal.class.getName());
    private static final ArrayList<OfflinePlayer> queue = database.getQueue();

    public StartBattleRoyal() {}

    @Override
    public void run() {
        logger.info("StartBattleRoyal task started.");
        database.initPointsPlayers(queue);
        logger.info("Initialized points for players in the queue.");
        database.clearQueue();

        generateChests();
        teleportPlayersAndStart();
    }

    private void teleportPlayersAndStart() {
        logger.info("Starting Velocity teleport.");

        // First teleport players through Velocity
        for (OfflinePlayer offlinePlayer : queue) {
            teleportPlayerVelocity(configManager.getServerName(), offlinePlayer.getName());
        }

        // Wait for players to connect and handle the rest
        new BukkitRunnable() {
            private int attempts = 0;
            private final int MAX_ATTEMPTS = 200;

            @Override
            public void run() {
                ArrayList<OfflinePlayer> players = database.getAllPlayersInGame();
                boolean allOnline = players.stream()
                        .allMatch(player -> player.getPlayer() != null);

                if (allOnline && players.size() >= configManager.getMinLobbySize()) {
                    cancel();
                    randomTeleportAndStart(players);
                } else if (attempts++ >= MAX_ATTEMPTS) {
                    cancel();
                    handleDisconnectedPlayers(players);
                }
            }
        }.runTaskTimer(plugin, 40L, 1L);
    }

    private void generateChests() {
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof Chest chest) {
                        ItemStack[] items = getChestLoot().toArray(new ItemStack[0]);
                        chest.getInventory().setContents(items);
                    }
                }
            }
        }
    }

    private ArrayList<ItemStack> getChestLoot() {
        ArrayList<ItemStack> items = new ArrayList<>();

        for (int i = 0; i < random.nextInt(10); i++) {
            items.add(itemManager.getRandomItem());
        }

        return items;
    }

    private void handleDisconnectedPlayers(ArrayList<OfflinePlayer> players) {
        List<OfflinePlayer> disconnectedPlayers = players.stream().filter(player -> player.getPlayer() == null).toList();

        //disconnect non connected players
        for (OfflinePlayer offlinePlayer: disconnectedPlayers) {
            database.removePlayerFromGame(offlinePlayer);
            database.sendMessageToRedis("cullinggames:velocity", "timeout:" + offlinePlayer.getUniqueId());
        }

        if (database.getAllPlayersInGame().size() >= configManager.getMinLobbySize()) {
            //start logic
            randomTeleportAndStart(players);
        } else {
            //send players to hub and send them a message
            for (Player player: Bukkit.getServer().getOnlinePlayers()) {
                database.sendMessageToRedis("cullinggames:velocity", "gameCanceled:" + player.getUniqueId());
            }
        }
    }

    private void randomTeleportAndStart(ArrayList<OfflinePlayer> players) {
        // Random teleport connected players
        for (OfflinePlayer player : players) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer != null) {
                randomTP(onlinePlayer.getWorld(), onlinePlayer);
            }
        }

        startCountdown();

        Bukkit.getScheduler().runTask(plugin, new DuringBattleRoyal());
    }

    private void startCountdown() {
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                for (Player player: Bukkit.getServer().getOnlinePlayers()) {
                    if (countdown == 0) {
                        player.showTitle(Title.title(
                                Component.text("GO!").decorate(TextDecoration.BOLD).color(TextColor.color(255, 0, 0)),
                                Component.empty()
                        ));
                    } else {
                        player.showTitle(Title.title(
                                Component.text("Game Starts In...").decorate(TextDecoration.BOLD).color(TextColor.color(255, 0, 0)),
                                Component.text(countdown).color(TextColor.color(255, 0, 0))
                        ));
                    }
                }

                if (countdown == 0) {
                    cancel();
                }

                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }
}