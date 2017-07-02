package io.github.vjames19.futures.jdk8

import org.amshove.kluent.AnyException
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.CompletableFuture

/**
 * Created by victor.reventos on 7/1/17.
 */
object FutureSpec : Spek({

    val success = 1.toFuture()
    val failed = IllegalArgumentException().toFuture<Int>()

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
                { failed.filter { it == 2 }.get() } shouldThrow AnyException
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

    describe("zipWith") {
        given("a successful future") {
            it("should zip them") {
                success.zipWith(success) { a, b -> a + b }.get() shouldEqual 2
                success.zipWith(ImmediateFuture { "Hello" }) { a, b -> a.toString() + b }.get() shouldEqual "1Hello"
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { failed.zipWith(failed) { a, b -> a + b }.get() } shouldThrow AnyException
                { success.zipWith(failed) { a, b -> a + b }.get() } shouldThrow AnyException
                { failed.zipWith(success) { a, b -> a + b }.get() } shouldThrow AnyException
            }
        }
    }

    describe("firstCompletedOf") {
        given("a successful future") {
            it("should return the first completed") {
                val f300 = Future { Thread.sleep(300); 3 }
                val f200 = Future { Thread.sleep(200); 2 }
                val f10 = Future { Thread.sleep(10); 1 }

                Future.firstCompletedOf(listOf(f300, f200, f10)).get() shouldEqual 1
            }
        }

        given("a failed future") {
            given("that its the first to complete") {
                it("should return it") {
                    val f300 = Future { Thread.sleep(300); 3 }
                    val f200 = Future { Thread.sleep(200); 2 }
                    val f10 = Future<Int> { Thread.sleep(10); throw IllegalArgumentException() }

                    ({ Future.firstCompletedOf(listOf(f300, f200, f10)).get() }) shouldThrow AnyException
                }
            }

            given("that its not the first one to complete") {
                it("should return the first one") {
                    val f300 = Future { Thread.sleep(300); 3 }
                    val f200 = Future<Int> { Thread.sleep(200); throw IllegalArgumentException() }
                    val f10 = Future { Thread.sleep(10); 1 }

                    Future.firstCompletedOf(listOf(f300, f200, f10)).get() shouldEqual 1
                }
            }
        }
    }

    describe("allAsList") {
        given("a list of all successful futures") {
            it("should return a successful future") {
                Future.allAsList(listOf(1.toFuture(), 2.toFuture(), 3.toFuture())).get() shouldEqual listOf(1, 2, 3)
            }
        }

        given("a list with a failed future") {
            it("should return a failed future") {
                { Future.allAsList(listOf(1.toFuture(), 2.toFuture(), IllegalArgumentException().toFuture())).get() } shouldThrow AnyException
            }
        }
    }

    describe("successfulList") {
        given("a list of all successful futures") {
            it("should return a successful future") {
                Future.successfulList(listOf(1.toFuture(), 2.toFuture(), 3.toFuture())).get() shouldEqual listOf(1, 2, 3)
            }
        }

        given("a list with a failed future") {
            it("should return a failed future") {
                Future.successfulList(listOf(1.toFuture(), 2.toFuture(), IllegalArgumentException().toFuture())).get() shouldEqual listOf(1, 2)
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
                val futures = (1..3).toList().map { it.toFuture() }
                Future.fold(futures, 0) { acc, i -> acc + i}.get() shouldEqual 1 + 2 + 3
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toFuture() } + IllegalArgumentException().toFuture<Int>()
                ({ Future.fold(futures, 0) { acc, i -> acc + i}.get() }) shouldThrow AnyException
            }
        }
    }

    describe("reduce") {
        given("a list of all successful futures") {
            it("should reduce it") {
                val futures = (1..3).toList().map { it.toFuture() }
                Future.reduce(futures) { acc, i -> acc + i}.get() shouldEqual (1 + 2 + 3)
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toFuture() } + IllegalArgumentException().toFuture<Int>()
                ({ Future.reduce(futures) { acc, i -> acc + i}.get() }) shouldThrow AnyException
            }
        }

        given("an empty list") {
            it("should throw UnsupportedOperationException") {
                ({ Future.reduce(emptyList<CompletableFuture<Int>>()) { acc, i -> acc + i}.get() }) shouldThrow UnsupportedOperationException::class
            }
        }
    }

    describe("transform") {
        given("a list of all successful futures") {
            it("should transform them") {
                Future.transform((1..3).toList().map { it.toFuture() }) { it + 1 }.get() shouldEqual (2..4).toList()
            }
        }

        given("a list with a failed future") {
            it("should return the failure") {
                val futures = (1..3).toList().map { it.toFuture() } + IllegalArgumentException().toFuture<Int>()
                ({ Future.transform(futures) { it + 1 }.get() }) shouldThrow AnyException
            }
        }
    }
})
