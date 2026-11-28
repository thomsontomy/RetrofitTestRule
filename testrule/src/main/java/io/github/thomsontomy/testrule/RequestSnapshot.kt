package io.github.thomsontomy.testrule

import java.util.TreeMap
import okhttp3.HttpUrl
import okhttp3.mockwebserver.RecordedRequest

/**
 * An immutable snapshot of an HTTP request captured by the mock server.
 *
 * The snapshot deliberately does not expose OkHttp's `RecordedRequest`: the mock server buffers
 * are consumed when read, and leaking OkHttp types into the public API would pin consumers to a
 * specific OkHttp version. Use [RequestAssertions] (or [RetrofitTestDsl.verifyRequest]) to assert
 * on a snapshot.
 *
 * @property sequence zero-based index of the request on the mock server
 * @property method HTTP method, for example `GET`
 * @property url full request URL including the query string
 * @property path request path without the query string
 * @property query decoded query parameters. A parameter declared without a value (`?flag`) is
 * represented by an empty string.
 * @property headers request headers. Lookups through [header] and [headerValues] are case
 * insensitive.
 * @property body request body decoded as UTF-8. Binary bodies are decoded lossily; use
 * [RetrofitTestDsl.server] if you need the raw bytes.
 * @see RequestAssertions
 */
data class RequestSnapshot(
    val sequence: Int,
    val method: String,
    val url: String,
    val path: String,
    val query: Map<String, List<String>>,
    val headers: Map<String, List<String>>,
    val body: String,
) {

    /** Returns the first value of [name], or `null` when the header is absent. */
    fun header(name: String): String? = headers[name]?.firstOrNull()

    /** Returns every value of [name], or an empty list when the header is absent. */
    fun headerValues(name: String): List<String> = headers[name].orEmpty()

    /** Returns the first value of the query parameter [name], or `null` when absent. */
    fun queryParameter(name: String): String? = query[name]?.firstOrNull()

    /** Returns every value of the query parameter [name], or an empty list when absent. */
    fun queryParameterValues(name: String): List<String> = query[name].orEmpty()

    /**
     * Renders the request in a form suitable for failure messages and logs.
     *
     * @param includeBody when `false` only the request line and headers are rendered
     */
    fun format(includeBody: Boolean = true): String = buildString {
        append("Request #").append(sequence + 1).append(": ").append(method).append(' ').append(url)
        if (headers.isNotEmpty()) {
            append('\n').append("Headers:")
            headers.forEach { (name, values) ->
                values.forEach { value -> append("\n  ").append(name).append(": ").append(value) }
            }
        }
        if (includeBody) {
            append('\n').append("Body:")
            if (body.isEmpty()) {
                append("\n  <empty>")
            } else {
                body.lineSequence().forEach { append("\n  ").append(it) }
            }
        }
    }

    override fun toString(): String = format()

    internal companion object {
        /** Builds a snapshot, copying every buffer so the recorded request stays readable. */
        fun from(request: RecordedRequest): RequestSnapshot {
            val httpUrl: HttpUrl? = request.requestUrl
            return RequestSnapshot(
                sequence = request.sequenceNumber,
                method = request.method.orEmpty(),
                url = httpUrl?.toString() ?: request.path.orEmpty(),
                path = httpUrl?.encodedPath ?: request.path.orEmpty().substringBefore('?'),
                query = httpUrl.toQueryMap(),
                headers = request.headers.toCaseInsensitiveMap(),
                body = request.body.copy().readUtf8(),
            )
        }

        private fun HttpUrl?.toQueryMap(): Map<String, List<String>> {
            if (this == null) return emptyMap()
            val result = LinkedHashMap<String, List<String>>()
            queryParameterNames.forEach { name ->
                result[name] = queryParameterValues(name).map { it ?: "" }
            }
            return result
        }

        private fun okhttp3.Headers.toCaseInsensitiveMap(): Map<String, List<String>> {
            val result = TreeMap<String, MutableList<String>>(String.CASE_INSENSITIVE_ORDER)
            for (index in 0 until size) {
                result.getOrPut(name(index)) { mutableListOf() }.add(value(index))
            }
            return result
        }
    }
}
