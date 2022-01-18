package gg.scala.cocoa.permission

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.handler.DataStoreHandler
import java.util.*
import javax.xml.crypto.Data

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
object CocoaPermissionEngine
{
    val permissions = mutableMapOf<UUID, List<String>>()

    fun calculatePermissions(uniqueId: UUID)
    {
        try
        {
            var playerRank = DataStoreHandler.rankOf[uniqueId]

            if (playerRank == null)
            {
                playerRank = DataStoreHandler.loadRankOfNow(uniqueId).uuid
            }

            val rank = DataStoreHandler
                .findRank(playerRank) ?:
                DataStoreHandler.getDefaultRank()

            permissions[uniqueId] = rank.getCompoundedPermissions()
                .filter { it.startsWith("%") }
                .map { it.removePrefix("%") }
        } catch (e: Exception)
        {
            e.printStackTrace()
            permissions[uniqueId] = mutableListOf()
        }
    }

    fun Player.hasCocoaPermission(permission: String): Boolean
    {
        return permissions[uniqueId]?.contains(permission) == true
    }

    fun UUID.hasPermission(permission: String): Boolean
    {
        return permissions[this]?.contains(permission) == true
    }

    fun UUID.hasPermissionRaw(permission: String): Boolean
    {
        return permissions[this]!!.contains(permission)
    }
}
