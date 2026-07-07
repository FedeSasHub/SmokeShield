package com.example.smokeshield

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.smokeshield.databinding.FragmentQuizBinding

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    // Dichiariamo il nostro nuovo cervello
    private lateinit var viewModel: QuizViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]

        val minutesReceived = arguments?.getInt("minutes") ?: 5
        viewModel.setInitialMinutes(minutesReceived)

        // Osserva il Timer
        viewModel.timeLeft.observe(viewLifecycleOwner) { timeString ->
            binding.tvTimer.text = timeString
        }

        // Osserva il Punteggio
        viewModel.score.observe(viewLifecycleOwner) { score ->
            binding.tvScore.text = getString(R.string.punteggio_format, score)
        }

        // Osserva la Domanda e aggiorna i testi a schermo
        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            binding.tvQuestion.text = question.text
            binding.btnAnswer1.text = question.answers[0]
            binding.btnAnswer2.text = question.answers[1]
            binding.btnAnswer3.text = question.answers[2]
            binding.btnAnswer4.text = question.answers[3]
        }

        // Gestione dei Click sui 4 bottoni. Passiamo al ViewModel l'indice della risposta (0, 1, 2, 3)
        binding.btnAnswer1.setOnClickListener { viewModel.checkAnswer(0) }
        binding.btnAnswer2.setOnClickListener { viewModel.checkAnswer(1) }
        binding.btnAnswer3.setOnClickListener { viewModel.checkAnswer(2) }
        binding.btnAnswer4.setOnClickListener { viewModel.checkAnswer(3) }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}