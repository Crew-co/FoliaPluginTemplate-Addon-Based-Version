package com.example.foliatemplate

import com.example.foliatemplate.addon.AddonManager
import com.example.foliatemplate.command.CommandManager
import com.example.foliatemplate.commands.AddonsCommand
import com.example.foliatemplate.scheduler.Schedulers
import org.bukkit.plugin.java.JavaPlugin

/**
 * Entry point.
 *
 * Startup order matters: helpers → host commands → addons LAST, so an addon can
 * rely on everything the host provides being ready inside its onEnable().
 *
 * Folia note: there is no main thread. For anything timed that touches the
 * world, use [schedulers], never Bukkit.getScheduler().
 */
class FoliaTemplatePlugin : JavaPlugin() {

companion object{
    lateinit var schedulers: Schedulers
        private set

    lateinit var commands: CommandManager
        private set

}
    lateinit var addons: AddonManager
        private set

    override fun onEnable() {
        schedulers = Schedulers(this)
        commands = CommandManager(this)

        // Host commands.
        commands.register(AddonsCommand(this))

        // Addons last — they register against a fully-initialized host.
        // This also creates plugins/FoliaTemplate/addons/ on first run.
        addons = AddonManager(this)
        addons.loadAll()

        logger.info("Enabled ${pluginMeta.name} v${pluginMeta.version} (Folia: ${Schedulers.isFolia})")
    }

    override fun onDisable() {
        // Disable addons first, while the host still works.
        if (::addons.isInitialized) addons.unloadAll()
        logger.info("Disabled.")
    }
}
