package gg.scala.cocoa.settings

import gg.scala.store.storage.storable.IDataStoreObject
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializable
import java.util.*

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
abstract class CocoaSettingsModel : AbstractTypeSerializable, IDataStoreObject
{
    override val identifier: UUID = UUID.randomUUID()
}
