package gg.scala.cocoa.lemon.category

enum class PunishmentCategory(val intensity: PunishmentCategoryIntensity) {

    KICK(PunishmentCategoryIntensity.MEDIUM),
    MUTE(PunishmentCategoryIntensity.LIGHT),
    BAN(PunishmentCategoryIntensity.MEDIUM),
    BLACKLIST(PunishmentCategoryIntensity.MEDIUM),
    IP_RELATIVE(PunishmentCategoryIntensity.MEDIUM);

    companion object {
        @JvmStatic
        val VALUES = values()

        @JvmStatic
        val PERSISTENT = listOf(MUTE, BAN, BLACKLIST)

        @JvmStatic
        val VALUES_2 = listOf(KICK, MUTE, BAN, BLACKLIST)

        @JvmStatic
        val IP_REL = listOf(BLACKLIST, BAN)
    }
}
