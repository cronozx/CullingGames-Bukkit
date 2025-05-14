package cronozx.cullinggames.events;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.TeleportUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.logging.Logger;


public class PlayerDiesEvent implements Listener {

    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();
    private static final Logger logger = CullingGames.getInstance().getLogger();

    @EventHandler
    public static void onPlayerDeath(PlayerDeathEvent event) {
        Player target = event.getPlayer();
        Player attacker = event.getPlayer().getKiller();

        if (database.inCullingGames(target)) {
            if (attacker != null && database.inCullingGames(attacker)) {
                database.setPoints(attacker, database.getPlayerPoints(attacker) + 5);
            }

            database.removePlayerFromGame(target);
            TeleportUtil.teleportPlayerVelocity("hub", target.getName());
        }
    }
}
