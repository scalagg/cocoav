package gg.scala.cocoa.command

import co.schemati.trevor.api.TrevorAPI
import co.schemati.trevor.api.TrevorService
import co.schemati.trevor.velocity.TrevorVelocity
import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.permission.CocoaPlayerPermissions.hasCocoaPermission
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Dependency
import revxrsal.commands.exception.CommandErrorException

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
object GListCommand
{
    @Dependency
    lateinit var plugin: CocoaVelocityPlugin

    @Command("glist")
    fun onHub(player: Player)
    {
        player.sendMessage(
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&eThere are &b${TrevorService.getAPI().instanceData.playerCount}&e players connected.")
        )
    }
}
