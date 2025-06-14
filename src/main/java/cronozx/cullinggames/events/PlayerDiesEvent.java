package cronozx.cullinggames.events;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.TeleportUtil;

import java.math.BigDecimal;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.willfp.ecobits.currencies.Currencies;
import com.willfp.ecobits.currencies.Currency;
import com.willfp.ecobits.currencies.CurrencyUtils;


public class PlayerDiesEvent implements Listener {

    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();

    @EventHandler
    public static void onPlayerDeath(PlayerDeathEvent event) {
        Player target = event.getPlayer();
        Player attacker = event.getPlayer().getKiller();
        Currency points = Currencies.getByID("Points");

        if (database.inCullingGames(target)) {
            if (attacker != null && database.inCullingGames(attacker)) {
                CurrencyUtils.adjustBalance(attacker, points, BigDecimal.valueOf(5));
            }

            database.removePlayerFromGame(target);
            TeleportUtil.teleportPlayerVelocity("hub", target.getName());
        }
    }
}
