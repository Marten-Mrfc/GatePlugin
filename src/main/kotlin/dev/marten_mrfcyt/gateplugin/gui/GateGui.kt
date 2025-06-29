package dev.marten_mrfcyt.gateplugin.gui

import dev.marten_mrfcyt.gateplugin.gate.Gate
import dev.marten_mrfcyt.gateplugin.gui.gategui.GateDeleteGui
import dev.marten_mrfcyt.gateplugin.gui.gategui.GateRepairGui
import dev.marten_mrfcyt.gateplugin.gui.gategui.GateUpgradeGui
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.entity.Player

class GateGui(private val gate: Gate, private val player: Player) {
    init {
        val material = when {
            gate.isDestroyed() -> Material.DAMAGED_ANVIL
            !gate.attackable -> Material.IRON_BARS
            !gate.isOpen -> Material.RED_CONCRETE
            else -> Material.LIME_CONCRETE
        }

        val statusColor = when {
            gate.isDestroyed() -> "<dark_red>"
            !gate.attackable -> "<gold>"
            !gate.isOpen -> "<red>"
            else -> "<green>"
        }

        val status = when {
            gate.isDestroyed() -> "Vernietigd"
            !gate.attackable -> "Locked"
            !gate.isOpen -> "Gesloten"
            else -> "Open"
        }

        val standardGui = StandardGuiBuilder()
            .title("Gate: ${gate.name}".asMini())
            .size(GuiSize.ROW_THREE)
            .setup { gui ->
                // Main gate info
                gui.item(material) {
                    name("<white>${gate.name}".asMini())
                    description(listOf(
                        "<gray>Tier: <white>${gate.tier}".asMini(),
                        "<gray>Status: $statusColor$status".asMini(),
                        "<gray>Health: <white>${gate.currentHealth}/${gate.maxHealth} HP".asMini()
                    ))
                    slots(22)

                    onClick { event ->
                        event.isCancelled = true
                    }
                }

                // Upgrade option
                gui.item(Material.EXPERIENCE_BOTTLE) {
                    name("<green>Upgrade Gate".asMini())
                    description(listOf(
                        "<gray>Verhoog de tier".asMini(),
                        "<gray>van deze gate.".asMini(),
                        "".asMini(),
                        "<white>Huidige tier: <yellow>${gate.tier}".asMini()
                    ))
                    slots(11)

                    onClick { event ->
                        event.isCancelled = true
                        GateUpgradeGui(gate, player)
                    }
                }

                // Repair option
                gui.item(Material.ANVIL) {
                    name("<yellow>Repair Gate".asMini())
                    description(listOf(
                        "<gray>Reset de health".asMini(),
                        "<gray>van deze gate.".asMini(),
                        "".asMini(),
                        "<white>Huidige Health: <yellow>${gate.currentHealth}/${gate.maxHealth} HP".asMini()
                    ))
                    slots(13)

                    onClick { event ->
                        event.isCancelled = true
                        GateRepairGui(gate, player)
                    }
                }

                // Delete option
                gui.item(Material.BARRIER) {
                    name("<red>Delete Gate".asMini())
                    description(listOf(
                        "<gray>Permanent deze gate verwijderen.".asMini(),
                        "".asMini(),
                        "<gold>Waarschuwing: Dit kan niet ontdaan worden!".asMini()
                    ))
                    slots(15)

                    onClick { event ->
                        event.isCancelled = true
                        GateDeleteGui(gate, player)
                    }
                }

                // Back button
                gui.item(Material.ARROW) {
                    name("<gray>Terug naar de gate lijst".asMini())
                    slots(18)
                    onClick { event ->
                        event.isCancelled = true
                        GateListGui(player)
                    }
                    modelData(300)
                }
            }
            .build()

        standardGui.open(player)
    }
}