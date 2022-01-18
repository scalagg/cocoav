package gg.scala.cocoa.permission

import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.permission.CocoaPermissionEngine.hasCocoaPermission

/**
 * @author GrowlyX
 * @since 1/17/2022
 */
class CocoaPermissionFunction(
    private val player: Player
) : PermissionFunction
{
    override fun getPermissionValue(permission: String): Tristate
    {
        return if (player.hasCocoaPermission(permission))
            Tristate.TRUE else Tristate.FALSE
    }
}
