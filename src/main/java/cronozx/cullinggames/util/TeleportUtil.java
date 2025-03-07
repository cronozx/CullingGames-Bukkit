package cronozx.cullinggames.util;

import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.util.Random;

public class TeleportUtil {

    private static final CoreDatabase database = CullingGames.getInstance().getDatabase();
    private static final Random random = CullingGames.getInstance().getRandom();

    public static void randomTP(World world, Player player) {
        WorldBorder border = world.getWorldBorder();

        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();
        double size = border.getSize() / 2;

        double x = centerX + (random.nextDouble() * size * 2) - size;
        double z = centerZ + (random.nextDouble() * size * 2) - size;
        double y = world.getHighestBlockYAt((int) x, (int) z) + 1;

        Location randomLocation = new Location(world, x, y, z);
        player.teleport(randomLocation);
    }

    public static void teleportPlayerVelocity(String server, String playerName) {
        String message = "teleportTo:" + server + ":" + playerName;
        database.sendMessageToDB("cullinggames:jedis", message);
    }
}
