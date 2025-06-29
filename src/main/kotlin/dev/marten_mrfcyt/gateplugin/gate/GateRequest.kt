package dev.marten_mrfcyt.gateplugin.gate

import com.gufli.kingdomcraft.api.domain.Kingdom
import org.bukkit.Location
import java.util.UUID

data class GateRequest(
    val id: UUID,
    val requesterId: UUID,
    val requesterName: String,
    val location: Location,
    val gateName: String,
    val timestamp: Long,
    var status: RequestStatus = RequestStatus.PENDING,
    var responseReason: String? = null,
    val provincie: Kingdom
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DENIED
}