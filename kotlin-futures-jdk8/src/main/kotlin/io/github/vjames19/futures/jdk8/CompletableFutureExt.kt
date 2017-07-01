package io.github.vjames19.futures.jdk8

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier

inline fun <T> Future(executor: Executor = ForkJoinPool.commonPool(), crossinline block: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(Supplier { block() }, executor)

inline fun <T> ImmediateFuture(crossinline block: () -> T): CompletableFuture<T> = Future(DirectExecutor, block)

inline fun <A, B> CompletableFuture<A>.map(executor: Executor = ForkJoinPool.commonPool(), crossinline f: (A) -> B): CompletableFuture<B> =
        thenApplyAsync(Function { f(it) }, executor)


inline fun <A, B> CompletableFuture<A>.flatMap(executor: Executor = ForkJoinPool.commonPool(), crossinline f: (A) -> CompletableFuture<B>): CompletableFuture<B> =
        thenComposeAsync(Function { f(it) }, executor)

fun <A> CompletableFuture<CompletableFuture<A>>.flatten(): CompletableFuture<A> = flatMap { it }

inline fun <A> CompletableFuture<A>.filter(executor: Executor = ForkJoinPool.commonPool(), crossinline predicate: (A) -> Boolean): CompletableFuture<A> =
        map(executor) {
            if (predicate(it)) it else throw NoSuchElementException("Future.filter predicate is not satisfied")
        }

inline fun <A> CompletableFuture<A>.recover(crossinline f: (Throwable) -> A): CompletableFuture<A> = exceptionally { f(it) }

inline fun <A> CompletableFuture<A>.fallbackTo(executor: Executor = ForkJoinPool.commonPool(), crossinline f: (Throwable) -> CompletableFuture<A>): CompletableFuture<A> {
    val future = CompletableFuture<A>()
    onComplete(executor,
            { f(it).onComplete(executor, { future.completeExceptionally(it) }, { future.complete(it) }) },
            { future.complete(it) }
    )
    return future
}


inline fun <A> CompletableFuture<A>.onFailure(executor: Executor = ForkJoinPool.commonPool(), crossinline f: (Throwable) -> Unit): CompletableFuture<A> =
        whenCompleteAsync(BiConsumer { _, throwable: Throwable? ->
            throwable?.let { f(it.cause ?: it) }
        }, executor)

inline fun <A> CompletableFuture<A>.onSuccess(executor: Executor = ForkJoinPool.commonPool(), crossinline f: (A) -> Unit): CompletableFuture<A> =
        whenCompleteAsync(BiConsumer { a: A, _ ->
            f(a)
        }, executor)

inline fun <A> CompletableFuture<A>.onComplete(executor: Executor = ForkJoinPool.commonPool(), crossinline g: (Throwable) -> Unit, crossinline f: (A) -> Unit): CompletableFuture<A> =
        whenCompleteAsync(BiConsumer { a: A, throwable: Throwable? ->
            if (throwable != null) {
                g(throwable.cause ?: throwable)
            } else {
                f(a)
            }
        }, executor)


fun <A, B> CompletableFuture<A>.zip(executor: Executor = ForkJoinPool.commonPool(), future: CompletableFuture<B>): CompletableFuture<Pair<A, B>> =
        zipWith(executor, future) { a, b -> a to b }

inline fun <A, B, C> CompletableFuture<A>.zipWith(executor: Executor = ForkJoinPool.commonPool(), other: CompletableFuture<B>, crossinline f: (A, B) -> C): CompletableFuture<C> =
        thenCombineAsync(other, BiFunction { a, b -> f(a, b) }, executor)
