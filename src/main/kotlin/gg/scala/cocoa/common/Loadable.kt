package gg.scala.cocoa.common

import java.util.concurrent.CompletableFuture

interface Loadable<T> {

    fun load(future: CompletableFuture<T>)

}
