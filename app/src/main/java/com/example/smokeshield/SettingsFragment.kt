package com.example.smokeshield

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smokeshield.databinding.FragmentSettingsBinding
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSchedule.setOnClickListener {
            val ore = binding.etHours.text.toString().toLongOrNull() ?: 0L
            val minuti = binding.etMinutes.text.toString().toLongOrNull() ?: 0L

            // Convertiamo tutto in minuti
            val totalMinutes = (ore * 60) + minuti

            // BLOCCO DI SICUREZZA ANDROID
            if (totalMinutes < 15) {
                Toast.makeText(requireContext(), "Android richiede un minimo di 15 minuti per i task ripetitivi!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Usiamo PERIODIC per ripetere all'infinito ogni X minuti
            val workRequest = PeriodicWorkRequestBuilder<MotivationWorker>(totalMinutes, TimeUnit.MINUTES)
                .build()

            // Cancelliamo eventuali routine vecchie (UPDATE/REPLACE) e facciamo partire quella nuova
            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "DailyMotivation",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Toast.makeText(requireContext(), "Routine attivata! Riceverai i tuoi dati ogni $totalMinutes minuti.", Toast.LENGTH_LONG).show()
        }
        // IL BOTTONE PER LA DEMO ALL'ESAME!
        binding.btnTestNow.setOnClickListener {
            val testRequest = androidx.work.OneTimeWorkRequestBuilder<MotivationWorker>()
                .setInitialDelay(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            androidx.work.WorkManager.getInstance(requireContext()).enqueueUniqueWork(
                "TestMotivation",
                androidx.work.ExistingWorkPolicy.REPLACE,
                testRequest
            )

            Toast.makeText(requireContext(), "Chiudi l'app! Notifica in arrivo tra 5 secondi...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}