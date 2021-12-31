package gg.scala.cocoa

import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*

/**
 * @author GrowlyX
 * @since 12/31/2021
 */
class CocoaVelocitySettingsEntry(
    val id: String,
    val value: Boolean,
    override val identifier: UUID =
        UUID.randomUUID(),
) : IDataStoreObject
