package gg.scala.cocoa.permission

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.handler.DataStoreHandler
import java.util.*

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
object CocoaPlayerPermissions
{
    val permissions = mutableMapOf<UUID, List<String>>()

    fun calculatePermissions(uniqueId: UUID)
    {
        val rank = DataStoreHandler.findRank(
            DataStoreHandler.rankOf[uniqueId]
        ) ?: DataStoreHandler.getDefaultRank()

        permissions[uniqueId] = rank.getCompoundedPermissions()
            .filter { it.startsWith("%") }
            .map { it.removePrefix("%") }

        println("Setting up permissions for: $uniqueId")
    }

    fun Player.hasCocoaPermission(permission: String): Boolean
    {
        return permissions[uniqueId]!!.contains(permission)
    }
}
