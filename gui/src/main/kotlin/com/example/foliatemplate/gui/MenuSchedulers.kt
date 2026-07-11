package com.example.foliatemplate.gui

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.entity.Entity

/**
 * The slice of Folia scheduling the GUI module needs. The host's `Schedulers`
 * satisfies this, so the GUI module stays standalone (no dependency on the
 * plugin), and addons can drive menus with their own `AddonSchedulers`.
 */
interface MenuSchedulers {
    fun entity(entity: Entity, retired: (() -> Unit)? = null, task: () -> Unit): ScheduledTask?

    fun entityRepeating(
        entity: Entity,
        initialDelayTicks: Long,
        periodTicks: Long,
        retired: (() -> Unit)? = null,
        task: (ScheduledTask) -> Unit,
    ): ScheduledTask?
}
