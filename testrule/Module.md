# Module Retrofit Test Rule

### JUnit test rule for testing retrofit services

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
        responseOptions = ResponseOptions(responseBody = "[]") // Response configurations
    )
    fun testListRepos() = runTest {
        // Get the service instance
        val service = retrofitTestRule.getService()

        // Make the API call
        service.listRepos("my_user")
    }
}
```

### Configuration

```groovy
testImplementation("io.github.thomsontomy:testrule:0.2.0")
```