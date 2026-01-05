package com.kerio.keriofly.economy;

import com.kerio.keriofly.KerioFly;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class EconomyManager {
    
    private final KerioFly plugin;
    private Economy economy = null;
    private boolean vaultEnabled = false;
    
    public EconomyManager(KerioFly plugin) {
        this.plugin = plugin;
        setupEconomy();
    }
    
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("未找到 Vault 插件，金錢兌換功能將無法使用");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("未找到經濟插件，金錢兌換功能將無法使用");
            return;
        }
        
        economy = rsp.getProvider();
        vaultEnabled = true;
        plugin.getLogger().info("已掛鉤經濟系統: " + economy.getName());
    }
    
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    public boolean isMoneyExchangeEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("economy.money-exchange.enabled", false);
    }
    
    public boolean isPointsExchangeEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("economy.points-exchange.enabled", true);
    }
    
    public double getCostPerHour() {
        return plugin.getConfigManager().getConfig().getDouble("economy.money-exchange.cost-per-hour", 1000.0);
    }
    
    public void setCostPerHour(double cost) {
        plugin.getConfigManager().getConfig().set("economy.money-exchange.cost-per-hour", cost);
        plugin.getConfigManager().saveConfigs();
    }
    
    public void setMoneyExchangeEnabled(boolean enabled) {
        plugin.getConfigManager().getConfig().set("economy.money-exchange.enabled", enabled);
        plugin.getConfigManager().saveConfigs();
    }
    
    public void setPointsExchangeEnabled(boolean enabled) {
        plugin.getConfigManager().getConfig().set("economy.points-exchange.enabled", enabled);
        plugin.getConfigManager().saveConfigs();
    }
    
    public double getBalance(Player player) {
        if (!vaultEnabled || economy == null) return 0;
        return economy.getBalance(player);
    }
    
    public boolean hasEnough(Player player, double amount) {
        if (!vaultEnabled || economy == null) return false;
        return economy.has(player, amount);
    }
    
    public boolean purchaseWithMoney(Player player, int hours) {
        if (!vaultEnabled || economy == null) return false;
        
        double cost = getCostPerHour() * hours;
        
        if (!hasEnough(player, cost)) {
            return false;
        }
        
        economy.withdrawPlayer(player, cost);
        plugin.getFlyManager().addFlyTime(player.getUniqueId(), hours * 3600L);
        return true;
    }
    
    public String formatMoney(double amount) {
        if (!vaultEnabled || economy == null) return String.valueOf(amount);
        return economy.format(amount);
    }
}