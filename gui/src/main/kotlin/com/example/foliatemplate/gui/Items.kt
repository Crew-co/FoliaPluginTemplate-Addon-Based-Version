package com.example.foliatemplate.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

private val MM = MiniMessage.miniMessage()

fun mm(text: String): Component = MM.deserialize(text)

/**
 * Builds a menu icon with a MiniMessage name and lore. Minecraft italicises
 * custom names by default; that's disabled here so menus look clean.
 *
 *   icon(Material.LIME_DYE, "<green>Confirm", "<gray>Click to accept")
 */
fun icon(material: Material, name: String? = null, vararg lore: String, amount: Int = 1): ItemStack {
    val stack = ItemStack(material, amount)
    stack.editMeta { meta ->
        if (name != null) meta.displayName(mm(name).noItalic())
        if (lore.isNotEmpty()) meta.lore(lore.map { mm(it).noItalic() })
    }
    return stack
}

private fun Component.noItalic(): Component = decoration(TextDecoration.ITALIC, false)
