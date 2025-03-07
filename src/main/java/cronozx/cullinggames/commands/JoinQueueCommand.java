package cronozx.cullinggames.commands;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class JoinQueueCommand implements CommandExecutor {
    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();

    public JoinQueueCommand(CullingGames plugin) {}

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (Bukkit.getPlayer(commandSender.getName()) != null) {
            database.queuePlayer(Bukkit.getPlayer(commandSender.getName()));
            System.out.println("Couldn't find player to queue");
        }

        System.out.println(database.getQueue().toString());

        return true;
    }
}
