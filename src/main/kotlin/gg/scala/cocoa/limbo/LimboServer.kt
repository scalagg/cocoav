package gg.scala.cocoa.limbo

import com.velocitypowered.api.plugin.PluginContainer
import gg.scala.cocoa.CocoaVelocityPlugin
import net.elytrium.limboapi.api.LimboFactory
import net.elytrium.limboapi.api.chunk.Dimension
import net.elytrium.limboapi.api.player.LimboPlayer
import java.util.*
import kotlin.jvm.optionals.getOrElse

/**
 * @author GrowlyX
 * @since 8/27/2024
 */
class LimboServer(plugin: CocoaVelocityPlugin)
{
    val players = LinkedList<LimboPlayer>()
    val limboFactory = plugin.server.pluginManager.getPlugin("limboapi")
        .flatMap(PluginContainer::getInstance)
        .map {
            it as LimboFactory
        }
        .getOrElse {
            throw IllegalStateException("No limbo system lol")
        }

    val world = limboFactory.createVirtualWorld(
        Dimension.THE_END,
        0.0,
        100.0,
        0.0,
        90f,
        0.0f
    )

    val server = limboFactory.createLimbo(world)
        .setName("limbo")
        .setWorldTime(18000)
}
