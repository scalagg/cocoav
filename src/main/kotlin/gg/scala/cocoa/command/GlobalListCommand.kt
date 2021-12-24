package gg.scala.cocoa.command

import co.schemati.trevor.api.TrevorService
import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityPlugin
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Dependency

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
object GlobalListCommand
{
    @Command("glist")
    fun onHub(player: Player)
    {
        val playerCount = TrevorService.getAPI().instanceData.playerCount

        player.sendMessage(
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aThere ${
                    if (playerCount == 1) "is" else "are"
                } &l$playerCount&a player${
                    if (playerCount == 1) "" else "s"
                } connected.")
        )
    }
}
