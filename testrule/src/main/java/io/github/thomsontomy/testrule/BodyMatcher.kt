package io.github.thomsontomy.testrule

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

/**
 * Strategy used to compare an expected request body with the body that was actually sent.
 *
 * Implement this interface to plug in a custom comparison, for example one based on
 * `org.skyscreamer:jsonassert` or on a tolerant numeric comparison.
 *
 * Implementations must throw [AssertionError] with a human readable diagnostic when the bodies do
 * not match, and return normally when they do.
 */
fun interface BodyMatcher {

    /**
     * Asserts that [actual] matches [expected].
     *
     * @throws AssertionError when the bodies differ
     */
    fun assertMatches(expected: String, actual: String)
}

/**
 * Default [BodyMatcher] used by [RetrofitTestRule] and [RetrofitTestExtension].
 *
 * When both bodies are valid JSON they are compared as trees, so object key order and whitespace
 * are ignored while array order is preserved. When either body is not JSON the comparison falls
 * back to a whitespace-insensitive text comparison.
 */
object StructuralBodyMatcher : BodyMatcher {

    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    override fun assertMatches(expected: String, actual: String) {
        val expectedJson = parseOrNull(expected)
        val actualJson = parseOrNull(actual)

        if (expectedJson != null && actualJson != null) {
            if (expectedJson != actualJson) {
                throw AssertionError(
                    buildString {
                        append("Request body did not match (structural JSON comparison).")
                        append("\n\nExpected:\n").append(prettyGson.toJson(expectedJson))
                        append("\n\nActual:\n").append(prettyGson.toJson(actualJson))
                    },
                )
            }
            return
        }

        if (expected.trim() != actual.trim()) {
            throw AssertionError(
                buildString {
                    append("Request body did not match (text comparison, at least one body is not JSON).")
                    append("\n\nExpected:\n").append(expected)
                    append("\n\nActual:\n").append(actual)
                },
            )
        }
    }

    private fun parseOrNull(text: String): JsonElement? = try {
        JsonParser.parseString(text)
    } catch (exception: JsonParseException) {
        null
    } catch (exception: IllegalStateException) {
        // Thrown by older Gson versions for malformed input.
        null
    }
}

/**
 * Compares request bodies as exact strings.
 *
 * Use this when whitespace and formatting are part of the contract, for example for signed
 * payloads or plain text bodies.
 */
object ExactBodyMatcher : BodyMatcher {
    override fun assertMatches(expected: String, actual: String) {
        if (expected != actual) {
            throw AssertionError(
                buildString {
                    append("Request body did not match (exact comparison).")
                    append("\n\nExpected:\n").append(expected)
                    append("\n\nActual:\n").append(actual)
                },
            )
        }
    }
}

/**
 * Ready to use [BodyMatcher] instances.
 *
 * `rule.bodyMatcher = BodyMatchers.EXACT` switches the rule to exact string comparison.
 */
object BodyMatchers {

    /** Structural JSON comparison with a whitespace-insensitive text fallback. */
    @JvmField
    val STRUCTURAL: BodyMatcher = StructuralBodyMatcher

    /** Exact string comparison. */
    @JvmField
    val EXACT: BodyMatcher = ExactBodyMatcher
}
