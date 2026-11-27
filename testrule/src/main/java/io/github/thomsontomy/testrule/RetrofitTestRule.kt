package io.github.thomsontomy.testrule

import com.google.gson.Gson
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration

/**
 * The default converter factories: Gson, so that JSON service interfaces work out of the box.
 *
 * Pass a different list to the constructor when production uses Moshi, kotlinx.serialization or a
 * custom converter.
 */
fun defaultConverterFactories(): List<Converter.Factory> =
    listOf(GsonConverterFactory.create(Gson()))

/**
 * Kotlin friendly factory that avoids passing the service class explicitly.
 *
 * ```kotlin
 * @get:Rule
 * val rule = retrofitTestRule<GitHubService>(logLevel = LogLevel.BODY)
 * ```
 */
inline fun <reified T : Any> retrofitTestRule(
    logLevel: LogLevel = LogLevel.NONE,
    converterFactories: List<Converter.Factory> = defaultConverterFactories(),
    callAdapterFactories: List<CallAdapter.Factory> = emptyList(),
    logger: TestLogger = TestLogger.STDOUT,
): RetrofitTestRule<T> =
    RetrofitTestRule(T::class.java, logLevel, converterFactories, callAdapterFactories, logger)

/**
 * JUnit 4 [TestRule] that runs a Retrofit service against an OkHttp [MockWebServer].
 *
 * The rule starts the server before the test body, enqueues the responses declared with
 * [RestRequest] (or programmatically through the [RetrofitTestDsl] methods), and validates every
 * captured request after the body completes.
 *
 * ```kotlin
 * class GitHubServiceTest {
 *     @get:Rule
 *     val rule = RetrofitTestRule(GitHubService::class.java)
 *
 *     @Test
 *     @RestRequest(
 *         expect = ExpectRequest(path = "/users/me", method = HttpMethod.GET),
 *         respond = RespondWith(bodyText = """{"login":"octocat"}"""),
 *     )
 *     fun `loads the profile`() = runTest {
 *         val service = rule.getService()
 *         assertEquals("octocat", service.profile().login)
 *     }
 * }
 * ```
 *
 * The rule is also a full programmatic API; see [RetrofitTestDsl] for enqueuing responses and
 * asserting on requests without annotations.
 *
 * @param T Retrofit service interface
 * @param serviceClass Retrofit service interface to instantiate
 * @param logLevel amount of request logging, [LogLevel.NONE] by default
 * @param converterFactories converter factories applied to the generated Retrofit instance
 * @param callAdapterFactories call adapter factories applied to the generated Retrofit instance
 * @param logger sink used for request logging
 */
class RetrofitTestRule<T : Any> @JvmOverloads constructor(
    serviceClass: Class<T>,
    logLevel: LogLevel = LogLevel.NONE,
    converterFactories: List<Converter.Factory> = defaultConverterFactories(),
    callAdapterFactories: List<CallAdapter.Factory> = emptyList(),
    logger: TestLogger = TestLogger.STDOUT,
) : TestRule, RetrofitTestDsl<T> {

    private val support = RetrofitTestSupport(
        serviceClass = serviceClass,
        converterFactories = converterFactories,
        callAdapterFactories = callAdapterFactories,
        logLevel = logLevel,
        logger = logger,
    )

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

    override fun enqueueResponse(
        code: Int,
        body: String,
        headers: Map<String, String>,
        delayMs: Long,
    ) = support.enqueueResponse(code, body, headers, delayMs)

    override fun enqueueResponseFromFile(
        file: String,
        code: Int,
        headers: Map<String, String>,
    ) = support.enqueueResponseFromFile(file, code, headers)

    override fun enqueueNetworkError(behavior: SocketBehavior) = support.enqueueNetworkError(behavior)

    override fun takeRequest(timeout: Duration): RequestSnapshot = support.takeRequest(timeout)

    override fun verifyRequest(
        timeout: Duration,
        block: RequestAssertions.() -> Unit,
    ): RequestSnapshot = support.verifyRequest(timeout, block)

    override fun assertAllRequestsConsumed() = support.assertAllRequestsConsumed()

    override fun assertNoPendingResponses() = support.assertNoPendingResponses()

    /**
     * Starts the mock server eagerly.
     *
     * Only needed when the rule is used outside of JUnit's rule lifecycle, for example as a
     * `@ClassRule` or from a custom harness. The server is started automatically otherwise.
     *
     * @param testClass test class used to resolve resource files
     */
    fun start(testClass: Class<*>? = null) = support.start(testClass)

    /** Stops the mock server eagerly. Idempotent. */
    fun stop() = support.stop()

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            support.start(description.testClass)
            try {
                val requests = description.restRequests()
                requests.forEach { support.enqueue(it) }
                base.evaluate()
                requests.forEachIndexed { index, request ->
                    support.verify(request, index, requests.size)
                }
                support.assertAllRequestsConsumed()
            } finally {
                support.stop()
            }
        }
    }
}

/**
 * Reads the [RestRequest] declarations of a test method.
 *
 * JUnit 4's [Description] does not implement [java.lang.reflect.AnnotatedElement], so repeatable
 * annotations are unwrapped manually from the [RestRequests] container.
 */
internal fun Description.restRequests(): List<RestRequest> {
    getAnnotation(RestRequests::class.java)?.let { return it.value.toList() }
    getAnnotation(RestRequest::class.java)?.let { return listOf(it) }
    return emptyList()
}
