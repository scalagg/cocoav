package gg.scala.cocoa.settings

import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializer
import kotlin.reflect.KClass

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
object CocoaSettingsProcessor
{
    val settings = mutableMapOf<KClass<*>, CocoaSettingsModel>()

    fun initialLoad()
    {
        Serializers.useGsonBuilderThenRebuild {
            it.registerTypeAdapter(
                CocoaSettingsModel::class.java,
                AbstractTypeSerializer<CocoaSettingsModel>()
            )
        }


    }
}
