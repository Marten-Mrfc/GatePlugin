package dev.marten_mrfcyt.gateplugin.utils

import com.gufli.kingdomcraft.api.KingdomCraftProvider
import com.gufli.kingdomcraft.api.domain.Kingdom
import java.util.UUID
import kotlin.text.get

fun getKingdoms(): List<String> {
    return KingdomCraftProvider.get().kingdoms.map { it.name }
}

fun getKingdomList(): List<Kingdom> {
    return KingdomCraftProvider.get().kingdoms.map { it }
}
fun getKingdomMembers(kingdom: String): List<UUID> {
    return KingdomCraftProvider.get().kingdoms.find { it.name == kingdom }?.members?.map { it.key } ?: listOf()
}

fun org.bukkit.entity.Player.getKingdom(): com.gufli.kingdomcraft.api.domain.Kingdom? {
    return KingdomCraftProvider.get().getUser(this.uniqueId).get().kingdom
}