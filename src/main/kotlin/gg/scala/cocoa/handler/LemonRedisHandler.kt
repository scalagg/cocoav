package gg.scala.cocoa.handler

import gg.scala.banana.annotate.Subscribe
import gg.scala.banana.message.Message
import gg.scala.banana.subscribe.marker.BananaHandler
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import java.util.*

/**
 * @author GrowlyX
 * @since 9/21/2021
 */
object LemonRedisHandler : BananaHandler
{
    @Subscribe("recalculate-grants")
    fun onGrantHanding(message: Message)
    {
        val targetUuid = UUID.fromString(
            message["uniqueId"]
        )

        DataStoreHandler.rankOf[targetUuid] =
            DataStoreHandler.loadRankOfNow(targetUuid).uuid
    }

    @Subscribe("recalculate-punishments")
    fun onPunishmentHandling(message: Message)
    {
        val targetUuid = UUID.fromString(
            message["uniqueId"]
        )

        DataStoreHandler.findPlayer(targetUuid).let {
            it.checkForIpRelative()
            it.recalculatePunishments()
        }
    }

    @Subscribe("rank-delete")
    fun onRankDelete(message: Message)
    {
        val rankUuid = UUID.fromString(
            message["uniqueId"]
        )

        DataStoreHandler.ranks.remove(rankUuid)
    }

    @Subscribe("rank-update")
    fun onRankUpdate(message: Message)
    {
        val completableFuture = DataStoreObjectControllerCache.findNotNull<Rank>()
            .load(
                UUID.fromString(message["uniqueId"]),
                DataStoreStorageType.MONGO
            )

        completableFuture.thenAccept {
            it?.let { rank ->
                DataStoreHandler.ranks[rank.uuid] = rank
            }
        }
    }
}
