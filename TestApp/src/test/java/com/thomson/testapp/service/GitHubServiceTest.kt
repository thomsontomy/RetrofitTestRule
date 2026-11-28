package com.thomson.testapp.service

import io.github.thomsontomy.testrule.ExpectRequest
import io.github.thomsontomy.testrule.HeaderValue
import io.github.thomsontomy.testrule.HttpMethod
import io.github.thomsontomy.testrule.RespondWith
import io.github.thomsontomy.testrule.RestRequest
import io.github.thomsontomy.testrule.RetrofitTestRule
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GitHubServiceTest {

    @get:Rule
    val retrofitTestRule = RetrofitTestRule(GitHubService::class.java)

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/users/my_user/repos", method = HttpMethod.GET),
        respond = RespondWith(bodyText = "[]"),
    )
    fun `listRepos should send correct userId`() = runTest {
        retrofitTestRule.getService().listRepos("my_user")
    }

    @Test
    @RestRequest(
        expect = ExpectRequest(
            path = "/users/test_user/repos",
            method = HttpMethod.POST,
            headers = [HeaderValue("Content-Type", "application/json")],
            // Key order is intentionally different from the serialized payload.
            bodyText = """{"url":"https://test.com","name":"tom"}""",
        ),
        respond = RespondWith(bodyText = "[]"),
    )
    fun `submitRepo should send correct parameters`() = runTest {
        retrofitTestRule.getService().submitRepo("test_user", User("tom", "https://test.com"))
    }

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/api/test?id=john", method = HttpMethod.PUT),
        respond = RespondWith(bodyText = "true"),
    )
    fun `submitCode should send correct parameters`() {
        retrofitTestRule.getService().submitCode("john").execute()
    }

    @Test
    @RestRequest(
        expect = ExpectRequest(
            path = "/api/get-user-type",
            method = HttpMethod.POST,
            bodyFile = "getUsersWithType_request_body.json",
        ),
        respond = RespondWith(bodyFile = "getUsersWithType_response_body.json"),
    )
    fun `getUsersWithType should send correct parameters`() {
        val response = retrofitTestRule.getService()
            .getUsersWithType(UserType("th", "admin"))
            .execute()

        assertEquals(2, response.body()?.size)
    }

    @Test
    @RestRequest(
        expect = ExpectRequest(path = "/users/first/repos"),
        respond = RespondWith(bodyText = "[]"),
    )
    @RestRequest(
        expect = ExpectRequest(path = "/users/second/repos"),
        respond = RespondWith(bodyText = "[]"),
    )
    fun `sequential requests are matched in declaration order`() = runTest {
        val service = retrofitTestRule.getService()

        service.listRepos("first")
        service.listRepos("second")
    }

    @Test
    fun `dsl api supports dynamic responses`() = runTest {
        retrofitTestRule.enqueueResponse(
            code = 200,
            body = """[{"name":"tom","url":"https://test.com"}]""",
            headers = mapOf("X-Rate-Limit" to "42"),
        )

        val repos = retrofitTestRule.getService().listRepos("tom")

        assertEquals(1, repos.size)
        retrofitTestRule.verifyRequest {
            hasMethod(HttpMethod.GET)
            hasPath("/users/tom/repos")
            hasHeader("Accept-Encoding")
        }
    }

    @Test
    fun `production okhttp client interceptors are preserved`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .header("Authorization", "Bearer token")
                        .build(),
                )
            }
            .build()

        retrofitTestRule.enqueueResponse(body = "[]")

        retrofitTestRule.getService(client).listRepos("me")

        retrofitTestRule.verifyRequest {
            hasHeader("authorization", "Bearer token")
        }
    }

    @Test
    fun `error responses are exposed to the service`() {
        retrofitTestRule.enqueueResponse(code = 500, body = """{"message":"boom"}""")

        val response = retrofitTestRule.getService().submitCode("x").execute()

        assertEquals(500, response.code())
        assertEquals("""{"message":"boom"}""", response.errorBody()?.string())
        retrofitTestRule.verifyRequest { hasPath("/api/test?id=x") }
    }
}
