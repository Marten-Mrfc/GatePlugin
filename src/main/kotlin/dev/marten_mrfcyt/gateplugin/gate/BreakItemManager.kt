package dev.marten_mrfcyt.gateplugin.gate

import dev.marten_mrfcyt.gateplugin.GatePlugin
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

var breakItems = listOf<GateBreakItem>()


fun loadBreakItems() {
    breakItems = GatePlugin.instance.config.getStringList("breakItems").map {
        val parts = it.split(":")
        val material = Material.getMaterial(parts[0])!!
        val customModelData = if (parts.size > 1 && parts[1].trim().lowercase() != "null" && parts[1].isNotEmpty()) {
            parts[1].toIntOrNull()
        } else {
            null
        }
        val gateDamage = if (parts.size > 2) parts[2].toIntOrNull() ?: 1 else 1
        val maxUses = if (parts.size > 3) parts[3].toIntOrNull() ?: 100 else 100

        GateBreakItem(material, customModelData, gateDamage, maxUses)
    }

    GatePlugin.instance.logger.info("Loaded $breakItems break items")
}

fun getBreakItem(item: ItemStack): GateBreakItem? {
    val meta = item.itemMeta ?: return null

    // First try to match items with custom model data
    if (meta.hasCustomModelData()) {
        val customModelDataMatch = breakItems.firstOrNull {
            it.type == item.type &&
                    it.customModelData != null &&
                    it.customModelData == meta.customModelData
        }

        if (customModelDataMatch != null) {
            return customModelDataMatch
        }
    }

    // If no match with custom model data, fall back to generic items
    return breakItems.firstOrNull {
        it.type == item.type
    }
}

fun isBreakItem(item: ItemStack): Boolean {
    return getBreakItem(item) != null
}