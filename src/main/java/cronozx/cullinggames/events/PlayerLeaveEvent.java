package cronozx.cullinggames.events;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerLeaveEvent implements Listener {

    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();

    @EventHandler
    public static void onPlayerLeave(PlayerConnectionCloseEvent event) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(event.getPlayerName());

        if (database.playerInQueue(player)) {
            database.unQueuePlayer(player);
        }

        if (database.inCullingGames(player)) {
            database.removePlayerFromGame(player);
        }
    }
}
