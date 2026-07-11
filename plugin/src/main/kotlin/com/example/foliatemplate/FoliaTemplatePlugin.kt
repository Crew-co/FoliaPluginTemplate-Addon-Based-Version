package com.example.foliatemplate

import com.example.foliatemplate.addon.AddonManager
import com.example.foliatemplate.command.CommandManager
import com.example.foliatemplate.commands.AddonsCommand
import com.example.foliatemplate.commands.MenuCommand
import com.example.foliatemplate.gui.ChatPrompt
import com.example.foliatemplate.gui.MenuListener
import com.example.foliatemplate.scheduler.Schedulers
import com.example.foliatemplate.util.Cooldowns
import org.bukkit.plugin.java.JavaPlugin

/**
 * Entry point.
 *
 * Startup order matters: helpers → GUI → commands → addons LAST, so an addon can
 * rely on everything the host provides being ready inside its onEnable.
 */
class FoliaTemplatePlugin : JavaPlugin() {

    lateinit var schedulers: Schedulers
        private set
    lateinit var commands: CommandManager
        private set
    lateinit var addons: AddonManager
        private set

    /** Registry backing ChatPrompt.ask(...) — pass this wherever you prompt. */
    lateinit var prompts: ChatPrompt.Registry
        private set

    val cooldowns = Cooldowns()

    override fun onEnable() {
        schedulers = Schedulers(this)
        commands = CommandManager(this)

        // GUI framework: one listener drives every menu; one registry drives prompts.
        server.pluginManager.registerEvents(MenuListener(), this)
        prompts = ChatPrompt.Registry(schedulers)
        server.pluginManager.registerEvents(prompts, this)

        // Host commands.
        commands.register(MenuCommand(this))
        commands.register(AddonsCommand(this))

        // Addons last: they can register commands/listeners/services against a
        // fully-initialized host.
        addons = AddonManager(this)
        addons.loadAll()

        logger.info("Enabled ${pluginMeta.name} v${pluginMeta.version} (Folia: ${Schedulers.isFolia})")
    }

    override fun onDisable() {
        // Disable addons first so they can clean up while the host still works.
        if (::addons.isInitialized) addons.unloadAll()
        logger.info("Disabled.")
    }
}
