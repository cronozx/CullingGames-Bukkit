package cronozx.cullinggames.commands;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.tasks.StartBattleRoyal;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class QueueTestCommand implements CommandExecutor {

    private static final CullingGames plugin = CullingGames.getInstance();
    private final StartBattleRoyal task = new StartBattleRoyal();

    public QueueTestCommand(CullingGames plugin) {}

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Bukkit.getScheduler().runTask(plugin, task);
        return true;
    }
}
