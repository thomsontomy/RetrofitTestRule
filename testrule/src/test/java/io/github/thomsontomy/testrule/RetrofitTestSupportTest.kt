package io.github.thomsontomy.testrule

import io.github.thomsontomy.testrule.service.TestService
import java.io.IOException
import java.time.Duration
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RetrofitTestSupportTest {

    private lateinit var support: RetrofitTestSupport<TestService>

    @BeforeEach
    fun setUp() {
        support = RetrofitTestSupport(TestService::class.java, defaultConverterFactories())
        support.start(RetrofitTestSupportTest::class.java)
    }

    @AfterEach
    fun tearDown() {
        support.stop()
    }

    @Test
    fun `detects requests that were never validated`() {
        support.enqueueResponse(body = "[]")
        support.enqueueResponse(body = "[]")
        val service = support.getService()
        service.repos("a").execute()
        service.repos("b").execute()

        support.takeRequest()

        val error = assertThrows(AssertionError::class.java) { support.assertAllRequestsConsumed() }
        assertTrue(error.message!!.contains("not covered"))
        assertTrue(error.message!!.contains("/users/b/repos"))
    }

    @Test
    fun `fails with a diagnostic when no request arrives`() {
        val error = assertThrows(AssertionError::class.java) {
            support.takeRequest(Duration.ofMillis(50))
        }
        assertTrue(error.message!!.contains("none arrived"))
    }

    @Test
    fun `detects enqueued responses that were never requested`() {
        support.enqueueResponse(body = "[]")
        support.enqueueResponse(body = "[]")

        val error = assertThrows(AssertionError::class.java) { support.assertNoPendingResponses() }
        assertTrue(error.message!!.contains("never requested"))
    }

    @Test
    fun `simulates a socket level failure`() {
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        support.enqueueNetworkError(SocketBehavior.DISCONNECT_AT_START)

        assertThrows(IOException::class.java) {
            support.getService(client).repos("tom").execute()
        }
    }

    @Test
    fun `snapshot exposes decoded query parameters and case insensitive headers`() {
        support.enqueueResponse(body = "[]")
        support.getService().search("a b", 3).execute()

        val request = support.takeRequest()

        assertEquals("/search", request.path)
        assertEquals("a b", request.queryParameter("q"))
        assertEquals(listOf("3"), request.queryParameterValues("page"))
        assertNotNull(request.header("host"))
        assertTrue(request.url.startsWith("http://localhost:"))
    }

    @Test
    fun `start and stop are idempotent and the support can be reused`() {
        support.stop()
        support.stop()
        support.start()

        support.enqueueResponse(body = "[]")
        support.getService().repos("tom").execute()

        support.verifyRequest { hasPath("/users/tom/repos") }
        assertEquals(1, support.recordedRequests.size)
    }

    @Test
    fun `pending response count reflects served responses`() {
        support.enqueueResponse(body = "[]")
        support.enqueueResponse(body = "[]")
        assertEquals(2, support.pendingResponses)

        support.getService().repos("tom").execute()
        assertEquals(1, support.pendingResponses)
    }

    @Test
    fun `verification failures include the captured request`() {
        support.enqueueResponse(body = "[]")
        support.getService().repos("tom").execute()

        val error = assertThrows(AssertionError::class.java) {
            support.verifyRequest { hasMethod(HttpMethod.POST) }
        }
        assertTrue(error.message!!.contains("Expected request method <POST>"))
        assertTrue(error.message!!.contains("GET http://localhost:"))
    }
}
