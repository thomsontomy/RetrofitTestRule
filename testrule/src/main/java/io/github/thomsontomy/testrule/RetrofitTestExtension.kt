package io.github.thomsontomy.testrule

import java.lang.reflect.AnnotatedElement
import java.time.Duration
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * JUnit 5 counterpart of [RetrofitTestRule].
 *
 * Register it with `@RegisterExtension` and use the exact same API:
 *
 * ```kotlin
 * class GitHubServiceTest {
 *     @RegisterExtension
 *     val rule = RetrofitTestExtension(GitHubService::class.java)
 *
 *     @Test
 *     @RestRequest(
 *         expect = ExpectRequest(path = "/users/me"),
 *         respond = RespondWith(bodyText = """{"login":"octocat"}"""),
 *     )
 *     fun `loads the profile`() = runTest {
 *         assertEquals("octocat", rule.getService().profile().login)
 *     }
 * }
 * ```
 *
 * JUnit 5 is an optional dependency: the extension is only loaded when the test already has
 * `org.junit.jupiter:junit-jupiter-api` on its classpath.
 *
 * Prefer an instance field over a `static` one, otherwise state is shared between test methods.
 *
 * @param T Retrofit service interface
 * @param serviceClass Retrofit service interface to instantiate
 * @param logLevel amount of request logging
 * @param converterFactories converter factories applied to the generated Retrofit instance
 * @param callAdapterFactories call adapter factories applied to the generated Retrofit instance
 * @param logger sink used for request logging
 */
class RetrofitTestExtension<T : Any> @JvmOverloads constructor(
    serviceClass: Class<T>,
    logLevel: LogLevel = LogLevel.NONE,
    converterFactories: List<Converter.Factory> = defaultConverterFactories(),
    callAdapterFactories: List<CallAdapter.Factory> = emptyList(),
    logger: TestLogger = TestLogger.STDOUT,
) : BeforeEachCallback,
    AfterEachCallback,
    RetrofitTestDsl<T> {

    private val support = RetrofitTestSupport(
        serviceClass = serviceClass,
        converterFactories = converterFactories,
        callAdapterFactories = callAdapterFactories,
        logLevel = logLevel,
        logger = logger,
    )

    private var requests: List<RestRequest> = emptyList()

    override fun beforeEach(context: ExtensionContext) {
        support.start(context.requiredTestClass)
        requests = context.requiredTestMethod.restRequests()
        requests.forEach { support.enqueue(it) }
    }

    override fun afterEach(context: ExtensionContext) {
        try {
            if (context.executionException.isEmpty) {
                requests.forEachIndexed { index, request ->
                    support.verify(request, index, requests.size)
                }
                support.assertAllRequestsConsumed()
            }
        } finally {
            support.stop()
        }
    }

    override val server: MockWebServer get() = support.server

    override val baseUrl: HttpUrl get() = support.baseUrl

    override val recordedRequests: List<RequestSnapshot> get() = support.recordedRequests

    override val pendingResponses: Int get() = support.pendingResponses

    override var bodyMatcher: BodyMatcher
        get() = support.bodyMatcher
        set(value) {
            support.bodyMatcher = value
        }

    override fun getService(): T = support.getService()

    override fun getService(client: OkHttpClient): T = support.getService(client)

    override fun getService(configure: Retrofit.Builder.() -> Unit): T = support.getService(configure)

    override fun service(factory: (HttpUrl) -> T): T = support.service(factory)

    override fun enqueue(response: ResponseSpec) = support.enqueue(response)

    override fun enqueueResponse(code: Int, body: String, headers: Map<String, String>, delayMs: Long) =
        support.enqueueResponse(code, body, headers, delayMs)

    override fun enqueueResponseFromFile(file: String, code: Int, headers: Map<String, String>) =
        support.enqueueResponseFromFile(file, code, headers)

    override fun enqueueNetworkError(behavior: SocketBehavior) = support.enqueueNetworkError(behavior)

    override fun takeRequest(timeout: Duration): RequestSnapshot = support.takeRequest(timeout)

    override fun verifyRequest(timeout: Duration, block: RequestAssertions.() -> Unit): RequestSnapshot =
        support.verifyRequest(timeout, block)

    override fun assertAllRequestsConsumed() = support.assertAllRequestsConsumed()

    override fun assertNoPendingResponses() = support.assertNoPendingResponses()

    /** Starts the mock server eagerly. Only needed outside of the JUnit lifecycle. */
    fun start(testClass: Class<*>? = null) = support.start(testClass)

    /** Stops the mock server eagerly. Idempotent. */
    fun stop() = support.stop()
}

/**
 * Reads the [RestRequest] declarations of a JUnit 5 test method.
 *
 * Repeatable annotations are unwrapped manually from the [RestRequests] container so the behaviour
 * matches the JUnit 4 adapter exactly.
 */
internal fun AnnotatedElement.restRequests(): List<RestRequest> {
    getAnnotation(RestRequests::class.java)?.let { return it.value.toList() }
    getAnnotation(RestRequest::class.java)?.let { return listOf(it) }
    return emptyList()
}
