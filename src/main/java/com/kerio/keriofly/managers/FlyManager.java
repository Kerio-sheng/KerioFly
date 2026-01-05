package com.kerio.keriofly.managers;

import com.kerio.keriofly.KerioFly;
import com.kerio.keriofly.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlyManager {
    
    private final KerioFly plugin;
    private final Map<UUID, Long> flyTime = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> flyEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();
    private BukkitTask consumptionTask;
    
    public FlyManager(KerioFly plugin) {
        this.plugin = plugin;
    }
    
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (plugin.getDatabaseManager().isEnabled()) {
            DatabaseManager.PlayerData data = plugin.getDatabaseManager().loadPlayerData(uuid);
            if (data != null) {
                flyTime.put(uuid, data.getFlyTime());
                flyEnabled.put(uuid, data.isFlyEnabled());
                
                // 檢查是否在登入時自動關閉飛行
                boolean disableOnJoin = plugin.getConfigManager().getConfig()
                    .getBoolean("settings.disable-fly-on-join", false);
                
                if (disableOnJoin && data.isFlyEnabled()) {
                    flyEnabled.put(uuid, false);
                    player.sendMessage(plugin.getConfigManager().getMessage("fly-auto-disabled"));
                } else if (data.isFlyEnabled() && data.getFlyTime() > 0) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
                return;
            }
        }
        
        // 預設值
        flyTime.put(uuid, 0L);
        flyEnabled.put(uuid, false);
    }
    
    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        if (plugin.getDatabaseManager().isEnabled()) {
            plugin.getDatabaseManager().savePlayerData(
                uuid,
                player.getName(),
                flyTime.getOrDefault(uuid, 0L),
                flyEnabled.getOrDefault(uuid, false)
            );
        }
    }
    
    public void saveAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }
    }
    
    public long getFlyTime(UUID uuid) {
        return flyTime.getOrDefault(uuid, 0L);
    }
    
    public void setFlyTime(UUID uuid, long seconds) {
        flyTime.put(uuid, Math.max(0, seconds));
    }
    
    public void addFlyTime(UUID uuid, long seconds) {
        long current = getFlyTime(uuid);
        setFlyTime(uuid, current + seconds);
    }
    
    public void reduceFlyTime(UUID uuid, long seconds) {
        long current = getFlyTime(uuid);
        setFlyTime(uuid, current - seconds);
    }
    
    public boolean isFlyEnabled(UUID uuid) {
        return flyEnabled.getOrDefault(uuid, false);
    }
    
    public void setFlyEnabled(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        flyEnabled.put(uuid, enabled);
        
        if (enabled) {
            if (getFlyTime(uuid) > 0 || player.hasPermission("keriofly.unlimited")) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }
    
    public void toggleFly(Player player) {
        setFlyEnabled(player, !isFlyEnabled(player.getUniqueId()));
    }
    
    private boolean hasPlayerMoved(Player player) {
        UUID uuid = player.getUniqueId();
        Location current = player.getLocation();
        Location last = lastLocation.get(uuid);
        
        if (last == null) {
            lastLocation.put(uuid, current.clone());
            return true;
        }
        
        boolean moved = last.getX() != current.getX() || 
                       last.getY() != current.getY() || 
                       last.getZ() != current.getZ();
        
        if (moved) {
            lastLocation.put(uuid, current.clone());
        }
        
        return moved;
    }
    
    public String formatTime(long seconds) {
        if (seconds <= 0) return "0秒";
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天 ");
        if (hours > 0) sb.append(hours).append("小時 ");
        if (minutes > 0) sb.append(minutes).append("分鐘 ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("秒");
        
        return sb.toString().trim();
    }
    
    public long parseTime(String timeString) {
        try {
            if (timeString.endsWith("d")) {
                return Long.parseLong(timeString.substring(0, timeString.length() - 1)) * 86400;
            } else if (timeString.endsWith("h")) {
                return Long.parseLong(timeString.substring(0, timeString.length() - 1)) * 3600;
            } else if (timeString.endsWith("m")) {
                return Long.parseLong(timeString.substring(0, timeString.length() - 1)) * 60;
            } else if (timeString.endsWith("s")) {
                return Long.parseLong(timeString.substring(0, timeString.length() - 1));
            }
        } catch (NumberFormatException ignored) {}
        return -1;
    }
    
    public void startTimeConsumptionTask() {
        int interval = plugin.getConfigManager().getConfig().getInt("settings.consume-interval-seconds", 1);
        boolean consumeWhenDisabled = plugin.getConfigManager().getConfig()
            .getBoolean("settings.consume-time-when-disabled", false);
        boolean consumeWhenNotMoving = plugin.getConfigManager().getConfig()
            .getBoolean("settings.consume-time-when-not-moving", false);
        boolean consumeWhenNotFlying = plugin.getConfigManager().getConfig()
            .getBoolean("settings.consume-time-when-not-flying", false);
        
        consumptionTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                
                // 無限飛行權限跳過
                if (player.hasPermission("keriofly.unlimited")) continue;
                
                boolean shouldConsume = false;
                
                // 檢查是否應該扣除時間
                if (isFlyEnabled(uuid)) {
                    // 檢查未移動設定
                    if (!consumeWhenNotMoving && !hasPlayerMoved(player)) {
                        continue;
                    }
                    
                    // 檢查未飛行設定
                    if (!consumeWhenNotFlying && !player.isFlying()) {
                        continue;
                    }
                    
                    shouldConsume = true;
                } else if (consumeWhenDisabled && player.isFlying()) {
                    shouldConsume = true;
                }
                
                if (shouldConsume) {
                    long time = getFlyTime(uuid);
                    if (time > 0) {
                        reduceFlyTime(uuid, interval);
                        
                        // 時間用完時關閉飛行
                        if (getFlyTime(uuid) <= 0) {
                            setFlyEnabled(player, false);
                            player.sendMessage(plugin.getConfigManager().getMessage("no-time"));
                        }
                    } else {
                        // 沒有時間時強制關閉飛行
                        if (player.isFlying() || player.getAllowFlight()) {
                            setFlyEnabled(player, false);
                            player.sendMessage(plugin.getConfigManager().getMessage("no-time"));
                        }
                    }
                }
            }
        }, 20L * interval, 20L * interval);
    }
    
    public void stopTimeConsumptionTask() {
        if (consumptionTask != null) {
            consumptionTask.cancel();
        }
    }
}