package com.example.foliatemplate.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * A menu that pages through a list of [T].
 *
 * Supply the items in [items], render each one with [render], and handle clicks
 * in [onClick]. Navigation buttons and page state are handled for you.
 *
 * By default the content area is every slot except the bottom row (which holds
 * the nav bar); override [contentSlots] to carve out a different area.
 *
 * ```
 * class KitMenu(s: MenuSchedulers, private val kits: List<Kit>)
 *     : PaginatedMenu<Kit>(s, mm("<gold>Kits"), rows = 6) {
 *     override fun items() = kits
 *     override fun render(item: Kit) = icon(item.icon, "<yellow>${item.name}")
 *     override fun onClick(item: Kit, ctx: ClickContext) { item.give(ctx.player); ctx.close() }
 * }
 * ```
 */
abstract class PaginatedMenu<T>(
    schedulers: MenuSchedulers,
    title: Component,
    rows: Int = 6,
) : Menu(schedulers, title, rows) {

    private var page = 0

    /** The full (unpaged) item list. Re-read on every draw, so it can change. */
    protected abstract fun items(): List<T>

    /** Turn one item into its icon. */
    protected abstract fun render(item: T): ItemStack

    /** Called when an item is clicked. */
    protected abstract fun onClick(item: T, context: ClickContext)

    /** Slots used for content. Defaults to everything above the bottom nav row. */
    protected open fun contentSlots(): List<Int> = (0 until (rows - 1) * 9).toList()

    /** Override to place extra buttons in the nav row, decorate, etc. */
    protected open fun decorate(player: Player) {}

    protected open fun previousIcon(): ItemStack = icon(Material.ARROW, "<yellow>Previous page")

    protected open fun nextIcon(): ItemStack = icon(Material.ARROW, "<yellow>Next page")

    protected open fun pageIndicator(current: Int, total: Int): ItemStack =
        icon(Material.PAPER, "<white>Page <yellow>$current</yellow>/<yellow>$total")

    /** Items shown per page. */
    val pageSize: Int get() = contentSlots().size

    final override fun build(player: Player) {
        val all = items()
        val slots = contentSlots()
        val totalPages = maxOf(1, (all.size + slots.size - 1) / slots.size)

        // Clamp, in case the list shrank since the last draw.
        page = page.coerceIn(0, totalPages - 1)

        val start = page * slots.size
        val visible = all.drop(start).take(slots.size)

        visible.forEachIndexed { index, item ->
            button(slots[index], render(item)) { context -> onClick(item, context) }
        }

        // ---- nav bar (bottom row) ----
        val navRow = (rows - 1) * 9

        if (page > 0) {
            button(navRow + 3, previousIcon()) {
                page--
                refresh()
            }
        }

        item(navRow + 4, pageIndicator(page + 1, totalPages))

        if (page < totalPages - 1) {
            button(navRow + 5, nextIcon()) {
                page++
                refresh()
            }
        }

        decorate(player)
    }

    /** Jump to a page (0-based) and redraw. */
    fun page(index: Int) {
        page = index
        refresh()
    }
}
