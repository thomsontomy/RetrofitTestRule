package io.github.thomsontomy.testrule

/**
 * Assertions over a captured [RequestSnapshot].
 *
 * Instances are created by [RetrofitTestDsl.verifyRequest] and by the annotation driven rule, so
 * assertions never need to be constructed directly.
 *
 * Every failure message includes the full request, which makes a failing test self explanatory
 * without enabling logging:
 *
 * ```kotlin
 * rule.verifyRequest {
 *     hasMethod(HttpMethod.POST)
 *     hasPath("/users")
 *     hasHeader("Authorization", "Bearer token")
 *     hasBody("""{"name":"tom"}""")
 * }
 * ```
 */
class RequestAssertions internal constructor(
    val request: RequestSnapshot,
    private val resolver: ResourceResolver,
    private val bodyMatcher: BodyMatcher,
) {

    /** Asserts the HTTP method equals [expected]. */
    fun hasMethod(expected: HttpMethod) = hasMethod(expected.name)

    /** Asserts the HTTP method equals [expected], for example `"GET"`. */
    fun hasMethod(expected: String) {
        if (!request.method.equals(expected, ignoreCase = true)) {
            fail("Expected request method <$expected> but was <${request.method}>.")
        }
    }

    /**
     * Asserts the request path equals [expected].
     *
     * A query string may be included for convenience (`/api/test?id=john`). Each parameter is then
     * validated individually, so parameter order does not matter.
     */
    fun hasPath(expected: String) {
        val expectedPath = expected.substringBefore('?')
        if (request.path != expectedPath) {
            fail("Expected request path <$expectedPath> but was <${request.path}>.")
        }
        expected.substringAfter('?', missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() }
            ?.split('&')
            ?.filter { it.isNotEmpty() }
            ?.forEach { parameter ->
                val name = parameter.substringBefore('=').decodeUrlComponent()
                val value = parameter.substringAfter('=', missingDelimiterValue = "").decodeUrlComponent()
                hasQuery(name, value)
            }
    }

    /** Asserts the query parameter [name] is present with [value]. */
    fun hasQuery(name: String, value: String) {
        val values = request.queryParameterValues(name)
        if (values.isEmpty()) {
            fail("Expected query parameter <$name> but it was not present.")
        }
        if (value !in values) {
            fail("Expected query parameter <$name=$value> but was <$name=${values.joinToString("&")}>.")
        }
    }

    /** Asserts the query parameter [name] is present with any value. */
    fun hasQuery(name: String) {
        if (request.queryParameterValues(name).isEmpty()) {
            fail("Expected query parameter <$name> but it was not present.")
        }
    }

    /**
     * Asserts the header [name] is present.
     *
     * Values are compared exactly. Media type parameters are tolerated: when the expected value has
     * no `;` and the actual value does, only the part before `;` is compared. This keeps
     * `hasHeader("Content-Type", "application/json")` working when Retrofit appends a charset.
     *
     * @param value when non-null, one of the header values must match it
     */
    fun hasHeader(name: String, value: String? = null) {
        val values = request.headerValues(name)
        if (values.isEmpty()) {
            fail("Expected request header <$name> but it was not present.")
        }
        if (value != null && values.none { it.matchesHeaderValue(value) }) {
            fail("Expected request header <$name: $value> but was <$name: ${values.joinToString(", ")}>.")
        }
    }

    /** Asserts the request body matches [expected] using the configured [BodyMatcher]. */
    fun hasBody(expected: String) {
        try {
            bodyMatcher.assertMatches(expected, request.body)
        } catch (exception: AssertionError) {
            fail(exception.message ?: "Request body did not match.")
        }
    }

    /** Asserts the request body matches the contents of [file]. */
    fun hasBodyFile(file: String) = hasBody(resolver.read(file))

    /** Asserts the request has no body. */
    fun hasNoBody() {
        if (request.body.isNotEmpty()) {
            fail("Expected an empty request body but was:\n${request.body}")
        }
    }

    /**
     * Applies every expectation declared on [expect].
     *
     * Empty values are skipped, so an all-default [ExpectRequest] validates nothing.
     */
    internal fun applyExpectation(expect: ExpectRequest) {
        if (expect.path.isNotEmpty()) hasPath(expect.path)
        if (expect.method != HttpMethod.ANY) hasMethod(expect.method)
        expect.query.forEach { hasQuery(it.name, it.value) }
        expect.headers.forEach { hasHeader(it.name, it.value) }
        when {
            expect.bodyText.isNotEmpty() && expect.bodyFile.isNotEmpty() ->
                fail("ExpectRequest declares both bodyText and bodyFile. Use exactly one of them.")
            expect.bodyText.isNotEmpty() -> hasBody(expect.bodyText)
            expect.bodyFile.isNotEmpty() -> hasBodyFile(expect.bodyFile)
        }
    }

    private fun fail(message: String): Nothing = throw AssertionError("$message\n\n${request.format()}")

    private fun String.matchesHeaderValue(expected: String): Boolean {
        if (this == expected) return true
        return expected.indexOf(';') < 0 && substringBefore(';').trim() == expected.trim()
    }

    private fun String.decodeUrlComponent(): String = java.net.URLDecoder.decode(this, Charsets.UTF_8.name())
}
