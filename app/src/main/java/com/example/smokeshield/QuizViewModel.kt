package com.example.smokeshield

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// 1. Creiamo una struttura dati interna per le nostre domande fittizie
data class Question(
    val text: String,
    val answers: List<String>,
    val correctAnswerIndex: Int
)

class QuizViewModel : ViewModel() {

    private val _timeLeft = MutableLiveData<String>()
    val timeLeft: LiveData<String> get() = _timeLeft

    // Nuovi LiveData per Punteggio e Domanda Corrente
    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> get() = _score

    private val _currentQuestion = MutableLiveData<Question>()
    val currentQuestion: LiveData<Question> get() = _currentQuestion

    private var timer: CountDownTimer? = null
    private var questionIndex = 0

    // Un mini-database di prova
    private val dummyQuestions = listOf(
        Question("Quale di questi film è stato diretto da Stanley Kubrick?", listOf("Arancia Meccanica", "Taxi Driver", "Apocalypse Now", "Blade Runner"), 0),
        Question("In quale anno è uscito il primo videogioco di Super Mario?", listOf("1981", "1985", "1990", "1993"), 1),
        Question("Quale console ha venduto più unità nella storia?", listOf("PlayStation 2", "Nintendo DS", "Game Boy", "Nintendo Switch"), 0),
        Question("Quale regista ha diretto 'Il Petroliere' (There Will Be Blood)?", listOf("Wes Anderson", "Paul Thomas Anderson", "Quentin Tarantino", "Martin Scorsese"), 1)
    )

    init {
        // Appena il ViewModel nasce, carichiamo la prima domanda
        _currentQuestion.value = dummyQuestions[questionIndex]
    }

    // -- BLOCCO TIMER (Identico a prima) --
    fun setInitialMinutes(minutes: Int) {
        if (timer == null) {
            startTimer(minutes)
        }
    }

    private fun startTimer(minutes: Int) {
        val timeInMillis = minutes * 60 * 1000L
        timer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minLeft = (millisUntilFinished / 1000) / 60
                val secLeft = (millisUntilFinished / 1000) % 60
                _timeLeft.value = String.format("%02d:%02d", minLeft, secLeft)
            }
            override fun onFinish() {
                _timeLeft.value = "00:00"
            }
        }.start()
    }

    // -- BLOCCO LOGICA QUIZ --
    fun checkAnswer(selectedIndex: Int) {
        val currentQ = _currentQuestion.value ?: return

        // Punteggio: +10 se esatta, -5 se errata
        if (selectedIndex == currentQ.correctAnswerIndex) {
            _score.value = (_score.value ?: 0) + 10
        } else {
            _score.value = (_score.value ?: 0) - 5
        }

        // Passiamo alla domanda successiva
        questionIndex = (questionIndex + 1) % dummyQuestions.size
        _currentQuestion.value = dummyQuestions[questionIndex]
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
