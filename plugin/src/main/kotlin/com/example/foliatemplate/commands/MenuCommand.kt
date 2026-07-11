package com.example.foliatemplate.commands

import com.example.foliatemplate.FoliaTemplatePlugin
import com.example.foliatemplate.command.Command
import com.example.foliatemplate.command.CommandContext
import com.example.foliatemplate.command.Default
import com.example.foliatemplate.menus.DemoMenu

/** `/menu` — opens the GUI framework demo. */
@Command(name = "menu", permission = "foliatemplate.menu", playerOnly = true, description = "Open the demo menu.")
class MenuCommand(private val plugin: FoliaTemplatePlugin) {

    @Default(playerOnly = true)
    fun open(ctx: CommandContext) {
        // One instance per open keeps this player's menu on their own region thread.
        DemoMenu(plugin).open(ctx.requireSenderPlayer())
    }
}
