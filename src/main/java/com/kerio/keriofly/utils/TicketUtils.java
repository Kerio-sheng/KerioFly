package com.kerio.keriofly.utils;

import com.kerio.keriofly.KerioFly;
import com.kerio.keriofly.managers.TicketManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class TicketUtils {
    
    private static final NamespacedKey TICKET_KEY = new NamespacedKey(KerioFly.getInstance(), "fly_ticket_time");
    private static final NamespacedKey TICKET_NAME_KEY = new NamespacedKey(KerioFly.getInstance(), "fly_ticket_name");
    
    public static ItemStack createTicket(TicketManager.TicketData data) {
        KerioFly plugin = KerioFly.getInstance();
        
        Material material = Material.matchMaterial(data.getMaterial());
        if (material == null) material = Material.PAPER;
        
        ItemStack ticket = new ItemStack(material);
        ItemMeta meta = ticket.getItemMeta();
        
        if (meta != null) {
            String timeFormatted = plugin.getFlyManager().formatTime(data.getTime());
            
            String name = plugin.getConfigManager().colorize(
                data.getDisplayName().replace("{time}", timeFormatted)
            );
            meta.setDisplayName(name);
            
            List<String> finalLore = new ArrayList<>();
            for (String line : data.getLore()) {
                finalLore.add(plugin.getConfigManager().colorize(
                    line.replace("{time}", timeFormatted)
                ));
            }
            meta.setLore(finalLore);
            
            if (data.isEnchanted()) {
                meta.addEnchant(Enchantment.LURE, 1, true);
            }
            
            if (data.getItemFlags() != null) {
                for (String flagName : data.getItemFlags()) {
                    try {
                        ItemFlag flag = ItemFlag.valueOf(flagName);
                        meta.addItemFlags(flag);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            
            // CustomModelData
            if (data.getCustomModelData() > 0) {
                meta.setCustomModelData(data.getCustomModelData());
            }
            
            // 儲存
            meta.getPersistentDataContainer().set(TICKET_KEY, PersistentDataType.LONG, data.getTime());
            meta.getPersistentDataContainer().set(TICKET_NAME_KEY, PersistentDataType.STRING, data.getName());
            
            ticket.setItemMeta(meta);
        }
        
        return ticket;
    }
    
    public static boolean isTicket(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(TICKET_KEY, PersistentDataType.LONG);
    }
    
    public static long getTicketTime(ItemStack item) {
        if (!isTicket(item)) return 0;
        
        ItemMeta meta = item.getItemMeta();
        Long time = meta.getPersistentDataContainer().get(TICKET_KEY, PersistentDataType.LONG);
        return time != null ? time : 0;
    }
    
    public static String getTicketName(ItemStack item) {
        if (!isTicket(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(TICKET_NAME_KEY, PersistentDataType.STRING);
    }
    
    public static void giveTicket(Player player, long seconds) {
        TicketManager.TicketData data = new TicketManager.TicketData(
            "temp",
            seconds,
            "PAPER",
            "&#00FFFF飛行票券 &7({time})",
            List.of("&7右鍵使用以獲得飛行時間", "&e飛行時間: &f{time}", "", "&#FFD700➤ 右鍵使用"),
            true,
            0,
            List.of("HIDE_ENCHANTS", "HIDE_ATTRIBUTES")
        );
        ItemStack ticket = createTicket(data);
        player.getInventory().addItem(ticket);
    }
}