package dev.marten_mrfcyt.gateplugin.gate

import mlib.api.utilities.asMini
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.roundToInt

class GateDisplayManager(private val gate: Gate, private val plugin: JavaPlugin) {
    private var healthDisplay: TextDisplay? = null
    private val totalBars = 10

    fun initDisplay() {
        plugin.logger.info("Initializing display for gate: ${gate.name}")

        try {
            // Try to find existing display
            if (gate.displayId != null) {
                gate.location.world?.entities
                    ?.filterIsInstance<TextDisplay>()
                    ?.firstOrNull { it.uniqueId == gate.displayId }
                    ?.let {
                        healthDisplay = it
                        updateDisplayPosition()
                        updateDisplay()
                        return
                    }
            }

            // Create new display
            createNewDisplay()
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize display for gate ${gate.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun calculateDisplayPosition() = gate.location.clone().add(0.5, 0.5, 0.5)

    private fun createNewDisplay() {
        val displayLoc = calculateDisplayPosition()
        displayLoc.world?.spawn(displayLoc, TextDisplay::class.java)?.also {
            it.billboard = Display.Billboard.CENTER
            it.isDefaultBackground = false
            it.isSeeThrough = true
            it.isShadowed = true
            it.viewRange = 48f
            it.alignment = TextDisplay.TextAlignment.CENTER  // Center the text horizontally

            healthDisplay = it
            gate.displayId = it.uniqueId
            updateDisplay()
        } ?: run {
            plugin.logger.warning("Failed to spawn TextDisplay for gate ${gate.name}")
        }
    }

    private fun updateDisplayPosition() {
        healthDisplay?.teleport(calculateDisplayPosition())
    }

    fun updateDisplay() {
        if (healthDisplay == null) {
            initDisplay()
            return
        }

        val percentage = (gate.currentHealth.toDouble() / gate.maxHealth * 100).roundToInt()
        val filledBars = ((gate.currentHealth.toDouble() / gate.maxHealth) * totalBars).roundToInt()
        val emptyBars = totalBars - filledBars

        val color = when {
            percentage > 80 -> "<green>"
            percentage > 60 -> "<yellow>"
            percentage > 40 -> "<gold>"
            percentage > 20 -> "<red>"
            else -> "<dark_red>"
        }

        val healthBar = buildString {
            append("<white>${gate.name}")
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
            plugin.logger.fine("Updated display for gate ${gate.name}: ${gate.currentHealth}/${gate.maxHealth}")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to update display for gate ${gate.name}: ${e.message}")
        }
    }

    fun removeDisplay() {
        try {
            healthDisplay?.getNearbyEntities(0.1, 0.1, 0.1)?.forEach { if(it is TextDisplay) { it.remove() } }
            gate.location.world?.entities?.removeIf { it.uniqueId == gate.displayId }
            healthDisplay?.remove()
            gate.displayId = null
            healthDisplay = null
            plugin.logger.info("Display removed for gate ${gate.name}")
        } catch (e: Exception) {
            plugin.logger.warning("Error removing display for gate ${gate.name}: ${e.message}")
        }
    }
}