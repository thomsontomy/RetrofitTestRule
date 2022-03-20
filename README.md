# RetrofitTestRule

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/thomsontomy/RetrofitTestRule/blob/main/LICENSE)

## JUnit test rule for testing retrofit services

Generally we don't unit test retrofit interfaces. But if you would like to have an integration test
for the retrofit interfaces, then this test rule can be handy. This rule can be used as an
integration test to make sure the retrofit service configuration is correct

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
testImplementation("io.github.thomsontomy:testrule:0.1.0")
```

### Under the hood

This rule makes use of
OkHttp's [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver) and creates a
mock server for the retrofit service to use.
