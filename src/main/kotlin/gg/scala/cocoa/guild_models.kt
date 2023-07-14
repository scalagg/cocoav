package gg.scala.cocoa

import gg.scala.aware.thread.AwareThreadContext
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 7/14/2023
 */
data class GuildMember(val uniqueId: UUID)

data class Guild(
    override val identifier: UUID,
    val creator: GuildMember,
    val members: Map<UUID, GuildMember>
) : IDataStoreObject
{
    fun sendMessage(message: String) = CompletableFuture
        .allOf(
            sendGlobalPlayerMessage("§6§lGuild §7§l» $message", creator.uniqueId),
            *members.keys
                .map {
                    sendGlobalPlayerMessage("§6§lGuild §7§l» $message", it)
                }
                .toTypedArray()
        )

    private fun sendGlobalPlayerMessage(s: String, uniqueId: UUID): CompletableFuture<Void>
    {
        return CompletableFuture
            .runAsync {
                CocoaRedisSubscriber
                    .buildMessage(
                        "player-message",
                        mapOf(
                            "message" to s,
                            "target" to uniqueId.toString()
                        )
                    )
                    .publish(
                        channel = "lemon",
                        context = AwareThreadContext.SYNC
                    )
            }
    }
}
