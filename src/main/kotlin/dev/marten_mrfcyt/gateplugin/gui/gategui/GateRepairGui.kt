package dev.marten_mrfcyt.gateplugin.gui.gategui

import dev.marten_mrfcyt.gateplugin.gate.Gate
import dev.marten_mrfcyt.gateplugin.gate.GateUpgradeManager
import dev.marten_mrfcyt.gateplugin.gui.GateGui
import mlib.api.gui.types.builder.ConfirmationGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.error
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.function.Consumer

class GateRepairGui(private val gate: Gate, private val player: Player) {
    init {
        if (gate.currentHealth == gate.maxHealth) {
            player.error("Deze poort heeft al volledige health!")
            GateGui(gate, player)
        }

        val canAfford = GateUpgradeManager.canAfford(player, GateUpgradeManager.REPAIR_PRICE)

        val confirmGui = ConfirmationGuiBuilder()
            .title("Poort Repareren".asMini())
            .message(listOf(
                "<white>Poort Repareren: ${gate.name}".asMini(),
                "<gray>Huidige health: <red>${gate.currentHealth}/${gate.maxHealth} HP</red>".asMini(),
                "<gray>Nieuwe health: <green>${gate.maxHealth}/${gate.maxHealth} HP</green>".asMini(),
                "".asMini(),
                "<gray>Kosten: <gold>${GateUpgradeManager.REPAIR_PRICE.toInt()} gulden".asMini(),
                "".asMini(),
                if (!canAfford) "<red>Je kunt het niet betalen om deze poort te repareren!".asMini() else
                    "<yellow>Weet je zeker dat je deze poort wilt repareren?".asMini()
            ))
            .confirmText("<green>Repareren".asMini())
            .cancelText("<red>Annuleren".asMini())
            .confirmMaterial(Material.ARROW)
            .confirmModel(307)
            .cancelMaterial(Material.ARROW)
            .cancelModel(308)
            .confirmEnabled(canAfford)
            .onConfirm(Consumer<Player> { player ->
                val (success, message) = GateUpgradeManager.repairGate(gate, player)

                if (success) {
                    player.message("<green>Poort ${gate.name} is succesvol gerepareerd!")
                } else {
                    player.error(message)
                }

                // Return to gate GUI
                GateGui(gate, player)
            })
            .onCancel(Consumer { player ->
                // Return to gate GUI without changes
                GateGui(gate, player)
            })
            .build()

        confirmGui.open(player)
    }
}
