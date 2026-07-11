package com.example.foliatemplate.gui

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * Base chest-style GUI.
 *
 * Identification is holder-based: the menu IS its inventory's [InventoryHolder],
 * so [MenuListener] finds it with `topInventory.holder is Menu`. No global
 * open-menus registry to keep thread-safe — the single biggest source of GUI
 * races on Folia is avoided by construction.
 *
 * Threading: create ONE instance per player per open. [open], [refresh] and
 * click dispatch all run on that player's region thread, so one player's menu is
 * effectively single-threaded and needs no locks.
 */
abstract class Menu(
    protected val schedulers: MenuSchedulers,
    private val title: Component,
    rows: Int,
) : InventoryHolder {

    init {
        require(rows in 1..6) { "A chest menu must have 1-6 rows, got $rows" }
    }

    private val handlers = HashMap<Int, (ClickContext) -> Unit>()
    private val inventory: Inventory = Bukkit.createInventory(this, rows * 9, title)

    private var animation: ScheduledTask? = null
    private var viewer: Player? = null

    /** Total slots. */
    val size: Int get() = inventory.size

    /** Rows in this menu. */
    val rows: Int get() = inventory.size / 9

    override fun getInventory(): Inventory = inventory

    /**
     * Draw the contents. Called on open and on every [refresh], on the viewer's
     * region thread — so reading live state (player, stats, config) is safe.
     */
    protected abstract fun build(player: Player)

    /** A clickable icon. [onClick] runs on the clicker's region thread. */
    protected fun button(slot: Int, icon: ItemStack, onClick: (ClickContext) -> Unit) {
        if (slot !in 0 until inventory.size) return
        inventory.setItem(slot, icon)
        handlers[slot] = onClick
    }

    /** A non-interactive decoration. */
    protected fun item(slot: Int, icon: ItemStack) {
        if (slot !in 0 until inventory.size) return
        inventory.setItem(slot, icon)
    }

    /** Fill every currently-empty slot. */
    protected fun fill(icon: ItemStack) {
        for (i in 0 until inventory.size) {
            if (inventory.getItem(i) == null) inventory.setItem(i, icon)
        }
    }

    /** Fill only the outer border ring. */
    protected fun border(icon: ItemStack) {
        val lastRow = rows - 1
        for (column in 0 until 9) {
            item(column, icon)
            item(lastRow * 9 + column, icon)
        }
        for (row in 0 until rows) {
            item(row * 9, icon)
            item(row * 9 + 8, icon)
        }
    }

    /** Open (or re-open) this menu for [player] on their region thread. */
    fun open(player: Player) {
        viewer = player
        schedulers.entity(player) {
            redraw(player)
            player.openInventory(inventory)
            onOpen(player)
        }
    }

    /**
     * Redraw contents in place — the inventory stays open, so unlike re-opening
     * there's no flicker and no cursor reset. Use this for live updates.
     */
    fun refresh() {
        val player = viewer ?: return
        schedulers.entity(player) { redraw(player) }
    }

    /**
     * Redraw every [periodTicks] until the menu closes. Use for countdowns,
     * spinners, live stats. Runs on the viewer's region thread.
     */
    protected fun animate(periodTicks: Long, onTick: (Int) -> Unit = {}) {
        val player = viewer ?: return
        var frame = 0
        animation?.cancel()
        animation = schedulers.entityRepeating(player, 0L, periodTicks) { task ->
            if (!player.isOnline || player.openInventory.topInventory.holder !== this) {
                task.cancel()
                animation = null
                return@entityRepeating
            }
            onTick(frame++)
            redraw(player)
        }
    }

    /** Called after the inventory is shown. Override for setup (e.g. animate()). */
    protected open fun onOpen(player: Player) {}

    /** Called when the player closes the menu. Override to clean up. */
    open fun onClose(player: Player) {}

    private fun redraw(player: Player) {
        handlers.clear()
        inventory.clear()
        build(player)
    }

    internal fun dispatch(context: ClickContext) {
        handlers[context.slot]?.invoke(context)
    }

    internal fun stopAnimation() {
        animation?.cancel()
        animation = null
    }
}
