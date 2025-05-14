package cronozx.cullinggames.commands;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.tasks.DuringBattleRoyalTasks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ForceStopCommand implements CommandExecutor {

    public static final CoreDatabase database = CullingGames.getInstance().getDatabase();

    public ForceStopCommand(CullingGames plugin) {}

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        DuringBattleRoyalTasks.stop();
        return true;
    }
}
