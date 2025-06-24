package cronozx.cullinggames.commands;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class ReloadCommand implements CommandExecutor {

    private static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();
    private static final Logger logger = CullingGames.getInstance().getLogger();

    public ReloadCommand(CullingGames plugin) {}

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        Player player = Bukkit.getPlayer(commandSender.getName());
        configManager.reloadConfig();

        if (player != null) {
            player.sendMessage("§4§lCulling Games §8§l>> §r§7Config Reloaded!");
        } else {
            logger.info("§aCulling Games Reloaded!");
        }

        return true;
    }
}
