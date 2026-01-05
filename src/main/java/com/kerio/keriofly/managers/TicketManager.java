package com.kerio.keriofly.managers;

import com.kerio.keriofly.KerioFly;
import com.kerio.keriofly.utils.TicketUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TicketManager {
    
    private final KerioFly plugin;
    private final Map<String, TicketData> tickets = new HashMap<>();
    
    public TicketManager(KerioFly plugin) {
        this.plugin = plugin;
        loadTickets();
    }
    
    public void loadTickets() {
        tickets.clear();
        FileConfiguration config = plugin.getConfigManager().getTicketConfig();
        ConfigurationSection ticketsSection = config.getConfigurationSection("tickets");
        
        if (ticketsSection != null) {
            for (String key : ticketsSection.getKeys(false)) {
                try {
                    TicketData data = new TicketData(
                        key,
                        config.getLong("tickets." + key + ".time"),
                        config.getString("tickets." + key + ".material", "PAPER"),
                        config.getString("tickets." + key + ".display-name", "飛行票券"),
                        config.getStringList("tickets." + key + ".lore"),
                        config.getBoolean("tickets." + key + ".enchanted", true),
                        config.getInt("tickets." + key + ".custom-model-data", 0),
                        config.getStringList("tickets." + key + ".item-flags")
                    );
                    tickets.put(key, data);
                } catch (Exception e) {
                    plugin.getLogger().warning("無法加載票券: " + key);
                }
            }
        }
        
        plugin.getLogger().info("已加載 " + tickets.size() + " 個票券");
    }
    
    public boolean createTicket(String name, long time) {
        if (tickets.containsKey(name)) {
            return false;
        }
        
        FileConfiguration config = plugin.getConfigManager().getTicketConfig();
        config.set("tickets." + name + ".time", time);
        config.set("tickets." + name + ".material", "PAPER");
        config.set("tickets." + name + ".display-name", "&#00FFFF飛行票券 &7({time})");
        config.set("tickets." + name + ".lore", Arrays.asList(
            "&7右鍵使用以獲得飛行時間",
            "&e飛行時間: &f{time}",
            "",
            "&#FFD700➤ 右鍵使用"
        ));
        config.set("tickets." + name + ".enchanted", true);
        config.set("tickets." + name + ".custom-model-data", 0);
        config.set("tickets." + name + ".item-flags", Arrays.asList("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));
        
        plugin.getConfigManager().saveConfigs();
        loadTickets();
        return true;
    }
    
    public boolean removeTicket(String name) {
        if (!tickets.containsKey(name)) {
            return false;
        }
        
        FileConfiguration config = plugin.getConfigManager().getTicketConfig();
        config.set("tickets." + name, null);
        plugin.getConfigManager().saveConfigs();
        tickets.remove(name);
        return true;
    }
    
    public boolean hasTicket(String name) {
        return tickets.containsKey(name);
    }
    
    public TicketData getTicket(String name) {
        return tickets.get(name);
    }
    
    public Set<String> getTicketNames() {
        return tickets.keySet();
    }
    
    public ItemStack createTicketItem(String name) {
        TicketData data = tickets.get(name);
        if (data == null) return null;
        return TicketUtils.createTicket(data);
    }
    
    public void giveTicket(Player player, String name, int amount) {
        ItemStack ticket = createTicketItem(name);
        if (ticket == null) return;
        
        ticket.setAmount(amount);
        player.getInventory().addItem(ticket);
    }
    
    public void openTicketGUI(Player player) {
        FileConfiguration config = plugin.getConfigManager().getTicketConfig();
        String title = plugin.getConfigManager().colorize(config.getString("gui.title", "飛行票券管理"));
        int size = config.getInt("gui.size", 54);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        // 添加填充物
        if (config.getBoolean("gui.filler.enabled", true)) {
            String fillerMaterial = config.getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE");
            String fillerName = plugin.getConfigManager().colorize(config.getString("gui.filler.name", " "));
            
            Material material = Material.matchMaterial(fillerMaterial);
            if (material != null) {
                ItemStack filler = new ItemStack(material);
                org.bukkit.inventory.meta.ItemMeta meta = filler.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(fillerName);
                    filler.setItemMeta(meta);
                }
                
                for (int i = 0; i < size; i++) {
                    inv.setItem(i, filler);
                }
            }
        }
        
        // 添加票券
        int slot = 10;
        for (String name : tickets.keySet()) {
            ItemStack ticket = createTicketItem(name);
            if (ticket != null && slot < size) {
                inv.setItem(slot, ticket);
                slot++;
                if (slot % 9 >= 7) {
                    slot += 3;
                }
            }
        }
        
        player.openInventory(inv);
    }
    
    public static class TicketData {
        private final String name;
        private long time;
        private String material;
        private String displayName;
        private List<String> lore;
        private boolean enchanted;
        private int customModelData;
        private List<String> itemFlags;
        
        public TicketData(String name, long time, String material, String displayName, 
                         List<String> lore, boolean enchanted, int customModelData, List<String> itemFlags) {
            this.name = name;
            this.time = time;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.enchanted = enchanted;
            this.customModelData = customModelData;
            this.itemFlags = itemFlags;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public long getTime() { return time; }
        public void setTime(long time) { this.time = time; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public List<String> getLore() { return lore; }
        public void setLore(List<String> lore) { this.lore = lore; }
        public boolean isEnchanted() { return enchanted; }
        public void setEnchanted(boolean enchanted) { this.enchanted = enchanted; }
        public int getCustomModelData() { return customModelData; }
        public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
        public List<String> getItemFlags() { return itemFlags; }
        public void setItemFlags(List<String> itemFlags) { this.itemFlags = itemFlags; }
    }
}