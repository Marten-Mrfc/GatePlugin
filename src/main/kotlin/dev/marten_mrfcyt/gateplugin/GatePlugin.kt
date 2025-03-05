package dev.marten_mrfcyt.gateplugin

import dev.marten_mrfcyt.gateplugin.gate.GateManager
import dev.marten_mrfcyt.gateplugin.gate.loadBreakItems
import dev.marten_mrfcyt.gateplugin.listeners.GateDamageListener
import mlib.api.architecture.KotlinPlugin
import mlib.api.architecture.extensions.registerEvents

class GatePlugin : KotlinPlugin() {
    companion object {
        lateinit var instance: GatePlugin
        lateinit var gateManager: GateManager
    }

    override fun onEnable() {
        logger.info("-------------------------------")
        logger.info("--- Loading GatePlugin v1.0 ---")
        super.onEnable()

        // Set instance first to ensure it's available for other components
        instance = this
        logger.info("Instance set, loading commands")

        // Initialize gateManager before registering commands
        logger.info("Loading GateManager")
        gateManager = GateManager(this)
        logger.info("GateManager loaded")

        // Now register commands that might use gateManager
        gatePluginCommands()
        logger.info("Commands loaded, loading events")

        registerEvents(GateDamageListener(this))
        logger.info("Events loaded, loading breakItems")
        loadBreakItems()
        logger.info("BreakItems loaded.")
        logger.info("--- Loaded GatePlugin v1.0 ---")
        logger.info("-------------------------------")
    }
    override fun onDisable() {
        gateManager.getAllGates().forEach { gate ->
            gate.getDisplayManager().removeDisplay()
        }
        gateManager.saveGates()
        logger.info("All gate displays removed")
        logger.info("GatePlugin v1.0 has been disabled")
    }
}
