package io.github.vjames19.futures.jdk8

import java.util.concurrent.*
import java.util.concurrent.Future

object DirectExecutor : ExecutorService {
    override fun execute(command: Runnable) {
        command.run()
    }

    override fun shutdown() {
    }

    override fun <T : Any?> submit(task: Callable<T>?): Future<T> {
        throw NotImplementedError()
    }

    override fun <T : Any?> submit(task: Runnable?, result: T): Future<T> {
        throw NotImplementedError()
    }

    override fun submit(task: Runnable?): Future<*> {
        throw NotImplementedError()
    }

    override fun shutdownNow(): MutableList<Runnable> {
        throw NotImplementedError()
    }

    override fun isShutdown(): Boolean {
        throw NotImplementedError()
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean {
        throw NotImplementedError()
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?): T {
        throw NotImplementedError()
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): T {
        throw NotImplementedError()
    }

    override fun isTerminated(): Boolean {
        throw NotImplementedError()
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?): MutableList<Future<T>> {
        throw NotImplementedError()
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): MutableList<Future<T>> {
        throw NotImplementedError()
    }
}

object ForkJoinExecutor : ExecutorService by ForkJoinPool.commonPool()