package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val items: List<SearchResultItem>? = null,
    val searchInformation: SearchInformation? = null
)

@JsonClass(generateAdapter = true)
data class SearchInformation(
    val totalResults: String? = null,
    val formattedSearchTime: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchResultItem(
    val title: String = "",
    val link: String = "",
    val snippet: String = "",
    val displayLink: String? = null
)

interface GoogleSearchService {
    @GET("customsearch/v1")
    suspend fun search(
        @Query("key") apiKey: String,
        @Query("cx") cx: String,
        @Query("q") query: String,
        @Query("num") num: Int = 10
    ): SearchResponse

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/"

        fun create(): GoogleSearchService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GoogleSearchService::class.java)
        }
    }
}
