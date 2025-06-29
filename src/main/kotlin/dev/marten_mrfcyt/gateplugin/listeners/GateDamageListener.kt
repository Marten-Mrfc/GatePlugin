package dev.marten_mrfcyt.gateplugin.listeners

import dev.marten_mrfcyt.gateplugin.GatePlugin
import dev.marten_mrfcyt.gateplugin.gate.gates
import dev.marten_mrfcyt.gateplugin.gate.getBreakItem
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
import kotlin.compareTo
import kotlin.text.toInt

class GateDamageListener(private val plugin: Plugin) : Listener {

    private val lastDamageTime = ConcurrentHashMap<Pair<UUID, String>, Long>()
    private val gateToggleCooldowns = ConcurrentHashMap<String, Long>()
    private val damageCooldownMs = 400
    private val toggleCooldownMs = 15000

    @EventHandler
    fun playerInteractGateEvent(event: PlayerInteractEvent) {
        val location = event.clickedBlock?.location ?: return
        val gate = gates.values.firstOrNull { gate ->
            gate.blocks.keys.any { it == location }
        } ?: return
        val player = event.player
        val damageKey = Pair(player.uniqueId, gate.name)
        val currentTime = System.currentTimeMillis()

        if (event.action.isLeftClick) {
            if (player.getKingdom() == gate.kingdom) {
                player.message("<red>Je kan deze poort niet beschadigen, omdat deze poort van jouw provincie is.")
                return
            }

            val lastTime = lastDamageTime.getOrDefault(damageKey, 0L)
            if (currentTime - lastTime < damageCooldownMs) return
            lastDamageTime[damageKey] = currentTime

            if (!gate.attackable) return
            if (!(player.fallDistance > 0.0)) return
            val item = event.item ?: return

            val breakItem = getBreakItem(item) ?: return
            event.isCancelled = true

            gate.damage(breakItem.gateDamage)

            val maxDurability = item.type.maxDurability.toInt()
            val durabilityPerUse = maxDurability / breakItem.maxUses

            item.damage(durabilityPerUse, player)

            GatePlugin.gateManager.saveGates()
        }

        if (event.action.isRightClick) {
            val lastToggleTime = gateToggleCooldowns.getOrDefault(gate.name, 0L)
            if (currentTime - lastToggleTime < toggleCooldownMs) {
                val remainingTime = ((toggleCooldownMs - (currentTime - lastToggleTime)) / 1000).toInt()
                player.message("<red>Deze poort heeft een cooldown van nog $remainingTime seconden.")
                return
            }

            val kingdom = player.getKingdom() ?: return
            if (kingdom == gate.kingdom) {
                gate.toggle()
                gateToggleCooldowns[gate.name] = currentTime
                player.message("<green>Je hebt de poort ${if (gate.isOpen) "geopend" else "gesloten"}! Er is nu een cooldown van 15 seconden.")
                GatePlugin.gateManager.saveGates()
            } else {
                player.message("<red>Je kan deze poort niet openen, omdat deze poort niet van jouw provincie is.")
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