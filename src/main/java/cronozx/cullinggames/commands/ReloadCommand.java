package cronozx.cullinggames.commands;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();

    public ReloadCommand(CullingGames plugin) {}

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        configManager.reloadConfig();
        if (Bukkit.getPlayer(commandSender.getName()) != null) {
            Bukkit.getPlayer(commandSender.getName()).sendMessage("Config Reloaded!");
        }
        return true;
    }
}
