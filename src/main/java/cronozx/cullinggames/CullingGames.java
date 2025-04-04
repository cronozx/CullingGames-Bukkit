package cronozx.cullinggames;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import cronozx.cullinggames.commands.ForceStartCommand;
import cronozx.cullinggames.commands.JoinQueueCommand;
import cronozx.cullinggames.commands.ReloadCommand;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.events.PlayerDiesEvent;
import cronozx.cullinggames.util.ConfigManager;
import cronozx.cullinggames.util.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.logging.Logger;

public final class CullingGames extends JavaPlugin {

    private CoreDatabase database;
    private ConfigManager configManager;
    private ItemManager itemManager;
    private final Logger logger = Logger.getLogger(CullingGames.class.getName());
    private final Random random = new Random();
    private final Thread rediThread = new Thread(() -> database.onMessageFromRedis());
    private boolean airDropsEnabled;
    private CrazyEnvoys crazyEnvoys;

    @Override
    public void onEnable() {
        printStartupMsg();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        database = new CoreDatabase();

        rediThread.start();

        itemManager = new ItemManager();
        registerCommands();

        if (configManager.isBattleRoyalServer()) {
            registerEvents();
            Bukkit.getScheduler().runTask(this, itemManager);
        }

        airDropsEnabled = getServer().getPluginManager().getPlugin("CrazyEnvoys") != null;

        if (airDropsEnabled) {
            logger.info("Airdrops are enabled.");
            crazyEnvoys = CrazyEnvoys.get();
        } else {
            logger.warning("Airdrops are not enabled.");
        }
    }

    @Override
    public void onDisable() {
        crazyEnvoys.getLocationSettings().clearSpawnLocations();
        database.clearPlayersInGame();
        configManager.saveConfig();
        rediThread.interrupt();
        database.closeConnection();
    }

    private void registerCommands() {
        JoinQueueCommand queueCommand = new JoinQueueCommand(this);
        ReloadCommand reloadCommand = new ReloadCommand(this);
        ForceStartCommand forceStartCommand = new ForceStartCommand(this);

        getCommand("queue").setExecutor(queueCommand);
        getCommand("reload").setExecutor(reloadCommand);
        getCommand("forceStart").setExecutor(forceStartCommand);
    }

    private void registerEvents() {
        PlayerDiesEvent playerDiesEvent = new PlayerDiesEvent();

        Bukkit.getPluginManager().registerEvents(playerDiesEvent, this);
    }

    public static CullingGames getInstance() {
        return getPlugin(CullingGames.class);
    }

    public CoreDatabase getDatabase() {
        return database;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    private void printStartupMsg() {
        logger.info(
                "Culling Games plugin created by cronozx"
        );
    }

    public Random getRandom() {
        return random;
    }

    public boolean isAirDropsEnabled() {
        return airDropsEnabled;
    }

    public CrazyEnvoys getCrazyEnvoys() {
        return crazyEnvoys;
    }
}
