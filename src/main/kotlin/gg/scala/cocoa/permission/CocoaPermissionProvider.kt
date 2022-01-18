package gg.scala.cocoa.permission

import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.PermissionProvider
import com.velocitypowered.api.permission.PermissionSubject
import com.velocitypowered.api.proxy.Player

/**
 * @author GrowlyX
 * @since 1/17/2022
 */
object CocoaPermissionProvider : PermissionProvider
{
    override fun createFunction(subject: PermissionSubject): PermissionFunction
    {
        if (subject !is Player)
        {
            throw Throwable("CocoaPermissionFunction only accept players.")
        }

        return CocoaPermissionFunction(subject)
    }
}
