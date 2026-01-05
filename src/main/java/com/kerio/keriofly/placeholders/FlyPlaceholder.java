package com.kerio.keriofly.placeholders;

import com.kerio.keriofly.KerioFly;
import com.kerio.keriofly.managers.FlyManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class FlyPlaceholder extends PlaceholderExpansion {
    
    private final KerioFly plugin;
    private final FlyManager flyManager;
    
    public FlyPlaceholder(KerioFly plugin) {
        this.plugin = plugin;
        this.flyManager = plugin.getFlyManager();
    }
    
    @Override
    public String getIdentifier() {
        return "keriofly";
    }
    
    @Override
    public String getAuthor() {
        return "Kerio";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        
        // %keriofly_time%
        if (identifier.equals("time")) {
            if (player.hasPermission("keriofly.unlimited")) {
                return "無限";
            }
            long time = flyManager.getFlyTime(player.getUniqueId());
            return flyManager.formatTime(time);
        }
        
        // %keriofly_time_seconds%
        if (identifier.equals("time_seconds")) {
            if (player.hasPermission("keriofly.unlimited")) {
                return "∞";
            }
            return String.valueOf(flyManager.getFlyTime(player.getUniqueId()));
        }
        
        // %keriofly_time_minutes%
        if (identifier.equals("time_minutes")) {
            if (player.hasPermission("keriofly.unlimited")) {
                return "∞";
            }
            return String.valueOf(flyManager.getFlyTime(player.getUniqueId()) / 60);
        }
        
        // %keriofly_time_hours%
        if (identifier.equals("time_hours")) {
            if (player.hasPermission("keriofly.unlimited")) {
                return "∞";
            }
            return String.valueOf(flyManager.getFlyTime(player.getUniqueId()) / 3600);
        }
        
        // %keriofly_time_days%
        if (identifier.equals("time_days")) {
            if (player.hasPermission("keriofly.unlimited")) {
                return "∞";
            }
            return String.valueOf(flyManager.getFlyTime(player.getUniqueId()) / 86400);
        }
        
        // %keriofly_use%
        if (identifier.equals("use")) {
            return flyManager.isFlyEnabled(player.getUniqueId()) ? "開啟" : "關閉";
        }
        
        // %keriofly_use_boolean%
        if (identifier.equals("use_boolean")) {
            return String.valueOf(flyManager.isFlyEnabled(player.getUniqueId()));
        }
        
        // %keriofly_flying%
        if (identifier.equals("flying")) {
            return player.isFlying() ? "是" : "否";
        }
        
        // %keriofly_flying_boolean%
        if (identifier.equals("flying_boolean")) {
            return String.valueOf(player.isFlying());
        }
        
        // %keriofly_unlimited%
        if (identifier.equals("unlimited")) {
            return player.hasPermission("keriofly.unlimited") ? "是" : "否";
        }
        
        // %keriofly_points%
        if (identifier.equals("points")) {
            return String.valueOf(plugin.getPointsManager().getPoints(player.getUniqueId()));
        }
        
        // %keriofly_points_time%
        if (identifier.equals("points_time")) {
            long points = plugin.getPointsManager().getPoints(player.getUniqueId());
            return plugin.getPointsManager().getConvertibleTime(points);
        }
        
        return null;
    }
}