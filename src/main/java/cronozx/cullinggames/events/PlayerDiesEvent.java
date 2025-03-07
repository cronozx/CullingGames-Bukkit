package cronozx.cullinggames.events;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.MiscUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.io.File;


public class PlayerDiesEvent implements Listener {

    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();

    @EventHandler
    public static void onPlayerDeath(PlayerDeathEvent event) {
        Player target = event.getPlayer();
        Player attacker = event.getPlayer().getKiller();

        if (database.inCullingGames(target) && database.inCullingGames(attacker)) {
            database.setPoints(attacker, database.getPlayerPoints(attacker) + 5);

            database.removePlayerFromGame(target);

        }

        if (database.playersLeft() <= 1) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().startsWith("lobby_")) {
                    File worldFile = new File(Bukkit.getWorldContainer(), world.getName());
                    MiscUtil.deleteDirectory(worldFile.toPath());
                }
            }
        }
    }
}
