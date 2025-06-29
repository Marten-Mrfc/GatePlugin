package dev.marten_mrfcyt.gateplugin.gui

import dev.marten_mrfcyt.gateplugin.gui.RequestGui
import mlib.api.gui.Gui
import mlib.api.gui.GuiItemProcessor
import mlib.api.gui.GuiSize
import mlib.api.utilities.*
import nl.jochem.nexus.utils.ItemStack
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta

class GateOptionGui(source: CommandSender) {
    init {
        if (source is Player) {
            val gui = Gui("Gate".asMini(), GuiSize.ROW_ONE).apply {
                item(Material.ARROW) {
                    name("Request".asMini())
                    description(listOf("<gray>Vraag een gate aan.".asMini(), "<dark_gray>De poort bij je locatie wordt gebruikt!".asMini()))
                    slots(3)
                    onClick { event ->
                        event.isCancelled = true
                        RequestGui.openRequestConfirmation(source, source.location)
                    }
                    modelData(301)
                }

                item(Material.PAPER) {
                    name("Lijst".asMini())
                    description(listOf("<gray>Lijst van al je gates.".asMini()))
                    slots(5)
                    onClick { event ->
                        event.isCancelled = true
                        GateListGui(source)
                    }
                    modelData(400)
                }
            }
            gui.open(source)
        } else {
            source.error("You must be a player to open a GUI")
        }
    }

}