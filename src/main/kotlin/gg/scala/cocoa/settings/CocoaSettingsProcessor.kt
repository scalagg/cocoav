package gg.scala.cocoa.settings

import com.google.common.io.Files
import gg.scala.cocoa.CocoaVelocityPlugin
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializer
import java.io.File
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

        CocoaVelocityPlugin.INSTANCE.logger
            .info("Configured Serializers.kt to support CocoaSettingsModel.")
    }

    inline fun <reified T> locate(): T?
    {
        return settings[T::class] as T
    }

    inline fun <reified T> loadSettings(id: String)
    {
        val reader = File(
            CocoaVelocityPlugin.INSTANCE.directoryFile,
            "$id.json"
        ).reader()

        saveSettingsToFile(T::class, id)

        settings[T::class] = Serializers.gson.fromJson(reader, T::class.java) as CocoaSettingsModel
    }

    fun saveSettingsToFile(
        kClass: KClass<*>, id: String
    )
    {
        val file = File(
            CocoaVelocityPlugin.INSTANCE.directoryFile,
            "$id.json"
        )

        if (!file.exists())
        {
            file.createNewFile()

            settings.putIfAbsent(
                kClass, kClass.java.newInstance() as CocoaSettingsModel
            )
        }

        if (settings[kClass] != null)
        {
            Files.write(
                Serializers.gson.toJson(
                    settings[kClass], kClass.java
                ),
                file, Charsets.UTF_8
            )
        }
    }
}
