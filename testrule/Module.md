# Module Retrofit Test Rule

### JUnit test rule for testing retrofit services

### Configuration

```groovy
testImplementation("io.github.thomsontomy:testrule:0.2.0")
```

### Example

Imagine you have an awesome retrofit service class like below

```kotlin
interface GitHubService {
    @GET("users/{user}/repos")
    suspend fun listRepos(@Path("user") user: String): List<GithubRepo>
}
```

The integration test would look like this

```kotlin
class GitHubServiceTest {
    @get:Rule
    val retrofitTestRule = RetrofitTestRule(GitHubService::class.java, logLevel = LogLevel.BODY)

    @Test
    @RestRequest(
        validations = RequestValidations(
            path = "/users/my_user/repos",
            method = "GET"
        ), // Validations on the request
        responseOptions = ResponseOptions(bodyText = "[]") // Response configurations
    )
    fun testListRepos() = runTest {
        // Get the service instance
        val service = retrofitTestRule.getService()

        // Make the API call
        service.listRepos("my_user")
    }
}
```

### Under the hood

This rule makes use of
OkHttp's [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver) and creates a
mock server for the retrofit service to use.