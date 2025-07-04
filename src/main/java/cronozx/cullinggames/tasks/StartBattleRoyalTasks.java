package cronozx.cullinggames.tasks;

import com.willfp.ecopets.pets.Pets;
import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.ItemManager;
import cronozx.cullinggames.util.TeleportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static cronozx.cullinggames.util.TeleportUtil.*;

public class StartBattleRoyalTasks implements Runnable {

    private static final CullingGames plugin = CullingGames.getInstance();
    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();
    private static final ItemManager itemManager = CullingGames.getInstance().getItemManager();
    private static final Random random = CullingGames.getInstance().getRandom();
    private static final Logger logger = Logger.getLogger(StartBattleRoyalTasks.class.getName());
    private static ArrayList<OfflinePlayer> queue;

    public StartBattleRoyalTasks() {}

    @Override
    public void run() {
        queue = database.getQueue();

        if (queue.size() >= database.getMinPlayers()) {
            generateChests();
            teleportPlayersAndStart();

            database.initPointsPlayers(queue);

            database.clearQueue();
        } else {
            database.sendMessageToRedis("cullinggames:velocity", "gameCanceledEarly");
            database.clearQueue();
        }
    }

    private void teleportPlayersAndStart() {
        logger.info("Starting Velocity teleport.");

        List<String> playerNames = queue.stream().map(OfflinePlayer::getName).toList();

        TeleportUtil.teleportPlayersVelocity("CullingGames", playerNames);

        BukkitRunnable waitingTask = new BukkitRunnable() {
            @Override
            public void run() {
                AttributeModifier noJump = new AttributeModifier(new NamespacedKey(plugin, "cul"), -1, Operation.ADD_NUMBER);

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.isOnline()) {
                        player.addPotionEffect(PotionEffectType.SLOWNESS.createEffect(40, 10000));
                        if (!player.getAttribute(Attribute.JUMP_STRENGTH).getModifiers().contains(noJump)) {
                            player.getAttribute(Attribute.JUMP_STRENGTH).addTransientModifier(noJump);
                        }
                        player.showTitle(Title.title(Component.text("Waiting For Players...").decorate(TextDecoration.BOLD).color(TextColor.color(255, 0, 0)), Component.empty()));
                    }
                }
            }
        };

        waitingTask.runTaskTimer(plugin, 0, 20);

        new BukkitRunnable() {
            private int attempts = 0;
            private final int MAX_ATTEMPTS = 200;
            
            @Override
            public void run() {
                ArrayList<OfflinePlayer> players = database.getAllPlayersInGame();
                boolean allOnline = players.stream()
                        .allMatch(player -> player.getPlayer() != null);

                if (allOnline && players.size() >= database.getMinPlayers()) {
                    cancel();
                    randomTeleportAndStart();
                    waitingTask.cancel();
                } else if (attempts++ >= MAX_ATTEMPTS) {
                    cancel();
                    handleDisconnectedPlayers(players);
                    waitingTask.cancel();
                }
            }
        }.runTaskTimer(plugin, 200L, 1L);
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

        if (database.getAllPlayersInGame().size() >= database.getMinPlayers()) {
            //start logic
            randomTeleportAndStart();
        } else {
            //send players to hub and send them a message
            database.sendMessageToRedis("cullinggames:velocity", "gameCanceled");
            DuringBattleRoyalTasks.stop();
        }
    }

    private void randomTeleportAndStart() {
        // Random teleport connected players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            randomTP(player.getWorld(), player);
        }

        givePlayersKogane();
        startCountdown();

        Bukkit.getScheduler().runTask(plugin, new DuringBattleRoyalTasks());
    }

    private void startCountdown() {
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                for (Player player: Bukkit.getServer().getOnlinePlayers()) {
                    player.addPotionEffect(PotionEffectType.SLOWNESS.createEffect(40, 10000));
                    player.addPotionEffect(PotionEffectType.JUMP_BOOST.createEffect(40, -100));
                    if (countdown == 0) {
                        player.showTitle(Title.title(
                            Component.text("GO!").decorate(TextDecoration.BOLD).color(TextColor.color(255, 0, 0)),
                            Component.empty()
                        ));

                        if (player.getAttribute(Attribute.JUMP_STRENGTH).getValue() != -1) {
                            player.getAttribute(Attribute.JUMP_STRENGTH).removeModifier(new NamespacedKey(plugin, "cul"));
                        }
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

    private void givePlayersKogane() {
        List<Player> players = database.getAllPlayersInGame().stream().map(OfflinePlayer::getPlayer).toList();

        for (Player player: players) {
            plugin.getEcoPets().setActivePet(player, Pets.getByID("kogane"));
        }
    }
}