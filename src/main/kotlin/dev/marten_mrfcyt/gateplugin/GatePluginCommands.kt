package dev.marten_mrfcyt.gateplugin

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import dev.marten_mrfcyt.gateplugin.gate.GateManager
import dev.marten_mrfcyt.gateplugin.gate.gates
import dev.marten_mrfcyt.gateplugin.utils.getKingdoms
import mlib.api.commands.builders.command
import mlib.api.utilities.error
import mlib.api.utilities.message
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@Suppress("DuplicatedCode")
fun Plugin.gatePluginCommands() = command("gate") {
    literal("save") {
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
                                    source.message("Kingdom $kingdom not found")
                                    return@executes
                                }

                                GateManager(GatePlugin.instance).saveGate(
                                    name = name,
                                    kingdom = kingdom,
                                    maxHealth = maxHealth,
                                    attackable = attackable,
                                    player = player
                                )
                                source.message("Gate $name created successfully")
                            } catch (e: Exception) {
                                source.error("Failed to create gate: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }
    literal("reset") {
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
                        source.error("Gate $name not found")
                        return@executes
                    }

                    // Reset the gate
                    gate.reset()
                    source.message("Gate $name has been reset")
                } catch (e: Exception) {
                    source.error("Failed to reset gate: ${e.message}")
                }
            }
        }
    }
    literal("delete") {
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
                        source.error("Gate $name not found")
                        return@executes
                    }
                    GateManager(GatePlugin.instance).deleteGate(name)
                    source.message("Gate $name has been deleted")
                } catch (e: Exception) {
                    source.error("Failed to delete gate: ${e.message}")
                }
            }
        }
    }
    literal("lock") {
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
                        source.error("Gate $name not found")
                        return@executes
                    }

                    gate.attackable = !gate.attackable
                    GatePlugin.gateManager.saveGates()
                    gate.refresh()
                    val status = if (gate.attackable) "unlocked" else "locked"
                    source.message("Gate $name has been $status")
                } catch (e: Exception) {
                    source.error("Failed to change gate lock status: ${e.message}")
                }
            }
        }
    }
    literal("lock-provincie") {
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
                    source.message("Gate $name is now locked for kingdom $kingdom")
                } catch (e: Exception) {
                    source.error("Failed to lock gate for kingdom: ${e.message}")
                }
            }
        }
    }
    literal("list") {
        executes {
            try {
                val player = source as? Player

                if (gates.isEmpty()) {
                    source.message("No gates found")
                    return@executes
                }

                source.message("Gates (${gates.size}):")

                gates.values.forEach { gate ->
                    val status = if (gate.attackable) "<green>Unlocked" else "<red>Locked"
                    val health = "${gate.currentHealth}/${gate.maxHealth}"
                    val kingdom = gate.kingdom.name

                    if (player != null) {
                        val message = Component.text()
                            .append(Component.text("- ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text(gate.name).color(NamedTextColor.WHITE))
                            .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text(status.replace("<green>", "").replace("<red>", ""))
                                .color(if (gate.attackable) NamedTextColor.GREEN else NamedTextColor.RED))
                            .append(Component.text(" | HP: $health").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text(" | Kingdom: $kingdom ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text("[Teleport]")
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/gate teleport ${gate.name}"))
                                .hoverEvent(Component.text("Click to teleport to ${gate.name}", NamedTextColor.GRAY)))
                        player.sendMessage(message)
                    } else {
                        source.message("<dark_gray>- <white>${gate.name} <dark_gray>| $status <dark_gray>| HP: $health <dark_gray>| Kingdom: $kingdom")
                    }
                }
            } catch (e: Exception) {
                source.error("Failed to list gates: ${e.message}")
            }
        }
    }
    literal("teleport") {
        argument("name", StringArgumentType.string()) {
            suggests { builder ->
                gates.keys.forEach { builder.suggest(it) }
                builder.build()
            }
            executes {
                try {
                    val player = source as? Player
                    if (player == null) {
                        source.error("Only players can use this command")
                        return@executes
                    }

                    val name = getArgument<String>("name")
                    val gate = gates[name]

                    if (gate == null) {
                        source.error("Gate $name not found")
                        return@executes
                    }

                    player.teleport(gate.location)
                    source.message("Teleported to gate $name")
                } catch (e: Exception) {
                    source.error("Failed to teleport: ${e.message}")
                }
            }
        }
    }
}