package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GitHubNetworkClient {

    @Volatile
    private var authToken: String = ""

    fun updateToken(token: String) {
        authToken = token.trim()
    }

    fun currentToken(): String = authToken

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val apiHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "RepoForge-Android")

        val token = authToken
        if (token.isNotBlank() && original.url.host.equals("api.github.com", ignoreCase = true)) {
            builder.header("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    private val apiOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiHeaderInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Dedicated OkHttpClient for binary artifact downloads that handles GitHub's 302 redirect
    // to external blob storage without forwarding the GitHub Bearer token to third-party hosts.
    val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun executeAuthorizedDownload(url: String): Response {
        val initialBuilder = okhttp3.Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "RepoForge-Android")

        val token = authToken
        if (token.isNotBlank() && url.contains("api.github.com", ignoreCase = true)) {
            initialBuilder.header("Authorization", "Bearer $token")
        }

        var response = downloadClient.newCall(initialBuilder.build()).execute()
        var redirects = 0
        while (response.code in listOf(301, 302, 303, 307, 308) && redirects < 5) {
            val location = response.header("Location") ?: break
            response.close()
            val nextReq = okhttp3.Request.Builder()
                .url(location)
                .header("User-Agent", "RepoForge-Android")
                .apply {
                    if (location.contains("api.github.com", ignoreCase = true) && token.isNotBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                }
                .build()
            response = downloadClient.newCall(nextReq).execute()
            redirects++
        }
        return response
    }

    val apiService: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(apiOkHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApiService::class.java)
}
