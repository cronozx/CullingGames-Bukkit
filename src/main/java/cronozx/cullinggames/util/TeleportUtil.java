package cronozx.cullinggames.util;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Random;

public class TeleportUtil {

    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();
    private static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();
    private static final Random random = CullingGames.getInstance().getRandom();

    public static void randomTP(World world, Player player) {
        double centerX = world.getSpawnLocation().getX();
        double centerZ = world.getSpawnLocation().getZ();
        double size = configManager.getInitialBorderSize() / 2;

        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = Math.sqrt(random.nextDouble()) * size;

        double x = centerX + distance * Math.cos(angle);
        double z = centerZ + distance * Math.sin(angle);
        double y = world.getHighestBlockYAt((int) x, (int) z) + 1;

        Location randomLocation = new Location(world, x, y, z);

        if (!randomLocation.getNearbyPlayers(20).isEmpty()) {
            randomTP(world, player);
            return;
        }

        player.teleport(randomLocation);
    }

    public static void teleportPlayerVelocity(String server, String playerName) {
        String message = "teleportTo:" + server + ":" + playerName;
        database.sendMessageToRedis("cullinggames:velocity", message);
    }
}
