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

class GateUpgradeGui(private val gate: Gate, private val player: Player) {
    init {
        val currentTier = gate.tier
        val nextTier = currentTier + 1

        if (nextTier > 3) {
            player.error("Deze poort is al op het maximale niveau!")
            GateGui(gate, player)
        }

        val upgradeCost = if (nextTier == 2) GateUpgradeManager.TIER_2_UPGRADE else GateUpgradeManager.TIER_3_UPGRADE
        val nexusRequirement = if (nextTier == 2) GateUpgradeManager.TIER_2_NEXUS else GateUpgradeManager.TIER_3_NEXUS
        val newHealth = if (nextTier == 2) GateUpgradeManager.TIER_2_HEALTH else GateUpgradeManager.TIER_3_HEALTH

        val playerNexusTier = GateUpgradeManager.getPlayerNexusTier(player)
        val canAfford = GateUpgradeManager.canAfford(player, upgradeCost)

        val confirmGui = ConfirmationGuiBuilder()
            .title("Poort Upgraden".asMini())
            .message(listOf(
                "<white>Poort Upgraden: ${gate.name}".asMini(),
                "<gray>Van niveau <yellow>$currentTier</yellow> naar <green>$nextTier</green>".asMini(),
                "".asMini(),
                "<gray>Kosten: <gold>${upgradeCost.toInt()} gulden".asMini(),
                "<gray>Nieuwe health: <green>$newHealth HP".asMini(),
                "<gray>Nexus Vereiste: <yellow>Niveau $nexusRequirement".asMini(),
                "<gray>Jouw Nexus Niveau: <${if(playerNexusTier >= nexusRequirement) "green" else "red"}>$playerNexusTier".asMini(),
                "".asMini(),
                if (!canAfford) "<red>Je kunt deze upgrade niet betalen!".asMini() else
                    if (playerNexusTier < nexusRequirement) "<red>Je nexus niveau is te laag!".asMini() else
                        "<yellow>Weet je zeker dat je deze poort wilt upgraden?".asMini()
            ))
            .confirmText("<green>Upgraden".asMini())
            .cancelText("<red>Annuleren".asMini())
            .confirmMaterial(Material.ARROW)
            .confirmModel(307)
            .cancelMaterial(Material.ARROW)
            .cancelModel(308)
            .confirmEnabled(canAfford && playerNexusTier >= nexusRequirement)
            .onConfirm(Consumer<Player> { player ->
                val (success, message) = GateUpgradeManager.upgradeGate(gate, player)

                if (success) {
                    player.message("<green>Poort ${gate.name} succesvol geüpgraded naar niveau $nextTier!")
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
