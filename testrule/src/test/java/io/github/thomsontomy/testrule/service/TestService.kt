package io.github.thomsontomy.testrule.service

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Service used by the library's own tests. It lives in a sub package on purpose, so that resource
 * resolution relative to the *test* class can be verified.
 */
interface TestService {

    @GET("users/{user}/repos")
    fun repos(@Path("user") user: String): Call<List<String>>

    @POST("users")
    fun create(@Body user: User): Call<User>

    @PUT("api/test")
    fun put(@Query("id") id: String): Call<Boolean>

    @GET("search")
    fun search(@Query("q") query: String, @Query("page") page: Int): Call<List<String>>

    @GET("secure")
    fun secure(@Header("X-Token") token: String): Call<Map<String, String>>
}

/** Simple payload used by [TestService.create]. */
data class User(val name: String, val url: String)
