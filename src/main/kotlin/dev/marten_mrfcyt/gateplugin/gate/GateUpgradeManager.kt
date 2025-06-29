package dev.marten_mrfcyt.gateplugin.gate

import dev.marten_mrfcyt.gateplugin.GatePlugin
import dev.marten_mrfcyt.gateplugin.utils.getKingdom
import net.milkbowl.vault.economy.Economy
import nl.jochem.nexus.command.nexus.NexusType
import nl.jochem.nexus.core.NexusManager
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import kotlin.math.cos
import kotlin.math.sin

object GateUpgradeManager {
    // Price constants
    const val TIER_1_PRICE = 1000.0
    const val TIER_2_UPGRADE = 1200.0
    const val TIER_3_UPGRADE = 2000.0
    const val REPAIR_PRICE = 350.0

    // Health values for each tier
    const val TIER_1_HEALTH = 60
    const val TIER_2_HEALTH = 120
    const val TIER_3_HEALTH = 240

    // Nexus tier requirements
    const val TIER_2_NEXUS = 15
    const val TIER_3_NEXUS = 25

    private val economy: Economy? by lazy {
        Bukkit.getServicesManager().getRegistration(Economy::class.java)?.provider
    }

    fun canAfford(player: Player, amount: Double): Boolean {
        return economy?.has(player, amount) ?: false
    }

    fun withdraw(player: Player, amount: Double): Boolean {
        return economy?.withdrawPlayer(player, amount)?.transactionSuccess() ?: false
    }

    fun getPlayerNexusTier(player: Player): Int {
        player.getKingdom()?.let { kingdom ->
            return NexusManager.localNexussen.nexus.firstOrNull {
                it.kingdomID == kingdom.id && it.type == NexusType.advanced
            }?.tier ?: 0
        }
        return 0
    }

    fun canUpgradeToTier2(player: Player): Pair<Boolean, String> {
        val nexusTier = getPlayerNexusTier(player)
        return if (nexusTier < TIER_2_NEXUS) {
            Pair(false, "Je nexus moet minimaal tier $TIER_2_NEXUS zijn (Huidig: $nexusTier)")
        } else if (!canAfford(player, TIER_2_UPGRADE)) {
            Pair(false, "Je hebt ${TIER_2_UPGRADE.toInt()} gulden nodig om te upgraden naar tier 2")
        } else {
            Pair(true, "")
        }
    }

    fun canUpgradeToTier3(player: Player): Pair<Boolean, String> {
        val nexusTier = getPlayerNexusTier(player)
        return if (nexusTier < TIER_3_NEXUS) {
            Pair(false, "Je nexus moet minimaal tier $TIER_3_NEXUS zijn (Huidig: $nexusTier)")
        } else if (!canAfford(player, TIER_3_UPGRADE)) {
            Pair(false, "Je hebt ${TIER_3_UPGRADE.toInt()} gulden nodig om te upgraden naar tier 3")
        } else {
            Pair(true, "")
        }
    }

fun upgradeGate(gate: Gate, player: Player): Pair<Boolean, String> {
    val currentTier = gate.tier

    when (currentTier) {
        1 -> {
            val (canUpgrade, reason) = canUpgradeToTier2(player)
            if (!canUpgrade) return Pair(false, reason)

            if (withdraw(player, TIER_2_UPGRADE)) {
                // Update gate to tier 2
                gate.maxHealth = TIER_2_HEALTH
                gate.currentHealth = TIER_2_HEALTH
                playUpgradeEffects(gate, 2)
                gate.getDisplayManager().updateDisplay()
                GatePlugin.gateManager.saveGates()
                return Pair(true, "Poort geüpgraded naar tier 2")
            }
            return Pair(false, "Transactie mislukt")
        }
        2 -> {
            val (canUpgrade, reason) = canUpgradeToTier3(player)
            if (!canUpgrade) return Pair(false, reason)

            if (withdraw(player, TIER_3_UPGRADE)) {
                // Update gate to tier 3
                gate.maxHealth = TIER_3_HEALTH
                gate.currentHealth = TIER_3_HEALTH
                playUpgradeEffects(gate, 3)
                gate.getDisplayManager().updateDisplay()
                GatePlugin.gateManager.saveGates()
                return Pair(true, "Poort geüpgraded naar tier 3")
            }
            return Pair(false, "Transactie mislukt")
        }
        else -> return Pair(false, "Poort is al op het maximale tier")
    }
}

private fun playUpgradeEffects(gate: Gate, tier: Int) {
    val location = gate.location
    val world = location.world
    val plugin = GatePlugin.instance

    // Schedule a sequence of effects
    for (i in 0 until 20) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            // Spiral ascending effect
            val angle = i * Math.PI / 10
            val radius = 1.5
            val xOffset = radius * cos(angle)
            val zOffset = radius * sin(angle)
            val particleLocation = location.clone().add(0.5 + xOffset, i * 0.15, 0.5 + zOffset)

            // Different particles based on tier
            when (tier) {
                2 -> world.spawnParticle(
                    Particle.TOTEM_OF_UNDYING, particleLocation, 3, 0.1, 0.1, 0.1, 0.05
                )
                3 -> world.spawnParticle(
                    Particle.SOUL_FIRE_FLAME, particleLocation, 3, 0.1, 0.1, 0.1, 0.02
                )
            }

            // Play sound at specific intervals
            if (i % 5 == 0) {
                val pitch = 0.8f + (i * 0.05f)
                world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, pitch)
            }
        }, i.toLong())
    }

    // Final explosion effect
    plugin.server.scheduler.runTaskLater(plugin, Runnable {
        world.spawnParticle(Particle.EXPLOSION, location.clone().add(0.5, 1.0, 0.5), 1, 0.0, 0.0, 0.0, 0.0)

        // Highlight blocks
        gate.blocks.keys.forEach { blockLoc ->
            world.spawnParticle(
                if (tier == 2) Particle.END_ROD else Particle.SOUL,
                blockLoc.clone().add(0.5, 0.5, 0.5),
                5, 0.3, 0.3, 0.3, 0.05
            )
        }

        // Final sounds
        world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.7f)
        world.playSound(location, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f)

        // Flash the display text
        gate.getDisplayManager().refreshDisplay()
    }, 22L)
}

    fun repairGate(gate: Gate, player: Player): Pair<Boolean, String> {
        if (gate.currentHealth == gate.maxHealth) {
            return Pair(false, "Poort heeft al volledige health")
        }

        if (!canAfford(player, REPAIR_PRICE)) {
            return Pair(false, "Je hebt ${REPAIR_PRICE.toInt()} gulden nodig om deze poort te repareren")
        }

        if (withdraw(player, REPAIR_PRICE)) {
            gate.reset()
            return Pair(true, "Poort succesvol gerepareerd")
        }

        return Pair(false, "Transactie mislukt")
    }
}
