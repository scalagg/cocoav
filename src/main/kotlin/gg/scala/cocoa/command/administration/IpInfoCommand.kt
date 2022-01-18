package gg.scala.cocoa.command.administration

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.network.CocoaNetworkAddressHandler
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/25/2021
 */
object IpInfoCommand
{
    @Command("inet-settings")
    @CommandPermission("cocoa.command.inet-settings")
    fun onInetSettings(
        actor: VelocityCommandActor
    )
    {
        val networking = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!.networking

        val message = """
            &a&lCocoa Networking:
            
              &7● &fHandling Networking Checks:
               &d${networking.handleNetworkingChecks}
              &7● &fAPI Key:
               &b${networking.apiKey}
            
        """.trimIndent()

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message)
        }
    }

    @Command("inet-info")
    @CommandPermission("cocoa.command.inet-info")
    fun onIpInfo(
        actor: VelocityCommandActor,
        player: Player
    )
    {
        val start = System.currentTimeMillis()

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aFinding ip info for this player...")
        }

        val settings = CocoaSettingsProcessor.locate<CocoaVelocitySettings>()!!

        if (!settings.networking.handleNetworkingChecks)
        {
            throw CommandErrorException("Cocoa networking is currently disabled.")
        }

        CompletableFuture.supplyAsync {
            CocoaNetworkAddressHandler.requestHostData(
                player.remoteAddress.address.hostAddress
            )
        }.whenComplete { data, throwable ->
            if (throwable == null && data != null)
            {
                val message = """
                    &eIP info for &b${player.username}&e:
                      &7● &fLegitimate: ${
                          if (data.shouldBlock)
                          {
                              "&cNo - VPN or Proxy"
                          } else
                          {
                              "&aYes"
                          }
                      }
                      &7● &fHost: &6${data.networkId} 
                     
                    &aThis took ${System.currentTimeMillis() - start}ms to run.
                """.trimIndent()

                actor.reply {
                    LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(message)
                }
            } else
            {
                throwable?.printStackTrace()

                actor.reply {
                    LegacyComponentSerializer.legacyAmpersand()
                        .deserialize("&cSomething went terribly wrong while executing this command.")
                }
            }
        }
    }
}
