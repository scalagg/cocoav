package gg.scala.cocoa.lemon

import gg.scala.common.Savable
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.storable.IDataStoreObject
import gg.scala.store.storage.type.DataStoreStorageType
import java.util.*

/**
 * @author GrowlyX
 * @since 8/26/2022
 */
data class TwoFactorModel(
    override val identifier: UUID,
    var currentIpAddress: String,
    var previousIpAddress: String? = null,
    var persistIpAddress: Boolean = true,
    var sessionAuthenticated: Boolean = false,
    var exempt: Boolean = false,
    var secret: String? = null
) : IDataStoreObject, Savable
{
    override fun save() =
        DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()
            .save(this, DataStoreStorageType.REDIS)
}
