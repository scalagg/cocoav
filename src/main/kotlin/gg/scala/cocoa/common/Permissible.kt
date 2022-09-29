package gg.scala.cocoa.common

interface Permissible<T> {

    fun getPermission(): String?

    fun hasPermission(t: T): Boolean

}
