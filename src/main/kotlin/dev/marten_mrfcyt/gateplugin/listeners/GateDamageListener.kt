package dev.marten_mrfcyt.gateplugin.listeners

import dev.marten_mrfcyt.gateplugin.GatePlugin
import dev.marten_mrfcyt.gateplugin.gate.gates
import dev.marten_mrfcyt.gateplugin.gate.isBreakItem
import dev.marten_mrfcyt.gateplugin.utils.getKingdom
import mlib.api.utilities.message
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GateDamageListener(private val plugin: Plugin) : Listener {

    private val lastDamageTime = ConcurrentHashMap<Pair<UUID, String>, Long>()
    private val cooldownMs = 400

    @EventHandler
    fun playerInteractGateEvent(event: PlayerInteractEvent) {
        val location = event.clickedBlock?.location ?: return
        val gate = gates.values.firstOrNull { gate ->
            gate.blocks.keys.any { it == location }
        } ?: return
        val player = event.player
        val damageKey = Pair(player.uniqueId, gate.name)
        val currentTime = System.currentTimeMillis()
        val lastTime = lastDamageTime.getOrDefault(damageKey, 0L)
        if (currentTime - lastTime < cooldownMs) return
        lastDamageTime[damageKey] = currentTime
        if (event.action.isLeftClick) {
            if (!gate.attackable) return
            if (!(event.player.fallDistance > 0.0)) return
            val item = event.item?: return
            if (!isBreakItem(item)) return
            event.isCancelled = true
            item.damage(100, event.player)
            gate.damage(1)
            GatePlugin.gateManager.saveGates()
        }
        if (event.action.isRightClick) {
            val kingdom = event.player.getKingdom() ?: return
            if (kingdom == gate.kingdom) {
                gate.toggle()
                GatePlugin.gateManager.saveGates()
            } else {
                event.player.message("<red>Je kan deze poort niet openen, omdat deze poort niet van jouw provincie is.")
            }
        }
    }

    @EventHandler
    fun playerMineGateEvent(event: BlockBreakEvent) {
        val location = event.block.location
        gates.values.firstOrNull { gate ->
            gate.blocks.keys.any { it == location }
        } ?: return
        event.isCancelled = true
    }
}