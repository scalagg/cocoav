package gg.scala.cocoa.lemon.metadata

import gg.scala.cocoa.dps.DPSDataSync
import gg.scala.cocoa.dps.DPSDataSyncKeys
import gg.scala.cocoa.dps.DPSDataSyncSource
import gg.scala.common.metadata.NetworkMetadata
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 8/27/2024
 */
object NetworkMetadataDataSync : DPSDataSync<NetworkMetadata>()
{
    object NetworkMetadataKeys : DPSDataSyncKeys
    {
        override fun store() = Key.key("network", "metadata")
        override fun newStore() = "network-metadata"
        override fun sync() = Key.key("network", "metasync")
    }

    override fun keys() = NetworkMetadataKeys
    override fun type() = NetworkMetadata::class.java

    override fun locatedIn() = DPSDataSyncSource.Mongo
}
