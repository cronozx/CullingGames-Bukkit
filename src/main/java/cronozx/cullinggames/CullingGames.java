package cronozx.cullinggames;

import com.badbones69.crazyenvoys.CrazyEnvoys;
import com.willfp.ecobits.EcoBitsPlugin;
import com.willfp.ecobits.currencies.Currencies;
import com.willfp.ecopets.api.EcoPetsAPI;
import cronozx.cullinggames.commands.ForceStartCommand;
import cronozx.cullinggames.commands.ForceStopCommand;
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
    private boolean ecoBitsEnabled;
    private boolean pointsEnabled;
    private CrazyEnvoys crazyEnvoys;
    private EcoPetsAPI ecoPets;
    private boolean ecoPetsEnabled;

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

        registerCommands();

        if (configManager.isBattleRoyalServer()) {
            database.saveMaxPlayers();
            database.saveMinPlayers();

            itemManager = new ItemManager();

            airDropsEnabled = getServer().getPluginManager().getPlugin("CrazyEnvoys") != null;
            if (airDropsEnabled) {
                crazyEnvoys = CrazyEnvoys.get();
                logger.info("Airdrops are enabled.");
            } else {
                logger.warning("Airdrops are not enabled.");
            }

            ecoBitsEnabled = getServer().getPluginManager().getPlugin("EcoBits") != null;
            if (ecoBitsEnabled) {
                logger.info("EcoBits enabled");
                Currencies.update(EcoBitsPlugin.getInstance());
                if (Currencies.getByID("points") == null) {
                    logger.severe("Add the points currency in the EcoBits config.");
                    pointsEnabled = false;
                } else {
                    pointsEnabled = true;
                }
            } else {
                logger.severe("EcoBits not enabled, shop function not available.");
            }

            ecoPetsEnabled = getServer().getPluginManager().getPlugin("EcoPets") != null;
            if (ecoPetsEnabled) {
                logger.info("EcoPets enabled");
                ecoPets = EcoPetsAPI.getInstance();
            }

            registerEvents();
            Bukkit.getScheduler().runTask(this, itemManager);
        }
    }

    @Override
    public void onDisable() {
        if (configManager.isBattleRoyalServer()) {
            crazyEnvoys.getLocationSettings().clearSpawnLocations();
            if (configManager.isBattleRoyalServer()) {
                database.clearPlayersInGame();
            }
        }


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
        if (configManager.isBattleRoyalServer()) {
            ForceStopCommand forceStopCommand = new ForceStopCommand(this);
            getCommand("forceStop").setExecutor(forceStopCommand);
        }
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerDiesEvent(), this);
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

    public Random getRandom() {
        return random;
    }

    public boolean isAirDropsEnabled() {
        return airDropsEnabled;
    }

    public CrazyEnvoys getCrazyEnvoys() {
        return crazyEnvoys;
    }

    public boolean getPointsEnabled() {
        return pointsEnabled;
    }

    public EcoPetsAPI getEcoPets() { return ecoPets; }

    private void printStartupMsg() {
        logger.info(
                "\n" +
                        "░█████╗░██████╗░░█████╗░███╗░░██╗░█████╗░███████╗██╗░░██╗\n" +
                        "██╔══██╗██╔══██╗██╔══██╗████╗░██║██╔══██╗╚════██║╚██╗██╔╝\n" +
                        "██║░░╚═╝██████╔╝██║░░██║██╔██╗██║██║░░██║░░███╔═╝░╚███╔╝░\n" +
                        "██║░░██╗██╔══██╗██║░░██║██║╚████║██║░░██║██╔══╝░░░██╔██╗░\n" +
                        "╚█████╔╝██║░░██║╚█████╔╝██║░╚███║╚█████╔╝███████╗██╔╝╚██╗\n" +
                        "░╚════╝░╚═╝░░╚═╝░╚════╝░╚═╝░░╚══╝░╚════╝░╚══════╝╚═╝░░╚═╝"
        );
    }
}
