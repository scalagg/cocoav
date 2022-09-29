package gg.scala.cocoa.common

import java.util.concurrent.CompletableFuture

interface Savable {

    fun save(): CompletableFuture<Void>

}
