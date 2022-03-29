package io.github.thomsontomy.testrule

import retrofit2.http.Header
import kotlin.annotation.AnnotationTarget.FUNCTION

internal const val MAGIC_NULL = "#"

/**
 * Top level annotation class for unit tests
 * @property validations Validations need to be done on the captured request
 * @property responseOptions Response configurations
 */
@Target(FUNCTION)
annotation class RestRequest(
    val validations: RequestValidations = RequestValidations(),
    val responseOptions: ResponseOptions = ResponseOptions()
)

/**
 * Response configurations
 * Child annotation class to be added in [RestRequest]
 * @property code HTTP response code
 * @property bodyText Response text. Default value is an empty json object string `{}`
 * @property bodyFile Response data from resource file. File need to be in the same package inside the resources
 */
@Target
annotation class ResponseOptions(
    val code: Int = 200,
    val bodyText: String = "{}",
    val bodyFile: String = ""
)

/**
 * Validations to be done on the captured HTTP request
 * Child annotation class to be added in [RestRequest]
 * @property path Request path(from the base url)
 * @property method HTTP Request method
 * @property headers Array of Retrofit [Header]s
 * @property bodyText Expected body text
 * @property bodyFile Expected body text from resource file. File need to be in the same package inside the resources
 */
@Target
annotation class RequestValidations(
    val path: String = MAGIC_NULL,
    val method: String = MAGIC_NULL,
    val headers: Array<Header> = [],
    val bodyText: String = MAGIC_NULL,
    val bodyFile: String = MAGIC_NULL
)