package com.kerio.keriofly.listeners;

import com.kerio.keriofly.KerioFly;
import com.kerio.keriofly.managers.FlyManager;
import com.kerio.keriofly.utils.TicketUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    
    private final KerioFly plugin;
    private final FlyManager flyManager;
    
    public PlayerListener(KerioFly plugin) {
        this.plugin = plugin;
        this.flyManager = plugin.getFlyManager();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        flyManager.loadPlayerData(player);
        plugin.getPointsManager().loadPlayerPoints(player.getUniqueId());
        
        if (flyManager.isFlyEnabled(player.getUniqueId())) {
            if (flyManager.getFlyTime(player.getUniqueId()) > 0 || player.hasPermission("keriofly.unlimited")) {
                player.setAllowFlight(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        flyManager.savePlayerData(player);
        plugin.getPointsManager().savePlayerPoints(player.getUniqueId());
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = plugin.getConfigManager().colorize(
            plugin.getConfigManager().getTicketConfig().getString("gui.title", "飛行票券管理")
        );
        
        if (!event.getView().getTitle().equals(title)) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        
        // 檢查票券
        if (TicketUtils.isTicket(clicked)) {
            ItemStack ticket = clicked.clone();
            ticket.setAmount(1);
            player.getInventory().addItem(ticket);
            player.sendMessage("§a已取得票券！");
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) return;
        
        if (TicketUtils.isTicket(item)) {
            event.setCancelled(true);
            
            long time = TicketUtils.getTicketTime(item);
            if (time > 0) {
                flyManager.addFlyTime(player.getUniqueId(), time);
                player.sendMessage(plugin.getConfigManager().getMessage("ticket-received")
                    .replace("{time}", flyManager.formatTime(time)));
                
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
    
    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        boolean allowCreative = plugin.getConfigManager().getConfig().getBoolean("settings.allow-fly-in-creative", false);
        
        // 創造模式飛行
        if (event.getNewGameMode() == GameMode.CREATIVE) {
            if (!allowCreative && flyManager.isFlyEnabled(player.getUniqueId())) {
                flyManager.setFlyEnabled(player, false);
            }
        }
    }
    
    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        
        if (!flyManager.isFlyEnabled(player.getUniqueId()) && !player.hasPermission("keriofly.unlimited")) {
            event.setCancelled(true);
            player.setAllowFlight(false);
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        boolean disableOnDamage = plugin.getConfigManager().getConfig().getBoolean("settings.disable-fly-on-damage", false);
        
        if (disableOnDamage && flyManager.isFlyEnabled(player.getUniqueId())) {
            flyManager.setFlyEnabled(player, false);
            player.sendMessage(plugin.getConfigManager().getMessage("fly-disabled"));
        }
    }
}