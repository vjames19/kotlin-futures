package io.github.vjames19.futures.guava

import com.google.common.util.concurrent.ListenableFuture
import io.github.vjames19.futures.guava.*
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * Created by victor.reventos on 7/1/17.
 */
object ListenableFutureSpec : Spek({

    val success = 1.toListenableFuture()
    val failed = IllegalArgumentException().toListenableFuture<Int>()

    describe("map") {
        given("a successful future") {
            it("should transform it") {
                success.map { it + 1 }.get() shouldEqual 2
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { failed.map { it + 1 }.get() } shouldThrow AnyException
            }
        }
    }

    describe("flatMap") {
        given("a successful future") {
            it("should transform it") {
                success.flatMap { ImmediateFuture { it + 1 } }.get() shouldEqual 2
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { failed.flatMap { ImmediateFuture { it + 1 } }.get() } shouldThrow AnyException
            }
        }
    }

    describe("flatten") {
        given("a successful future") {
            it("should flatten it") {
                Future { Future { 1 } }.flatten().get() shouldEqual 1
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { Future { Future { throw IllegalArgumentException() } }.flatten().get() } shouldThrow AnyException
            }
        }
    }

    describe("filter") {
        given("a future that meets the predicate") {
            it("should return it as is") {
                success.filter { it == 1 }.get() shouldEqual 1
            }
        }

        given("a future that doesn't meet the predicate") {
            it("should throw the exception") {
                { success.filter { it == 2 }.get() } shouldThrow AnyException
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { failed.filter { it == 1 }.get() } shouldThrow AnyException
            }
        }
    }

    describe("recover") {
        given("a successful future") {
            it("it shouldn't have to recover") {
                success.recover { 2 }.get() shouldEqual 1
            }
        }

        given("a failed future") {
            it("should recover") {
                failed.recover { 2 }.get() shouldEqual 2
            }
        }

        given("an exception that we can't recover from") {
            it("should throw the exception") {
                { failed.recover { throw IllegalArgumentException() }.get() } shouldThrow AnyException
            }
        }
    }

    describe("fallbackTo") {
        given("a successful future") {
            it("shouldn't have to use the fallback") {
                success.fallbackTo { Future { 2 } }.get() shouldEqual 1
            }
        }

        given("a failed future") {
            it("should fallback to the specified future") {
                failed.fallbackTo { Future { 2 } }.get() shouldEqual 2
            }
        }
    }

    describe("mapError") {
        given("a successful future") {
            it("it shouldn't have to map the error") {
                success.mapError { _: IllegalArgumentException -> IllegalStateException() }.get() shouldEqual 1
            }
        }

        given("a failed future") {
            on("an exception type that its willing to handle") {
                it("should map the error") {
                    { failed.mapError { _: IllegalArgumentException -> UnsupportedOperationException() }.get() } shouldThrowTheException AnyException withCause (UnsupportedOperationException::class)
                }
            }

            on("an exception type that it didn't register for") {
                it("should throw the original error") {
                    { failed.mapError { _: ClassNotFoundException -> UnsupportedOperationException() }.get() } shouldThrowTheException AnyException withCause (IllegalArgumentException::class)
                }
            }

            on("handling the supertype Throwable") {
                it("should map the error") {
                    { failed.mapError { _: Throwable -> UnsupportedOperationException() }.get() } shouldThrowTheException AnyException withCause (UnsupportedOperationException::class)
                }
            }
        }
    }

    describe("onFailure") {
        given("a successful future") {
            it("shouldn't call the callback") {
                success.onFailure(DirectExecutor) { throw IllegalStateException("onFailure shouldn't be called on a Successful future") }.get()
            }
        }

        given("a failed future") {
            it("should call the callback") {
                var capturedThrowable: Throwable? = null
                failed.onFailure(DirectExecutor) { capturedThrowable = it }.recover { 1 }.get()

                capturedThrowable!!.shouldBeInstanceOf(IllegalArgumentException::class.java)
            }
        }
    }

    describe("onSuccess") {
        given("a successful future") {
            it("should call the callback") {
                var capturedResult = 0
                success.onSuccess(DirectExecutor) { capturedResult = it }.get()

                capturedResult shouldEqual 1
            }
        }

        given("a failed future") {
            it("shouldn't call the callback") {
                failed.onSuccess { throw IllegalStateException("onSuccess shouldn't be called on a Failed future") }
                        .recover { 1 }.get()
            }
        }
    }

    describe("zip") {
        given("a successful future") {
            it("should zip them") {
                success.zip(success).get() shouldEqual (1 to 1)
                success.zip(ImmediateFuture { "Hello" }).get() shouldEqual (1 to "Hello")
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { failed.zip(failed).get() } shouldThrow AnyException
                { success.zip(failed).get() } shouldThrow AnyException
                { failed.zip(success).get() } shouldThrow AnyException
            }
        }
    }

    describe("zip with a function being passed") {
        given("a successful future") {
            it("should zip them") {
                success.zip(success) { a, b -> a + b }.get() shouldEqual 2
                success.zip(ImmediateFuture { "Hello" }) { a, b -> a.toString() + b }.get() shouldEqual "1Hello"
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { failed.zip(failed) { a, b -> a + b }.get() } shouldThrow AnyException
                { success.zip(failed) { a, b -> a + b }.get() } shouldThrow AnyException
                { failed.zip(success) { a, b -> a + b }.get() } shouldThrow AnyException
            }
        }
    }

    describe("fold") {
        given("a list of all successful futures") {
            it("should fold them") {
                val futures = (1..3).toList().map { it.toListenableFuture() }
                Future.fold(futures, 0) { acc, i -> acc + i }.get() shouldEqual 1 + 2 + 3
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toListenableFuture() } + IllegalArgumentException().toListenableFuture<Int>()
                ({ Future.fold(futures, 0) { acc, i -> acc + i }.get() }) shouldThrow AnyException
            }
        }
    }

    describe("reduce") {
        given("a list of all successful futures") {
            it("should reduce it") {
                val futures = (1..3).toList().map { it.toListenableFuture() }
                Future.reduce(futures) { acc, i -> acc + i }.get() shouldEqual (1 + 2 + 3)
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toListenableFuture() } + IllegalArgumentException().toListenableFuture<Int>()
                ({ Future.reduce(futures) { acc, i -> acc + i }.get() }) shouldThrow AnyException
            }
        }

        given("an empty list") {
            it("should throw UnsupportedOperationException") {
                ({ Future.reduce(emptyList<ListenableFuture<Int>>()) { acc, i -> acc + i }.get() }) shouldThrow UnsupportedOperationException::class
            }
        }
    }

    describe("transform") {
        given("a list of all successful futures") {
            it("should transform them") {
                Future.transform((1..3).toList().map { it.toListenableFuture() }) { it + 1 }.get() shouldEqual (2..4).toList()
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toListenableFuture() } + IllegalArgumentException().toListenableFuture<Int>()
                ({ Future.transform(futures) { it + 1 }.get() }) shouldThrow AnyException
            }
        }
    }
})
