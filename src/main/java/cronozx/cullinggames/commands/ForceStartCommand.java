package cronozx.cullinggames.commands;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ForceStartCommand implements CommandExecutor {

    public static final CoreDatabase database = CullingGames.getInstance().getDatabase();

    public ForceStartCommand(CullingGames plugin) {}

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        database.sendMessageToRedis("cullinggames:velocity", "forceStart");

        return true;
    }
}
