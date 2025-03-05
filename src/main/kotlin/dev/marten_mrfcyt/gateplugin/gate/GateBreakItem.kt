package dev.marten_mrfcyt.gateplugin.gate

import org.bukkit.Material

data class GateBreakItem(
    val type: Material,
    val customModelData: Int? = null
)
