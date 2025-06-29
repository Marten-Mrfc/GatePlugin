package dev.marten_mrfcyt.gateplugin.gui.gategui

import dev.marten_mrfcyt.gateplugin.GatePlugin
import dev.marten_mrfcyt.gateplugin.gate.Gate
import dev.marten_mrfcyt.gateplugin.gui.GateGui
import dev.marten_mrfcyt.gateplugin.gui.GateListGui
import mlib.api.gui.types.builder.ConfirmationGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.function.Consumer

class GateDeleteGui(private val gate: Gate, private val player: Player) {
    init {
        val confirmGui = ConfirmationGuiBuilder()
            .title("Verwijder Poort".asMini())
            .message(listOf(
                "<red>Verwijder Poort: ${gate.name}".asMini(),
                "".asMini(),
                "<gold>WAARSCHUWING: Deze actie kan niet ongedaan worden gemaakt!".asMini(),
                "<gold>De poort zal permanent verwijderd worden.".asMini(),
                "".asMini(),
                "<red>Weet je absoluut zeker dat je door wilt gaan?".asMini()
            ))
            .onConfirm(Consumer<Player> { player ->
                player.message("<green>Poort ${gate.name} is succesvol verwijderd!")
                GatePlugin.gateManager.deleteGate(gate.name)
                GatePlugin.gateManager.saveGates()
                GateListGui(player)
            })
            .confirmMaterial(Material.ARROW)
            .confirmModel(307)
            .cancelMaterial(Material.ARROW)
            .cancelModel(308)
            .onCancel(Consumer { player ->
                GateGui(gate, player)
            })
            .build()

        confirmGui.open(player)
    }
}
