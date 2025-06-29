package dev.marten_mrfcyt.gateplugin.gui

import dev.marten_mrfcyt.gateplugin.GatePlugin
import dev.marten_mrfcyt.gateplugin.gate.GateRequest
import dev.marten_mrfcyt.gateplugin.gate.RequestStatus
import mlib.api.forms.Form
import mlib.api.forms.FormType
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.*

object RequestListGui {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

    fun openRequestList(player: Player) {
        val isAdmin = player.hasPermission("gate.admin")

        // Show main view based on permissions
        if (isAdmin) {
            openAdminMainMenu(player)
        } else {
            openPlayerRequests(player)
        }
    }

    private fun openAdminMainMenu(player: Player) {
        StandardGuiBuilder()
            .title("<gold>Poort Verzoeken - Admin".asMini())
            .size(mlib.api.gui.GuiSize.ROW_THREE)
            .setup { gui ->
                gui.fill(Material.GRAY_STAINED_GLASS_PANE) {}

                // Pending requests
                gui.item(Material.IRON_BARS) {
                    name("<yellow>Openstaande Verzoeken".asMini())
                    description(listOf(
                        "<gray>Bekijk alle openstaande poortverzoeken".asMini(),
                        "<gray>die je aandacht nodig hebben".asMini()
                    ))
                    slots(11)
                    onClick {
                        it.isCancelled = true
                        openAdminPendingRequests(player)
                    }
                }

                // Your requests
                gui.item(Material.BOOK) {
                    name("<green>Jouw Verzoeken".asMini())
                    description(listOf(
                        "<gray>Bekijk je persoonlijke poortverzoeken".asMini(),
                        "<gray>en hun status".asMini()
                    ))
                    slots(15)
                    onClick {
                        it.isCancelled = true
                        openPlayerRequests(player)
                    }
                }
            }
            .build()
            .open(player)
    }

    private fun openAdminPendingRequests(player: Player) {
        val requests = GatePlugin.instance.requestManager.getRequests()
            .filter { it.status == RequestStatus.PENDING }

        if (requests.isEmpty()) {
            player.sendMessage("<yellow>Er zijn geen openstaande poortverzoeken.".asMini())
            player.closeInventory()
            return
        }

        val builder = PaginatedGuiBuilder()
            .title("<gold>Openstaande Poortverzoeken".asMini())
            .setBackground(Material.GRAY_STAINED_GLASS_PANE)

        // Sort by timestamp (newest first)
        requests.sortedByDescending { it.timestamp }.forEach { request ->
            val date = dateFormat.format(Date(request.timestamp))

            builder.addItem(
                Material.IRON_BARS,
                "<yellow>${request.gateName}".asMini(),
                listOf(
                    "<gray>Aangevraagd door: <white>${request.requesterName}".asMini(),
                    "<gray>Datum: <white>$date".asMini(),
                    " ".asMini(),
                    "<green>Klik om dit verzoek te beoordelen".asMini()
                )
            )
        }

        builder.onItemClick { p, _, index ->
            if (index < requests.size) {
                val request = requests[index]
                reviewRequest(p, request)
            }
        }

        builder.build().open(player)
    }

    private fun openPlayerRequests(player: Player) {
        val allRequests = GatePlugin.instance.requestManager.getRequests()
        val playerRequests = allRequests.filter { it.requesterId == player.uniqueId }

        if (playerRequests.isEmpty()) {
            player.sendMessage("<yellow>Je hebt geen gate verzoeken.".asMini())
            player.closeInventory()
            return
        }

        val builder = PaginatedGuiBuilder()
            .title("<gold>Jouw gate verzoeken".asMini())
            .setBackground(Material.GRAY_STAINED_GLASS_PANE)

        // Group by status and sort by timestamp (newest first)
        val grouped = playerRequests.groupBy { it.status }.toSortedMap()

        // Add requests in order: PENDING, ACCEPTED, DENIED
        val sortedRequests = mutableListOf<GateRequest>()
        grouped[RequestStatus.PENDING]?.sortedByDescending { it.timestamp }?.let { sortedRequests.addAll(it) }
        grouped[RequestStatus.ACCEPTED]?.sortedByDescending { it.timestamp }?.let { sortedRequests.addAll(it) }
        grouped[RequestStatus.DENIED]?.sortedByDescending { it.timestamp }?.let { sortedRequests.addAll(it) }

        sortedRequests.forEach { request ->
            val date = dateFormat.format(Date(request.timestamp))
            val material = when(request.status) {
                RequestStatus.PENDING -> Material.IRON_BARS
                RequestStatus.ACCEPTED -> Material.LIME_CONCRETE
                RequestStatus.DENIED -> Material.RED_CONCRETE
            }

            val statusColor = when(request.status) {
                RequestStatus.PENDING -> "<yellow>"
                RequestStatus.ACCEPTED -> "<green>"
                RequestStatus.DENIED -> "<red>"
            }

            val description = mutableListOf(
                "<gray>Status: $statusColor${request.status}".asMini(),
                "<gray>Datum: <white>$date".asMini(),
                " ".asMini()
            )

            if (request.status == RequestStatus.DENIED && request.responseReason != null) {
                description.add("<gray>Reden: <white>${request.responseReason}".asMini())
            }
            builder.addItem(
                material,
                "<yellow>${request.gateName}".asMini(),
                description
            )
            builder.onClick { event ->
                event.isCancelled = true
            }
        }

        builder.build().open(player)
    }

    private fun reviewRequest(player: Player, request: GateRequest) {
        // Teleport player to request location
        val location = request.location.clone().add(0.0, 1.0, 0.0)
        player.teleport(location)
        player.sendMessage(Component.text().content("")
            .append("<gold>Gate verzoek Beoordelen".asMini())
            .append(Component.newline())
            .append("<yellow>${request.gateName}".asMini())
            .append(Component.newline())
            .append("<gray>Aangevraagd door: <white>${request.requesterName}".asMini())
            .append(Component.newline())
            .append("<gray>Wil je dit gate verzoek goedkeuren of afwijzen?".asMini())
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text()
                .content("[Goedkeuren]")
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/gate approve ${request.id}"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText("<green>Klik om goed te keuren".asMini()))
                .build()
            )
            .append(Component.space())
            .append(Component.text()
                .content("[Afwijzen]")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/gate deny ${request.id}"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText("<red>Klik om af te wijzen".asMini()))
                .build()
            )
            .build()
        )
    }

    internal fun askForDenialReason(player: Player, request: GateRequest) {
        val form = Form(
            "Wat is de reden voor het afwijzen van dit gate verzoek?",
            FormType.STRING,
            600
        ) { p, input ->
            val reason = input as String

            // Update request status
            request.status = RequestStatus.DENIED
            request.responseReason = reason
            GatePlugin.instance.requestManager.saveRequests()

            p.message("<green>Gate verzoek afgewezen met reden: <yellow>$reason")

            // Notify the requester if online
            val requester = player.server.getPlayer(request.requesterId)
            requester?.message("<red>Je gate verzoek voor <yellow>${request.gateName}<red> is afgewezen.")
            requester?.message("<red>Reden: <yellow>$reason")
        }

        form.show(player)
    }
}