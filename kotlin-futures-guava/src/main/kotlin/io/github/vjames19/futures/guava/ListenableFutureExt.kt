package io.github.vjames19.futures.jdk8

import com.google.common.util.concurrent.*
import java.util.*
import java.util.concurrent.*
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier

// Creation
inline fun <A> Future(executor: ExecutorService = ForkJoinExecutor, crossinline block: () -> A): ListenableFuture<A> {
    val service: ListeningExecutorService = MoreExecutors.listeningDecorator(executor)
    return service.submit(Callable<A>{ block() })
}

inline fun <A> ImmediateFuture(crossinline block: () -> A): ListenableFuture<A> = Future(DirectExecutor, block)

fun <A> A.toListenableFuture(): ListenableFuture<A> = Futures.immediateFuture(this)

fun <A> Throwable.toListenableFuture(): ListenableFuture<A> = Futures.immediateFailedFuture(this)

// Monadic Operations
inline fun <A, B> ListenableFuture<A>.map(executor: Executor = ForkJoinExecutor, crossinline f: (A) -> B): ListenableFuture<B> =
        Futures.transform(this, com.google.common.base.Function { f(it!!) }, executor)

inline fun <A, B> ListenableFuture<A>.flatMap(executor: Executor = ForkJoinExecutor, crossinline f: (A) -> ListenableFuture<B>): ListenableFuture<B> =
        Futures.transformAsync(this, AsyncFunction { f(it!!) }, executor)

fun <A> ListenableFuture<ListenableFuture<A>>.flatten(): ListenableFuture<A> = flatMap { it }

inline fun <A> ListenableFuture<A>.filter(executor: Executor = ForkJoinExecutor, crossinline predicate: (A) -> Boolean): ListenableFuture<A> =
        map(executor) {
            if (predicate(it)) it else throw NoSuchElementException("ListenableFuture.filter predicate is not satisfied")
        }

fun <A, B> ListenableFuture<A>.zip(other: ListenableFuture<B>, executor: Executor = ForkJoinPool.commonPool()): ListenableFuture<Pair<A, B>> =
        zip(other, executor) { a, b -> a to b }

inline fun <A, B, C> ListenableFuture<A>.zip(other: ListenableFuture<B>, executor: Executor = ForkJoinExecutor, crossinline f: (A, B) -> C): ListenableFuture<C> =
        flatMap(executor) { a -> other.map(executor) { b -> f(a, b) } }

// Error handling / Recovery
inline fun <A> ListenableFuture<A>.recover(crossinline f: (Throwable) -> A): ListenableFuture<A> =
        Futures.catching(this, Throwable::class.java) { f(it!!.cause ?: it) }

inline fun <A> ListenableFuture<A>.recoverWith(executor: Executor = ForkJoinExecutor, crossinline f: (Throwable) -> ListenableFuture<A>): ListenableFuture<A> {
    return Futures.catchingAsync(this, Throwable::class.java) { f(it!!.cause ?: it)}
}

inline fun <A, reified E : Throwable> ListenableFuture<A>.mapError(crossinline f: (E) -> Throwable): ListenableFuture<A> {
    return Futures.catching(this, E::class.java) { throw f(it!!) }
}

inline fun <A> ListenableFuture<A>.fallbackTo(executor: Executor = ForkJoinExecutor, crossinline f: () -> ListenableFuture<A>): ListenableFuture<A> =
        recoverWith(executor, { f() })


// Callbacks
inline fun <A> ListenableFuture<A>.onFailure(executor: Executor = ForkJoinExecutor, crossinline f: (Throwable) -> Unit): ListenableFuture<A> {
    Futures.addCallback(this, object : FutureCallback<A> {
        override fun onSuccess(result: A?) {
        }

        override fun onFailure(t: Throwable) {
            f(t)
        }
    }, executor)
    return this
}

inline fun <A> ListenableFuture<A>.onSuccess(executor: Executor = ForkJoinExecutor, crossinline f: (A) -> Unit): ListenableFuture<A> {
    Futures.addCallback(this, object : FutureCallback<A> {
        override fun onSuccess(result: A?) {
            f(result!!)
        }

        override fun onFailure(t: Throwable) {
        }
    }, executor)
    return this
}

inline fun <A> ListenableFuture<A>.onComplete(executor: Executor = ForkJoinExecutor, crossinline onFailure: (Throwable) -> Unit, crossinline onSuccess: (A) -> Unit): ListenableFuture<A> {
    Futures.addCallback(this, object : FutureCallback<A> {
        override fun onSuccess(result: A?) {
            onSuccess(result!!)
        }

        override fun onFailure(t: Throwable) {
            onFailure(t)
        }
    }, executor)
    return this
}


object Future {

    fun <A, R> fold(futures: Iterable<ListenableFuture<A>>, initial: R, executor: Executor = ForkJoinExecutor, op: (R, A) -> R): ListenableFuture<R> =
            fold(futures.iterator(), initial, executor, op)

    fun <A, R> fold(iterator: Iterator<ListenableFuture<A>>, initial: R, executor: Executor = ForkJoinExecutor, op: (R, A) -> R): ListenableFuture<R> =
            if (!iterator.hasNext()) initial.toListenableFuture()
            else iterator.next().flatMap(executor) { fold(iterator, op(initial, it), executor, op) }

    fun <A> reduce(iterable: Iterable<ListenableFuture<A>>, executor: Executor = ForkJoinExecutor, op: (A, A) -> A): ListenableFuture<A> {
        val iterator = iterable.iterator()
        return if (!iterator.hasNext()) throw UnsupportedOperationException("Empty collection can't be reduced.")
        else iterator.next().flatMap { fold(iterator, it, executor, op) }
    }

    fun <A, B> transform(iterable: Iterable<ListenableFuture<A>>, executor: Executor = ForkJoinExecutor, f: (A) -> B): ListenableFuture<List<B>> =
            iterable.fold(mutableListOf<B>().toListenableFuture()) { fr, fa ->
                fr.zip(fa, executor) { r, a -> r.add(f(a)); r }
            }.map(executor) { it.toList() }
}
