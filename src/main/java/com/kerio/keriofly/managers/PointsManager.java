package com.kerio.keriofly.managers;

import com.kerio.keriofly.KerioFly;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PointsManager {
    
    private final KerioFly plugin;
    private final Map<UUID, Long> flyPoints = new ConcurrentHashMap<>();
    
    public PointsManager(KerioFly plugin) {
        this.plugin = plugin;
    }
    
    public void loadPlayerPoints(UUID uuid) {
        if (plugin.getDatabaseManager().isEnabled()) {
            Long points = plugin.getDatabaseManager().loadPlayerPoints(uuid);
            if (points != null) {
                flyPoints.put(uuid, points);
                return;
            }
        }
        flyPoints.put(uuid, 0L);
    }
    
    public void savePlayerPoints(UUID uuid) {
        if (plugin.getDatabaseManager().isEnabled()) {
            plugin.getDatabaseManager().savePlayerPoints(uuid, getPoints(uuid));
        }
    }
    
    public void saveAllPoints() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerPoints(player.getUniqueId());
        }
    }
    
    public long getPoints(UUID uuid) {
        return flyPoints.getOrDefault(uuid, 0L);
    }
    
    public void setPoints(UUID uuid, long points) {
        flyPoints.put(uuid, Math.max(0, points));
    }
    
    public void addPoints(UUID uuid, long points) {
        long current = getPoints(uuid);
        setPoints(uuid, current + points);
    }
    
    public void reducePoints(UUID uuid, long points) {
        long current = getPoints(uuid);
        setPoints(uuid, current - points);
    }
    
    public boolean hasPoints(UUID uuid, long points) {
        return getPoints(uuid) >= points;
    }
    
    // 點數購買
    public boolean purchaseWithPoints(UUID uuid, int hours) {
        long pointsNeeded = hours;
        
        if (!hasPoints(uuid, pointsNeeded)) {
            return false;
        }
        
        reducePoints(uuid, pointsNeeded);
        plugin.getFlyManager().addFlyTime(uuid, hours * 3600L);
        return true;
    }
    
    // 點數轉換
    public String getConvertibleTime(long points) {
        return plugin.getFlyManager().formatTime(points * 3600);
    }
}