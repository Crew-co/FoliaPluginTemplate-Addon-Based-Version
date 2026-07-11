package com.example.foliatemplate.menus

import com.example.foliatemplate.FoliaTemplatePlugin
import com.example.foliatemplate.gui.ChatPrompt
import com.example.foliatemplate.gui.ClickContext
import com.example.foliatemplate.gui.ConfirmMenu
import com.example.foliatemplate.gui.Menu
import com.example.foliatemplate.gui.PaginatedMenu
import com.example.foliatemplate.gui.icon
import com.example.foliatemplate.gui.mm
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Shows off every GUI feature. `/menu` opens this.
 */
class DemoMenu(private val plugin: FoliaTemplatePlugin) :
    Menu(plugin.schedulers, mm("<gradient:#7f5af0:#2cb67d>GUI Demo</gradient>"), rows = 3) {

    // Animation state — only ever touched on the viewer's region thread.
    private var spinnerFrame = 0

    override fun onOpen(player: Player) {
        // Redraw 4x/second so the spinner below actually spins.
        animate(periodTicks = 5L) { frame -> spinnerFrame = frame }
    }

    override fun build(player: Player) {
        // 1. A live/animated item — redrawn by animate() above.
        val faces = listOf(Material.WHITE_CONCRETE, Material.YELLOW_CONCRETE, Material.ORANGE_CONCRETE, Material.RED_CONCRETE)
        item(
            10,
            icon(
                faces[spinnerFrame % faces.size],
                "<yellow>Animated item",
                "<gray>Redraws in place — no flicker.",
                "<dark_gray>frame $spinnerFrame",
            ),
        )

        // 2. Paginated submenu.
        button(12, icon(Material.BOOKSHELF, "<aqua>Paginated menu", "<gray>50 items, auto-paged")) { ctx ->
            NumbersMenu(plugin).open(ctx.player)
        }

        // 3. Chat input prompt.
        button(14, icon(Material.NAME_TAG, "<light_purple>Chat prompt", "<gray>Type a value in chat")) { ctx ->
            ChatPrompt.ask(
                plugin.prompts,
                ctx.player,
                prompt = "<yellow>Type anything in chat (or 'cancel'):",
                onInput = { p, text ->
                    p.sendMessage(mm("<green>You typed: <white>$text"))
                    DemoMenu(plugin).open(p) // callback already runs on their region
                },
                onCancel = { p -> DemoMenu(plugin).open(p) },
            )
        }

        // 4. Confirmation dialog.
        button(16, icon(Material.TNT, "<red>Confirm dialog", "<gray>Are you sure?")) { ctx ->
            ConfirmMenu(
                plugin.schedulers,
                mm("<red>Really do the thing?"),
                onConfirm = { p -> p.sendMessage(mm("<green>Confirmed!")) },
                onDeny = { p -> DemoMenu(plugin).open(p) },
            ).open(ctx.player)
        }

        border(icon(Material.BLACK_STAINED_GLASS_PANE, " "))
    }
}

/** A paginated list of 50 items — pagination/nav is handled by the base class. */
class NumbersMenu(private val plugin: FoliaTemplatePlugin) :
    PaginatedMenu<Int>(plugin.schedulers, mm("<aqua>Numbers"), rows = 6) {

    override fun items(): List<Int> = (1..50).toList()

    override fun render(item: Int): ItemStack =
        icon(Material.PAPER, "<white>Item <yellow>$item", "<gray>Click me", amount = item.coerceAtMost(64))

    override fun onClick(item: Int, context: ClickContext) {
        context.reply("<green>You picked <white>$item</white>.")
    }

    override fun decorate(player: Player) {
        // Bottom-left: back to the demo menu.
        button((rows - 1) * 9, icon(Material.BARRIER, "<red>Back")) { ctx ->
            DemoMenu(plugin).open(ctx.player)
        }
    }
}
