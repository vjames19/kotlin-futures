package io.github.vjames19.futures.jdk8

import java.util.concurrent.Executor

object DirectExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}
