package gg.scala.cocoa.command.administration

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.cocoa.*
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.network.CocoaNetworkAddressHandler
import gg.scala.cocoa.presence.CocoaServerPresenceRunnable
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Dependency
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Optional
import gg.scala.commons.acf.annotation.Single
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.acf.velocity.contexts.OnlinePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
@CommandAlias("cpm")
@CommandPermission("cocoa.command.cocoa")
object CocoaCommand : BaseCommand()
{
    @Dependency
    lateinit var plugin: CocoaVelocityPlugin

    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("fix-fallback-presence")
    fun onFixFallbackPresence(player: Player)
    {
        CocoaServerPresenceRunnable.run()

        player.sendMessage(
            Component.text("Fixed fallback presence. (Should look into restarting proxy soon)")
        )
    }

    @Subcommand("max-player-count")
    fun onValueThing(player: Player, newValue: Int) = CocoaVelocitySettingsDataSync
        .useThenSave {
            presence.playerCap = newValue
        }
        .thenRun {
            player.sendMessage {
                "&aYou've updated the max player count to: &f$newValue".legacy()
            }
        }

    @Subcommand("maintenance deny-component update")
    @Description("Update the component shown when a player logs in when not whitelisted.")
    fun onDenyComponentUpdate(player: Player, message: String) = CocoaVelocitySettingsDataSync
        .useThenSave {
            presence.whitelistedNotification = message
                .replace("<nl>", "\n")
        }
        .thenRun {
            player.sendMessage {
                "&cYou've updated the component to:".legacy()
            }

            player.sendMessage(
                MiniMessage
                    .miniMessage()
                    .deserialize(
                        message.replace("<nl>", "\n")
                    )
            )
        }

    @Subcommand("maintenance deny-component append")
    @Description("Append text to the component shown when a player logs in when not whitelisted.")
    fun onDenyComponentAppend(player: Player, message: String) = CocoaVelocitySettingsDataSync
        .useThenSave {
            presence.whitelistedNotification += message
                .replace("<nl>", "\n")
        }
        .thenRun {
            player.sendMessage {
                "&cYou've added the following text to the component:".legacy()
            }

            player.sendMessage(
                MiniMessage
                    .miniMessage()
                    .deserialize(
                        message.replace("<nl>", "\n")
                    )
            )
        }

    @Subcommand("maintenance deny-component view")
    @Description("View the text shown when a player logs in when not whitelisted.")
    fun onDenyComponentView(player: Player)
    {
        val settings = CocoaVelocitySettingsDataSync.cached()
        player.sendMessage {
            "&aThe following text is shown:".legacy()
        }

        player.sendMessage {
            MiniMessage
                .miniMessage()
                .deserialize(settings.presence.whitelistedNotification)
        }
    }

    @CommandCompletion("@ranks")
    @Subcommand("maintenance update")
    @Description("Update maintenance mode's rank.")
    fun onUpdateRank(sender: Player, rank: Rank)
    {
        val settings = CocoaVelocitySettingsDataSync.cached()
        if (!settings.maintenance.enabled)
        {
            throw ConditionFailedException("You must enable maintenance mode to perform this command.")
        }

        plugin.updateMaintenanceRank(rank)

        sender.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aMaintenance mode rank has been set to: &f${rank.name}")
        }
    }

    @CommandCompletion("@ranks")
    @Subcommand("maintenance toggle")
    @Description("Toggle maintenance mode.")
    fun onToggle(
        sender: Player,
        @Optional rank: Rank?
    )
    {
        val settings = CocoaVelocitySettingsDataSync.cached()
        if (!settings.maintenance.enabled && rank == null)
        {
            throw ConditionFailedException("You must define a rank to toggle maintenance mode!")
        }

        plugin.updateMaintenanceMode(rank)

        sender.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(
                    if (!settings.maintenance.enabled)
                    {
                        CocoaRedisSubscriber.buildMessage(
                            "maintenance-kick",
                            mapOf()
                        ).publish(
                            channel = "cocoa"
                        )

                        "&aMaintenance mode has been enabled."
                    } else
                    {
                        "&cMaintenance mode has been disabled."
                    }
                )
        }
    }

    @Subcommand("maintenance info")
    @Description("View maintenance information.")
    fun onList(sender: Player)
    {
        val settings = CocoaVelocitySettingsDataSync.cached()
        if (!settings.maintenance.enabled)
        {
            throw ConditionFailedException("Maintenance mode is not enabled.")
        }

        val maintenanceRank = plugin.maintenanceRank()

        sender.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(
                    "&aThe server is currently restricted to players with a rank weight above: &f${maintenanceRank.name}"
                )
        }
    }

    enum class PresenceType
    {
        Maintenance, Public
    }

    enum class PresenceLine
    {
        Top, Bottom
    }

    @Subcommand("presence lines view")
    @Description("View presence lines.")
    fun onViewPresence(
        actor: Player, type: PresenceType
    )
    {
        val settings = CocoaVelocitySettingsDataSync.cached()
        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aViewing ${type.name} motd below:")
        }

        val presence = when (type)
        {
            PresenceType.Maintenance -> settings.presence.whitelistedPresence
            PresenceType.Public -> settings.presence.normalPresence
        }

        presence.split("\n")
            .forEach {
                actor.sendMessage {
                    LegacyComponentSerializer
                        .legacySection()
                        .deserialize(
                            LegacyComponentSerializer
                                .legacySection()
                                .serialize(
                                    MiniMessage
                                        .miniMessage()
                                        .deserialize(
                                            if (settings.presence.shouldCenterPresence)
                                                it.centered(132.2) else it
                                        )
                                )
                        )
                }
            }
    }

    @Subcommand("presence lines update")
    @Description("Update presence lines.")
    fun onUpdateLine(
        actor: Player, type: PresenceType,
        line: PresenceLine, content: String
    ) = CocoaVelocitySettingsDataSync
        .useThenSave {
            val lineIndex = line.ordinal

            when (type)
            {
                PresenceType.Maintenance ->
                {
                    val split = presence
                        .whitelistedPresence
                        .split("\n")
                        .toMutableList()

                    split[lineIndex] = content
                        .replace("&", "§")

                    actor.sendMessage {
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(
                                "&aYou've updated the ${type.name} presence ${line.name} line to:\n${
                                    LegacyComponentSerializer.legacyAmpersand()
                                        .serialize(
                                            MiniMessage.miniMessage().deserialize(split[lineIndex])
                                        )
                                }"
                            )
                    }

                    presence.whitelistedPresence = split
                        .joinToString("\n")
                }

                PresenceType.Public ->
                {
                    val split = presence
                        .normalPresence
                        .split("\n")
                        .toMutableList()

                    split[lineIndex] = content
                        .replace("&", "§")

                    actor.sendMessage {
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(
                                "&aYou've updated the ${type.name} presence ${line.name} line to:\n${
                                    LegacyComponentSerializer.legacyAmpersand()
                                        .serialize(
                                            MiniMessage.miniMessage().deserialize(split[lineIndex])
                                        )
                                }"
                            )
                    }

                    presence.normalPresence = split
                        .joinToString("\n")
                }
            }
        }
        .thenRun {
            actor.sendMessage {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&aYou've toggled presence centering.")
            }
        }

    @Subcommand("presence center")
    @Description("Center/left-align cocoa presences.")
    fun onCenter(actor: Player) = CocoaVelocitySettingsDataSync
        .useThenSave {
            presence.shouldCenterPresence = !presence.shouldCenterPresence
        }
        .thenRun {
            actor.sendMessage {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&aYou've toggled presence centering.")
            }
        }

    @Subcommand("network settings")
    @Description("View network configuration.")
    @CommandPermission("cocoa.command.inet-settings")
    fun onInetSettings(actor: Player)
    {
        val networking = CocoaVelocitySettingsDataSync.cached().networking

        val message = """
            &a&lCocoa Networking:
            
              &7● &fHandling Networking Checks:
               &d${networking.handleNetworkingChecks}
              &7● &fAPI Key: &b<redacted>
            
        """.trimIndent()

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message)
        }
    }

    @Subcommand("network info")
    @Description("View network info for a player.")
    @CommandPermission("cocoa.command.inet-info")
    fun onIpInfo(
        actor: Player, player: OnlinePlayer
    ): CompletableFuture<Void>
    {
        val start = System.currentTimeMillis()

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aFinding ip info for this player...")
        }

        val settings = CocoaVelocitySettingsDataSync.cached()
        if (!settings.networking.handleNetworkingChecks)
        {
            throw ConditionFailedException("Cocoa networking is currently disabled.")
        }

        return CocoaNetworkAddressHandler
            .requestHostData(
                player.player.remoteAddress.address.hostAddress
            )
            .exceptionally {
                it.printStackTrace()
                return@exceptionally null
            }
            .thenAccept { data ->
                if (data == null)
                {
                    actor.sendMessage {
                        LegacyComponentSerializer
                            .legacyAmpersand()
                            .deserialize("Something went wrong")
                    }
                    return@thenAccept
                }

                val message = """
                    &eIP info for &b${player.player.username}&e:
                      &7● &fLegitimate: ${
                    if (data.shouldBlock) "&cNo - VPN or Proxy" else "&aYes"
                }
                      &7● &fHost: &6${data.networkId} 
                     
                    &aThis took ${System.currentTimeMillis() - start}ms to run.
                """.trimIndent()

                actor.sendMessage {
                    LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(message)
                }
            }

    }

    @Subcommand("manager register-server")
    @Description("Register a server dynamically.")
    fun onServerRegister(
        actor: Player,
        @Single id: String,
        @Single hostName: String,
        port: Int
    )
    {
        CocoaVelocityPlugin.INSTANCE.server.registerServer(
            ServerInfo(id, InetSocketAddress(hostName, port))
        )

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&e$id&a has been registered.")
        }
    }

    @Subcommand("unregister")
    @Description("Remove a server dynamically.")
    fun onServerUnregister(
        actor: Player, @Single id: String
    )
    {
        val server = CocoaVelocityPlugin.INSTANCE.server.getServer(id).orElse(null)
            ?: throw ConditionFailedException("That server does not exist.")

        CocoaVelocityPlugin.INSTANCE.server.unregisterServer(server.serverInfo)

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&e$server&a has been unregistered.")
        }
    }
}
