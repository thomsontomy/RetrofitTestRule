package com.thomson.testapp.service

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GitHubService {

    @GET("users/{user}/repos")
    suspend fun listRepos(@Path("user") user: String): List<GithubRepo>

    @POST("users/{user}/repos")
    suspend fun submitRepo(@Path("user") user: String, @Body userObj: User): List<GithubRepo>
}

data class User(
    @SerializedName("name")
    val name: String,
    @SerializedName("url")
    val url: String
)