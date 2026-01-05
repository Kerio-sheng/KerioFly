package com.kerio.keriofly.config;

import com.kerio.keriofly.KerioFly;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    
    private final KerioFly plugin;
    private FileConfiguration config;
    private FileConfiguration ticketConfig;
    private FileConfiguration messagesConfig;
    private File ticketFile;
    private File messagesFile;
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public ConfigManager(KerioFly plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        // 加載主配置
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // 創建 ticket.yml
        ticketFile = new File(plugin.getDataFolder(), "ticket.yml");
        if (!ticketFile.exists()) {
            plugin.saveResource("ticket.yml", false);
        }
        ticketConfig = YamlConfiguration.loadConfiguration(ticketFile);
        
        // 創建 messages.yml
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        setDefaults();
    }
    
    private void setDefaults() {
        // config.yml 預設值
        config.addDefault("database.enabled", true);
        config.addDefault("database.host", "localhost");
        config.addDefault("database.port", 3306);
        config.addDefault("database.database", "minecraft");
        config.addDefault("database.username", "root");
        config.addDefault("database.password", "password");
        config.addDefault("database.table-prefix", "keriofly_");
        
        config.addDefault("settings.consume-time-when-disabled", false);
        config.addDefault("settings.consume-time-when-not-moving", false);
        config.addDefault("settings.consume-time-when-not-flying", false);
        config.addDefault("settings.consume-interval-seconds", 1);
        config.addDefault("settings.disable-fly-on-join", false);
        config.addDefault("settings.disable-fly-on-combat", true);
        config.addDefault("settings.disable-fly-on-damage", false);
        config.addDefault("settings.allow-fly-in-creative", false);
        
        config.addDefault("economy.money-exchange.enabled", false);
        config.addDefault("economy.money-exchange.cost-per-hour", 1000.0);
        config.addDefault("economy.points-exchange.enabled", true);
        
        // ticket.yml 預設值
        ticketConfig.addDefault("tickets.default.time", 3600);
        ticketConfig.addDefault("tickets.default.material", "PAPER");
        ticketConfig.addDefault("tickets.default.display-name", "&#00FFFF飛行票券 &7({time})");
        ticketConfig.addDefault("tickets.default.lore", Arrays.asList(
            "&7右鍵使用以獲得飛行時間",
            "&e飛行時間: &f{time}",
            "",
            "&#FFD700➤ 右鍵使用"
        ));
        ticketConfig.addDefault("tickets.default.enchanted", true);
        ticketConfig.addDefault("tickets.default.custom-model-data", 0);
        ticketConfig.addDefault("tickets.default.item-flags", Arrays.asList("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));
        
        ticketConfig.addDefault("gui.title", "&#00FFFF飛行票券管理");
        ticketConfig.addDefault("gui.size", 54);
        ticketConfig.addDefault("gui.filler.enabled", true);
        ticketConfig.addDefault("gui.filler.material", "GRAY_STAINED_GLASS_PANE");
        ticketConfig.addDefault("gui.filler.name", " ");
        
        config.options().copyDefaults(true);
        ticketConfig.options().copyDefaults(true);
        messagesConfig.options().copyDefaults(true);
        
        saveConfigs();
    }
    
    public void saveConfigs() {
        try {
            plugin.saveConfig();
            ticketConfig.save(ticketFile);
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("無法保存配置文件: " + e.getMessage());
        }
    }
    
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        ticketConfig = YamlConfiguration.loadConfiguration(ticketFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getTicketConfig() {
        return ticketConfig;
    }
    
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    public String getMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "&#00FFFF[KerioFly] &r");
        String message = messagesConfig.getString(key, key);
        return colorize(prefix + message);
    }
    
    public String getMessageWithoutPrefix(String key) {
        String message = messagesConfig.getString(key, key);
        return colorize(message);
    }
    
    /**
     * 支援 &、§ 和 Hex 色碼 (&#RRGGBB)
     */
    public String colorize(String text) {
        if (text == null) return "";
        
        // 處理 Hex 色碼 &#RRGGBB
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + hex.charAt(0) + "§" + hex.charAt(1) + 
                "§" + hex.charAt(2) + "§" + hex.charAt(3) + "§" + hex.charAt(4) + "§" + hex.charAt(5));
        }
        matcher.appendTail(buffer);
        
        // 處理 & 色碼
        return buffer.toString().replace("&", "§");
    }
}