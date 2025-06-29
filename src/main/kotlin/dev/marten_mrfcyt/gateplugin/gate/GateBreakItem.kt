package dev.marten_mrfcyt.gateplugin.gate

import org.bukkit.Material

data class GateBreakItem(
    val type: Material,
    val customModelData: Int? = null,
    val gateDamage: Int = 1,
    val maxUses: Int = 100
)