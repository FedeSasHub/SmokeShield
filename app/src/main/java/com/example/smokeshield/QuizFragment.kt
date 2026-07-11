package com.example.smokeshield

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.smokeshield.databinding.FragmentQuizBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: QuizViewModel

    private var minutiScelti: Int = 5
    private var livelloScelto: String = "Sconosciuto"

    private val colorCorrect = Color.parseColor("#4CAF50")
    private val colorWrong = Color.parseColor("#F44336")
    private val colorDefault = Color.parseColor("#3F51B5")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]

        resetButtonsColors()

        val minutesReceived = arguments?.getInt("minutes") ?: 5
        minutiScelti = minutesReceived

        livelloScelto = when {
            minutiScelti <= 1 -> "Facile"
            minutiScelti <= 5 -> "Medio"
            else -> "Difficile"
        }

        viewModel.setInitialMinutes(minutiScelti)

        viewModel.timeLeft.observe(viewLifecycleOwner) { timeString ->
            binding.tvTimer.text = timeString
        }

        viewModel.score.observe(viewLifecycleOwner) { score ->
            binding.tvScore.text = getString(R.string.punteggio_format, score)
        }

        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            binding.tvQuestion.text = question.text
            binding.btnAnswer1.text = question.answers[0]
            binding.btnAnswer2.text = question.answers[1]
            binding.btnAnswer3.text = question.answers[2]
            binding.btnAnswer4.text = question.answers[3]
        }

        viewModel.isQuizFinished.observe(viewLifecycleOwner) { isFinished ->
            if (isFinished) {
                salvaPunteggioEFinisci()
            }
        }

        binding.btnAnswer1.setOnClickListener { handleAnswerClick(binding.btnAnswer1, 0) }
        binding.btnAnswer2.setOnClickListener { handleAnswerClick(binding.btnAnswer2, 1) }
        binding.btnAnswer3.setOnClickListener { handleAnswerClick(binding.btnAnswer3, 2) }
        binding.btnAnswer4.setOnClickListener { handleAnswerClick(binding.btnAnswer4, 3) }

        binding.btnTerminaOra.setOnClickListener {
            viewModel.forceEndQuiz()
        }

        return binding.root
    }

    private fun handleAnswerClick(clickedButton: Button, selectedIndex: Int) {
        setButtonsEnabled(false)

        val isCorrect = viewModel.evaluateAnswer(selectedIndex)

        if (isCorrect) {
            clickedButton.backgroundTintList = ColorStateList.valueOf(colorCorrect)
        } else {
            clickedButton.backgroundTintList = ColorStateList.valueOf(colorWrong)
            val correctIndex = viewModel.getCorrectAnswerIndex()
            val correctButton = getButtonByIndex(correctIndex)
            correctButton.backgroundTintList = ColorStateList.valueOf(colorCorrect)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)

            if (viewModel.isQuizFinished.value == false) {
                resetButtonsColors()
                setButtonsEnabled(true)
                viewModel.loadNextQuestion()
            }
        }
    }

    private fun salvaPunteggioEFinisci() {
        setButtonsEnabled(false)
        binding.tvQuestion.text = "Tempo scaduto! Salvataggio in corso..."

        val finalScore = viewModel.score.value ?: 0
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val currentDate = sdf.format(Date())
            val minutiTrascorsi = viewModel.getElapsedMinutes()

            val historyData = hashMapOf(
                "punteggio" to finalScore,
                "data" to currentDate,
                "durata_minuti" to minutiTrascorsi,
                "livello" to livelloScelto
            )

            db.collection("users").document(userId).collection("history").add(historyData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Crisi superata! Punteggio: $finalScore", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_quizFragment_to_profileFragment)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Errore di salvataggio.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_quizFragment_to_homeFragment)
                }
        }
    }

    private fun setButtonsEnabled(isEnabled: Boolean) {
        binding.btnAnswer1.isEnabled = isEnabled
        binding.btnAnswer2.isEnabled = isEnabled
        binding.btnAnswer3.isEnabled = isEnabled
        binding.btnAnswer4.isEnabled = isEnabled
    }

    private fun resetButtonsColors() {
        val defaultTint = ColorStateList.valueOf(colorDefault)
        binding.btnAnswer1.backgroundTintList = defaultTint
        binding.btnAnswer2.backgroundTintList = defaultTint
        binding.btnAnswer3.backgroundTintList = defaultTint
        binding.btnAnswer4.backgroundTintList = defaultTint

        binding.btnAnswer1.setTextColor(Color.WHITE)
        binding.btnAnswer2.setTextColor(Color.WHITE)
        binding.btnAnswer3.setTextColor(Color.WHITE)
        binding.btnAnswer4.setTextColor(Color.WHITE)
    }

    private fun getButtonByIndex(index: Int): Button {
        return when (index) {
            0 -> binding.btnAnswer1
            1 -> binding.btnAnswer2
            2 -> binding.btnAnswer3
            3 -> binding.btnAnswer4
            else -> binding.btnAnswer1
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}