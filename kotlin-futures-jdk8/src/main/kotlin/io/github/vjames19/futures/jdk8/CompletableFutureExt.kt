package io.github.vjames19.futures.jdk8

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier

// Creation
inline fun <T> Future(executor: Executor = ForkJoinPool.commonPool(), crossinline block: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(Supplier { block() }, executor)

inline fun <T> ImmediateFuture(crossinline block: () -> T): CompletableFuture<T> = Future(DirectExecutor, block)

fun <T> T.toFuture(): CompletableFuture<T> = CompletableFuture.completedFuture(this)

fun <T> Throwable.toFuture(): CompletableFuture<T> = CompletableFuture<T>().apply { completeExceptionally(this@toFuture) }

// Monadic Operations
inline fun <A, B> CompletableFuture<A>.map(executor: Executor = ForkJoinPool.commonPool(), crossinline f: (A) -> B): CompletableFuture<B> =
        thenApplyAsync(Function { f(it) }, executor)

inline fun <A, B> CompletableFuture<A>.flatMap(executor: Executor = ForkJoinPool.commonPool(), crossinline f: (A) -> CompletableFuture<B>): CompletableFuture<B> =
        thenComposeAsync(Function { f(it) }, executor)

fun <A> CompletableFuture<CompletableFuture<A>>.flatten(): CompletableFuture<A> = flatMap { it }

inline fun <A> CompletableFuture<A>.filter(executor: Executor = ForkJoinPool.commonPool(), crossinline predicate: (A) -> Boolean): CompletableFuture<A> =
        map(executor) {
            if (predicate(it)) it else throw NoSuchElementException("CompletableFuture.filter predicate is not satisfied")
        }

fun <A, B> CompletableFuture<A>.zip(other: CompletableFuture<B>, executor: Executor = ForkJoinPool.commonPool()): CompletableFuture<Pair<A, B>> =
        zipWith(other, executor) { a, b -> a to b }

inline fun <A, B, C> CompletableFuture<A>.zipWith(other: CompletableFuture<B>, executor: Executor = ForkJoinPool.commonPool(), crossinline f: (A, B) -> C): CompletableFuture<C> =
        thenCombineAsync(other, BiFunction { a, b -> f(a, b) }, executor)

// Error handling / Recovery
inline fun <A> CompletableFuture<A>.recover(crossinline f: (Throwable) -> A): CompletableFuture<A> = exceptionally { f(it) }

inline fun <A> CompletableFuture<A>.recoverWith(executor: Executor = ForkJoinPool.commonPool(), crossinline f: (Throwable) -> CompletableFuture<A>): CompletableFuture<A> {
    val future = CompletableFuture<A>()
    onComplete(executor,
            { f(it).onComplete(executor, { future.completeExceptionally(it) }, { future.complete(it) }) },
            { future.complete(it) }
    )
    return future
}

inline fun <A> CompletableFuture<A>.fallbackTo(executor: Executor = ForkJoinPool.commonPool(), crossinline f: () -> CompletableFuture<A>): CompletableFuture<A> =
        recoverWith(executor, { f() })


// Callbacks
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


object Future {

    fun <A> firstCompletedOf(futures: Iterable<CompletableFuture<A>>, executor: Executor = ForkJoinPool.commonPool()): CompletableFuture<A> {
        val future = CompletableFuture<A>()
        val onCompleteFirst: (CompletableFuture<A>) -> Unit = { it.onComplete(executor, { future.completeExceptionally(it) }, { future.complete(it) }) }
        futures.forEach(onCompleteFirst)
        return future
    }

    fun <A> allAsList(futures: Iterable<CompletableFuture<A>>, executor: Executor = ForkJoinPool.commonPool()): CompletableFuture<List<A>> =
            futures.fold(mutableListOf<A>().toFuture()) { fr, fa ->
                fr.zipWith(fa, executor) { r, a -> r.add(a); r }
            }.map(executor) { it.toList() }

    fun <A> successfulList(futures: Iterable<CompletableFuture<A>>, executor: Executor = ForkJoinPool.commonPool()): CompletableFuture<List<A>> =
            futures.fold(mutableListOf<A>().toFuture()) { fr, fa ->
                fr.map(executor) { r -> fa.map(executor) { r.add(it) }; r }
            }.map(executor) { it.toList() }

    fun <A, R> fold(futures: Iterable<CompletableFuture<A>>, initial: R, executor: Executor = ForkJoinPool.commonPool(), op: (R, A) -> R): CompletableFuture<R> =
            fold(futures.iterator(), initial, executor, op)

    fun <A, R> fold(iterator: Iterator<CompletableFuture<A>>, initial: R, executor: Executor = ForkJoinPool.commonPool(), op: (R, A) -> R): CompletableFuture<R> =
            if (!iterator.hasNext()) initial.toFuture()
            else iterator.next().flatMap(executor) { fold(iterator, op(initial, it), executor, op) }

    fun <A> reduce(iterable: Iterable<CompletableFuture<A>>, executor: Executor = ForkJoinPool.commonPool(), op: (A, A) -> A): CompletableFuture<A> {
        val iterator = iterable.iterator()
        return if (!iterator.hasNext()) throw UnsupportedOperationException("Empty collection can't be reduced.")
        else iterator.next().flatMap { fold(iterator, it, executor, op) }
    }

    fun <A, B> transform(iterable: Iterable<CompletableFuture<A>>, executor: Executor = ForkJoinPool.commonPool(), f: (A) -> B): CompletableFuture<List<B>> =
            iterable.fold(mutableListOf<B>().toFuture()) { fr, fa ->
                fr.zipWith(fa, executor) { r, a -> r.add(f(a)); r }
            }.map(executor) { it.toList() }
}
