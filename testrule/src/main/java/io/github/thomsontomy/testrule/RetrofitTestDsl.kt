package io.github.thomsontomy.testrule

import java.time.Duration
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit

/**
 * Default time the rule waits for an expected request before failing.
 *
 * Override per call through [RetrofitTestDsl.takeRequest] or [RetrofitTestDsl.verifyRequest].
 */
@JvmField
val DEFAULT_REQUEST_TIMEOUT: Duration = Duration.ofSeconds(5)

/**
 * Declarative description of a mocked response.
 *
 * Used by [RetrofitTestDsl.enqueue] when the annotation API is not expressive enough, for example
 * when the response depends on values computed at runtime.
 *
 * @property code HTTP status code
 * @property body response body, already resolved to text
 * @property headers response headers to send
 * @property delayMs delay before the body is written
 * @property socketBehavior low level socket behaviour for network failure simulation
 */
data class ResponseSpec(
    val code: Int = 200,
    val body: String = "",
    val headers: Map<String, String> = emptyMap(),
    val delayMs: Long = 0,
    val socketBehavior: SocketBehavior = SocketBehavior.NORMAL,
)

/**
 * Programmatic API shared by [RetrofitTestRule] (JUnit 4) and [RetrofitTestExtension] (JUnit 5).
 *
 * The mock server is started before the test body and stopped afterwards, so every method below
 * can be called directly from a test method, from `@Before`/`@BeforeEach`, or from a helper.
 *
 * ```kotlin
 * @Test
 * fun `retries on 401`() {
 *     rule.enqueueResponse(code = 401)
 *     rule.enqueueResponse(body = """{"token":"new"}""")
 *
 *     val service = rule.getService()
 *     runBlocking { service.profile() }
 *
 *     rule.verifyRequest { hasHeader("Authorization", "Bearer expired") }
 *     rule.verifyRequest { hasHeader("Authorization", "Bearer new") }
 * }
 * ```
 *
 * @param T Retrofit service interface
 */
interface RetrofitTestDsl<T : Any> {

    /** The underlying mock web server, for advanced scenarios such as custom dispatchers. */
    val server: MockWebServer

    /** Base URL of the running mock server, always ending with `/`. */
    val baseUrl: HttpUrl

    /** Every request captured so far, in the order it arrived. */
    val recordedRequests: List<RequestSnapshot>

    /** Number of enqueued responses that have not been served yet. */
    val pendingResponses: Int

    /**
     * Comparison strategy used by [RequestAssertions.hasBody].
     *
     * Defaults to [BodyMatchers.STRUCTURAL]. Set to [BodyMatchers.EXACT] to require byte for byte
     * equality.
     */
    var bodyMatcher: BodyMatcher

    /** Creates the service backed by the mock server, using the configured converter factories. */
    fun getService(): T

    /**
     * Creates the service with a custom client, preserving production interceptors,
     * authenticators and timeouts. The base URL still points at the mock server.
     */
    fun getService(client: OkHttpClient): T

    /**
     * Creates the service after applying [configure] to a pre-configured [Retrofit.Builder].
     *
     * The builder already has the mock server base URL, the converter factories and the call
     * adapter factories applied. Calling `baseUrl(...)` inside [configure] overrides the mock
     * server URL, which is occasionally useful to assert redirect handling.
     */
    fun getService(configure: Retrofit.Builder.() -> Unit): T

    /**
     * Creates the service from a factory that receives the mock server base URL.
     *
     * Use this when the production code builds Retrofit itself and the rule should only supply
     * the URL.
     */
    fun service(factory: (HttpUrl) -> T): T

    /** Enqueues a fully described response. */
    fun enqueue(response: ResponseSpec)

    /**
     * Enqueues a response.
     *
     * @param code HTTP status code
     * @param body response body
     * @param headers response headers
     * @param delayMs delay before the body is written
     */
    fun enqueueResponse(
        code: Int = 200,
        body: String = "",
        headers: Map<String, String> = emptyMap(),
        delayMs: Long = 0,
    )

    /** Enqueues a response whose body is read from a test resource or file. */
    fun enqueueResponseFromFile(file: String, code: Int = 200, headers: Map<String, String> = emptyMap())

    /**
     * Enqueues a response that fails at the socket level.
     *
     * Useful for retry and timeout tests. The client must configure a timeout shorter than the
     * test timeout when using [SocketBehavior.NO_RESPONSE].
     */
    fun enqueueNetworkError(behavior: SocketBehavior = SocketBehavior.DISCONNECT_AT_START)

    /**
     * Waits for the next request and returns it.
     *
     * @throws AssertionError when no request arrives within [timeout]
     */
    fun takeRequest(timeout: Duration = DEFAULT_REQUEST_TIMEOUT): RequestSnapshot

    /**
     * Waits for the next request and applies [block] to it.
     *
     * @return the captured request, for further assertions
     * @throws AssertionError when no request arrives or an assertion fails
     */
    fun verifyRequest(
        timeout: Duration = DEFAULT_REQUEST_TIMEOUT,
        block: RequestAssertions.() -> Unit,
    ): RequestSnapshot

    /**
     * Fails when the service made requests that were never validated.
     *
     * The annotation driven rule calls this automatically after the test body.
     */
    fun assertAllRequestsConsumed()

    /**
     * Fails when responses were enqueued but never requested, which usually means the test forgot
     * to exercise a code path.
     */
    fun assertNoPendingResponses()
}
