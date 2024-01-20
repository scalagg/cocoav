package gg.scala.cocoa.dps

import io.lettuce.core.api.StatefulRedisConnection

/**
 * @author GrowlyX
 * @since 9/24/2023
 */
object DPSRedisShared
{
    val keyValueCache: StatefulRedisConnection<String, String> = DPSRedisService<Any>("dpskv")
        .configure { internal().connect() }
}
