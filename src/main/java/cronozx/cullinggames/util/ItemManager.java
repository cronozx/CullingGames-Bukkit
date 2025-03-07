package cronozx.cullinggames.util;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import cronozx.cullinggames.CullingGames;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class ItemManager implements Runnable {

    private CullingGames plugin = CullingGames.getInstance();
    private static final ConfigManager configManager = CullingGames.getInstance().getConfigManager();
    private ArrayList<ItemStack> items = new ArrayList<>();

    @Override
    public void run() {
        FileConfiguration config = plugin.getConfig();
        for (String itemID : config.getConfigurationSection("items").getKeys(false)) {
            int amount = config.getInt("items." + itemID + ".amount");
            ItemStack item = checkValidItem(itemID, amount);
            items.add(item);
        }
    }

    public ItemStack checkValidItem(String itemID, int amount) {
        if (NexoItems.exists(itemID)) {
            return checkValidNexoItem(itemID, amount);
        } else if (Material.getMaterial(itemID.toUpperCase()) == null) {
            return ItemStack.empty();
        }

        return new ItemStack(Material.getMaterial(itemID.toUpperCase()), amount);
    }

    public ItemStack checkValidNexoItem(String itemID, int amount) {
        ItemBuilder itemBuilder = NexoItems.itemFromId(itemID);
        ItemStack itemStack = itemBuilder.build();
        itemStack.setAmount(amount);

        return itemStack;
    }

    public ArrayList<ItemStack> getItems() {
        return items;
    }

    public ItemStack getRandomItem() {
        Random random = new Random();
        double totalChance = configManager.getItemChances().values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalChance;
        double currentChance = 0.0;

        for (Map.Entry<String, Double> entry : configManager.getItemChances().entrySet()) {
            currentChance += entry.getValue();
            if (currentChance > randomValue) {
                return checkValidItem(entry.getKey(), configManager.getItemAmount(entry.getKey()));
            }
        }

        return null;
    }
}