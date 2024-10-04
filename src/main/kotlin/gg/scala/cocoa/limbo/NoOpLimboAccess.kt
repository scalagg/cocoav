package gg.scala.cocoa.limbo

import com.velocitypowered.api.proxy.Player

/**
 * @author GrowlyX
 * @since 10/4/2024
 */
object NoOpLimboAccess : LimboAccess
{
    override fun spawn(player: Player)
    {
    }
}
