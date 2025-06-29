package dev.marten_mrfcyt.gateplugin.gui

import dev.marten_mrfcyt.gateplugin.GatePlugin
import dev.marten_mrfcyt.gateplugin.gate.GateRequest
import dev.marten_mrfcyt.gateplugin.gate.GateUpgradeManager
import dev.marten_mrfcyt.gateplugin.gate.RequestStatus
import dev.marten_mrfcyt.gateplugin.utils.getKingdom
import mlib.api.forms.Form
import mlib.api.forms.FormType
import mlib.api.gui.types.builder.ConfirmationGuiBuilder
import mlib.api.utilities.asMini
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*

object RequestGui {
    fun openRequestConfirmation(player: Player, location: Location) {
        // Create confirmation GUI
        ConfirmationGuiBuilder()
            .title("<gold>Gate Verzoek Aanmaken".asMini())
            .message(listOf(
                "<yellow>Gate verzoek hier aanmaken?".asMini(),
                "<gray>Dit kost <yellow>${GateUpgradeManager.TIER_1_PRICE.toInt()} gulden".asMini(),
                " ".asMini(),
                "<gray>Als het goedgekeurd wordt, wordt er een gate".asMini(),
                "<gray>op deze locatie gemaakt.".asMini()
            ))
            .confirmText("Aanmaken".asMini())
            .cancelText("Annuleren".asMini())
            .confirmMaterial(Material.ARROW)
            .confirmModel(307)
            .cancelMaterial(Material.ARROW)
            .cancelModel(308)
            .confirmEnabled(GateUpgradeManager.canAfford(player, GateUpgradeManager.TIER_1_PRICE))
            .onConfirm { p ->
                // Player confirmed, now ask for a name using form
                askForGateName(p, location)
            }
            .onCancel { p ->
                p.sendMessage("<red>Gate verzoek geannuleerd.".asMini())
                GateOptionGui(player)
            }
            .build()
            .open(player)
    }

    private fun askForGateName(player: Player, location: Location) {
        val form = Form(
            "Welke naam moet deze gate krijgen?",
            FormType.STRING,
            600
        ) // 60 seconds timeout
        { p, input ->
            val gateName = input as String
            if (gateName.length < 3 || gateName.length > 24) {
                p.sendMessage("<red>Gate naam moet tussen 3 en 24 tekens zijn.".asMini())
                return@Form
            }

            // Process payment
            if (GateUpgradeManager.withdraw(player, GateUpgradeManager.TIER_1_PRICE)) {
                // Create and save the request
                val request = GateRequest(
                    UUID.randomUUID(),
                    player.uniqueId,
                    player.name,
                    location,
                    gateName,
                    System.currentTimeMillis(),
                    RequestStatus.PENDING,
                    null,
                    player.getKingdom() ?: return@Form
                )

                GatePlugin.instance.requestManager.addRequest(request)
                p.sendMessage("<green>Gate verzoek succesvol ingediend! Een beheerder zal het binnenkort beoordelen.".asMini())
            } else {
                p.sendMessage("<red>Betaling mislukt. Verzoek geannuleerd.".asMini())
            }
        }

        form.show(player)
    }
}