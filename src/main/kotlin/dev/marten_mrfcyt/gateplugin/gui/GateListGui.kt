package dev.marten_mrfcyt.gateplugin.gui

import dev.marten_mrfcyt.gateplugin.gate.gates
import dev.marten_mrfcyt.gateplugin.utils.getKingdom
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.error
import org.bukkit.Material
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component

class GateListGui(player: Player) {
    init {
        val allGates = gates.values.toList()
        val playerKingdom = player.getKingdom()

        if (allGates.isEmpty()) {
            player.error("Geen Gates Gevonden!")
        }

        // Filter gates by player's kingdom
        val kingdomGates = allGates.filter { it.kingdom == playerKingdom }

        // Create paginated GUI
        val paginatedGui = PaginatedGuiBuilder()
            .title("Gate List".asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.AIR)
            .apply {
                // Add all gates as items
                kingdomGates.forEach { gate ->
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

                    addItem(
                        material = material,
                        name = "<white>${gate.name}".asMini(),
                        description = listOf(
                            "<gray>Tier: <white>${gate.tier}".asMini(),
                            "<gray>Status: $statusColor$status".asMini(),
                            "<gray>Health: <white>${gate.currentHealth}/${gate.maxHealth} HP".asMini(),
                            "".asMini(),
                            "<yellow>Klik om de gate aan te passen".asMini()
                        )
                    )
                }
            }
            .onItemClick { player, _, index ->
                if (index < kingdomGates.size) {
                    GateGui(kingdomGates[index], player)
                }
            }
            .customizeGui { gui ->
                gui.item(Material.ARROW) {
                    name("<red>Terug".asMini())
                    description(listOf("<gray>Ga terug naar het hoofdmenu".asMini()))
                    slots(gui.size - 5)

                    onClick { event ->
                        event.isCancelled = true
                        GateOptionGui(player)
                    }
                    modelData(300)
                }
            }
            .build()

        paginatedGui.open(player)
    }
}