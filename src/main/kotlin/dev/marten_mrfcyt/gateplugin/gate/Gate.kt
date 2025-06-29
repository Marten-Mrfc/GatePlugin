package dev.marten_mrfcyt.gateplugin.gate

import com.gufli.kingdomcraft.api.domain.Kingdom
import dev.marten_mrfcyt.gateplugin.GatePlugin
import org.bukkit.Effect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.data.BlockData
import java.util.UUID

data class Gate(
    val name: String,
    val location: Location,
    private var _currentHealth: Int,
    var maxHealth: Int,
    val blocks: Map<Location, BlockData>,
    val kingdom: Kingdom,
    var attackable: Boolean,
    var displayId: UUID? = null,
    var interactionId: UUID? = null,
    var isOpen: Boolean = false
) {
    val tier: Int
        get() = when {
            maxHealth <= 60 -> 1
            maxHealth <= 120 -> 2
            else -> 3
    }
    var currentHealth: Int
        get() = _currentHealth
        set(value) {
            _currentHealth = value.coerceIn(0, maxHealth)
        }

    private val displayManager = GateDisplayManager(this, GatePlugin.instance)

    init {
        currentHealth = _currentHealth
        displayManager.initDisplay()
    }

    fun damage(amount: Int) {
        if (!attackable || isDestroyed()) return

        val previousHealth = currentHealth
        currentHealth -= amount
        displayId = null
        displayManager.removeDisplay()
        displayManager.initDisplay()
        displayManager.updateDisplay()

        val damagePercentage = amount.toFloat() / maxHealth
        val healthPercentage = currentHealth.toFloat() / maxHealth

        playDamageEffects(damagePercentage)

        if (healthPercentage < 0.25f && previousHealth > maxHealth * 0.25f) {
            location.world.playSound(location, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.5f)
        }

        GatePlugin.gateManager.saveGates()

        if (currentHealth <= 0) {
            destroyGate()
        }
    }

    private fun playDamageEffects(damagePercentage: Float) {
        val effectIntensity = (damagePercentage * 15).toInt().coerceAtLeast(3)
        val randomBlocks = blocks.keys.shuffled().take(3 + effectIntensity)

        randomBlocks.forEach { loc ->
            loc.world.playEffect(loc, Effect.STEP_SOUND, loc.block.type)
            loc.world.spawnParticle(Particle.BLOCK, loc, effectIntensity, 0.5, 0.5, 0.5, 0.1, loc.block.blockData)
        }

        val volume = 0.8f + (damagePercentage * 0.4f).coerceAtMost(0.2f)
        val pitch = 0.8f - (damagePercentage * 0.3f)
        location.world.playSound(location, Sound.BLOCK_STONE_HIT, volume, pitch)

        if (damagePercentage > 0.25f) {
            location.world.playSound(location, Sound.BLOCK_STONE_BREAK, 1.0f, 0.7f)
            location.world.spawnParticle(Particle.WHITE_SMOKE, location, 3, 0.5, 0.5, 0.5, 0.1)
        }
    }

    private fun destroyGate() {
        blocks.keys.forEach { loc ->
            loc.world.spawnParticle(Particle.EXPLOSION, loc, 1, 0.3, 0.3, 0.3, 0.1)
            loc.world.playEffect(loc, Effect.STEP_SOUND, loc.block.type)
            loc.world.spawnParticle(Particle.BLOCK, loc, 10, 0.5, 0.5, 0.5, 0.2, loc.block.blockData)
            loc.block.type = Material.AIR
        }

        location.world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f)
        location.world.playSound(location, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.5f)

        displayManager.removeDisplay()
        displayId = null
        currentHealth = 0
        GatePlugin.gateManager.saveGates()
    }

    fun reset() {
        // Play initial repair sound
        location.world.playSound(location, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f)

        // Reset blocks with effects
        blocks.forEach { (loc, data) ->
            // Schedule block restoration with slight delay for visual effect
            GatePlugin.instance.server.scheduler.runTaskLater(GatePlugin.instance, Runnable {
                loc.block.blockData = data

                // Spawn repair particles at each block
                loc.world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    loc.clone().add(0.5, 0.5, 0.5),
                    5, 0.3, 0.3, 0.3, 0.05
                )

                // Add some block placing effects
                loc.world.playEffect(loc, Effect.STEP_SOUND, data.material)
                loc.world.playSound(loc, Sound.BLOCK_STONE_PLACE, 0.2f, 1.0f)
            }, (Math.random() * 10).toLong())
        }

        // Central repair effect
        location.world.spawnParticle(
            Particle.TOTEM_OF_UNDYING,
            location.clone().add(0.5, 1.0, 0.5),
            15, 0.5, 1.0, 0.5, 0.2
        )

        // Additional effects after short delay
        GatePlugin.instance.server.scheduler.runTaskLater(GatePlugin.instance, Runnable {
            location.world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f)
            location.world.spawnParticle(
                Particle.END_ROD,
                location.clone().add(0.5, 1.5, 0.5),
                10, 0.5, 0.5, 0.5, 0.1
            )
        }, 15L)

        // Update gate state
        displayId = null
        displayManager.removeDisplay()
        displayManager.initDisplay()
        currentHealth = maxHealth
        displayManager.updateDisplay()
        GatePlugin.gateManager.saveGates()
    }

    fun refresh() {
        val updatedGate = gates[name] ?: return
        this.attackable = updatedGate.attackable
        this._currentHealth = updatedGate.currentHealth
        displayId = null
        displayManager.removeDisplay()
        displayManager.initDisplay()
        displayManager.updateDisplay()
        GatePlugin.gateManager.saveGates()
        GatePlugin.instance.logger.info("Gate $name refreshed with updated data")
    }

    fun isDestroyed() = currentHealth <= 0

    fun getDisplayManager() = displayManager

    fun open(): Boolean {
        if (isDestroyed()) return false

        val blocksByY = blocks.entries.groupBy { it.key.blockY }.toSortedMap()

        val maxLevelsToRemove = 4
        val levelsToRemove = blocksByY.keys.toList()
            .take(minOf(blocksByY.size - 1, maxLevelsToRemove))
        val plugin = GatePlugin.instance

        for ((index, y) in levelsToRemove.withIndex()) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                blocksByY[y]?.forEach { (loc, _) ->
                    loc.world.playEffect(loc, Effect.STEP_SOUND, loc.block.type)
                    loc.world.spawnParticle(Particle.CLOUD, loc, 5, 0.3, 0.3, 0.3, 0.02)
                    loc.block.type = Material.AIR
                }

                if (blocksByY[y]?.isNotEmpty() == true) {
                    val pitch = 0.8f + (index.toFloat() / levelsToRemove.size * 0.4f)
                    location.world.playSound(location, Sound.BLOCK_CHAIN_BREAK, 0.3f, pitch)
                }
            }, index * 10L)
        }

        location.world.playSound(location, Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 0.8f)
        isOpen = true
        return true
    }

    fun close(): Boolean {
        if (isDestroyed() || !isOpen) return false

        val blocksByY = blocks.entries.groupBy { it.key.blockY }.toSortedMap(reverseOrder())
        val plugin = GatePlugin.instance

        location.world.playSound(location, Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.8f)

        for ((index, entry) in blocksByY.entries.withIndex()) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                entry.value.forEach { (loc, data) ->
                    loc.world.spawnParticle(Particle.CLOUD, loc, 3, 0.2, 0.2, 0.2, 0.02)
                    loc.block.blockData = data
                    loc.world.playEffect(loc, Effect.STEP_SOUND, data.material)
                }

                if (entry.value.isNotEmpty()) {
                    val pitch = 1.2f - (index.toFloat() / blocksByY.size * 0.4f)
                    location.world.playSound(location, Sound.BLOCK_CHAIN_FALL, 0.4f, pitch)
                }
            }, index * 2L)
        }

        isOpen = false
        return true
    }

    fun toggle() = if (isOpen) close() else open()
}