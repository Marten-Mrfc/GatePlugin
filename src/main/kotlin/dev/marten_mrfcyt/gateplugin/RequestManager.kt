package dev.marten_mrfcyt.gateplugin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.gufli.kingdomcraft.api.KingdomCraft
import com.gufli.kingdomcraft.api.domain.Kingdom
import dev.marten_mrfcyt.gateplugin.gate.GateRequest
import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*

class RequestManager(private val plugin: GatePlugin) {
    private val requestsFile = File(plugin.dataFolder, "gateRequests.json")
    private val requests = mutableListOf<GateRequest>()
    private val gson = GsonBuilder()
        .registerTypeAdapter(Location::class.java, LocationAdapter())
        .registerTypeAdapter(UUID::class.java, UUIDAdapter())
        .registerTypeAdapter(Kingdom::class.java, KingdomAdapter())
        .setPrettyPrinting()
        .create()

    init {
        loadRequests()
    }

    fun addRequest(request: GateRequest) {
        requests.add(request)
        saveRequests()
    }

    fun removeRequest(id: UUID) {
        requests.removeIf { it.id == id }
        saveRequests()
    }

    fun getRequests(): List<GateRequest> {
        return requests.toList()
    }

    private fun loadRequests() {
        if (!requestsFile.exists()) {
            plugin.dataFolder.mkdirs()
            requestsFile.createNewFile()
            return
        }

        try {
            FileReader(requestsFile).use { reader ->
                val type = object : TypeToken<List<GateRequest>>() {}.type
                val loadedRequests: List<GateRequest> = gson.fromJson(reader, type) ?: emptyList()
                requests.clear()
                requests.addAll(loadedRequests)
                plugin.logger.info("Loaded ${requests.size} gate requests")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load gate requests: ${e.message}")
        }
    }

    fun saveRequests() {
        try {
            requestsFile.parentFile.mkdirs()
            FileWriter(requestsFile).use { writer ->
                gson.toJson(requests, writer)
            }
            plugin.logger.info("Saved ${requests.size} gate requests")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save gate requests: ${e.message}")
        }
    }

    // Custom adapters for Location and UUID serialization
    private class LocationAdapter : com.google.gson.JsonSerializer<Location>, com.google.gson.JsonDeserializer<Location> {
        override fun serialize(src: Location, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext): com.google.gson.JsonElement {
            val obj = com.google.gson.JsonObject()
            obj.addProperty("world", src.world?.name)
            obj.addProperty("x", src.x)
            obj.addProperty("y", src.y)
            obj.addProperty("z", src.z)
            obj.addProperty("yaw", src.yaw)
            obj.addProperty("pitch", src.pitch)
            return obj
        }

        override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): Location {
            val obj = json.asJsonObject
            val worldName = obj.get("world").asString
            val world = Bukkit.getWorld(worldName)
            val x = obj.get("x").asDouble
            val y = obj.get("y").asDouble
            val z = obj.get("z").asDouble
            val yaw = obj.get("yaw").asFloat
            val pitch = obj.get("pitch").asFloat
            return Location(world, x, y, z, yaw, pitch)
        }
    }

    private class UUIDAdapter : com.google.gson.JsonSerializer<UUID>, com.google.gson.JsonDeserializer<UUID> {
        override fun serialize(src: UUID, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext): com.google.gson.JsonElement {
            return com.google.gson.JsonPrimitive(src.toString())
        }

        override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): UUID {
            return UUID.fromString(json.asString)
        }
    }

    private class KingdomAdapter : com.google.gson.JsonSerializer<Kingdom>, com.google.gson.JsonDeserializer<Kingdom> {
        override fun serialize(src: Kingdom, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext): com.google.gson.JsonElement {
            val obj = com.google.gson.JsonObject()
            obj.addProperty("name", src.name)
            return obj
        }

        override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): Kingdom {
            val obj = json.asJsonObject
            val kingdomName = obj.get("name").asString

            // Get the kingdom by name from the KingdomCraft API
            val kingdomCraft = GatePlugin.instance.server.servicesManager.getRegistration(KingdomCraft::class.java)?.provider
                ?: throw IllegalStateException("KingdomCraft service not available")

            return kingdomCraft.getKingdom(kingdomName)
                ?: throw IllegalStateException("Kingdom with name $kingdomName not found")
        }
    }
}