package com.thomson.testapp.service

import io.github.thomsontomy.testrule.*
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GitHubServiceTest {

    @get:Rule
    val retrofitTestRule = RetrofitTestRule(GitHubService::class.java, logLevel = LogLevel.BODY)

    @Test
    @RestRequest(
        validations = RequestValidations(path = "/users/my_user/repos", method = "GET"),
        responseOptions = ResponseOptions(bodyText = "[]")
    )
    fun `listRepos should send correct userId`() = runTest {
        val service = retrofitTestRule.getService()

        service.listRepos("my_user")
    }

    @Test
    @RestRequest(
        validations = RequestValidations(
            path = "/users/test_user/repos",
            bodyText = """{"name":"tom","url":"https://test.com"}"""
        ),
        responseOptions = ResponseOptions(bodyText = "[]")
    )
    fun `submitRepo should send correct parameters`() = runTest {
        val service = retrofitTestRule.getService()

        service.submitRepo("test_user", User("tom", "https://test.com"))
    }

    @Test
    @RestRequest(
        validations = RequestValidations(path = "/api/test?id=john"),
        responseOptions = ResponseOptions(bodyText = "true")
    )
    fun `submitCode should send correct parameters`() {
        val service = retrofitTestRule.getService()

        service.submitCode("john").execute()
    }

    @Test
    @RestRequest(
        validations = RequestValidations(
            path = "/api/get-user-type",
            bodyFile = "getUsersWithType_request_body.json"
        ),
        responseOptions = ResponseOptions(bodyFile = "getUsersWithType_response_body.json")
    )
    fun `getUsersWithType should send correct parameters`() {
        val service = retrofitTestRule.getService()

        service.getUsersWithType(UserType("th", "admin")).execute()
    }
}