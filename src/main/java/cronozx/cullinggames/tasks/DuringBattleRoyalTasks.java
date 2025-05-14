package cronozx.cullinggames.tasks;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import cronozx.cullinggames.CullingGames;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.util.ConfigManager;
import cronozx.cullinggames.util.MiscUtil;
import cronozx.cullinggames.util.TeleportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuringBattleRoyalTasks implements Runnable {

    private static final CullingGames plugin = CullingGames.getInstance();
    private static final ConfigManager configManager = plugin.getConfigManager();
    private static final CoreDatabase database = plugin.getDatabase();

    //storm vars
    private final Location center;
    private double currentRadius;
    private final World world;
    private static boolean isRunning;
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private int currentPhase = 0;
    private final int totalPhases = configManager.getZonePhases();
    private final double initialRadius;
    private final double[] phaseRadii;
    private final int phaseInterval;
    private int phaseTimeLeft;
    private boolean isInPause;
    private final int PAUSE_DURATION = configManager.getPauseDuration();
    private final int AIRDROP_INTERVAL = configManager.getAirdropInterval();
    private final int airdropStartPhase = configManager.getAirdropStartPhase();
    private static CrazyEnvoys crazyEnvoys;

    public DuringBattleRoyalTasks() {
        this.world = Bukkit.getWorld(configManager.getWorldName());
        this.center = world.getSpawnLocation();
        this.initialRadius = configManager.getInitialBorderSize() / 2.0;
        this.currentRadius = initialRadius;
        this.phaseInterval = configManager.getShrinkTime() / totalPhases;
        this.phaseRadii = calculatePhaseRadii();
        this.isInPause = false;
        isRunning = true;
        if (plugin.isAirDropsEnabled()) {
            crazyEnvoys = plugin.getCrazyEnvoys();
        }
    }

    @Override
    public void run() {
        startStormAsync();

        if (plugin.isAirDropsEnabled()) {
            crazyEnvoys.getLocationSettings().clearSpawnLocations();
            startAirdrops();
        }

        checkForEndGame();
    }

    //storm methods
    private void startStormAsync() {
        checkZonePhaseAsync();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    cancel();
                    return;
                }
                showBorderParticles();
            }
        }.runTaskTimer(plugin, 0L, 10L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    cancel();
                    return;
                }
                //check if player is in zone
                checkPlayersAsync();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    cancel();
                    return;
                }
                showActionBarMessage();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    private void showBorderParticles() {
        double particleSpacing = Math.max(0.5, currentRadius / 100);
        int particleCount = (int) (2 * Math.PI * currentRadius / particleSpacing);
        int verticalRange = 16;
        double angleIncrement = 2 * Math.PI / particleCount;
        double centerX = center.getX();
        double centerZ = center.getZ();

        double[] cosValues = new double[particleCount];
        double[] sinValues = new double[particleCount];
        for (int i = 0; i < particleCount; i++) {
            double angle = angleIncrement * i;
            cosValues[i] = Math.cos(angle);
            sinValues[i] = Math.sin(angle);
        }

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.PURPLE, 1);

        for (Player player : world.getPlayers()) {
            Location playerLoc = player.getLocation();
            int playerY = playerLoc.getBlockY();
            int minY = Math.max(-64, playerY - verticalRange);
            int maxY = Math.min(320, playerY + verticalRange);
            double playerX = playerLoc.getX();
            double playerZ = playerLoc.getZ();

            if (Math.abs(playerX - centerX) > currentRadius + 48 ||
                Math.abs(playerZ - centerZ) > currentRadius + 48) {
                continue;
            }

            for (int i = 0; i < particleCount; i++) {
                double x = centerX + currentRadius * cosValues[i];
                double z = centerZ + currentRadius * sinValues[i];

                double dx = x - playerX;
                double dz = z - playerZ;
                if ((dx * dx + dz * dz) <= 2304) {
                    for (int y = minY; y < maxY; y += 2) {
                        world.spawnParticle(Particle.DUST, x, y, z, 1, dustOptions);
                    }
                }
            }
        }
    }

    private void checkPlayersAsync() {
        for (Player player : world.getPlayers()) {
            UUID uuid = player.getUniqueId();
            if (isOutsideZone(player.getLocation())) {
                long now = System.currentTimeMillis();
                if (!lastDamageTime.containsKey(uuid) || now - lastDamageTime.get(uuid) >= 1000) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.damage(1.5 * currentPhase);
                            player.showTitle(Title.title(
                                Component.text("WARNING").decorate(TextDecoration.BOLD).color(TextColor.color(255, 0, 0)),
                                Component.text("Return to Zone").color(TextColor.color(255, 255, 0))
                            ));
                            lastDamageTime.put(uuid, now);
                        }
                    });
                }
            }
        }
    }

    private double[] calculatePhaseRadii() {
        double[] radii = new double[totalPhases];
        double finalRadius = configManager.getFinalBorderSize() / 2.0;
        double shrinkPerPhase = (initialRadius - finalRadius) / totalPhases;

        for (int i = 0; i < totalPhases; i++) {
            radii[i] = initialRadius - (shrinkPerPhase * (i + 1));
        }

        return radii;
    }

    private void checkZonePhaseAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning || currentPhase >= totalPhases - 1) {
                    cancel();
                    return;
                }

                isInPause = true;
                phaseTimeLeft = PAUSE_DURATION;

                new BukkitRunnable() {
                    private int pauseTicks = 0;

                    @Override
                    public void run() {
                        if (!isRunning || pauseTicks >= PAUSE_DURATION) {
                            isInPause = false;
                            startShrinkPhase();
                            cancel();
                            return;
                        }
                        phaseTimeLeft = PAUSE_DURATION - pauseTicks;
                        pauseTicks++;
                    }
                }.runTaskTimer(plugin, 0L, 20L);
            }
        }.runTaskTimer(plugin, 0L, (phaseInterval + PAUSE_DURATION) * 20L);
    }

    private void startShrinkPhase() {
        double nextRadius = phaseRadii[currentPhase];
        double shrinkAmount = (currentRadius - nextRadius) / phaseInterval;

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!isRunning || ticks > phaseInterval) {
                    currentPhase++;
                    cancel();
                    return;
                }
                currentRadius -= shrinkAmount;
                phaseTimeLeft = Math.max(0, phaseInterval - ticks);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private boolean isOutsideZone(Location location) {
        double dx = center.getX() - location.getX();
        double dz = center.getZ() - location.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        return distance > currentRadius;
    }

    private void showActionBarMessage() {
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            String phaseText = currentPhase >= totalPhases ?
                    "Final Phase" :
                    "Phase: " + (currentPhase) + "/" + totalPhases;

            String timeText = isInPause ?
                    "Next Phase In: " + MiscUtil.formatTime(phaseTimeLeft) :
                    "Shrinking: " + MiscUtil.formatTime(phaseTimeLeft);

            Component message = Component.text()
                    .append(Component.text(phaseText).color(TextColor.color(180, 0, 255)))
                    .append(Component.text(" | "))
                    .append(Component.text(timeText).color(TextColor.color(255, 255, 0)))
                    .build();
            player.sendActionBar(message);
        }
    }

    //supply drops
    private void startAirdrops() {
        new BukkitRunnable() {
            private final BukkitRunnable dropSpawner = new BukkitRunnable() {
                @Override
                public void run() {
                    if (configManager.getStopOnFinalPhase() && currentPhase >= totalPhases) {
                        cancel();
                    } else {
                        spawnAirdrops();
                    }
                }
            };

            private boolean started = false;

            @Override
            public void run() {
                if (currentPhase >= airdropStartPhase && !started) {
                    dropSpawner.runTaskTimer(plugin, 0, AIRDROP_INTERVAL * 20L);
                    started = true;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20L);
    }

    private void spawnAirdrops() {
        for (int i = 0; i < configManager.getMaxAirdrops(); i++) {
           crazyEnvoys.getLocationSettings().addSpawnLocation(getRandomLocationInCircle().getBlock());
        }

        crazyEnvoys.getCrazyManager().startEnvoyEvent();
        crazyEnvoys.getLocationSettings().clearSpawnLocations();
    }

    private Location getRandomLocationInCircle() {
        double angle = Math.random() * 2 * Math.PI;
        double radius = Math.sqrt(Math.random()) * currentRadius;

        double x = center.getX() + radius * Math.cos(angle);
        double z = center.getZ() + radius * Math.sin(angle);

        Block block = world.getHighestBlockAt((int) x, (int) z);
        double y = block.getY() + 1;

        return new Location(world, x, y, z);
    }

    private void checkForEndGame() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    cancel();
                    return;
                }

                if (database.playersLeft() <= 1 && !database.getAllPlayersInGame().isEmpty()) {
                    var optionalPlayer = database.getAllPlayersInGame().getFirst().getPlayer();
                    if (optionalPlayer != null) {
                        optionalPlayer.showTitle(Title.title(
                                Component.text("You Win!!!!").decorate(TextDecoration.BOLD).color(TextColor.color(190, 0, 255)),
                                Component.empty()
                        ));

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        TeleportUtil.teleportPlayerVelocity("hub", optionalPlayer.getName());
                        DuringBattleRoyalTasks.stop();
                        this.cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public static void stop() {
        isRunning = false;

        crazyEnvoys.getLocationSettings().clearSpawnLocations();
        crazyEnvoys.getCrazyManager().removeAllEnvoys();

        database.clearPlayersInGame();
    }
}