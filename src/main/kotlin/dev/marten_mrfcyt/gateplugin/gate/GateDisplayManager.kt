package dev.marten_mrfcyt.gateplugin.gate

import mlib.api.utilities.asMini
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt

class GateDisplayManager(private val gate: Gate, private val plugin: JavaPlugin) {
    private var healthDisplay: TextDisplay? = null
    private val totalBars = 10
    private val viewDistance = 10.0 // Distance in blocks at which players can see the display
    val displayVisibilityTracker = DisplayVisibilityTracker(plugin)

    // Unique keys for persistent data
    private val gateIdKey = NamespacedKey(plugin, "gate_id")
    private val gateDisplayKey = NamespacedKey(plugin, "gate_display")

    init {
        // Register the tracker if not already registered
        if (!displayVisibilityTracker.isRegistered) {
            plugin.server.pluginManager.registerEvents(displayVisibilityTracker, plugin)
            displayVisibilityTracker.isRegistered = true
        }
    }

    fun initDisplay() {

        try {
            // Clean up any duplicate displays for this gate
            cleanupExistingDisplays().thenRun {
                // Try to find existing display by UUID if available
                if (gate.displayId != null) {
                    val world = gate.location.world ?: return@thenRun

                    for (entity in world.entities) {
                        if (entity.uniqueId == gate.displayId && entity is TextDisplay) {
                            healthDisplay = entity
                            validateDisplay(entity)
                            updateDisplay()
                            // Register the display with the tracker
                            displayVisibilityTracker.registerDisplay(entity, gate.location)
                            return@thenRun
                        }
                    }
                }

                // Try to find by persistent data
                findDisplayByPersistentData()?.let {
                    healthDisplay = it
                    gate.displayId = it.uniqueId
                    validateDisplay(it)
                    updateDisplay()
                    // Register the display with the tracker
                    displayVisibilityTracker.registerDisplay(it, gate.location)
                    return@thenRun
                }

                // Create new if none found
                createNewDisplay()
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize display for gate ${gate.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun validateDisplay(display: TextDisplay) {
        // Ensure display has correct settings
        display.billboard = Display.Billboard.CENTER
        display.isDefaultBackground = false
        display.isSeeThrough = true
        display.isShadowed = true
        display.viewRange = Float.MAX_VALUE // Make it visible at any distance
        display.alignment = TextDisplay.TextAlignment.CENTER

        // Ensure position is correct
        val displayLoc = calculateDisplayPosition()
        if (display.location.distance(displayLoc) > 1.0) {
            display.teleport(displayLoc)
        }

        // Ensure persistent data is set correctly
        val pdc = display.persistentDataContainer
        pdc.set(gateIdKey, PersistentDataType.STRING, gate.name)
        pdc.set(gateDisplayKey, PersistentDataType.BYTE, 1)
    }

    private fun cleanupExistingDisplays(): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        val world = gate.location.world

        if (world == null) {
            future.complete(null)
            return future
        }

        plugin.server.scheduler.runTask(plugin, Runnable {
            try {
                var count = 0
                val radius = 5.0
                val center = gate.location

                // Find displays with this gate's name in PDC
                world.getNearbyEntities(center, radius, radius, radius) { entity ->
                    if (entity is TextDisplay && entity.uniqueId != gate.displayId) {
                        val pdc = entity.persistentDataContainer
                        val gateName = pdc.get(gateIdKey, PersistentDataType.STRING)

                        if (gateName == gate.name) {
                            // Unregister from tracker before removing
                            displayVisibilityTracker.unregisterDisplay(entity.uniqueId)
                            entity.remove()
                            count++
                            return@getNearbyEntities true
                        }
                    }
                    return@getNearbyEntities false
                }

                if (count > 0) {
                    plugin.logger.info("Cleaned up $count duplicate displays for gate ${gate.name}")
                }

                future.complete(null)
            } catch (e: Exception) {
                plugin.logger.warning("Error cleaning up displays: ${e.message}")
                future.complete(null)
            }
        })

        return future
    }

    private fun findDisplayByPersistentData(): TextDisplay? {
        val world = gate.location.world ?: return null
        val radius = 3.0
        val center = gate.location

        return world.getNearbyEntities(center, radius, radius, radius)
            .filterIsInstance<TextDisplay>()
            .firstOrNull {
                val pdc = it.persistentDataContainer
                val gateName = pdc.get(gateIdKey, PersistentDataType.STRING)
                gateName == gate.name
            }
    }

    private fun calculateDisplayPosition(): Location {
        return gate.location.clone().add(0.5, 1.5, 0.5)
    }

    private fun createNewDisplay() {
        val displayLoc = calculateDisplayPosition()
        val world = displayLoc.world ?: return

        plugin.server.scheduler.runTask(plugin, Runnable {
            world.spawn(displayLoc, TextDisplay::class.java).also { display ->
                display.billboard = Display.Billboard.CENTER
                display.isDefaultBackground = false
                display.isSeeThrough = true
                display.isShadowed = true
                display.viewRange = Float.MAX_VALUE // Make it visible at any distance
                display.alignment = TextDisplay.TextAlignment.CENTER

                // Set persistent data for recovery after restart
                val pdc = display.persistentDataContainer
                pdc.set(gateIdKey, PersistentDataType.STRING, gate.name)
                pdc.set(gateDisplayKey, PersistentDataType.BYTE, 1)
                plugin.server.onlinePlayers.forEach { player ->
                    player.hideEntity(plugin, display)
                }
                healthDisplay = display
                gate.displayId = display.uniqueId

                // Register with tracker
                displayVisibilityTracker.registerDisplay(display, gate.location)

                updateDisplay()
                plugin.logger.info("Created new display for gate ${gate.name}")
            }
        })
    }

    fun updateDisplay() {
        val nearbyDisplay = findNearbyDisplay()
        if (nearbyDisplay != null) {
            healthDisplay = nearbyDisplay
            gate.displayId = nearbyDisplay.uniqueId
            validateDisplay(nearbyDisplay)
            // Register with tracker if it's a newly found display
            displayVisibilityTracker.registerDisplay(nearbyDisplay, gate.location)
        }

        val percentage = (gate.currentHealth.toDouble() / gate.maxHealth * 100).roundToInt()
        val filledBars = ((gate.currentHealth.toDouble() / gate.maxHealth) * totalBars).roundToInt().coerceIn(0, totalBars)
        val emptyBars = totalBars - filledBars

        val color = when {
            percentage > 80 -> "<green>"
            percentage > 60 -> "<yellow>"
            percentage > 40 -> "<gold>"
            percentage > 20 -> "<red>"
            else -> "<dark_red>"
        }

        val lockStatus = if (!gate.attackable) " <dark_gray>[<red>Locked<dark_gray>]" else ""

        val healthBar = buildString {
            append("<white>${gate.name}$lockStatus")
            if (gate.kingdom.name.isNotEmpty()) {
                append(" <gray>(${gate.kingdom.name})")
            }
            append("\n")
            append(color)
            repeat(filledBars) { append("█") }
            append("<dark_gray>")
            repeat(emptyBars) { append("░") }
            append(" <white>${gate.currentHealth}/${gate.maxHealth}")
        }

        try {
            healthDisplay?.text(healthBar.asMini())
        } catch (e: Exception) {
            plugin.logger.warning("Failed to update display for gate ${gate.name}: ${e.message}")
            initDisplay() // Try to recover by reinitializing
        }
    }

    private fun findNearbyDisplay(): TextDisplay? {
        val world = gate.location.world ?: return null
        val center = calculateDisplayPosition()
        val radius = 0.5 // Small radius to find very nearby displays

        return world.getNearbyEntities(center, radius, radius, radius)
            .filterIsInstance<TextDisplay>()
            .firstOrNull { display ->
                val pdc = display.persistentDataContainer
                pdc.has(gateIdKey, PersistentDataType.STRING) &&
                        pdc.get(gateIdKey, PersistentDataType.STRING) == gate.name
            }
    }

    fun removeDisplay() {
        try {
            val displayId = gate.displayId
            val display = healthDisplay

            // Hide the display from all online players first
            if (display != null) {
                plugin.server.onlinePlayers.forEach { player ->
                    player.hideEntity(plugin, display)
                }
            }

            // Unregister from tracker
            if (displayId != null) {
                displayVisibilityTracker.unregisterDisplay(displayId)
            }

            // Find and remove by UUID if we have it
            if (displayId != null) {
                gate.location.world?.entities
                    ?.filter { it.uniqueId == displayId }
                    ?.forEach(Entity::remove)
            }
            // Clean up references
            healthDisplay = null
            gate.displayId = null
            // Find and remove by persistent data as backup
            gate.location.world?.let { world ->
                val radius = 3.0
                world.getNearbyEntities(gate.location, radius, radius, radius)
                    .filterIsInstance<TextDisplay>()
                    .filter {
                        it.persistentDataContainer.has(gateIdKey, PersistentDataType.STRING) &&
                                it.persistentDataContainer.get(gateIdKey, PersistentDataType.STRING) == gate.name
                    }
                    .forEach { entity ->
                        // Hide from all players before removing
                        plugin.server.onlinePlayers.forEach { player ->
                            player.hideEntity(plugin, entity)
                        }
                        // Unregister from tracker before removing
                        displayVisibilityTracker.unregisterDisplay(entity.uniqueId)
                        entity.remove()
                    }
            }
            plugin.logger.info("Display removed for gate ${gate.name}")
        } catch (e: Exception) {
            plugin.logger.warning("Error removing display for gate ${gate.name}: ${e.message}")
        }
    }
    // Convenience method for reloading the display
    fun refreshDisplay() {
        removeDisplay()
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            initDisplay()
        }, 5L)
    }
}

// Class to handle visibility of displays based on player proximity
class DisplayVisibilityTracker(private val plugin: JavaPlugin) : Listener {
    // Map of display UUIDs to their locations
    private val displayLocations = mutableMapOf<UUID, Location>()
    // Map to track which displays are visible to which players
    private val playerVisibility = mutableMapOf<UUID, MutableSet<UUID>>()
    // The view distance threshold
    private val viewDistance = 10.0 * 10.0 // Squared distance for efficiency

    var isRegistered = false

    fun registerDisplay(display: TextDisplay, location: Location) {
        displayLocations[display.uniqueId] = location

        // Hide from all players initially, then show only to nearby players
        plugin.server.onlinePlayers.forEach { player ->
            player.hideEntity(plugin, display)
        }

    }

    fun unregisterDisplay(displayId: UUID) {
        displayLocations.remove(displayId)
        // Clean up player visibility tracking
        playerVisibility.values.forEach { it.remove(displayId) }
    }
}