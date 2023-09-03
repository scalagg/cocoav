package gg.scala.cocoa.command

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.samstevens.totp.recovery.RecoveryCodeGenerator
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.totp.TimeBasedOneTimePasswordUtil
import gg.scala.cocoa.legacy
import gg.scala.cocoa.lemon.TwoFactorModel
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Dependency
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Single
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.acf.velocity.contexts.OnlinePlayer
import gg.scala.store.controller.DataStoreObjectControllerCache
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
@CommandAlias("2fa|mfa")
@CommandPermission("cocoa.2fa")
object MfaCommand : BaseCommand()
{
    @Dependency
    lateinit var plugin: CocoaVelocityPlugin

    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("authenticate")
    @Description("Authenticate using your 2fa code.")
    fun onAuthenticate(
        player: Player, @Single code: String
    ): CompletableFuture<Void>
    {
        val model = DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()
            .localCache()[player.uniqueId]
            ?: throw ConditionFailedException(
                "Please rejoin, we did not load your MFA account."
            )

        if (model.exempt)
        {
            throw ConditionFailedException("You are exempt from two-factor-authentication.")
        }

        if (model.secret == null)
        {
            throw ConditionFailedException("You must configure 2fa first by performing /mfa configure.")
        }

        if (model.sessionAuthenticated)
        {
            throw ConditionFailedException("You have already authenticated this session.")
        }

        val correctCode = TimeBasedOneTimePasswordUtil
            .generateCurrentNumberString(model.secret!!)

        if (!correctCode.equals(code, true))
        {
            var foundRecoveryCode = false

            if (model.recoveryCodes != null)
            {
                if (model.recoveryCodes!!.contains(code))
                {
                    model.recoveryCodes!! -= code
                    model.save()

                    player.sendMessage(
                        Component
                            .text(
                                "[You have ${model.recoveryCodes!!.size} remaining recovery codes]"
                            )
                            .color(TextColor.fromHexString("#ff0000"))
                    )

                    foundRecoveryCode = true
                }
            }

            if (!foundRecoveryCode)
            {
                throw ConditionFailedException("The code you have provided is invalid.")
            }
        }

        model.sessionAuthenticated = true
        model.persistIpAddress = true

        model.currentIpAddress = player
            .remoteAddress.address.hostAddress

        return model.save().thenRun {
            player.sendMessage("&a[auth] &aYou have been authenticated.".legacy())

            if (
                player.currentServer.isPresent &&
                player.currentServer.get().serverInfo.name.contains("pack")
            )
            {
                val isStaffMember = player
                    .hasPermission("cocoa.staff")

                val bestHubServer = if (isStaffMember)
                    plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()

                player.createConnectionRequest(bestHubServer).connect()
            }
        }
    }

    @Subcommand("generate-recovery-codes")
    @Description("Generate recovery codes.")
    fun onRecoveryCodeRegen(player: Player)
    {
        val modelSelf = DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()
            .localCache()[player.uniqueId]
            ?: throw ConditionFailedException(
                "Please rejoin, we did not load your MFA account."
            )

        if (!modelSelf.sessionAuthenticated)
        {
            throw ConditionFailedException(
                "You must authenticate before performing this command"
            )
        }

        modelSelf.recoveryCodes = RecoveryCodeGenerator()
            .generateCodes(8)
            .toMutableList()

        player.sendMessage(
            Component
                .text("[Click to copy your new recovery codes]")
                .color(TextColor.fromHexString("#dffc03"))
                .clickEvent(
                    ClickEvent.copyToClipboard(
                        modelSelf.recoveryCodes!!.joinToString("\n")
                    )
                )
                .hoverEvent(
                    HoverEvent.showText(Component.text("Click to copy!"))
                )
        )
        player.sendMessage(
            Component
                .text("[These codes will not be shown again!]")
                .color(TextColor.fromHexString("#fc03c2"))
        )
    }

    @Subcommand("configure")
    @Description("Configure 2fa for your account")
    fun onConfigure(player: Player)
    {
        val model = DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()
            .localCache()[player.uniqueId]
            ?: throw ConditionFailedException(
                "Please rejoin, we did not load your MFA account."
            )

        if (model.secret != null)
        {
            throw ConditionFailedException("Multi-factor authentication has already been configured on your account.")
        }

        if (model.exempt)
        {
            throw ConditionFailedException("You are exempt from two-factor-authentication.")
        }

        val secret = TimeBasedOneTimePasswordUtil.generateBase32Secret()
        model.secret = secret
        model.save().join()

        val url = TimeBasedOneTimePasswordUtil
            .qrImageUrl(
                CocoaSettingsProcessor
                    .locate<CocoaVelocitySettings>()
                    ?.totpIssuer
                    ?: "Scala%20Studios",
                secret,
                player.username
            )

        player.sendMessage(
            "&a[auth] &a&l(Click this message to copy the QR code!)".legacy()
                .clickEvent(
                    ClickEvent.copyToClipboard(url)
                )
                .hoverEvent(
                    HoverEvent.showText("&aClick to copy the QR code URL to your clipboard!".legacy())
                )
        )

        player.sendMessage(
            "&a[auth] &6&l(Click this message to open the QR code!)".legacy()
                .clickEvent(
                    ClickEvent.openUrl(url)
                )
                .hoverEvent(
                    HoverEvent.showText("&aClick to open the QR code URL to your clipboard!".legacy())
                )
        )

        player.sendMessage(
            "&a[auth] &aOnce you receive your 6 character code, enter it in &f/mfa authenticate&a.".legacy()
        )
    }

    @Subcommand("reset")
    @Description("Reset a player's 2FA configuration.")
    @CommandPermission("cocoa.2fa.command.reset")
    fun onReset(player: CommandSource, target: OnlinePlayer)
    {
        val model = DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()
            .localCache()[target.player.uniqueId]
            ?: throw ConditionFailedException(
                "Their MFA account was not loaded."
            )

        if (model.exempt)
        {
            throw ConditionFailedException("This player is already exempt.")
        }

        if (player is Player)
        {
            val modelSelf = DataStoreObjectControllerCache
                .findNotNull<TwoFactorModel>()
                .localCache()[player.uniqueId]
                ?: throw ConditionFailedException(
                    "Please rejoin, we did not load your MFA account."
                )

            if (!modelSelf.sessionAuthenticated)
            {
                throw ConditionFailedException(
                    "You must authenticate before performing this command"
                )
            }
        }

        model.secret = null
        model.exempt = false
        model.sessionAuthenticated = false
        model.save().join()

        target.player.sendMessage(
            "&a[auth] &aYour authentication configuration has been reset.".legacy()
        )

        player.sendMessage("&9[MFA Admin] &aYou have reset their 2FA configuration.".legacy())
    }

    @Subcommand("exempt")
    @Description("Exempt a user from 2FA.")
    @CommandPermission("cocoa.2fa.command.exempt")
    fun onExempt(player: CommandSource, target: OnlinePlayer)
    {
        val model = DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()
            .localCache()[target.player.uniqueId]
            ?: throw ConditionFailedException(
                "Their MFA account was not loaded."
            )

        if (model.exempt)
        {
            throw ConditionFailedException("This player is already exempt.")
        }

        if (player is Player)
        {
            val modelSelf = DataStoreObjectControllerCache
                .findNotNull<TwoFactorModel>()
                .localCache()[player.uniqueId]
                ?: throw ConditionFailedException(
                    "Please rejoin, we did not load your MFA account."
                )

            if (!modelSelf.sessionAuthenticated)
            {
                throw ConditionFailedException(
                    "You must authenticate before performing this command"
                )
            }
        }

        model.exempt = true
        model.sessionAuthenticated = true
        model.save()

        target.player.sendMessage(
            "&a[auth] &aYou are now exempt from MFA. You no longer need to authenticate.".legacy()
        )

        player.sendMessage("&9[MFA Admin] &aThis player has been exempt.".legacy())
    }

    @Subcommand("unexempt")
    @Description("Un-exempt a user from 2FA.")
    @CommandPermission("cocoa.2fa.command.exempt")
    fun onUnExempt(player: CommandSource, target: OnlinePlayer)
    {
        val model = DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()
            .localCache()[target.player.uniqueId]
            ?: throw ConditionFailedException(
                "Their MFA account was not loaded."
            )

        if (!model.exempt)
        {
            throw ConditionFailedException("This player is not exempt.")
        }

        if (player is Player)
        {
            val modelSelf = DataStoreObjectControllerCache
                .findNotNull<TwoFactorModel>()
                .localCache()[player.uniqueId]
                ?: throw ConditionFailedException(
                    "Please rejoin, we did not load your MFA account."
                )

            if (!modelSelf.sessionAuthenticated)
            {
                throw ConditionFailedException(
                    "You must authenticate before performing this command"
                )
            }
        }

        model.exempt = false
        model.sessionAuthenticated = false
        model.save()

        target.player.sendMessage(
            "&a[auth] &aYou are no longer exempt, you must authenticate now.".legacy()
        )

        player.sendMessage("&9[MFA Admin] &aThis player has been un-exempt.".legacy())
    }
}
