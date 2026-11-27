package io.github.thomsontomy.testrule

import com.google.gson.GsonBuilder
import io.github.thomsontomy.testrule.service.TestService
import io.github.thomsontomy.testrule.service.User
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitTestRuleTest {

    @get:Rule
    val rule = RetrofitTestRule(TestService::class.java)

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/users/tom/repos", method = HttpMethod.GET),
        respond = RespondWith(bodyText = """["a","b"]"""),
    )
    fun `annotation api validates the request and serves the response`() {
        val response = rule.getService().repos("tom").execute()

        assertEquals(listOf("a", "b"), response.body())
    }

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/users/a/repos"),
        respond = RespondWith(bodyText = "[]"),
    )
    @RestRequest(
        expect = ExpectRequest(path = "/users/b/repos"),
        respond = RespondWith(bodyText = "[]"),
    )
    fun `repeatable annotations match requests in declaration order`() {
        val service = rule.getService()

        service.repos("a").execute()
        service.repos("b").execute()
    }

    @Test
    @RestRequest(
        expect = ExpectRequest(
            path = "/users",
            method = HttpMethod.POST,
            headers = [HeaderValue("Content-Type", "application/json")],
            bodyFile = "request_body.json",
        ),
        respond = RespondWith(bodyText = """{"name":"tom","url":"https://example.com/tom"}"""),
    )
    fun `validates a body loaded from a resource next to the test class`() {
        val response = rule.getService().create(User("tom", "https://example.com/tom")).execute()

        assertEquals("tom", response.body()?.name)
    }

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/search?page=2&q=retrofit"),
        respond = RespondWith(bodyText = "[]"),
    )
    fun `query parameters are validated independently of their order`() {
        rule.getService().search("retrofit", 2).execute()
    }

    @Test
    @RestRequest(
        expect = ExpectRequest(
            path = "/secure",
            headers = [HeaderValue("X-Token", "secret")],
        ),
        respond = RespondWith(bodyFile = "body.json"),
    )
    fun `serves a response body from a resource file`() {
        val response = rule.getService().secure("secret").execute()

        assertEquals("octocat", response.body()?.get("login"))
    }

    @Test
    fun `dsl api works without annotations`() {
        rule.enqueueResponse(body = """["x"]""")

        val response = rule.getService().repos("tom").execute()

        assertEquals(listOf("x"), response.body())
        rule.verifyRequest {
            hasMethod(HttpMethod.GET)
            hasPath("/users/tom/repos")
        }
    }

    @Test
    fun `structural json comparison ignores key order`() {
        rule.enqueueResponse(body = """{"name":"tom","url":"u"}""")

        rule.getService().create(User("tom", "u")).execute()

        rule.verifyRequest {
            hasBody("""{"url":"u","name":"tom"}""")
        }
    }

    @Test
    fun `custom okhttp client interceptors are preserved`() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("X-Trace", "abc").build())
            }
            .build()

        rule.enqueueResponse(body = "[]")
        rule.getService(client).repos("tom").execute()

        rule.verifyRequest { hasHeader("x-trace", "abc") }
    }

    @Test
    fun `retrofit builder configuration is applied on top of the defaults`() {
        rule.enqueueResponse(body = "[]")

        val service = rule.getService {
            addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        }

        service.repos("tom").execute()
        rule.verifyRequest { hasPath("/users/tom/repos") }
    }

    @Test
    fun `service factory receives the mock server url`() {
        rule.enqueueResponse(body = "[]")
        var receivedUrl: HttpUrl? = null

        val service = rule.service { url ->
            receivedUrl = url
            Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TestService::class.java)
        }
        service.repos("tom").execute()

        assertNotNull(receivedUrl)
        assertEquals(rule.baseUrl, receivedUrl)
        rule.verifyRequest { hasPath("/users/tom/repos") }
    }

    @Test
    fun `response status codes and headers are served`() {
        rule.enqueueResponse(
            code = 404,
            body = """{"message":"not found"}""",
            headers = mapOf("X-Request-Id" to "abc-123"),
        )

        val response = rule.getService().repos("missing").execute()

        assertEquals(404, response.code())
        assertEquals("abc-123", response.headers()["X-Request-Id"])
        assertEquals("""{"message":"not found"}""", response.errorBody()?.string())
        rule.verifyRequest { hasPath("/users/missing/repos") }
    }
}
