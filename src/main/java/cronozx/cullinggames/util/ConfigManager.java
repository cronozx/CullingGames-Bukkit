package cronozx.cullinggames.util;

import cronozx.cullinggames.CullingGames;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private CullingGames plugin;
    private File configFile;
    private FileConfiguration config;

    public ConfigManager(CullingGames plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        config.options().copyDefaults(true);
        plugin.saveConfig();

        if (!configFile.exists()) {
            config = new YamlConfiguration();

            config.set("items", null);

            try {
                config.save(configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Set<String> getItems() {
        return config.getConfigurationSection("items").getKeys(false);
    }

    public double getItemChance(String itemName) {
        return config.getDouble("items" + itemName + ".chance", 100);
    }

    public int getItemAmount(String itemName) {
        return config.getInt("items" + itemName + ".amount", 1);
    }

    public Map<String, Double> getItemChances() {
        Map<String, Double> itemChances = new HashMap<>();
        for (String item : getItems()) {
            itemChances.put(item, getItemChance(item));
        }
        return itemChances;
    }

    public String getDbServerIp() {
        return config.getString("redis_db" + ".ip");
    }

    public int getDbServerPort() {
        return config.getInt("redis_db" + ".port");
    }

    public String getDbServerPass() { return config.getString("redis_db" + ".password"); }

    public String getServerName() { return config.getString("battle_royal_server_info" + ".server-name"); }

    public boolean isBattleRoyalServer() { return config.getBoolean("battle_royal_server_info" + ".is_battle_royal_server"); }

    public int getMinLobbySize() { return config.getInt("lobbies" + ".min-lobby-size"); }

    public int getMaxLobbySize() { return config.getInt("lobbies" + ".max-lobby-size"); }

    public String getWorldName() { return plugin.getConfig().getString("storm-info" + ".world-name"); }

    public double getInitialBorderSize() { return plugin.getConfig().getDouble("storm-info" + ".initial-border-size"); }

    public double getFinalBorderSize() {return plugin.getConfig().getDouble("storm-info" + ".final-border-size"); }

    public int getShrinkTime() { return plugin.getConfig().getInt("storm-info" + ".shrink-time"); }

    public int getZonePhases() { return plugin.getConfig().getInt("storm-info" + ".zone-phases"); }

    public int getPauseDuration() { return plugin.getConfig().getInt("storm-info" + ".pause-duration"); }

    public int getAirdropStartPhase() { return plugin.getConfig().getInt("airdrops" + ".airdrop-phase-start"); }

    public int getAirdropInterval() { return plugin.getConfig().getInt("airdrops" + ".airdrop-interval"); }

    public int getMaxAirdrops() { return plugin.getConfig().getInt("airdrops" + ".max-airdrops"); }

    public boolean getStopOnFinalPhase() { return plugin.getConfig().getBoolean("airdrops" + ".stop-on-final-phase"); }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("cant save config file");
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
}
