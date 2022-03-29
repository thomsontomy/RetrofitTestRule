package io.github.thomsontomy.testrule

import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.fail
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * JUnit test rule for testing Retrofit services
 *
 * @author Thomson Thomas
 *
 * @constructor Creates an instance of [RetrofitTestRule]
 *
 * @param T Retrofit service interface
 * @property clazz Class instance of the retrofit service interface
 * @property logLevel [LogLevel] from the rule
 * @property converterFactories List of retrofit converter factories. Default value is [GsonConverterFactory]
 * @property callAdapterFactories List of retrofit call adapter factories
 */
class RetrofitTestRule<T>(
    private val clazz: Class<T>,
    private val logLevel: LogLevel = LogLevel.NONE,
    private val converterFactories: List<Converter.Factory> = listOf(
        GsonConverterFactory.create(
            Gson()
        )
    ),
    private val callAdapterFactories: List<CallAdapter.Factory> = emptyList()
) : TestRule {

    private val mockWebServer = MockWebServer()

    /**
     * Returns the service instance which was passed in while creating [RetrofitTestRule].
     * To be used inside the unit test to test the service.
     * @return T instance of the retrofit service
     */
    fun getService(): T {
        val url = mockWebServer.url("")
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(url)
        converterFactories.forEach { retrofitBuilder.addConverterFactory(it) }
        callAdapterFactories.forEach { retrofitBuilder.addCallAdapterFactory(it) }
        return retrofitBuilder
            .build()
            .create(clazz)
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val restRequest = description.getAnnotation(RestRequest::class.java)
                try {
                    restRequest?.prepareMockServer()
                    base.evaluate()
                    restRequest?.validateRequest()
                } finally {
                    cleanupMockServer()
                }
            }
        }
    }

    private fun RestRequest.prepareMockServer() {
        mockWebServer.start()
        val responseBody = if (responseOptions.bodyFile.isBlank()) {
            responseOptions.bodyText
        } else {
            getResourceContents(responseOptions.bodyFile)
        }
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(responseOptions.code)
                .setBody(responseBody)
        )
    }

    private fun getResourceContents(file: String): String {
        return try {
            clazz.getResourceAsStream(file).use { it?.bufferedReader()?.readText() }!!
        } catch (exception: Exception) {
            fail("Could not access resource file from $file. Make sure the file is at the class path")
            throw Exception("Could not access resource file from $file. Make sure the file is at the class path")
        }
    }

    private fun RestRequest.validateRequest() {
        val request = mockWebServer.takeRequest()
        request.logRequest()
        if (validations.path.isNotMagicNull() && request.path != validations.path) {
            fail("Expected request route <${validations.path}> but was <${request.path}>")
        }
        if (validations.method.isNotMagicNull() && request.method != validations.method) {
            fail("Expected request method <${validations.method}> but was <${request.method}>")
        }
        if (validations.headers.isNotEmpty()) {
            validations.headers.forEach { header ->
                val (key, value) = header.value.split(":").map { it.trim() }
                if (!request.headers.names().contains(key)) {
                    fail("Expected request header <$key> but not found")
                }
                if (request.headers[key] != value) {
                    fail("Expected request header value <$key : $value> but was <${request.headers[key]}>")
                }
            }
        }
        val validationBodyText = when {
            validations.bodyText.isNotMagicNull() -> validations.bodyText
            validations.bodyFile.isNotMagicNull() -> getResourceContents(validations.bodyFile)
            else -> ""
        }
        if (validationBodyText.isNotBlank() && request.body.copy()
                .readUtf8() != validationBodyText
        ) {
            fail("Expected request body \n<$validationBodyText>\n but was \n<${request.body.readUtf8()}>\n")
        }
    }

    private fun RecordedRequest.logRequest() {
        if (logLevel in arrayOf(LogLevel.BODY, LogLevel.HEADERS, LogLevel.BASIC)) {
            println("--> $requestLine")
        }
        if (logLevel in arrayOf(LogLevel.BODY, LogLevel.HEADERS)) {
            println("$headers")
        }
        if (logLevel == LogLevel.BODY) {
            println(body.copy().readUtf8())
        }
        if (logLevel in arrayOf(LogLevel.BODY, LogLevel.HEADERS, LogLevel.BASIC)) {
            println("<-- END HTTP")
        }
    }

    private fun String.isNotMagicNull(): Boolean = this != MAGIC_NULL

    private fun cleanupMockServer() {
        mockWebServer.shutdown()
    }
}

/**
 * Level of log output from the rule
 */
enum class LogLevel {
    /**
     * No logs
     */
    NONE,

    /**
     * Basic logs. Logs request line
     * Eg:
     * ```
     * --> GET /users/my_user/repos HTTP/1.1
     * <-- END HTTP
     * ```
     */
    BASIC,

    /**
     * Logs including headers
     * Eg:
     * ```
     * --> GET /users/my_user/repos HTTP/1.1
     * Host: localhost:52428
     * Connection: Keep-Alive
     * Accept-Encoding: gzip
     * User-Agent: okhttp/4.9.3
     * <-- END HTTP
     * ```
     */
    HEADERS,

    /**
     * Logs including body
     */
    BODY
}