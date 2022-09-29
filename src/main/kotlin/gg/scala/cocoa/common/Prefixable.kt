package gg.scala.cocoa.common

interface Prefixable {

    fun getPrefix(): String?

    fun shouldCheckForPrefix(): Boolean

    fun isPrefixed(message: String): Boolean

}
