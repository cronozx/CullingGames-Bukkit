package cronozx.cullinggames.commands;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.ConfigManager;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class JoinQueueCommand implements CommandExecutor {

    private final CullingGames plugin;
    private final CoreDatabase database;
    private final ConfigManager configManager;

    public JoinQueueCommand(CullingGames plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(commandSender.getName());

        if (offlinePlayer.getPlayer() != null) {
            Player player = offlinePlayer.getPlayer();
            ArrayList<OfflinePlayer> queue = database.getQueue();

            if (!queue.contains(offlinePlayer) && !(queue.size() >= database.getMaxPlayers())) {
                Bukkit.getScheduler().runTaskAsynchronously(CullingGames.getInstance(), () -> {
                    database.sendMessageToRedis("cullinggames:velocity", "queue:" + player.getUniqueId());
                });
            } else {
                if (queue.contains(offlinePlayer)) {
                    player.sendMessage(Component.newline().content("§4§lCulling Games §r§8§l>> §r§7You are already queued."));
                } else if (queue.size() >= database.getMaxPlayers()) {
                    player.sendMessage(Component.newline().content("§4§lCulling Games §r§8§l>> §r§7The queue is full."));
                }
            }
        }

        return true;
    }
}
