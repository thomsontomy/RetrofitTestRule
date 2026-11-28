package io.github.thomsontomy.testrule

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StructuralBodyMatcherTest {

    @Test
    fun `ignores object key order`() {
        BodyMatchers.STRUCTURAL.assertMatches(
            """{"name":"tom","url":"https://example.com"}""",
            """{"url":"https://example.com","name":"tom"}""",
        )
    }

    @Test
    fun `ignores whitespace and newlines`() {
        BodyMatchers.STRUCTURAL.assertMatches(
            """{"a":1,"b":[1,2]}""",
            """
            {
              "a" : 1,
              "b" : [ 1, 2 ]
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `array order is significant`() {
        val error = assertThrows(AssertionError::class.java) {
            BodyMatchers.STRUCTURAL.assertMatches("""["a","b"]""", """["b","a"]""")
        }
        assertTrue(error.message!!.contains("structural JSON comparison"))
    }

    @Test
    fun `falls back to a trimmed text comparison when a body is not JSON`() {
        BodyMatchers.STRUCTURAL.assertMatches("  hello world\n", "hello world")
    }

    @Test
    fun `reports expected and actual bodies on mismatch`() {
        val error = assertThrows(AssertionError::class.java) {
            BodyMatchers.STRUCTURAL.assertMatches("""{"a":1}""", """{"a":2}""")
        }
        assertTrue(error.message!!.contains("\"a\": 1"))
        assertTrue(error.message!!.contains("\"a\": 2"))
    }

    @Test
    fun `exact matcher requires identical strings`() {
        BodyMatchers.EXACT.assertMatches("{}", "{}")
        assertThrows(AssertionError::class.java) {
            BodyMatchers.EXACT.assertMatches("{}", "{ }")
        }
    }
}
