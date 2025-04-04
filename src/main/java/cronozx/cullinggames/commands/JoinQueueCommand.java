package cronozx.cullinggames.commands;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class JoinQueueCommand implements CommandExecutor {

    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();
    private static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();

    public JoinQueueCommand(CullingGames plugin) {}

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(commandSender.getName());
        if (offlinePlayer.getPlayer() != null && !database.getQueue().contains(offlinePlayer) && !(database.getQueue().size() >= configManager.getMaxLobbySize())) {
            database.sendMessageToRedis("cullinggames:velocity", "queue:" + offlinePlayer.getUniqueId());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            offlinePlayer.getPlayer().sendMessage(Component.newline().content("§4§lCulling Games §r§8§l>> §r§7You are now queued. " + "§8(§r" + database.getQueue().size() + "§8/§r" + configManager.getMaxLobbySize() + "§8)§r"));
        } else {
            if (offlinePlayer.getPlayer() != null && database.getQueue().contains(offlinePlayer)) {
                offlinePlayer.getPlayer().sendMessage(Component.newline().content("§4§lCulling Games §r§8§l>> §r§7You are already queued."));
            } else if (offlinePlayer.getPlayer() != null && database.getQueue().size() >= configManager.getMaxLobbySize()) {
                offlinePlayer.getPlayer().sendMessage(Component.newline().content("§4§lCulling Games §r§8§l>> §r§7The queue is full."));
            }
        }

        return true;
    }
}
