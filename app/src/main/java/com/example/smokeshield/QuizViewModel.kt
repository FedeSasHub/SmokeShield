package com.example.smokeshield

import android.os.CountDownTimer
import android.text.Html
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class Question(
    val text: String,
    val answers: List<String>,
    val correctAnswerIndex: Int
)

class QuizViewModel : ViewModel() {

    private val _timeLeft = MutableLiveData<String>()
    val timeLeft: LiveData<String> get() = _timeLeft

    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> get() = _score

    private val _currentQuestion = MutableLiveData<Question>()
    val currentQuestion: LiveData<Question> get() = _currentQuestion

    private val _isQuizFinished = MutableLiveData<Boolean>(false)
    val isQuizFinished: LiveData<Boolean> get() = _isQuizFinished

    private var timer: CountDownTimer? = null
    private var questionIndex = 0
    private var totalMillis: Long = 0
    private var currentMillisLeft: Long = 0
    private var realQuestions = mutableListOf<Question>()

    init {
        _currentQuestion.value = Question(
            "Connessione in corso...",
            listOf("Attendere prego...", "Attendere prego...", "Attendere prego...", "Attendere prego..."),
            0
        )
        fetchQuestionsFromApi()
    }

    private fun fetchQuestionsFromApi() {
        viewModelScope.launch {
            try {
                val gistUrl = "https://gist.githubusercontent.com/FedeSasHub/9c54bb581029d032826d290640aafb5e/raw/1118f2f18806e5b3b67a1c58a51feb5f92886d03/crisi_ita.json"
                val response = TriviaApi.retrofitService.getItalianQuestions(gistUrl)
                realQuestions = response.results.map { parseTriviaQuestion(it) }.shuffled().toMutableList()

                if (realQuestions.isNotEmpty()) {
                    _currentQuestion.value = realQuestions[0]
                }
            } catch (e: Exception) {
                val errorQuestion = Question(
                    "Errore di rete. Controlla la connessione e riavvia la crisi.",
                    listOf("Riprova", "Riprova", "Riprova", "Riprova"),
                    0
                )
                realQuestions = mutableListOf(errorQuestion)
                _currentQuestion.value = errorQuestion
            }
        }
    }

    private fun parseTriviaQuestion(trivia: TriviaQuestion): Question {
        val decodedQuestion = Html.fromHtml(trivia.question, Html.FROM_HTML_MODE_LEGACY).toString()
        val decodedCorrect = Html.fromHtml(trivia.correctAnswer, Html.FROM_HTML_MODE_LEGACY).toString()
        val decodedIncorrect = trivia.incorrectAnswers.map {
            Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString()
        }

        val allAnswers = decodedIncorrect.toMutableList()
        allAnswers.add(decodedCorrect)
        allAnswers.shuffle()

        val correctIndex = allAnswers.indexOf(decodedCorrect)
        return Question(decodedQuestion, allAnswers, correctIndex)
    }

    fun setInitialMinutes(minutes: Int) {
        if (timer == null) {
            startTimer(minutes)
        }
    }

    private fun startTimer(minutes: Int) {
        totalMillis = minutes * 60 * 1000L
        currentMillisLeft = totalMillis

        timer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentMillisLeft = millisUntilFinished
                val minLeft = (millisUntilFinished / 1000) / 60
                val secLeft = (millisUntilFinished / 1000) % 60
                _timeLeft.value = String.format("%02d:%02d", minLeft, secLeft)
            }
            override fun onFinish() {
                currentMillisLeft = 0
                _timeLeft.value = "00:00"
                _isQuizFinished.value = true
            }
        }.start()
    }

    fun evaluateAnswer(selectedIndex: Int): Boolean {
        if (realQuestions.isEmpty() || (realQuestions.size == 1 && realQuestions[0].text.contains("Errore"))) return false

        val currentQ = _currentQuestion.value ?: return false
        val isCorrect = (selectedIndex == currentQ.correctAnswerIndex)

        if (isCorrect) {
            _score.value = (_score.value ?: 0) + 10
        } else {
            _score.value = (_score.value ?: 0) - 5
        }

        return isCorrect
    }

    fun getCorrectAnswerIndex(): Int {
        return _currentQuestion.value?.correctAnswerIndex ?: 0
    }

    fun loadNextQuestion() {
        if (realQuestions.isEmpty()) return
        questionIndex = (questionIndex + 1) % realQuestions.size
        _currentQuestion.value = realQuestions[questionIndex]
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }

    fun forceEndQuiz() {
        timer?.cancel()
        _isQuizFinished.value = true
    }

    fun getElapsedMinutes(): Int {
        val elapsedMillis = totalMillis - currentMillisLeft
        return Math.ceil(elapsedMillis / 60000.0).toInt()
    }
}