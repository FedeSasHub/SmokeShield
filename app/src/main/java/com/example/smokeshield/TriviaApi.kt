package com.example.smokeshield

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface TriviaApiService {
    @GET("api.php")
    suspend fun getQuestions(
        @Query("amount") amount: Int = 10,
        @Query("type") type: String = "multiple"
    ): TriviaResponse

    @GET
    suspend fun getItalianQuestions(@Url url: String): TriviaResponse
}

object TriviaApi {
    private const val BASE_URL = "https://opentdb.com/"

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .build()

    val retrofitService: TriviaApiService by lazy {
        retrofit.create(TriviaApiService::class.java)
    }
}