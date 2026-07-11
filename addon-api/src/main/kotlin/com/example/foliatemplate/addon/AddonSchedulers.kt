package com.example.foliatemplate.addon

import com.example.foliatemplate.gui.MenuSchedulers
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Location
import org.bukkit.entity.Entity
import java.util.concurrent.TimeUnit

/**
 * Folia-safe scheduling, exposed to addons.
 *
 * Folia has NO main thread: each region ticks on its own thread. Addons MUST
 * NOT use `Bukkit.getScheduler()`. Instead schedule onto whatever owns the
 * thing you're touching:
 *
 *  - [global] — world-wide state (time, weather, your own shared data)
 *  - [region] — blocks/world at a Location
 *  - [entity] — a specific entity or player
 *  - [async]  — network/disk I/O; never touch world state here
 *
 * Tasks are tracked per-addon and cancelled when the addon is disabled.
 *
 * Extends [MenuSchedulers], so an addon can pass `context.schedulers` straight
 * into any Menu without casting.
 */
interface AddonSchedulers : MenuSchedulers {

    fun global(task: () -> Unit): ScheduledTask

    fun globalDelayed(delayTicks: Long, task: () -> Unit): ScheduledTask

    fun globalRepeating(initialDelayTicks: Long, periodTicks: Long, task: (ScheduledTask) -> Unit): ScheduledTask

    fun region(location: Location, task: () -> Unit): ScheduledTask

    fun regionDelayed(location: Location, delayTicks: Long, task: () -> Unit): ScheduledTask

    fun entityDelayed(entity: Entity, delayTicks: Long, retired: (() -> Unit)? = null, task: () -> Unit): ScheduledTask?

    fun async(task: () -> Unit): ScheduledTask

    fun asyncDelayed(delay: Long, unit: TimeUnit = TimeUnit.SECONDS, task: () -> Unit): ScheduledTask

    fun asyncRepeating(initialDelay: Long, period: Long, unit: TimeUnit = TimeUnit.SECONDS, task: (ScheduledTask) -> Unit): ScheduledTask
}
