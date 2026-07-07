package com.example.smokeshield

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

// 1. Definiamo le regole della chiamata (vogliamo 10 domande a risposta multipla)
interface TriviaApiService {
    @GET("api.php")
    suspend fun getQuestions(
        @Query("amount") amount: Int = 10,
        @Query("type") type: String = "multiple"
    ): TriviaResponse
    @GET
    suspend fun getItalianQuestions(@Url url: String): TriviaResponse
}

// 2. Creiamo l'oggetto Retrofit che useremo in tutta l'app
object TriviaApi {
    private const val BASE_URL = "https://opentdb.com/"

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create()) // Il traduttore Gson
        .baseUrl(BASE_URL)
        .build()

    // Questo è il "telefono" pronto all'uso
    val retrofitService: TriviaApiService by lazy {
        retrofit.create(TriviaApiService::class.java)
    }
}