package dev.marten_mrfcyt.gateplugin.gate

import com.google.gson.GsonBuilder
import com.gufli.kingdomcraft.api.KingdomCraftProvider
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import dev.marten_mrfcyt.gateplugin.GatePlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

private data class GateData(
    val name: String,
    val location: LocationData,
    val currentHealth: Int,
    val maxHealth: Int,
    val blocks: List<SerializedBlock>,
    val kingdomName: String,
    val attackable: Boolean,
    val isOpen: Boolean = false
)

private data class LocationData(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
)

private data class SerializedBlock(
    val location: LocationData,
    val data: String
)
val gates = mutableMapOf<String, Gate>()

class GateManager(private val plugin: JavaPlugin) {

    private val gateFile = File(plugin.dataFolder, "gates.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var gatesLoaded = false

    init {
        plugin.dataFolder.mkdirs()
        loadGates()
    }

    private fun getSelectionCenter(selection: com.sk89q.worldedit.regions.Region): Location {
        val center = selection.center
        return Location(
            BukkitAdapter.adapt(selection.world),
            center.x(),
            center.y(),
            center.z()
        )
    }

    fun saveGate(name: String, kingdom: String, maxHealth: Int, attackable: Boolean, player: Player) {
        val selection = (plugin.server.pluginManager.getPlugin("WorldEdit") as WorldEditPlugin)
            .getSession(player)
            .getSelection(BukkitAdapter.adapt(player.world))
            ?: throw IllegalStateException("No WorldEdit selection")

        val blocks = selection.associate { vec ->
            player.world.getBlockAt(vec.x(), vec.y(), vec.z())
                .let { it.location to it.blockData }
        }

        val kingdomObj = KingdomCraftProvider.get().getKingdom(kingdom)
            ?: throw IllegalArgumentException("Kingdom $kingdom not found")

        gates[name] = Gate(
            name = name,
            location = getSelectionCenter(selection),
            _currentHealth = maxHealth,
            maxHealth = maxHealth,
            blocks = blocks,
            kingdom = kingdomObj,
            attackable = attackable,
            isOpen = false
        )
        saveGates()
    }

    private fun loadGates() {
        if (gatesLoaded) {
            plugin.logger.info("Gates already loaded, skipping...")
            return
        }

        if (!gateFile.exists()) {
            plugin.logger.info("Gates file does not exist")
            gatesLoaded = true
            return
        }
        try {
            val gatesData = gson.fromJson(gateFile.readText(), Array<GateData>::class.java).toList()
            plugin.logger.info("Loaded ${gatesData.size} gates from file")

            gatesData.forEach { data ->
                plugin.logger.info("Loading gate: ${data.name}")

                val world = Bukkit.getWorld(data.location.world) ?: run {
                    plugin.logger.warning("World ${data.location.world} not found for gate ${data.name}")
                    return@forEach
                }

                val location = Location(world, data.location.x, data.location.y, data.location.z,
                    data.location.yaw, data.location.pitch)

                val blocks = data.blocks.associate { block ->
                    val blockWorld = Bukkit.getWorld(block.location.world) ?: run {
                        plugin.logger.warning("World ${block.location.world} not found for block in gate ${data.name}")
                        return@forEach
                    }
                    val blockLoc = Location(blockWorld, block.location.x, block.location.y, block.location.z)
                    blockLoc to Bukkit.createBlockData(block.data)
                }

                val kingdom = KingdomCraftProvider.get().getKingdom(data.kingdomName) ?: run {
                    plugin.logger.warning("Kingdom ${data.kingdomName} not found for gate ${data.name}")
                    return@forEach
                }

                gates[data.name] = Gate(
                    name = data.name,
                    location = location,
                    _currentHealth = data.currentHealth,
                    maxHealth = data.maxHealth,
                    blocks = blocks,
                    kingdom = kingdom,
                    attackable = data.attackable,
                    isOpen = data.isOpen
                )
                plugin.logger.info("Successfully loaded gate: ${data.name}")
            }
            plugin.logger.info("Finished loading gates, total: ${gates.size}")
            gatesLoaded = true
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load gates: ${e.message}")
            e.printStackTrace()
        }
    }
    fun deleteGate(name: String) {
        gates.remove(name)
        GateDisplayManager(gates[name]!!, plugin).removeDisplay()
        saveGates()
    }

    fun saveGates() {
        try {
            val gateDataList = gates.values.map { gate ->
                GateData(
                    name = gate.name,
                    location = LocationData(
                        gate.location.world?.name ?: "world",
                        gate.location.x,
                        gate.location.y,
                        gate.location.z,
                        gate.location.yaw,
                        gate.location.pitch
                    ),
                    currentHealth = gate.currentHealth,
                    maxHealth = gate.maxHealth,
                    blocks = gate.blocks.map { (loc, data) ->
                        SerializedBlock(
                            LocationData(
                                loc.world?.name ?: "world",
                                loc.x,
                                loc.y,
                                loc.z,
                                0f,
                                0f
                            ),
                            data.asString
                        )
                    },
                    kingdomName = gate.kingdom.name,
                    attackable = gate.attackable,
                    isOpen = gate.isOpen
                )
            }
            gateFile.writeText(gson.toJson(gateDataList))
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save gates: ${e.message}")
        }
    }

    fun getGate(name: String): Gate? = gates[name]
    fun getAllGates(): Collection<Gate> = gates.values
}