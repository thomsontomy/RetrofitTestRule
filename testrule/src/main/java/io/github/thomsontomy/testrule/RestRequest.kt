package io.github.thomsontomy.testrule

import retrofit2.http.Header

annotation class RestRequest(
    val validations: RequestValidations = RequestValidations(),
    val responseOptions: ResponseOptions = ResponseOptions()
)

annotation class ResponseOptions(
    val code: Int = 200,
    val bodyText: String = "{}",
    val bodyFile: String = ""
)

annotation class RequestValidations(
    val path: String = "",
    val method: String = "",
    val headers: Array<Header> = [],
    val bodyText: String = "",
    val bodyFile: String = ""
)