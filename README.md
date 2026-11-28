# RetrofitTestRule

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/thomsontomy/RetrofitTestRule/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.thomsontomy/testrule)](https://search.maven.org/artifact/io.github.thomsontomy/testrule)

Integration testing for Retrofit services on the JVM, backed by OkHttp's
[MockWebServer](https://square.github.io/okhttp/mockwebserver/).

`RetrofitTestRule` gives you a real HTTP server on localhost and a Retrofit instance pointed at it,
so you test the parts unit tests usually miss: paths, query parameters, headers, serialized bodies,
status codes and error handling. It is a JUnit 4 `TestRule` and a JUnit 5 extension, with both an
annotation API and a programmatic DSL.

## Installation

```kotlin
testImplementation("io.github.thomsontomy:testrule:0.5.0")
```

The rule pulls in JUnit 4, Retrofit and OkHttp's MockWebServer. JUnit 5 is optional: add
`org.junit.jupiter:junit-jupiter` yourself if you want to use `RetrofitTestExtension`.

## Quick start

Given a service:

```kotlin
interface GitHubService {
    @GET("users/{user}/repos")
    suspend fun listRepos(@Path("user") user: String): List<GithubRepo>
}
```

### Annotation API

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
        val repos = rule.getService().listRepos("my_user")

        assertEquals(0, repos.size)
    }
}
```

The response is enqueued before the test body and the captured request is validated after it.
Requests are matched **in declaration order**, so sequential workflows are just repeated
annotations:

```kotlin
@Test
@RestRequest(
    expect = ExpectRequest(path = "/users/first/repos"),
    respond = RespondWith(bodyText = "[]"),
)
@RestRequest(
    expect = ExpectRequest(path = "/users/second/repos"),
    respond = RespondWith(bodyText = "[]"),
)
fun `loads two users in order`() = runTest {
    val service = rule.getService()

    service.listRepos("first")
    service.listRepos("second")
}
```

If the service makes fewer or more requests than declared, the test fails with a diagnostic that
prints the unexpected requests instead of silently passing.

### Programmatic DSL

Use the DSL when payloads are computed at runtime:

```kotlin
@Test
fun `retries after an expired token`() = runTest {
    rule.enqueueResponse(code = 401)
    rule.enqueueResponse(body = """{"token":"fresh"}""")

    val service = rule.getService()
    service.profile()

    rule.verifyRequest { hasHeader("Authorization", "Bearer expired") }
    rule.verifyRequest { hasHeader("Authorization", "Bearer fresh") }
}
```

Available methods:

| Method | Description |
| --- | --- |
| `getService()` | Retrofit instance using the configured converter factories |
| `getService(client)` | Same, with your production `OkHttpClient` |
| `getService { ... }` | Configure a `Retrofit.Builder` that already points at the mock server |
| `service { url -> ... }` | Build the service yourself from the mock server URL |
| `enqueueResponse(code, body, headers, delayMs)` | Enqueue a response |
| `enqueueResponseFromFile(file, code, headers)` | Enqueue a response read from a resource or file |
| `enqueue(ResponseSpec)` | Enqueue a fully described response |
| `enqueueNetworkError(behavior)` | Fail at the socket level (timeouts, disconnects) |
| `takeRequest(timeout)` | Wait for and return a `RequestSnapshot` |
| `verifyRequest(timeout) { ... }` | Wait for a request and assert on it |
| `assertAllRequestsConsumed()` | Fail if the service made unvalidated requests |
| `assertNoPendingResponses()` | Fail if enqueued responses were never requested |
| `server`, `baseUrl`, `recordedRequests`, `pendingResponses` | Escape hatches for advanced tests |

Assertions available inside `verifyRequest`:

```kotlin
rule.verifyRequest {
    hasMethod(HttpMethod.POST)
    hasPath("/users/42/repos")          // query strings may be inlined
    hasQuery("page", "2")               // order independent, URL decoded
    hasHeader("Authorization", "Bearer x")
    hasBody("""{"name":"tom"}""")       // structural JSON by default
    hasBodyFile("expected_request.json")
    hasNoBody()
}
```

### JSON bodies are compared structurally

Request bodies are compared as JSON trees, so key order and whitespace do not matter:

```kotlin
@RestRequest(
    expect = ExpectRequest(
        path = "/users",
        method = HttpMethod.POST,
        // Serialized as {"name":"tom","url":"https://example.com"}
        bodyText = """{"url":"https://example.com","name":"tom"}""",
    ),
    respond = RespondWith(bodyText = """{"id":1}"""),
)
```

Array order is still significant. Non-JSON bodies fall back to a trimmed text comparison. Switch to
exact matching with `rule.bodyMatcher = BodyMatchers.EXACT`, or supply your own `BodyMatcher`.

### Production clients and interceptors

`getService()` builds a bare client, which is not what production uses. Pass your client to keep
interceptors, authenticators, headers and timeouts:

```kotlin
@Test
fun `sends the authorization header`() = runTest {
    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenProvider))
        .build()

    rule.enqueueResponse(body = "[]")

    rule.getService(client).listRepos("me")

    rule.verifyRequest { hasHeader("Authorization", "Bearer token") }
}
```

The base URL is always rewritten to the mock server; everything else is preserved.

### Files and resources

```kotlin
@RestRequest(
    expect = ExpectRequest(
        path = "/api/get-user-type",
        bodyFile = "getUsersWithType_request_body.json",
    ),
    respond = RespondWith(bodyFile = "getUsersWithType_response_body.json"),
)
```

Files are resolved, in order, from:

1. the file system (absolute path, or relative to the working directory)
2. `src/test/resources`, relative to the **test class** package
3. `src/test/resources`, relative to the service interface package
4. an absolute classpath path such as `/fixtures/body.json`

A missing file produces an error listing every attempted location.

### Delays and network failures

```kotlin
rule.enqueueResponse(body = """{"slow":true}""", delayMs = 2_000)  // loading states
rule.enqueueNetworkError(SocketBehavior.DISCONNECT_AT_START)         // retry logic
rule.enqueueNetworkError(SocketBehavior.NO_RESPONSE)                 // timeouts
```

For timeout tests, configure a client timeout shorter than the delay.

### JUnit 5

```kotlin
class GitHubServiceTest {
    @JvmField
    @RegisterExtension
    val rule = RetrofitTestExtension(GitHubService::class.java)

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/users/me"),
        respond = RespondWith(bodyText = """{"login":"octocat"}"""),
    )
    fun `loads the profile`() = runTest { /* ... */ }
}
```

Use an instance field rather than a `static` one so state is not shared between test methods.

## Logging

```kotlin
val rule = RetrofitTestRule(
    GitHubService::class.java,
    logLevel = LogLevel.BODY,
    logger = TestLogger { message -> LOGGER.info(message) },
)
```

`LogLevel` is one of `NONE`, `BASIC`, `HEADERS`, `BODY`. Failure messages always include the full
request, so logging is only needed while writing a test.

## Annotation reference

| Annotation | Purpose |
| --- | --- |
| `@RestRequest(expect, respond)` | Declares one HTTP interaction. Repeatable. |
| `@ExpectRequest(path, method, query, headers, bodyText, bodyFile)` | Request expectations. Empty values are skipped. |
| `@RespondWith(code, bodyText, bodyFile, headers, delayMs, socketBehavior)` | Response to serve. |
| `@HeaderValue(name, value)` | Header pair for requests and responses. |
| `@QueryParam(name, value)` | Query parameter expectation. |
| `HttpMethod` | `ANY`, `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`. |
| `SocketBehavior` | `NORMAL`, `DISCONNECT_AT_START`, `DISCONNECT_AFTER_REQUEST`, `DISCONNECT_AT_END`, `NO_RESPONSE`, `STALL_AT_START`, `DO_NOT_READ_REQUEST_BODY`. |

`ExpectRequest.bodyText` and `bodyFile` are mutually exclusive; declaring both fails the test.
The same applies to `RespondWith`.

## Migration from 0.4.x

| 0.4.x | 0.5.0 |
| --- | --- |
| `@RestRequest(validations = RequestValidations(...))` | `@RestRequest(expect = ExpectRequest(...))` |
| `@RestRequest(responseOptions = ResponseOptions(...))` | `@RestRequest(respond = RespondWith(...))` |
| `method = "GET"` | `method = HttpMethod.GET` |
| `headers = [@Header("Name: value")]` | `headers = [HeaderValue("Name", "value")]` |
| `path = "#"` / `bodyText = "#"` (magic null) | `""` (empty means "not validated") |
| `ResponseOptions.bodyText = "{}"` default | `RespondWith.bodyText = ""` default |
| — | `HttpMethod.ANY` means "any method" |

`LogLevel` and `RetrofitTestRule(serviceClass, logLevel, converterFactories, callAdapterFactories)`
are unchanged.

## How it works

Each rule instance owns a `MockWebServer`. The server is started before the test body, so
`getService()` and the DSL work with or without annotations, and stopped afterwards. Requests are
captured as immutable `RequestSnapshot`s, which keeps OkHttp's buffers (and its version) out of
your test code.

## Limitations

- JUnit 4 and JUnit 5 only; there is no TestNG adapter.
- Plain HTTP. Testing `certificatePinner` or TLS interceptors requires configuring
  `server.useHttps(...)` yourself through the `server` escape hatch.
- Binary request bodies are decoded as UTF-8 lossily; use `server` for raw bytes.
- Structural JSON comparison treats numbers literally: `1` and `1.0` are different.

## Linting

The repository uses [Trunk](https://trunk.io) to aggregate its linters. Pinned versions live in
[`.trunk/trunk.yaml`](.trunk/trunk.yaml), and the Kotlin style is defined in
[`.editorconfig`](.editorconfig).

| Linter | Scope |
| --- | --- |
| `ktlint` | Kotlin and Gradle Kotlin DSL |
| `actionlint` | GitHub Actions workflows |
| `markdownlint` | Markdown |
| `yamllint` | YAML |
| `gitleaks` | Committed secrets |

Install the CLI once, then check or auto-format:

```shell
curl https://get.trunk.io -fsSL | bash
trunk check --all
trunk fmt
```

CI runs `trunk-io/trunk-action@v2.0.0` on every push and pull request.

## License

```text
Copyright 2022 Thomson Thomas

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
