package cronozx.cullinggames;

import com.velocitypowered.api.proxy.server.ServerInfo;
import cronozx.cullinggames.commands.JoinQueueCommand;
import cronozx.cullinggames.commands.QueueTestCommand;
import cronozx.cullinggames.commands.ReloadCommand;
import cronozx.cullinggames.database.CoreDatabase;
import cronozx.cullinggames.events.PlayerDiesEvent;
import cronozx.cullinggames.events.PlayerLeaveEvent;
import cronozx.cullinggames.util.ConfigManager;
import cronozx.cullinggames.util.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.logging.Logger;

public final class CullingGames extends JavaPlugin implements @NotNull PluginMessageListener {

    private CoreDatabase database;
    private ConfigManager configManager;
    private ItemManager itemManager;
    private ServerInfo serverInfo;
    private final Logger logger = Logger.getLogger(CullingGames.class.getName());
    private final Random random = new Random();

    @Override
    public void onEnable() {
        printStartupMsg();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "cullinggames:jedis");

        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 25570);
        serverInfo = new ServerInfo("CullingGames", address);

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        database = new CoreDatabase();

        itemManager = new ItemManager();
        registerCommands();
        registerEvents();
        Bukkit.getScheduler().runTask(this, itemManager);

        //new ChatTask();
    }

    @Override
    public void onDisable() {
        configManager.saveConfig();
        database.clearQueue();
        database.closeConnection();
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, "cullinggames:jedis");
    }

    private void registerCommands() {
        JoinQueueCommand queueCommand = new JoinQueueCommand(this);
        ReloadCommand reloadCommand = new ReloadCommand(this);
        QueueTestCommand startCommand = new QueueTestCommand(this);

        getCommand("queue").setExecutor(queueCommand);
        getCommand("reload").setExecutor(reloadCommand);
        getCommand("starttest").setExecutor(startCommand);
    }

    private void registerEvents() {
        PlayerLeaveEvent playerLeaveEvent = new PlayerLeaveEvent();
        PlayerDiesEvent playerDiesEvent = new PlayerDiesEvent();

        Bukkit.getPluginManager().registerEvents(playerLeaveEvent, this);
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

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public Random getRandom() {
        return random;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, @NotNull byte[] bytes) {

    }
}
