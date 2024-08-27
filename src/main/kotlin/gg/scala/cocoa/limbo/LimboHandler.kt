package gg.scala.cocoa.limbo

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.scheduler.ScheduledTask
import gg.scala.cocoa.CocoaVelocityPlugin
import net.elytrium.limboapi.api.Limbo
import net.elytrium.limboapi.api.LimboSessionHandler
import net.elytrium.limboapi.api.player.LimboPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

val hors = ImageIO.read(File("hors.jpg"))

class LimboHandler(
    private val plugin: CocoaVelocityPlugin,
    private val reconnectAttempt: (Player) -> RegisteredServer?
) : LimboSessionHandler
{
    private lateinit var player: LimboPlayer
    private lateinit var task: ScheduledTask

    var hasDoneInitialLimbo = false

    override fun onSpawn(server: Limbo, player: LimboPlayer)
    {
        this.player = player
        plugin.limboServer.players.add(player)

        player.disableFalling()
        player.sendImage(hors)

        this.task = plugin.server.scheduler
            .buildTask(
                plugin
            ) {
                if (hasDoneInitialLimbo)
                {
                    val reconnectServer = reconnectAttempt(player.proxyPlayer)
                    if (reconnectServer != null)
                    {
                        player.proxyPlayer.sendMessage(
                            Component
                                .text(
                                    "We found a lobby server for you to join! You're being sent now..."
                                )
                                .color(NamedTextColor.GREEN)
                        )
                        player.disconnect(reconnectServer)
                        return@buildTask
                    }
                }

                player.proxyPlayer.sendMessage(
                    Component
                        .text("You are in Limbo!")
                        .color(NamedTextColor.RED)
                )

                player.proxyPlayer.sendMessage(
                    Component
                        .text("We're attempting to connect you to the main server... This may take a while!")
                        .color(NamedTextColor.GRAY)
                )

                player.proxyPlayer.sendMessage(
                    Component
                        .text(
                            "There ${
                                if (plugin.limboServer.players.size == 1) "is" else "are"
                            } currently ${plugin.limboServer.players.size} player${
                                if (plugin.limboServer.players.size == 1) "" else "s"
                            } in Limbo."
                        )
                        .color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.ITALIC)
                )

                hasDoneInitialLimbo = true
            }
            .repeat(5L, TimeUnit.SECONDS)
            .schedule()
    }

    override fun onDisconnect()
    {
        plugin.limboServer.players.remove(this.player)
        task.cancel()
    }
}
