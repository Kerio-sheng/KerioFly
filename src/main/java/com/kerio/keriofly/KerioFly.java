package com.kerio.keriofly;

import com.kerio.keriofly.commands.FlyCommand;
import com.kerio.keriofly.config.ConfigManager;
import com.kerio.keriofly.database.DatabaseManager;
import com.kerio.keriofly.economy.EconomyManager;
import com.kerio.keriofly.listeners.PlayerListener;
import com.kerio.keriofly.managers.FlyManager;
import com.kerio.keriofly.managers.PointsManager;
import com.kerio.keriofly.managers.TicketManager;
import com.kerio.keriofly.placeholders.FlyPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KerioFly extends JavaPlugin {
    
    private static KerioFly instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private FlyManager flyManager;
    private TicketManager ticketManager;
    private EconomyManager economyManager;
    private PointsManager pointsManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化配置
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // 初始化資料庫
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();
        
        // 初始化管理器
        flyManager = new FlyManager(this);
        ticketManager = new TicketManager(this);
        pointsManager = new PointsManager(this);
        economyManager = new EconomyManager(this);
        
        // 註冊指令
        getCommand("kfly").setExecutor(new FlyCommand(this));
        
        // 註冊監聽器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // 註冊 PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FlyPlaceholder(this).register();
            getLogger().info("已掛鉤 PlaceholderAPI");
        }
        
        // 啟動飛行時間消耗任務
        flyManager.startTimeConsumptionTask();
        
        getLogger().info("KerioFly 插件已啟動！");
        getLogger().info("作者: Kerio | 版本: 1.0.0");
    }
    
    @Override
    public void onDisable() {
        // 保存所有玩家數據
        if (flyManager != null) {
            flyManager.saveAllPlayers();
            flyManager.stopTimeConsumptionTask();
        }
        
        if (pointsManager != null) {
            pointsManager.saveAllPoints();
        }
        
        // 關閉資料庫連接
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        
        getLogger().info("KerioFly 插件已關閉！");
    }
    
    public static KerioFly getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public FlyManager getFlyManager() {
        return flyManager;
    }
    
    public TicketManager getTicketManager() {
        return ticketManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public PointsManager getPointsManager() {
        return pointsManager;
    }
}