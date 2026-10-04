package com.example.smokeshield

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smokeshield.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Nuovi import per la lettura dei dati e le Coroutines
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter

// Health Connect Imports
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(permissions)) {
            // PERMESSI OK! Lanciamo subito la lettura dei dati!
            leggiDatiSalute()
        } else if (granted.isNotEmpty()) {
            Toast.makeText(requireContext(), "Permessi parziali concessi.", Toast.LENGTH_SHORT).show()
        } else {
            mostraDialogImpostazioniSalute()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.tvEmailDisplay.text = auth.currentUser?.email ?: "Email sconosciuta"

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val motivo = document.getString("motivazione")
                        binding.tvMotivoDisplay.text = motivo ?: "Nessuna motivazione inserita"
                    } else {
                        binding.tvMotivoDisplay.text = "Dati non trovati nel Cloud"
                    }
                }

            scaricaCronologiaECalcolaStatistiche(userId)
        }

        binding.btnTestHealth.setOnClickListener {
            chiediPermessiSalute()
        }

        binding.fabSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }

        return binding.root
    }

    // FASE 2: FUNZIONE PER LEGGERE I DATI REALI
    private fun leggiDatiSalute() {
        // Avviamo un processo in background (Coroutine) per non bloccare l'app
        lifecycleScope.launch {
            try {
                val healthConnectClient = HealthConnectClient.getOrCreate(requireContext())

                // Impostiamo l'arco temporale: Gli ultimi 7 giorni
                val endTime = Instant.now()
                val startTime = endTime.minus(7, ChronoUnit.DAYS)
                val timeRange = TimeRangeFilter.between(startTime, endTime)

                // 1. Richiediamo e sommiamo i PASSI
                val stepsRequest = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeRange
                )
                val stepsResponse = healthConnectClient.readRecords(stepsRequest)
                val totalSteps = stepsResponse.records.sumOf { it.count }

                // 2. Richiediamo il BATTITO CARDIACO (prendiamo l'ultimo registrato)
                val heartRateRequest = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = timeRange
                )
                val heartRateResponse = healthConnectClient.readRecords(heartRateRequest)
                val lastHeartRate = heartRateResponse.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute ?: 0L

                // Mostriamo il risultato magico!
                Toast.makeText(requireContext(), "Dati 7gg estratti!\nPassi totali: $totalSteps\nUltimo battito: $lastHeartRate bpm", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore nella lettura: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun chiediPermessiSalute() {
        try {
            val sdkStatus = HealthConnectClient.getSdkStatus(requireContext())
            if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Dati Sanitari")
                    .setMessage("Per mostrarti i tuoi progressi, ti chiederemo l'accesso a Passi e Battito Cardiaco.")
                    .setPositiveButton("Procedi") { _, _ ->
                        requestPermissions.launch(permissions)
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Health Connect non attivo sul dispositivo.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostraDialogImpostazioniSalute() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permessi Bloccati dal Sistema")
            .setMessage("Android ha bloccato la richiesta rapida.\n\nTi porteremo nelle Impostazioni: cerca 'Health Connect', vai in 'Autorizzazioni app', trova SmokeShield e accendi gli interruttori manualmente.")
            .setPositiveButton("Apri Impostazioni") { _, _ ->
                try {
                    val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intentGenerico = Intent(android.provider.Settings.ACTION_SETTINGS)
                        startActivity(intentGenerico)
                        Toast.makeText(requireContext(), "Cerca 'Health Connect' con la lente in alto!", Toast.LENGTH_LONG).show()
                    } catch (e2: Exception) {
                        Toast.makeText(requireContext(), "Impossibile aprire le impostazioni", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun scaricaCronologiaECalcolaStatistiche(userId: String) {
        db.collection("users").document(userId).collection("history")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val listaCrisi = mutableListOf<HistoryItem>()
                var minutiTotali = 0
                var recordFacile = 0
                var recordMedio = 0
                var recordDifficile = 0

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                for (documento in querySnapshot.documents) {
                    val dataString = documento.getString("data") ?: "Data sconosciuta"
                    val punteggio = documento.getLong("punteggio")?.toInt() ?: 0
                    val durata = documento.getLong("durata_minuti")?.toInt() ?: 0
                    val livelloGrezzo = documento.getString("livello") ?: "Facile"
                    val livelloNormalizzato = when (livelloGrezzo) {
                        "Test" -> "Facile"
                        "Facile" -> if (durata > 1) "Medio" else "Facile"
                        else -> livelloGrezzo
                    }

                    minutiTotali += durata
                    when (livelloNormalizzato) {
                        "Facile" -> if (punteggio > recordFacile) recordFacile = punteggio
                        "Medio" -> if (punteggio > recordMedio) recordMedio = punteggio
                        "Difficile" -> if (punteggio > recordDifficile) recordDifficile = punteggio
                    }
                    val dateObj = try { sdf.parse(dataString) ?: Date(0) } catch (e: Exception) { Date(0) }
                    listaCrisi.add(HistoryItem(dataString, punteggio, dateObj, livelloNormalizzato))
                }
                listaCrisi.sortByDescending { it.dateObj }
                binding.tvStatsCrises.text = "Crisi superate: ${listaCrisi.size}"
                binding.tvStatsTime.text = "Tempo totale: $minutiTotali min"
                binding.tvStatsRecord.text = "Record - Facile: $recordFacile | Medio: $recordMedio | Difficile: $recordDifficile"

                val adapter = HistoryAdapter(listaCrisi)
                binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
                binding.rvHistory.adapter = adapter
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}