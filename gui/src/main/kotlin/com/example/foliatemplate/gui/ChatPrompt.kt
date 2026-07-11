package com.example.foliatemplate.gui

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * "Close the menu, type a value in chat, come back."
 *
 * ```
 * button(11, icon(Material.NAME_TAG, "<yellow>Set amount")) { ctx ->
 *     ChatPrompt.ask(
 *         plugin.prompts, ctx.player,
 *         prompt = "<yellow>Type an amount in chat, or 'cancel'.",
 *         onInput = { player, text -> ... ; MyMenu(...).open(player) },
 *         onCancel = { player -> MyMenu(...).open(player) },
 *     )
 * }
 * ```
 *
 * Folia notes:
 *  - Pending prompts live in a [ConcurrentHashMap]; chat fires ASYNC and clicks
 *    fire on the player's region, so this map is genuinely touched from multiple
 *    threads and must be concurrent.
 *  - The chat event is async, so the callback is hopped back onto the player's
 *    region thread before it runs — you can safely open menus / touch the player
 *    inside [onInput].
 */
class ChatPrompt private constructor(
    val prompt: String,
    val onInput: (Player, String) -> Unit,
    val onCancel: (Player) -> Unit,
) {

    /** Registers pending prompts. One instance, created by the host. */
    class Registry(private val schedulers: MenuSchedulers) : Listener {

        private val pending = ConcurrentHashMap<UUID, ChatPrompt>()

        internal fun put(player: Player, prompt: ChatPrompt) {
            pending[player.uniqueId] = prompt
        }

        fun cancel(player: Player) {
            pending.remove(player.uniqueId)
        }

        fun isWaiting(player: Player): Boolean = pending.containsKey(player.uniqueId)

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        fun onChat(event: AsyncChatEvent) {
            val player = event.player
            val prompt = pending.remove(player.uniqueId) ?: return

            // Swallow the message — it's an answer, not chat.
            event.isCancelled = true

            val text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim()

            // We're on an async chat thread; hop to the player's region before
            // touching the player or opening any inventory.
            schedulers.entity(player) {
                if (text.equals("cancel", ignoreCase = true)) prompt.onCancel(player)
                else prompt.onInput(player, text)
            }
        }

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            pending.remove(event.player.uniqueId)
        }
    }

    companion object {
        /**
         * Close [player]'s menu and wait for their next chat message.
         * Typing "cancel" triggers [onCancel] instead of [onInput].
         */
        fun ask(
            registry: Registry,
            player: Player,
            prompt: String,
            onInput: (Player, String) -> Unit,
            onCancel: (Player) -> Unit = {},
        ) {
            player.closeInventory()
            registry.put(player, ChatPrompt(prompt, onInput, onCancel))
            player.sendMessage(mm(prompt))
        }

        internal fun mm(text: String): Component =
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(text)
    }
}
