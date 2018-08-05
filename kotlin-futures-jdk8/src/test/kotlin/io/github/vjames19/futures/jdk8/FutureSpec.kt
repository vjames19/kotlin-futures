package io.github.vjames19.futures.jdk8

import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.util.concurrent.CompletableFuture

/**
 * Created by victor.reventos on 7/1/17.
 */
object FutureSpec : Spek({

    val success = 1.toCompletableFuture()
    val failed = IllegalArgumentException().toCompletableFuture<Int>()

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
                success.onFailure { throw IllegalStateException("onFailure shouldn't be called on a Successful future") }.get()
            }
        }

        given("a failed future") {
            it("should call the callback") {
                var capturedThrowable: Throwable? = null
                failed.onFailure { capturedThrowable = it }.recover { 1 }.get()

                capturedThrowable!!.shouldBeInstanceOf(IllegalArgumentException::class.java)
            }
        }
    }

    describe("onSuccess") {
        given("a successful future") {
            it("should call the callback") {
                var capturedResult = 0
                success.onSuccess { capturedResult = it }.get()

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

    describe("firstCompletedOf") {

        given("a successful future") {
            it("should return the first completed") {
                val f3 = Future { Thread.sleep(3000); 3 }
                val f2 = Future { Thread.sleep(2000); 2 }
                val f1 = Future { Thread.sleep(1); 1 }


                withFutures(listOf(f3, f2, f1)) {
                    Future.firstCompletedOf(it).get() shouldEqual 1
                }

            }
        }

        given("a failed future") {
            given("that its the first to complete") {
                it("should return it") {

                    val f3 = Future { Thread.sleep(3000); 3 }
                    val f2 = Future { Thread.sleep(2000); 2 }
                    val f1 = Future<Int> { Thread.sleep(1); throw IllegalArgumentException() }
                    withFutures(listOf(f3, f2, f1)) {
                        ({ Future.firstCompletedOf(it).get() }) shouldThrow AnyException
                    }
                }
            }

            given("that its not the first one to complete") {
                it("should return the first one") {
                    val f3 = Future { Thread.sleep(3000); 3 }
                    val f2 = Future<Int> { Thread.sleep(2000); throw IllegalArgumentException() }
                    val f1 = Future { Thread.sleep(1); 1 }

                    withFutures(listOf(f3, f2, f1)) {
                        Future.firstCompletedOf(listOf(f3, f2, f1)).get() shouldEqual 1
                    }
                }
            }
        }
    }

    describe("allAsList") {
        given("a list of all successful futures") {
            it("should return a successful future") {
                Future.allAsList(listOf(1.toCompletableFuture(), 2.toCompletableFuture(), 3.toCompletableFuture())).get() shouldEqual listOf(1, 2, 3)
            }
        }

        given("a list with a failed future") {
            it("should return a failed future") {
                { Future.allAsList(listOf(1.toCompletableFuture(), 2.toCompletableFuture(), IllegalArgumentException().toCompletableFuture())).get() } shouldThrow AnyException
            }
        }
    }

    describe("successfulList") {
        given("a list of all successful futures") {
            it("should return a successful future") {
                Future.successfulList(listOf(1.toCompletableFuture(), 2.toCompletableFuture(), Future { Thread.sleep(10); 3 })).get() shouldEqual listOf(1, 2, 3)
            }
        }

        given("a list with a failed future") {
            it("should return the list with the successful futures") {
                Future.successfulList(listOf(1.toCompletableFuture(), 2.toCompletableFuture(), IllegalArgumentException().toCompletableFuture())).get() shouldEqual listOf(1, 2)
                Future.successfulList(listOf(failed, 2.toCompletableFuture(), IllegalArgumentException().toCompletableFuture())).get() shouldEqual listOf(2)
            }
        }

        given("a list with all failed futures") {
            it("should return an empty list") {
                Future.successfulList(listOf(failed, failed, failed)).get() shouldEqual emptyList()
            }
        }
    }

    describe("fold") {
        given("a list of all successful futures") {
            it("should fold them") {
                val futures = (1..3).toList().map { it.toCompletableFuture() }
                Future.fold(futures, 0) { acc, i -> acc + i }.get() shouldEqual 1 + 2 + 3
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toCompletableFuture() } + IllegalArgumentException().toCompletableFuture<Int>()
                ({ Future.fold(futures, 0) { acc, i -> acc + i }.get() }) shouldThrow AnyException
            }
        }
    }

    describe("reduce") {
        given("a list of all successful futures") {
            it("should reduce it") {
                val futures = (1..3).toList().map { it.toCompletableFuture() }
                Future.reduce(futures) { acc, i -> acc + i }.get() shouldEqual (1 + 2 + 3)
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toCompletableFuture() } + IllegalArgumentException().toCompletableFuture<Int>()
                ({ Future.reduce(futures) { acc, i -> acc + i }.get() }) shouldThrow AnyException
            }
        }

        given("an empty list") {
            it("should throw UnsupportedOperationException") {
                ({ Future.reduce(emptyList<CompletableFuture<Int>>()) { acc, i -> acc + i }.get() }) shouldThrow UnsupportedOperationException::class
            }
        }
    }

    describe("transform") {
        given("a list of all successful futures") {
            it("should transform them") {
                Future.transform((1..3).toList().map { it.toCompletableFuture() }) { it + 1 }.get() shouldEqual (2..4).toList()
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toCompletableFuture() } + IllegalArgumentException().toCompletableFuture<Int>()
                ({ Future.transform(futures) { it + 1 }.get() }) shouldThrow AnyException
            }
        }
    }
})

private fun <T> withFutures(futures: List<CompletableFuture<T>>, f: (List<CompletableFuture<T>>) -> Unit): Unit {
    f(futures)
    futures.forEach { it.cancel(true) }
}