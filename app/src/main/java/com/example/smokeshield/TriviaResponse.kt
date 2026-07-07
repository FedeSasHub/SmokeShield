package com.example.smokeshield

import com.google.gson.annotations.SerializedName

// Questa è la scatola principale che ci manda il server
data class TriviaResponse(
    val response_code: Int,
    val results: List<TriviaQuestion>
)

// Questa è la singola domanda contenuta nella scatola
data class TriviaQuestion(
    val question: String,
    @SerializedName("correct_answer") val correctAnswer: String,
    @SerializedName("incorrect_answers") val incorrectAnswers: List<String>
)