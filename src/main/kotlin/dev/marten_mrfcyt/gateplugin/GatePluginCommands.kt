package dev.marten_mrfcyt.gateplugin

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import dev.marten_mrfcyt.gateplugin.gate.GateManager
import dev.marten_mrfcyt.gateplugin.gate.RequestStatus
import dev.marten_mrfcyt.gateplugin.gate.gates
import dev.marten_mrfcyt.gateplugin.gui.GateOptionGui
import dev.marten_mrfcyt.gateplugin.gui.RequestListGui
import dev.marten_mrfcyt.gateplugin.utils.getKingdoms
import mlib.api.commands.builders.command
import mlib.api.utilities.asMini
import mlib.api.utilities.error
import mlib.api.utilities.message
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.text.append

@Suppress("DuplicatedCode")
fun Plugin.gatePluginCommands() = command("gate") {
    requiresPermissions("gate.menu")
    executes {
        GateOptionGui(source)
    }
    literal("save") {
        requiresPermissions("gate.admin")
        argument("name", StringArgumentType.string()) {
            argument("kingdom", StringArgumentType.string()) {
                suggests { builder ->
                    getKingdoms().forEach { builder.suggest(it) }
                    builder.build()
                }
                argument("maxHealth", IntegerArgumentType.integer(1)) {
                    argument("attackable", BoolArgumentType.bool()) {
                        suggests { builder ->
                            builder.suggest("true")
                            builder.suggest("false")
                            builder.build()
                        }
                        executes {
                            val player = source as Player
                            try {
                                val name = getArgument<String>("name")
                                val kingdom = getArgument<String>("kingdom")
                                val maxHealth = getArgument<Int>("maxHealth")
                                val attackable = getArgument<Boolean>("attackable")

                                if (!getKingdoms().contains(kingdom)) {
                                    source.message("Koninkrijk $kingdom niet gevonden")
                                    return@executes
                                }
                                if(GateManager(GatePlugin.instance).getGate(name) != null) {
                                    source.message("Poort $name bestaat al")
                                    return@executes
                                }
                                GateManager(GatePlugin.instance).saveGate(
                                    name = name,
                                    kingdom = kingdom,
                                    maxHealth = maxHealth,
                                    attackable = attackable,
                                    player = player
                                )
                                source.message("Poort $name succesvol aangemaakt")
                            } catch (e: Exception) {
                                source.error("Poort aanmaken mislukt: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }
    literal("reset") {
        requiresPermissions("gate.admin")
        argument("name", StringArgumentType.string()) {
            suggests { builder ->
                gates.keys.forEach { builder.suggest(it) }
                builder.build()
            }
            executes {
                try {
                    val name = getArgument<String>("name")
                    val gate = gates.values.firstOrNull { it.name == name }

                    if (gate == null) {
                        source.error("Poort $name niet gevonden")
                        return@executes
                    }

                    // Reset the gate
                    gate.reset()
                    source.message("Poort $name is gereset")
                } catch (e: Exception) {
                    source.error("Poort resetten mislukt: ${e.message}")
                }
            }
        }
    }
    literal("delete") {
        requiresPermissions("gate.admin")
        argument("name", StringArgumentType.string()) {
            suggests { builder ->
                gates.keys.forEach { builder.suggest(it) }
                builder.build()
            }
            executes {
                try {
                    val name = getArgument<String>("name")
                    val gate = gates.values.firstOrNull { it.name == name }

                    if (gate == null) {
                        source.error("Poort $name niet gevonden")
                        return@executes
                    }
                    source.message("Poort $name is verwijderd")
                    GatePlugin.gateManager.deleteGate(name)
                    GatePlugin.gateManager.saveGates()
                } catch (e: Exception) {
                    source.error("Poort verwijderen mislukt: ${e.message}")
                }
            }
        }
    }
    literal("lock") {
        requiresPermissions("gate.admin")
        argument("name", StringArgumentType.string()) {
            suggests { builder ->
                gates.keys.forEach { builder.suggest(it) }
                builder.build()
            }
            executes {
                try {
                    val name = getArgument<String>("name")
                    val gate = gates.values.firstOrNull { it.name == name }

                    if (gate == null) {
                        source.error("Poort $name niet gevonden")
                        return@executes
                    }

                    gate.attackable = !gate.attackable
                    GatePlugin.gateManager.saveGates()
                    val status = if (gate.attackable) "ontgrendeld" else "vergrendeld"
                    gate.refresh()
                    source.message("Poort $name is $status")
                } catch (e: Exception) {
                    source.error("Vergrendelstatus wijzigen mislukt: ${e.message}")
                }
            }
        }
    }
    literal("lock-provincie") {
        requiresPermissions("gate.admin")
        argument("kingdom", StringArgumentType.string()) {
            suggests { builder ->
                getKingdoms().forEach { builder.suggest(it) }
                builder.build()
            }
            executes {
                try {
                    val kingdom = getArgument<String>("kingdom")
                    gates.forEach { (_, gate) ->
                        if (gate.kingdom.name == kingdom) {
                            gate.attackable = !gate.attackable
                            gate.refresh()
                        }
                    }
                    source.message("Poort $name is nu vergrendeld voor koninkrijk $kingdom")
                } catch (e: Exception) {
                    source.error("Poort vergrendelen voor koninkrijk mislukt: ${e.message}")
                }
            }
        }
    }
    literal("list") {
            requiresPermissions("gate.admin")
            executes {
                val player = source as? Player
                if (player != null) {
                    showGateList(player, 1)
                } else {
                    source.message("<red>Dit commando kan alleen door spelers worden gebruikt")
                }
            }
            argument("page", IntegerArgumentType.integer(1)) {
                executes {
                    val player = source as? Player
                    if (player != null) {
                        val page = getArgument<Int>("page")
                        showGateList(player, page)
                    } else {
                        source.message("<red>Dit commando kan alleen door spelers worden gebruikt")
                    }
                }
            }
        }
    literal("teleport") {
        requiresPermissions("gate.admin")
        argument("name", StringArgumentType.string()) {
            suggests { builder ->
                gates.keys.forEach { builder.suggest(it) }
                builder.build()
            }
            executes {
                try {
                    val player = source as? Player
                    if (player == null) {
                        source.error("Alleen spelers kunnen dit commando gebruiken")
                        return@executes
                    }

                    val name = getArgument<String>("name")
                    val gate = gates[name]

                    if (gate == null) {
                        source.error("Poort $name niet gevonden")
                        return@executes
                    }

                    player.teleport(gate.location)
                    source.message("Geteleporteerd naar poort $name")
                } catch (e: Exception) {
                    source.error("Teleporteren mislukt: ${e.message}")
                }
            }
        }
    }
    literal("requests") {
        executes {
            try {
                val player = source as? Player
                if (player == null) {
                    source.error("Alleen spelers kunnen dit commando gebruiken")
                    return@executes
                }

                RequestListGui.openRequestList(player)
            } catch (e: Exception) {
                source.error("Verzoeken weergeven mislukt: ${e.message}")
            }
        }
    }
    literal("approve") {
        requiresPermissions("gate.admin")
        argument("id", StringArgumentType.string()) {
            executes {
                try {
                    val player = source as? Player
                    if (player == null) {
                        source.error("Alleen spelers kunnen dit commando gebruiken")
                        return@executes
                    }

                    val idString = getArgument<String>("id")
                    try {
                        val requestId = java.util.UUID.fromString(idString)
                        val request = GatePlugin.instance.requestManager.getRequests().find { it.id == requestId }

                        if (request == null) {
                            player.message("<red>Geen gate verzoek gevonden met dit ID")
                            return@executes
                        }

                        // Approve the request
                        request.status = RequestStatus.ACCEPTED
                        GatePlugin.instance.requestManager.saveRequests()
                        player.message("<green>Je hebt het gate verzoek voor <yellow>${request.gateName}</yellow> goedgekeurd!")
                        player.message("<yellow>Maak een WorldEdit-selectie en ")
                        player.sendMessage(
                            "<gray>klik <bold><click:suggest_command:/gate save ${request.gateName} ${request.provincie.name} 60 true><yellow>hier</yellow></click></bold> om de poort te maken </gray>".asMini()
                        )
                    } catch (e: IllegalArgumentException) {
                        player.message("<red>Ongeldig UUID format")
                    }
                } catch (e: Exception) {
                    source.error("Verzoek goedkeuren mislukt: ${e.message}")
                }
            }
        }
    }
    literal("deny") {
        requiresPermissions("gate.admin")
        argument("id", StringArgumentType.string()) {
            executes {
                try {
                    val player = source as? Player
                    if (player == null) {
                        source.error("Alleen spelers kunnen dit commando gebruiken")
                        return@executes
                    }

                    val idString = getArgument<String>("id")
                    try {
                        val requestId = java.util.UUID.fromString(idString)
                        val request = GatePlugin.instance.requestManager.getRequests().find { it.id == requestId }

                        if (request == null) {
                            player.message("<red>Geen gate verzoek gevonden met dit ID")
                            return@executes
                        }

                        // Ask for denial reason
                        RequestListGui.askForDenialReason(player, request)
                    } catch (e: IllegalArgumentException) {
                        player.message("<red>Ongeldig UUID format")
                    }
                } catch (e: Exception) {
                    source.error("Verzoek afwijzen mislukt: ${e.message}")
                }
            }
        }
    }
}

private fun showGateList(source: Player, page: Int = 1) {
    try {
        if (gates.isEmpty()) {
            source.message("<red>Geen poorten gevonden")
            return
        }

        val gateList = gates.values.toList()
        val totalPages = (gateList.size + 4) / 5
        val safePageNumber = page.coerceIn(1, maxOf(1, totalPages))

        val startIndex = (safePageNumber - 1) * 5
        val endIndex = minOf(startIndex + 5, gateList.size)
        val currentPageGates = gateList.subList(startIndex, endIndex)

        source.message("<gold>Poorten <gray>(Pagina $safePageNumber/$totalPages, Totaal: ${gates.size})")

        val componentBuilder = Component.text()
            .append(Component.newline())

        currentPageGates.forEach { gate ->
            val status = if (gate.attackable) "<green>✓" else "<red>✗"
            val health = "<yellow>${gate.currentHealth}<gray>/<green>${gate.maxHealth}"
            val kingdom = "<aqua>${gate.kingdom.name}"

            val gateText = "<dark_gray>- <white>${gate.name} <dark_gray>• $status <dark_gray>• HP: $health <dark_gray>• <gray>P: $kingdom ".asMini()
            val teleportButton = Component.text("[TP]")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/gate teleport ${gate.name}"))
                .hoverEvent(Component.text("Teleporteer").color(NamedTextColor.GRAY))

            componentBuilder
                .append(gateText)
                .append(teleportButton)
                .append(Component.newline())
        }

        source.sendMessage(componentBuilder.build())

        val navigation = StringBuilder()

        if (safePageNumber > 1) {
            navigation.append("<hover:show_text:'<gray>Vorige pagina'><click:run_command:/gate list ${safePageNumber - 1}><yellow>◀</click></hover> ")
        } else {
            navigation.append("<gray>◀ ")
        }

        navigation.append("<gray>Pagina $safePageNumber/$totalPages")

        if (safePageNumber < totalPages) {
            navigation.append(" <hover:show_text:'<gray>Volgende pagina'><click:run_command:/gate list ${safePageNumber + 1}><yellow>▶</click></hover>")
        } else {
            navigation.append(" <gray>▶")
        }

        source.message(navigation.toString())

    } catch (e: Exception) {
        source.error("Poorten weergeven mislukt: ${e.message}")
    }
}