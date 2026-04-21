package gg.scala.cocoa.symphony

import gg.scala.cocoa.dps.DPSRedisShared
import java.util.concurrent.atomic.AtomicInteger

object SymphonyCompat
{
    private val cachedGlobalPlayers = AtomicInteger(0)

    fun globalPlayerCount(): Int = cachedGlobalPlayers.get()

    fun refreshGlobalPlayerCount()
    {
        val total = runCatching {
            DPSRedisShared.keyValueCache
                .sync()
                .hgetall("symphony:instances")
                .values
                .sumOf { count ->
                    count.toIntOrNull() ?: 0
                }
        }.getOrNull() ?: 0

        cachedGlobalPlayers.set(total)
    }

    fun proxyId(fallback: String = "proxy-1"): String
    {
        return System.getenv("GAME_SERVER_ID")
            ?: System.getenv("PROXY_ID")
            ?: fallback
    }
}
