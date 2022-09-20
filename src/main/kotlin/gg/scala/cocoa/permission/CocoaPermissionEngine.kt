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
    private val permissions = mutableMapOf<UUID, List<String>>()

    fun calculatePermissions(uniqueId: UUID)
    {
        try
        {
            val thing = DataStoreHandler.getPermissionGrants(uniqueId)

            permissions[uniqueId] = thing
                .filter { it.isActive && it.scopes.contains("global") }
                .flatMap {
                    it.getRank()?.getCompoundedPermissions() ?: arrayListOf()
                }
                .filter { it.startsWith("%") }
                .map { it.removePrefix("%") }
        } catch (exception: Exception)
        {
            exception.printStackTrace()
            permissions[uniqueId] = mutableListOf()
        }
    }

    fun Player.hasCocoaPermission(permission: String): Boolean
    {
        return permissions[uniqueId]?.contains(permission) == true
    }
}
