package com.example.foliatemplate.gui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * A ready-made yes/no dialog.
 *
 *   ConfirmMenu(schedulers, mm("<red>Delete your home?"),
 *       onConfirm = { p -> deleteHome(p); p.sendMessage("Deleted.") },
 *       onDeny = { p -> MainMenu(schedulers).open(p) },
 *   ).open(player)
 */
class ConfirmMenu(
    schedulers: MenuSchedulers,
    title: Component,
    private val onConfirm: (Player) -> Unit,
    private val onDeny: (Player) -> Unit = { it.closeInventory() },
    private val confirmText: String = "<green><bold>Confirm",
    private val denyText: String = "<red><bold>Cancel",
) : Menu(schedulers, title, rows = 3) {

    override fun build(player: Player) {
        button(11, icon(Material.LIME_DYE, confirmText)) { context ->
            context.close()
            onConfirm(context.player)
        }
        button(15, icon(Material.RED_DYE, denyText)) { context ->
            context.close()
            onDeny(context.player)
        }
        fill(icon(Material.GRAY_STAINED_GLASS_PANE, " "))
    }
}
