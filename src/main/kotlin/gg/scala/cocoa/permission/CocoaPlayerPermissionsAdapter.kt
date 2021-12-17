package gg.scala.cocoa.permission

import gg.scala.cocoa.permission.CocoaPlayerPermissions.hasCocoaPermission
import gg.scala.cocoa.permission.annotation.CocoaPermission
import revxrsal.commands.command.CommandPermission
import revxrsal.commands.command.ExecutableCommand
import revxrsal.commands.process.PermissionReader

/**
 * @author GrowlyX
 * @since 12/17/2021
 */
object CocoaPlayerPermissionsAdapter : PermissionReader
{
    override fun getPermission(
        command: ExecutableCommand
    ): CommandPermission?
    {
        val permission = command.getAnnotation(CocoaPermission::class.java)
            ?: return null

        return CommandPermission {
            it.uniqueId.hasCocoaPermission(permission.value)
        }
    }
}
