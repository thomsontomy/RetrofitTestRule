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
}