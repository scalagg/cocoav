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
    private val permissions = mutableMapOf<UUID, List<String>>()

    fun calculatePermissions(uniqueId: UUID)
    {
        try
        {
            val rank = DataStoreHandler.findRank(
                DataStoreHandler.rankOf[uniqueId]
            ) ?: DataStoreHandler.getDefaultRank()

            permissions[uniqueId] = rank.getCompoundedPermissions()
                .filter { it.startsWith("%") }
                .map { it.removePrefix("%") }
        } catch (ignored: Exception)
        {
            permissions[uniqueId] = mutableListOf()
        }
    }

    fun Player.hasCocoaPermission(permission: String): Boolean
    {
        return permissions[uniqueId]!!.contains(permission)
    }

    fun UUID.hasCocoaPermission(permission: String): Boolean
    {
        return permissions[this]!!.contains(permission)
    }
}
