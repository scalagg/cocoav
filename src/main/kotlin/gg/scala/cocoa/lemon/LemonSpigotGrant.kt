package gg.scala.cocoa.lemon

import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.lemon.rank.LemonSpigotRank
import gg.scala.cocoa.lemon.expirable.LemonExpirable
import java.util.*

class LemonSpigotGrant(
    val uuid: UUID,
    var target: UUID,
    var rankId: UUID,
    var addedBy: UUID?,
    addedAt: Long,
    var addedOn: String,
    var addedReason: String,
    duration: Long
): LemonExpirable(addedAt, duration) {

    var scopes: MutableList<String> = mutableListOf("global")

    var removedReason: String? = null
    var removedOn: String? = null
    var removedBy: UUID? = null
    var removedAt: Long = -1
    var isRemoved = false

    val isActive: Boolean
        get() = !isRemoved && !hasExpired

    fun getRank(): LemonSpigotRank? {
        return DataStoreHandler.findRank(rankId)
    }
}
