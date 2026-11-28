package io.github.thomsontomy.testrule

import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * Framework agnostic engine behind [RetrofitTestRule] and [RetrofitTestExtension].
 *
 * Most tests should use one of those two adapters. This class is public so that other test
 * frameworks, or hand written test harnesses, can reuse the same lifecycle and assertions:
 *
 * ```kotlin
 * val support = RetrofitTestSupport(GitHubService::class.java, converters)
 * support.start(MyTest::class.java)
 * try {
 *     support.enqueueResponse(body = "[]")
 *     val service = support.getService()
 *     // ...
 *     support.assertAllRequestsConsumed()
 * } finally {
 *     support.stop()
 * }
 * ```
 *
 * @param T Retrofit service interface
 * @param serviceClass Retrofit service interface to instantiate
 * @param converterFactories converter factories applied to the generated Retrofit instance
 * @param callAdapterFactories call adapter factories applied to the generated Retrofit instance
 * @param logLevel amount of request logging
 * @param logger sink used for request logging
 */
class RetrofitTestSupport<T : Any>(
    private val serviceClass: Class<T>,
    private val converterFactories: List<Converter.Factory>,
    private val callAdapterFactories: List<CallAdapter.Factory> = emptyList(),
    private val logLevel: LogLevel = LogLevel.NONE,
    private val logger: TestLogger = TestLogger.STDOUT,
) : RetrofitTestDsl<T>,
    AutoCloseable {

    private var mockWebServer = MockWebServer()
    private val capturedRequests = mutableListOf<RequestSnapshot>()
    private val enqueuedResponses = mutableListOf<ResponseSpec>()

    private var testClass: Class<*>? = null
    private var started = false

    /**
     * A [MockWebServer] cannot be restarted once shut down, so a fresh instance is created when the
     * support is reused (for example by a static JUnit 5 `@RegisterExtension` field).
     */
    private var serverConsumed = false

    override var bodyMatcher: BodyMatcher = BodyMatchers.STRUCTURAL

    override val server: MockWebServer
        get() = mockWebServer

    override val baseUrl: HttpUrl
        get() {
            ensureStarted()
            return mockWebServer.url("/")
        }

    override val recordedRequests: List<RequestSnapshot>
        get() = capturedRequests.toList()

    override val pendingResponses: Int
        get() {
            if (!started) return enqueuedResponses.size
            return (enqueuedResponses.size - mockWebServer.requestCount).coerceAtLeast(0)
        }

    /**
     * Starts the mock server.
     *
     * Idempotent: calling it again, or calling [ensureStarted] implicitly through any DSL method,
     * is safe.
     *
     * @param testClass test class used to resolve resource files; defaults to the service class
     */
    @Synchronized
    fun start(testClass: Class<*>? = null) {
        if (testClass != null) this.testClass = testClass
        if (started) return
        if (serverConsumed) {
            mockWebServer = MockWebServer()
            capturedRequests.clear()
            enqueuedResponses.clear()
            serverConsumed = false
        }
        try {
            mockWebServer.start()
        } catch (exception: IOException) {
            throw IllegalStateException("Could not start the mock web server", exception)
        }
        started = true
    }

    /** Stops the mock server. Idempotent. */
    @Synchronized
    fun stop() {
        if (!started) return
        started = false
        serverConsumed = true
        try {
            mockWebServer.shutdown()
        } catch (exception: IOException) {
            // The server is already unusable; there is nothing useful a test could do about it.
        }
    }

    /** Alias for [stop], allowing `use { }` blocks. */
    override fun close() = stop()

    override fun getService(): T = getService { }

    override fun getService(client: OkHttpClient): T = getService { client(client) }

    override fun getService(configure: Retrofit.Builder.() -> Unit): T {
        val builder = Retrofit.Builder().baseUrl(baseUrl)
        converterFactories.forEach { factory -> builder.addConverterFactory(factory) }
        callAdapterFactories.forEach { factory -> builder.addCallAdapterFactory(factory) }
        builder.configure()
        return builder.build().create(serviceClass)
    }

    override fun service(factory: (HttpUrl) -> T): T = factory(baseUrl)

    override fun enqueue(response: ResponseSpec) {
        ensureStarted()
        mockWebServer.enqueue(response.toMockResponse())
        enqueuedResponses += response
    }

    override fun enqueueResponse(code: Int, body: String, headers: Map<String, String>, delayMs: Long) =
        enqueue(ResponseSpec(code = code, body = body, headers = headers, delayMs = delayMs))

    override fun enqueueResponseFromFile(file: String, code: Int, headers: Map<String, String>) =
        enqueue(ResponseSpec(code = code, body = resolver.read(file), headers = headers))

    override fun enqueueNetworkError(behavior: SocketBehavior) = enqueue(ResponseSpec(socketBehavior = behavior))

    override fun takeRequest(timeout: Duration): RequestSnapshot {
        ensureStarted()
        val recorded = try {
            mockWebServer.takeRequest(timeout.toMillis(), TimeUnit.MILLISECONDS)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw AssertionError("Interrupted while waiting for an HTTP request", exception)
        } ?: throw AssertionError(noRequestMessage(timeout))

        val snapshot = RequestSnapshot.from(recorded)
        capturedRequests += snapshot
        log(snapshot)
        return snapshot
    }

    override fun verifyRequest(timeout: Duration, block: RequestAssertions.() -> Unit): RequestSnapshot {
        val request = takeRequest(timeout)
        RequestAssertions(request, resolver, bodyMatcher).block()
        return request
    }

    override fun assertAllRequestsConsumed() {
        val received = if (started) mockWebServer.requestCount else 0
        if (received <= capturedRequests.size) return

        val unexpected = mutableListOf<RequestSnapshot>()
        while (true) {
            val recorded = mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS) ?: break
            val snapshot = RequestSnapshot.from(recorded)
            capturedRequests += snapshot
            unexpected += snapshot
        }
        if (unexpected.isEmpty()) return

        throw AssertionError(
            buildString {
                append("The service made ").append(unexpected.size)
                append(" request(s) that were not covered by @RestRequest or verifyRequest.")
                unexpected.forEach { append("\n\n").append(it.format()) }
            },
        )
    }

    override fun assertNoPendingResponses() {
        val unused = pendingResponses
        if (unused <= 0) return

        throw AssertionError(
            buildString {
                append(unused).append(" enqueued response(s) were never requested.")
                enqueuedResponses.takeLast(unused).forEachIndexed { index, spec ->
                    append("\n  ").append(index + 1).append(". ").append(spec)
                }
            },
        )
    }

    /**
     * Enqueues the response declared by [annotation].
     *
     * Used by the JUnit adapters; exposed to the module only.
     */
    internal fun enqueue(annotation: RestRequest) {
        enqueue(annotation.respond.toResponseSpec())
    }

    /**
     * Validates the request described by [annotation].
     *
     * Used by the JUnit adapters; exposed to the module only.
     */
    internal fun verify(annotation: RestRequest, index: Int, total: Int) {
        val request = try {
            takeRequest()
        } catch (exception: AssertionError) {
            throw AssertionError(
                "Expected request #${index + 1} of $total declared by @RestRequest, but it never arrived.\n\n" +
                    exception.message,
                exception,
            )
        }

        try {
            RequestAssertions(request, resolver, bodyMatcher).applyExpectation(annotation.expect)
        } catch (exception: AssertionError) {
            throw AssertionError(
                "Request #${index + 1} of $total did not match the @RestRequest expectations.\n\n" +
                    exception.message,
                exception,
            )
        }
    }

    private fun ensureStarted() {
        if (!started) start()
    }

    private val resolver: ResourceResolver
        get() = ResourceResolver(testClass ?: serviceClass, serviceClass)

    private fun log(request: RequestSnapshot) {
        when (logLevel) {
            LogLevel.NONE -> Unit
            LogLevel.BASIC -> logger.log("--> ${request.method} ${request.url}")
            LogLevel.HEADERS -> logger.log(request.format(includeBody = false))
            LogLevel.BODY -> logger.log(request.format(includeBody = true))
        }
    }

    private fun noRequestMessage(timeout: Duration): String = buildString {
        append("Expected an HTTP request within ").append(timeout.toMillis())
        append(" ms, but none arrived.")
        append("\nRequests captured so far: ").append(capturedRequests.size)
        if (pendingResponses > 0) {
            append("\nEnqueued responses still waiting: ").append(pendingResponses)
        }
        if (capturedRequests.isNotEmpty()) {
            append("\n\n").append(capturedRequests.joinToString("\n\n") { it.format() })
        }
    }

    private fun ResponseSpec.toMockResponse(): MockResponse {
        val mockResponse = MockResponse()
        mockResponse.setResponseCode(code)
        if (body.isNotEmpty()) mockResponse.setBody(body)
        headers.forEach { (name, value) -> mockResponse.addHeader(name, value) }
        if (delayMs > 0) mockResponse.setBodyDelay(delayMs, TimeUnit.MILLISECONDS)
        mockResponse.setSocketPolicy(socketBehavior.toSocketPolicy())
        return mockResponse
    }

    private fun RespondWith.toResponseSpec(): ResponseSpec {
        if (bodyText.isNotEmpty() && bodyFile.isNotEmpty()) {
            throw IllegalArgumentException(
                "RespondWith declares both bodyText and bodyFile. Use exactly one of them.",
            )
        }
        return ResponseSpec(
            code = code,
            body = if (bodyFile.isNotEmpty()) resolver.read(bodyFile) else bodyText,
            headers = headers.associate { it.name to it.value },
            delayMs = delayMs,
            socketBehavior = socketBehavior,
        )
    }

    private fun SocketBehavior.toSocketPolicy(): SocketPolicy = when (this) {
        SocketBehavior.NORMAL -> SocketPolicy.KEEP_OPEN
        SocketBehavior.DISCONNECT_AT_START -> SocketPolicy.DISCONNECT_AT_START
        SocketBehavior.DISCONNECT_AFTER_REQUEST -> SocketPolicy.DISCONNECT_AFTER_REQUEST
        SocketBehavior.DISCONNECT_AT_END -> SocketPolicy.DISCONNECT_AT_END
        SocketBehavior.NO_RESPONSE -> SocketPolicy.NO_RESPONSE
        SocketBehavior.STALL_AT_START -> SocketPolicy.STALL_SOCKET_AT_START
        SocketBehavior.DO_NOT_READ_REQUEST_BODY -> SocketPolicy.DO_NOT_READ_REQUEST_BODY
    }
}
