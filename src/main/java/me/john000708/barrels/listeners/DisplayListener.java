package me.john000708.barrels.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import me.john000708.barrels.Barrels;
import me.john000708.barrels.DisplayItem;

/**
 * Created by John on 10.05.2016.
 */
public class DisplayListener implements Listener {

    public DisplayListener(Barrels plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPickpup(EntityPickupItemEvent e) {
        if (!e.getItem().hasMetadata("no_pickup") && PersistentDataAPI.hasString(e.getItem(), DisplayItem.NAMESPACED_KEY)) {
            e.setCancelled(true);
            e.getItem().remove();
        }
    }

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent e) {
        if (!e.getItem().hasMetadata("no_pickup") && PersistentDataAPI.hasString(e.getItem(), DisplayItem.NAMESPACED_KEY)) {
            e.setCancelled(true);
            e.getItem().remove();
        }
    }
}
