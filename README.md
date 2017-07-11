# kotlin-futures
A collections of extension functions to make the CompletableFuture API more **functional** and **Kotlin** like.

[![](https://jitpack.io/v/vjames19/kotlin-futures.svg)](https://jitpack.io/#vjames19/kotlin-futures) [![](https://travis-ci.org/vjames19/kotlin-futures.svg?branch=master)

# Table of Contents
- [kotlin-futures](#kotlin-futures)
- [Motivation](#motivation)
- [Download](#download)
- [How to use](#how-to-use)
  * [Creation](#creation)
  * [Composition](#composition)
    + [map](#map)
    + [flatMap](#flatmap)
    + [flatten](#flatten)
    + [filter](#filter)
    + [zip](#zip)
  * [Error Handling](#error-handling)
    + [recover](#recover)
    + [recoverWith](#recoverwith)
    + [fallbackTo](#fallbackto)
  * [Callbacks](#callbacks)
    + [onSuccess](#onsuccess)
    + [onFailure](#onfailure)
    + [onComplete](#oncomplete)
- [Tests](#tests)

# Motivation

Having worked in [Scala](https://www.scala-lang.org/what-is-scala.html) for some time and specifically using their 
[Future API](http://docs.scala-lang.org/overviews/core/futures.html) and then going back to **Kotlin a language I :heart: :heart:**,
I came to realize that the [CompletableFuture API](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
seems odd for defining and composing async operations. 

Every time I use the CompletableFuture API I find myself going back to the documentation to double check what a given function would do.

Now this might be a matter of taste, but being heavily inspired by [Scala's Future API](http://www.scala-lang.org/api/current/scala/concurrent/Future.html),
I decided to make this library to hopefully make the [CompletableFuture API](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
more **functional and kotlin like**

To achieve this I didn't want to introduce a new Future type and have to change any project to use the new Future type, hence by using
**extension functions** and **inlining** we can have better API without any extra cost.

# Download

Gradle
```groovy
repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile 'com.github.vjames19:kotlin-futures:0.1.0'
}
```

Maven
```xml
<repositories>
 <repository>
     <id>jitpack.io</id>
     <url>https://jitpack.io</url>
 </repository>
</repositories>

 <dependency>
    <groupId>com.github.vjames19</groupId>
    <artifactId>kotlin-futures</artifactId>
    <version>0.1.0</version>
</dependency>
```

For the rest: https://jitpack.io/#vjames19/kotlin-futures/

# How to use

Every single operation accepts an Executor as its argument, by default it uses the ForkJoinPool. 

For IO / blocking operations you should specify your own.

## Creation

Creating a Future that runs on a given Executor (by default its the ForkJoinExecutor)

```kotlin
import io.github.vjames19.futures.jdk8.ForkJoinExecutor
import io.github.vjames19.futures.jdk8.Future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

val future: CompletableFuture<Int> = Future { 10 }

// ForkJoinExecutor its just an alias ForkJoinPool.commonPool()
val futureOnForkJoin = Future(ForkJoinExecutor) { 10 }

val myExecutor = Executors.newSingleThreadExecutor()
val futureWithCustomExecutor = Future(myExecutor) {
    10
}
```

vs

```kotlin
val future: CompletableFuture<Int> = CompletableFuture.supplyAsync { 10 }

// ForkJoinExecutor its just an alias ForkJoinPool.commonPool()
val futureOnForkJoin = CompletableFuture.supplyAsync(Supplier { 10 }, ForkJoinExecutor)

val myExecutor = Executors.newSingleThreadExecutor()
val futureWithCustomExecutor = CompletableFuture.supplyAsync(Supplier { 10 }, myExecutor)
```

Creating immediate futures that run on the given thread.

```kotlin
import io.github.vjames19.futures.jdk8.ImmediateFuture
import io.github.vjames19.futures.jdk8.toCompletableFuture
import java.util.concurrent.CompletableFuture

val future: CompletableFuture<String> = ImmediateFuture { "Hello" }

val anotherImmediateFuture = "Hello".toString()

val aFailedImmediateFuture = IllegalArgumentException().toCompletableFuture<String>()

val futureWithTypeInference: CompletableFuture<String> = IllegalArgumentException().toCompletableFuture()
```

vs

```kotlin
val future: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")

val aFailedImmediateFuture = CompletableFuture<String>().apply { completeExceptionally(IllegalArgumentException()) }
```

## Composition

### map

Map allows you to transform the success of this future into another future.
```kotlin
val future: CompletableFuture<String> = Future { 10 }.map { "Hello user with id: $it" }
```

vs

```kotlin
val future: CompletableFuture<String> = Future { 10 }
        .thenApplyAsync(Function { userId -> "Hello user with id: $userId" }, ForkJoinExecutor)
```

### flatMap

flatMap allows you to do sequential composition. Creating a new future dependent on another one.

```kotlin
import io.github.vjames19.futures.jdk8.*
import java.util.concurrent.CompletableFuture


data class User(val id: Long, val name: String)
data class Post(val id: Long, val content: String)
data class UserPosts(val user: User, val posts: List<Post>)
fun fetchUser(id: Long): CompletableFuture<User> = Future { User(1, "Victor")}
fun fetchPosts(user: User): CompletableFuture<List<Post>> = Future { emptyList<Post>() }


// Fetching the posts depends on fetching the User
val posts = fetchUser(1).flatMap { fetchPosts(it) }

// Fetching both the user and the posts and then combining them into one
val userPosts =  fetchUser(1).flatMap { user ->
    fetchPosts(user).map { UserPosts(user, it) }
}
```

vs

```kotlin
val posts: CompletableFuture<List<Post>> = fetchUser(1)
        .thenComposeAsync(Function { fetchPosts(it) }, ForkJoinExecutor)

val userPosts: CompletableFuture<UserPosts> =  fetchUser(1)
        .thenComposeAsync(Function { user ->
            fetchPosts(user).thenApplyAsync(Function { posts ->
                UserPosts(user, posts)
            }, ForkJoinExecutor)
        }, ForkJoinExecutor)
```

### flatten

flatten removes a level from a nested future.

```kotlin
import io.github.vjames19.futures.jdk8.*
import java.util.concurrent.CompletableFuture


val nestedFuture = Future { Future { 10 } }
val flattened: CompletableFuture<Int> = nestedFuture.flatten()
```
### filter

filter will convert this future to a failed future if it doesn't match the predicate.

```kotlin
import io.github.vjames19.futures.jdk8.*

val future = Future { 10 }

// This future will succeed
val success = future.filter { it % 2 == 0 }

// This future will throw NoSuchElementException
val failed = future.filter { it % 3 == 0 }
```
### zip

zip allows you to combine two futures and apply a transformation to it

```kotlin
import io.github.vjames19.futures.jdk8.*

val idFuture = Future { 10 }
val nameFuture = Future { "Victor" }

val helloFuture = nameFuture.zip(idFuture) { name, id -> "Hello $name with id $id" }
```

vs

```kotlin
val helloFuture: CompletableFuture<String> = nameFuture
        .thenCombineAsync(idFuture, BiFunction { name, id -> "Hello $name with id $id" }, ForkJoinExecutor)
```

## Error Handling

The CompletableFuture API on its error handling callbacks it returns the exception wrapped into a [CompletionException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionException.html), 
I found this cumbersome to deal with and instead I'm returning the actual cause to all the error handling methods provided.

### recover

recover allows you to map an exception into a value you can recover from.  

```kotlin
import io.github.vjames19.futures.jdk8.*

val failed = Future<String> { throw IllegalArgumentException() }

val recovered = failed.recover { "recovered" }

val recoveredOnlyWhenYouCanHandleTheException = failed.recover {
    if (it is NoSuchElementException) "recovered"
    else throw it
}
```

vs

```kotlin
val recoveredOnlyWhenYouCanHandleTheException = failed.exceptionally {
    // unwrap the CompletionException
    val throwable = it.cause ?: it
    
    if (throwable is NoSuchElementException) "recovered"
    else throw throwable
}
```

### recoverWith

recoverWith allows to recover with a new CompletableFuture

```kotlin
import io.github.vjames19.futures.jdk8.*

val failed = Future<String> { throw IllegalArgumentException() }

val recovered = failed.recoverWith { Future { "recovered" } }

val recoveredOnlyWhenYouCanHandleTheException = failed.recoverWith {
    if (it is NoSuchElementException) Future { "recovered" }
    else throw it
    // else it.toCompletableFuture()
}
```

### mapError

mapError allows you only to transform the error types you are interested

```kotlin
import io.github.vjames19.futures.jdk8.*

val failed = Future<String> { throw IllegalArgumentException() }
        .mapError(IllegalArgumentException::class) {
            // handle the IllegalArgumentException here and return a more pertinent exception
        }
```

### fallbackTo

fallbackTo fallbacks to the specified future, in the event that the original future fails

```kotlin
import io.github.vjames19.futures.jdk8.*

val failed = Future<String> { throw IllegalArgumentException() }

val fallbacked = failed.fallbackTo{ Future { "recovered" } }

val success = Future { 10 }

// Keeps the value 10
val keepsTheOriginalValue = success.fallbackTo { Future { 20 } }
```

## Callbacks

### onSuccess

onSuccess only gets called when the future is successful
```kotlin
import io.github.vjames19.futures.jdk8.*

val future = Future<String> { TODO() }

val s = future.onSuccess { result -> 
    // do something with the result
}
```

### onFailure

onFailure only gets called when the future fails

```kotlin
import io.github.vjames19.futures.jdk8.*

val future = Future<String> { TODO() }

val s = future.onFailure { throwable -> 
    // do something with the error
}
```

### onComplete

onComplete allows you to register callbacks for both onFailure and onSuccess
```kotlin
import io.github.vjames19.futures.jdk8.*

val future = Future<String> { throw IllegalArgumentException() }

val f = future.onComplete(
        onFailure = { throwable ->
            // do something with the error
        },
        onSuccess = { result -> 
            // do something with the result
        })
```

vs

```kotlin
val f = future.whenCompleteAsync(BiConsumer({ result, throwable ->
    if (throwable != null) {
        // do something with the error
    } else {
        // do something with the result
    }
}), ForkJoinExecutor)
```

# Tests

In the tests you can find more example as to how to use a given operator.

```
./gradlew test
```
