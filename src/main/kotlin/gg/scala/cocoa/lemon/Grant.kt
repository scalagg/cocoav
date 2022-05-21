package gg.scala.cocoa.lemon

import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.lemon.expirable.LemonExpirable
import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*

class Grant(
    val uuid: UUID,
    var rankId: UUID,
    addedAt: Long,
    duration: Long
): LemonExpirable(addedAt, duration), IDataStoreObject
{
    override val identifier: UUID
        get() = uuid

    var scopes: MutableList<String> = mutableListOf("global")

    var removedAt: Long = -1

    val isRemoved: Boolean
        get() = removedAt != -1L

    val isActive: Boolean
        get() = !isRemoved && !hasExpired

    fun getRank(): Rank? {
        return DataStoreHandler.findRank(rankId)
    }
}
