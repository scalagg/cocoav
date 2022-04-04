package gg.scala.cocoa.settings

import com.google.common.io.Files
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.store.storage.type.DataStoreStorageType
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
            it.setPrettyPrinting()
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
        saveSettingsToFile(T::class, id, commit = false)

        val reader = File(
            CocoaVelocityPlugin.INSTANCE.directoryFile,
            "$id.json"
        ).reader()

        settings[T::class] = Serializers.gson.fromJson(reader, T::class.java) as CocoaSettingsModel
    }

    fun saveSettingsToFile(
        kClass: KClass<*>, id: String, commit: Boolean = true
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

            if (commit)
            {
                try
                {
                    CocoaVelocityPlugin.INSTANCE.cloudLayer
                        .save(settings[kClass]!!, DataStoreStorageType.REDIS)
                        .thenRun {
                            CocoaRedisSubscriber.buildMessage(
                                "cloud-sync", mutableMapOf()
                            ).publish(
                                channel = "cocoa"
                            )
                        }
                } catch (exception: Exception)
                {
                    println("[Cocoa] Failed to commit to cloud: ${exception.message}")
                }
            }
        }
    }
}
