package com.example.foliatemplate.gui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

/**
 * Routes inventory interaction to the right [Menu]. Register once:
 * `server.pluginManager.registerEvents(MenuListener(), this)`
 *
 * Inventory events for a player fire on that player's region thread on Folia, so
 * these handlers are already on the correct thread to touch the player.
 */
class MenuListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val menu = event.view.topInventory.holder as? Menu ?: return

        // Cancel the whole event: stops taking items out AND shift-clicking items
        // in from the player's own inventory.
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        if (event.rawSlot in 0 until event.view.topInventory.size) {
            menu.dispatch(ClickContext(player, event.rawSlot, event.click, event))
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is Menu) event.isCancelled = true
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val menu = event.view.topInventory.holder as? Menu ?: return
        val player = event.player as? Player ?: return
        menu.stopAnimation()   // don't leave a repeating task running on a closed menu
        menu.onClose(player)
    }
}
