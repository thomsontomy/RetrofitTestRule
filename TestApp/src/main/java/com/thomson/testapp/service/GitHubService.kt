package com.thomson.testapp.service

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubService {

    @GET("users/{user}/repos")
    suspend fun listRepos(@Path("user") user: String): List<GithubRepo>

    @POST("users/{user}/repos")
    suspend fun submitRepo(@Path("user") user: String, @Body userObj: User): List<GithubRepo>

    @PUT("api/test")
    fun submitCode(@Query("id") id: String): Call<Boolean>

    @POST("api/get-user-type")
    fun getUsersWithType(@Body userType: UserType): Call<List<User>>
}

data class User(
    @SerializedName("name")
    val name: String,
    @SerializedName("url")
    val url: String,
)

data class UserType(
    @SerializedName("startsWith")
    val startsWith: String,
    @SerializedName("permission")
    val permission: String,
)
