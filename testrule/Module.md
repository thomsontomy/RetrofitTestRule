# Module RetrofitTestRule

Integration testing for Retrofit services on the JVM, backed by OkHttp's MockWebServer.

`RetrofitTestRule` (JUnit 4) and `RetrofitTestExtension` (JUnit 5) start a real HTTP server before
each test, point a Retrofit instance at it, and validate the requests your service sends.

## Configuration

```kotlin
testImplementation("io.github.thomsontomy:testrule:0.5.0")
```

## Annotation API

```kotlin
class GitHubServiceTest {
    @get:Rule
    val rule = RetrofitTestRule(GitHubService::class.java)

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/users/my_user/repos", method = HttpMethod.GET),
        respond = RespondWith(bodyText = "[]"),
    )
    fun `listRepos sends the right path`() = runTest {
        rule.getService().listRepos("my_user")
    }
}
```

Declare `@RestRequest` more than once to describe sequential requests. Requests and responses are
matched in declaration order, and the test fails if the service makes more or fewer calls than
declared.

## Programmatic DSL

```kotlin
@Test
fun `retries after an expired token`() = runTest {
    rule.enqueueResponse(code = 401)
    rule.enqueueResponse(body = """{"token":"fresh"}""")

    rule.getService().profile()

    rule.verifyRequest { hasHeader("Authorization", "Bearer expired") }
    rule.verifyRequest { hasHeader("Authorization", "Bearer fresh") }
}
```

## Features

- Annotation and DSL APIs, usable together
- Structural JSON comparison of request bodies (key order and whitespace insensitive)
- Custom `OkHttpClient` and `Retrofit.Builder` support, preserving production interceptors
- Response headers, status codes, body delays and socket level failures
- Request/response bodies loaded from resources or the file system
- Failure messages that include the full captured request
- JUnit 5 extension available as an optional dependency

## Under the hood

The rule uses OkHttp's [MockWebServer](https://square.github.io/okhttp/mockwebserver/) to serve
responses and capture requests. See the project
[README](https://github.com/thomsontomy/RetrofitTestRule) for the full API reference and migration
guide.
