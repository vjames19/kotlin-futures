package io.github.vjames19.futures.jdk8

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

object DirectExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}

object ForkJoinExecutor : ExecutorService by ForkJoinPool.commonPool()