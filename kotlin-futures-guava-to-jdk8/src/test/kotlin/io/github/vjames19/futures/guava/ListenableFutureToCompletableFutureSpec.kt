package io.github.vjames19.futures.guava

import io.github.vjames19.futures.jdk8.ImmediateFuture
import io.github.vjames19.futures.jdk8.toListenableFuture
import org.amshove.kluent.shouldEqualTo
import org.amshove.kluent.shouldThrowTheException
import org.amshove.kluent.withCause
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.ExecutionException


object ListenableFutureToCompletableFutureSpec : Spek({

    describe("toCompletableFuture") {
        given("a successful future") {
            it("it should be able to return a successful CompletableFuture") {
                val listenableFuture = ImmediateFuture { 10 }
                val future = listenableFuture.toCompletableFuture()

                future.isDone shouldEqualTo true
                future.isCompletedExceptionally shouldEqualTo false

                future.get() shouldEqualTo 10
            }
        }

        given("a failed future") {
            it("should return the exception returned by the ListenableFuture") {
                val listenableFuture = IllegalArgumentException().toListenableFuture<String>()
                val future = listenableFuture.toCompletableFuture()

                ({ future.get() }) shouldThrowTheException ExecutionException::class withCause IllegalArgumentException::class
            }
        }
    }
})
