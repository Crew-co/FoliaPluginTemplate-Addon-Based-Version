package com.example.foliatemplate.gui

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * Passed to every button handler. Handlers run on the clicking player's region
 * thread, so touching [player] is safe.
 */
class ClickContext(
    val player: Player,
    /** Slot within the menu that was clicked. */
    val slot: Int,
    val type: ClickType,
    /** The underlying event, already cancelled by [MenuListener]. */
    val event: InventoryClickEvent,
) {
    val isLeftClick: Boolean get() = type.isLeftClick
    val isRightClick: Boolean get() = type.isRightClick
    val isShiftClick: Boolean get() = type.isShiftClick

    fun close() = player.closeInventory()

    fun reply(message: String) = player.sendMessage(mm(message))
    fun reply(message: Component) = player.sendMessage(message)
}
