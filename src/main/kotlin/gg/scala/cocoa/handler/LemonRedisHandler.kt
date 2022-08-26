package gg.scala.cocoa.handler

import gg.scala.aware.annotation.Subscribe
import gg.scala.aware.message.AwareMessage
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.permission.CocoaPermissionEngine
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import java.util.*

/**
 * @author GrowlyX
 * @since 9/21/2021
 */
object LemonRedisHandler
{
    @Subscribe("recalculate-grants")
    fun onGrantHanding(message: AwareMessage)
    {
        val targetUuid = message
            .retrieve<UUID>("uniqueId")

        DataStoreHandler.rankOf[targetUuid] =
            DataStoreHandler.loadRankOfNow(targetUuid).uuid

        CocoaPermissionEngine
            .calculatePermissions(
                targetUuid, forceCalculate = true
            )
    }

    @Subscribe("recalculate-punishments")
    fun onPunishmentHandling(message: AwareMessage)
    {
        val targetUuid = message
            .retrieve<UUID>("uniqueId")

        DataStoreHandler.findPlayer(targetUuid).let {
            it.checkForIpRelative()
            it.recalculatePunishments()
        }
    }

    @Subscribe("rank-delete")
    fun onRankDelete(message: AwareMessage)
    {
        val rankUuid = message
            .retrieve<UUID>("uniqueId")

        DataStoreHandler.ranks.remove(rankUuid)
    }

    @Subscribe("rank-update")
    fun onRankUpdate(message: AwareMessage)
    {
        val completableFuture = DataStoreObjectControllerCache
            .findNotNull<Rank>()
            .load(
                message.retrieve("uniqueId"),
                DataStoreStorageType.MONGO
            )

        completableFuture.thenAccept {
            it?.let { rank ->
                DataStoreHandler.ranks[rank.uuid] = rank
            }
        }
    }
}
