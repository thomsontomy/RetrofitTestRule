package io.github.thomsontomy.testrule

import io.github.thomsontomy.testrule.service.TestService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class RetrofitTestExtensionTest {

    @JvmField
    @RegisterExtension
    val rule = RetrofitTestExtension(TestService::class.java)

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/users/tom/repos", method = HttpMethod.GET),
        respond = RespondWith(bodyText = """["a"]"""),
    )
    fun `annotation api works with junit 5`() {
        val response = rule.getService().repos("tom").execute()

        assertEquals(listOf("a"), response.body())
    }

    @Test
    fun `dsl api works with junit 5`() {
        rule.enqueueResponse(body = """["b"]""")

        val response = rule.getService().repos("tom").execute()

        assertEquals(listOf("b"), response.body())
        rule.verifyRequest { hasPath("/users/tom/repos") }
    }
}
