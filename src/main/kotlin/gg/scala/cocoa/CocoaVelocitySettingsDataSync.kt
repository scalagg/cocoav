package gg.scala.cocoa

import gg.scala.cocoa.dps.DPSDataSync
import gg.scala.cocoa.dps.DPSDataSyncKeys
import gg.scala.cocoa.dps.DPSDataSyncSource
import net.kyori.adventure.key.Key
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 1/19/2024
 */
object CocoaVelocitySettingsDataSync : DPSDataSync<CocoaVelocitySettings>()
{
    object CocoaDataSyncKeys : DPSDataSyncKeys
    {
        override fun newStore() = "cocoa-settings"

        override fun store() = Key.key("cocoa", "settings")
        override fun sync() = Key.key("cocoa", "settingsync")
    }

    override fun locatedIn() = DPSDataSyncSource.Mongo

    override fun keys() = CocoaDataSyncKeys
    override fun type() = CocoaVelocitySettings::class.java

    fun useThenSave(block: CocoaVelocitySettings.() -> Unit): CompletableFuture<Void>
    {
        val cached = cached()
        cached.block()

        return sync(cached)
    }
}
