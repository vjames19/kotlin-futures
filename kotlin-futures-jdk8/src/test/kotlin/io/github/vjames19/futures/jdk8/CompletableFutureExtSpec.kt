package io.github.vjames19.futures.jdk8

import org.amshove.kluent.AnyException
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

/**
 * Created by victor.reventos on 7/1/17.
 */
object CompletableFutureExtSpec : Spek({

    val success = ImmediateFuture { 1 }
    val failed = ImmediateFuture<Int> { throw IllegalArgumentException() }

    describe("map") {
        given("a successful future") {
            it("should transform it") {
                success.map { it + 1 }.get() shouldEqual 2
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { failed.map { it + 1 }.get() } shouldThrow Exception::class
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
                { failed.flatMap { ImmediateFuture { it + 1 } }.get() } shouldThrow Exception::class
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
                { Future { Future { throw IllegalArgumentException() } }.flatten().get() } shouldThrow Exception::class
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
                { success.filter { it == 2 }.get() } shouldThrow Exception::class
            }
        }

        given("a failed future") {
            it("should throw the exception") {
                { failed.filter { it == 2 }.get() } shouldThrow Exception::class
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
})
