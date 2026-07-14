package com.example.smokeshield

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smokeshield.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
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

        val prefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val currentMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        when(currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.rbLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.rbDark.isChecked = true
            else -> binding.rbSystem.isChecked = true
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when(checkedId) {
                R.id.rb_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rb_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            prefs.edit().putInt("theme_mode", mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.btnSchedule.setOnClickListener {
            val ore = binding.etHours.text.toString().toLongOrNull() ?: 0L
            val minuti = binding.etMinutes.text.toString().toLongOrNull() ?: 0L

            val totalMinutes = (ore * 60) + minuti

            if (totalMinutes < 15) {
                Toast.makeText(requireContext(), "Android richiede un minimo di 15 minuti per i task ripetitivi!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val workRequest = PeriodicWorkRequestBuilder<MotivationWorker>(totalMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "DailyMotivation",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Toast.makeText(requireContext(), "Routine attivata! Riceverai i tuoi dati ogni $totalMinutes minuti.", Toast.LENGTH_LONG).show()
        }

        binding.btnTestNow.setOnClickListener {
            val testRequest = OneTimeWorkRequestBuilder<MotivationWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(requireContext()).enqueueUniqueWork(
                "TestMotivation",
                ExistingWorkPolicy.REPLACE,
                testRequest
            )

            Toast.makeText(requireContext(), "Notifica in arrivo tra 5 secondi...", Toast.LENGTH_SHORT).show()
        }

        binding.btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Elimina Account")
                .setMessage("Sei sicuro di voler eliminare definitivamente il tuo account e tutti i progressi? Questa azione è irreversibile.")
                .setPositiveButton("Elimina") { _, _ ->
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.delete()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Account eliminato con successo", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.loginFragment)
                        } else {
                            Toast.makeText(requireContext(), "Errore durante l'eliminazione. Riprova effettuando prima il logout e login.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}