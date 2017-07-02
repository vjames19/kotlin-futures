import io.github.vjames19.futures.jdk8.*
import java.util.concurrent.CompletableFuture
import java.util.function.Function


data class User(val id: Long, val name: String)
data class Post(val id: Long, val content: String)
data class UserPosts(val user: User, val posts: List<Post>)
fun fetchUser(id: Long): CompletableFuture<User> = Future { User(1, "Victor")}
fun fetchPosts(user: User): CompletableFuture<List<Post>> = Future { emptyList<Post>() }


// Fetching the posts depends on fetching the User
val posts = fetchUser(1).flatMap { fetchPosts(it) }

// Fetching both the user and the posts and then combining them into one
val userPosts: CompletableFuture<UserPosts> =  fetchUser(1)
        .thenComposeAsync(Function { user ->
            fetchPosts(user).thenApplyAsync(Function { posts ->
                UserPosts(user, posts)
            }, ForkJoinExecutor)
        }, ForkJoinExecutor)

// Keeps the value 10
val keepsTheOriginalValue = success.fallbackTo { Future { 20 } }

//data class User(val id: Long, val name: String)
//data class Post(val id: Long, val content: String)
//data class UserPosts(val user: User, val posts: List<Post>)
//fun fetchUser(id: Long): CompletableFuture<User> = Future { User(1, "Victor")}
//fun fetchPosts(user: User): CompletableFuture<List<Post>> = Future { emptyList<Post>() }

