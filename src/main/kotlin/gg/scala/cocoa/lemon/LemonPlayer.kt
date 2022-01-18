package gg.scala.cocoa.lemon

import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.lemon.category.PunishmentCategory
import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 1/17/2022
 */
class LemonPlayer(
    var uniqueId: UUID,
    var name: String
) : IDataStoreObject
{
    override val identifier: UUID
        get() = uniqueId

    var pastIpAddresses = mutableMapOf<String, Long>()
    val punishments = mutableMapOf<PunishmentCategory, Punishment?>()

    var metadata = mutableMapOf<String, Metadata>()

    init
    {
        for (value in PunishmentCategory.VALUES)
        {
            punishments[value] = null
        }
    }

    fun recalculatePunishments(): CompletableFuture<Void>
    {
        return DataStoreHandler.fetchAllPunishmentsForTarget(uniqueId).thenAcceptAsync { list ->
            for (value in PunishmentCategory.VALUES)
            {
                punishments[value] = null
            }

            for (value in PunishmentCategory.VALUES)
            {
                val newList = list.filter {
                    it.category == value && it.isActive
                }

                if (newList.isEmpty())
                {
                    punishments[value] = null
                    continue
                }

                punishments[value] = newList[0]
            }
        }
    }

    fun checkForIpRelative(): CompletableFuture<Void>
    {
        return DataStoreHandler.fetchAlternateAccountsFor(uniqueId).thenAcceptAsync { lemonPlayers ->
            lemonPlayers.forEach {
                val lastIpAddress = getMetadata("last-ip-address")?.asString() ?: ""
                val targetLastIpAddress = it.getMetadata("last-ip-address")?.asString() ?: ""

                val matchingIpInfo = lastIpAddress == targetLastIpAddress

                if (matchingIpInfo)
                {
                    for (punishmentCategory in PunishmentCategory.IP_REL)
                    {
                        val punishments = DataStoreHandler
                            .fetchPunishmentsForTargetOfCategoryAndActive(it.uniqueId, punishmentCategory)
                            .join()

                        if (punishments.isNotEmpty())
                        {
                            this.punishments[PunishmentCategory.IP_RELATIVE] = punishments[0]
                        }
                    }
                }
            }
        }
    }

    fun updateOrAddMetadata(id: String, data: Metadata)
    {
        metadata[id] = data
    }

    fun removeMetadata(id: String): Metadata?
    {
        return metadata.remove(id)
    }

    fun hasMetadata(id: String): Boolean
    {
        return metadata.containsKey(id)
    }

    fun getMetadata(id: String): Metadata?
    {
        return metadata[id]
    }
}
