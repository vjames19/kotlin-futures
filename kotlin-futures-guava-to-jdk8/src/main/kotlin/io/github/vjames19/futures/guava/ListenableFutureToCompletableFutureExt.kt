package io.github.vjames19.futures.guava

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

fun <T> ListenableFuture<T>.toCompletableFuture(executor: Executor = DirectExecutor): CompletableFuture<T> {
    val future = CompletableFuture<T>()

    onComplete(
            executor = executor,
            onFailure = { future.completeExceptionally(it) },
            onSuccess = { future.complete(it)}
    )

    return future
}
