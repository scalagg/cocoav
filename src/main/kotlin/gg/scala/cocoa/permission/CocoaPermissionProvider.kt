package gg.scala.cocoa.permission

import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.PermissionProvider
import com.velocitypowered.api.permission.PermissionSubject
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.permission.CommandPermissionEngine.hasPermission

/**
 * @author GrowlyX
 * @since 1/17/2022
 */
class CommandPermissionProvider(
    private val player: Player
) : PermissionProvider, PermissionFunction
{
    override fun createFunction(subject: PermissionSubject): PermissionFunction
    {
        return this
    }

    override fun getPermissionValue(permission: String): Tristate
    {
        return if (player.hasPermission(permission))
            Tristate.TRUE else Tristate.FALSE
    }
}
