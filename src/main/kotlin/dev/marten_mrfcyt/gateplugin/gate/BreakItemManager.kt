package dev.marten_mrfcyt.gateplugin.gate

import dev.marten_mrfcyt.gateplugin.GatePlugin
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

var breakItems = listOf<GateBreakItem>()


fun loadBreakItems() {
    breakItems = GatePlugin.instance.config.getStringList("breakItems").map {
        val split = it.split(":")
        val material = Material.getMaterial(split[0])!!
        val customModelData = if (split.size > 1 && split[1].trim().lowercase() != "null" && split[1].isNotEmpty()) {
            split[1].toIntOrNull()
        } else {
            null
        }
        GateBreakItem(material, customModelData)
    }
}

fun getBreakItem(item: ItemStack): GateBreakItem? {
    val meta = item.itemMeta ?: return null

    return breakItems.firstOrNull {
        it.type == item.type &&
                (it.customModelData == null || (meta.hasCustomModelData() && it.customModelData == meta.customModelData))
    }
}

fun isBreakItem(item: ItemStack): Boolean {
    return getBreakItem(item) != null
}