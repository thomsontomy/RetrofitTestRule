package io.github.thomsontomy.testrule

import io.github.thomsontomy.testrule.service.TestService
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResourceResolverTest {

    private fun resolver() = ResourceResolver(ResourceResolverTest::class.java, TestService::class.java)

    @Test
    fun `resolves a resource relative to the test class package`() {
        assertTrue(resolver().read("body.json").contains("octocat"))
    }

    @Test
    fun `resolves a resource at an absolute classpath path`() {
        assertTrue(
            resolver().read("/io/github/thomsontomy/testrule/body.json").contains("octocat"),
        )
    }

    @Test
    fun `resolves a file from disk`() {
        val file = Files.createTempFile("retrofit-test-rule", ".json")
        try {
            Files.writeString(file, """{"disk":true}""")
            assertEquals(
                """{"disk":true}""",
                resolver().read(file.toAbsolutePath().toString()),
            )
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun `failure lists every attempted location`() {
        val error = assertThrows(AssertionError::class.java) {
            resolver().read("does-not-exist.json")
        }
        assertTrue(error.message!!.contains("Tried:"))
        assertTrue(error.message!!.contains("does-not-exist.json"))
    }
}
