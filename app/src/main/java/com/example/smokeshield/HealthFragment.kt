package com.example.smokeshield

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.smokeshield.databinding.FragmentHealthBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Import per Health Connect
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter

// Import di Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Import di Vico
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.axis.vertical.VerticalAxis
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer

class HealthFragment : Fragment() {

    private var _binding: FragmentHealthBinding? = null
    private val binding get() = _binding!!

    // Inizializza Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(permissions)) {
            leggiDatiSalute()
        } else {
            mostraDialogImpostazioniSalute()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackProfile.setOnClickListener {
            findNavController().popBackStack()
        }

        impostaFormattazioneAssiY()

        binding.toggleGroupTime.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_7_days -> caricaDatiReali(7)
                    R.id.btn_30_days -> caricaDatiReali(30)
                }
            }
        }

        caricaDatiReali(7)
        controllaEAvviaSalute()
    }

    private fun impostaFormattazioneAssiY() {
        val asseCrisi = binding.vicoChartCrises.startAxis as? VerticalAxis<AxisPosition.Vertical.Start>
        if (asseCrisi != null) {
            asseCrisi.valueFormatter = AxisValueFormatter { value, _ -> value.toInt().toString() }
            asseCrisi.itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 6)
        }

        val assePassi = binding.vicoChartSteps.startAxis as? VerticalAxis<AxisPosition.Vertical.Start>
        if (assePassi != null) {
            assePassi.valueFormatter = AxisValueFormatter { value, _ -> value.toInt().toString() }
            assePassi.itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 5)
        }

        // NUOVO: Formattazione per i Battiti (BPM interi)
        val asseCuore = binding.vicoChartHeartRate.startAxis as? VerticalAxis<AxisPosition.Vertical.Start>
        if (asseCuore != null) {
            asseCuore.valueFormatter = AxisValueFormatter { value, _ -> value.toInt().toString() }
            asseCuore.itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 5)
        }
    }

    private fun controllaEAvviaSalute() {
        lifecycleScope.launch {
            try {
                val sdkStatus = HealthConnectClient.getSdkStatus(requireContext())
                if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                    val client = HealthConnectClient.getOrCreate(requireContext())
                    val granted = client.permissionController.getGrantedPermissions()
                    if (granted.containsAll(permissions)) {
                        leggiDatiSalute()
                    } else {
                        requestPermissions.launch(permissions)
                    }
                } else {
                    Toast.makeText(requireContext(), "Health Connect non attivo.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun leggiDatiSalute() {
        lifecycleScope.launch {
            try {
                val healthConnectClient = HealthConnectClient.getOrCreate(requireContext())
                val endTime = Instant.now()
                val startTime = endTime.minus(7, ChronoUnit.DAYS)
                val timeRange = TimeRangeFilter.between(startTime, endTime)

                val stepsRequest = AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeRange
                )
                val stepsResponse = healthConnectClient.aggregate(stepsRequest)
                val totalSteps = stepsResponse[StepsRecord.COUNT_TOTAL] ?: 0L

                val heartRateRequest = ReadRecordsRequest(HeartRateRecord::class, timeRange)
                val heartRateResponse = healthConnectClient.readRecords(heartRateRequest)
                val lastHeartRate = heartRateResponse.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute ?: 0L

                binding.tvStepsCount.text = totalSteps.toString()
                binding.tvBpmCount.text = if (lastHeartRate > 0) "$lastHeartRate" else "N/D"

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore lettura dati: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun caricaDatiReali(giorni: Int) {
        val uid = auth.currentUser?.uid
        if (uid == null) return

        lifecycleScope.launch {
            try {
                val oggi = LocalDate.now()

                val contatorePassiPerGiorno = IntArray(giorni) { 0 }
                val bpmPerGiorno = FloatArray(giorni) { 0f } // Nuovo array per i battiti
                val sdkStatus = HealthConnectClient.getSdkStatus(requireContext())

                if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                    val client = HealthConnectClient.getOrCreate(requireContext())
                    val granted = client.permissionController.getGrantedPermissions()

                    val endTimeLocal = LocalDateTime.now()
                    val startTimeLocal = endTimeLocal.minusDays(giorni.toLong())
                    val filter = TimeRangeFilter.between(startTimeLocal, endTimeLocal)

                    // 1. CARICAMENTO PASSI AGGREGATI
                    if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
                        val requestPassi = AggregateGroupByPeriodRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = filter,
                            timeRangeSlicer = java.time.Period.ofDays(1)
                        )
                        val responsePassi = client.aggregateGroupByPeriod(requestPassi)

                        for (bucket in responsePassi) {
                            val totalSteps = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                            val dataBucket = bucket.startTime.toLocalDate()
                            val giorniTrascorsi = ChronoUnit.DAYS.between(dataBucket, oggi).toInt()

                            if (giorniTrascorsi in 0 until giorni) {
                                val indiceArray = (giorni - 1) - giorniTrascorsi
                                contatorePassiPerGiorno[indiceArray] = totalSteps.toInt()
                            }
                        }
                    }

                    // 2. CARICAMENTO BATTITO CARDIACO AGGREGATO (NUOVO)
                    if (granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) {
                        val requestCuore = AggregateGroupByPeriodRequest(
                            metrics = setOf(HeartRateRecord.BPM_AVG),
                            timeRangeFilter = filter,
                            timeRangeSlicer = java.time.Period.ofDays(1)
                        )
                        val responseCuore = client.aggregateGroupByPeriod(requestCuore)

                        for (bucket in responseCuore) {
                            // Conversione ultra-sicura (gestisce tutti i formati numerici di Google)
                            val mediaBpm = bucket.result[HeartRateRecord.BPM_AVG]?.toString()?.toFloatOrNull() ?: 0f
                            val dataBucket = bucket.startTime.toLocalDate()
                            val giorniTrascorsi = ChronoUnit.DAYS.between(dataBucket, oggi).toInt()

                            if (giorniTrascorsi in 0 until giorni) {
                                val indiceArray = (giorni - 1) - giorniTrascorsi
                                bpmPerGiorno[indiceArray] = mediaBpm
                            }
                        }
                    }
                }

                // Generiamo il grafico dei Passi
                val datiPassi = Array(giorni) { indice ->
                    val giornoX = (indice + 1).toFloat()
                    val passiY = contatorePassiPerGiorno[indice].toFloat()
                    giornoX to passiY
                }
                binding.vicoChartSteps.setModel(entryModelOf(*datiPassi))

                // Generiamo il grafico del Battito Cardiaco (NUOVO)
                val datiCuore = Array(giorni) { indice ->
                    val giornoX = (indice + 1).toFloat()
                    val cuoreY = bpmPerGiorno[indice]
                    giornoX to cuoreY
                }
                binding.vicoChartHeartRate.setModel(entryModelOf(*datiCuore))

                // 3. CARICAMENTO CRISI DA FIREBASE
                db.collection("users").document(uid).collection("history")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val contatoreCrisiPerGiorno = IntArray(giorni) { 0 }
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

                        for (document in querySnapshot.documents) {
                            val dataStringa = document.getString("data")
                            if (dataStringa != null) {
                                try {
                                    val dataCrisi = LocalDateTime.parse(dataStringa, formatter).toLocalDate()
                                    val giorniTrascorsi = ChronoUnit.DAYS.between(dataCrisi, oggi).toInt()

                                    if (giorniTrascorsi in 0 until giorni) {
                                        val indiceArray = (giorni - 1) - giorniTrascorsi
                                        contatoreCrisiPerGiorno[indiceArray]++
                                    }
                                } catch (e: Exception) {
                                    // Continua in caso di errore di parse
                                }
                            }
                        }

                        val datiCrisi = Array(giorni) { indice ->
                            val giornoX = (indice + 1).toFloat()
                            val crisiY = contatoreCrisiPerGiorno[indice].toFloat()
                            giornoX to crisiY
                        }
                        binding.vicoChartCrises.setModel(entryModelOf(*datiCrisi))

                    }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore generazione grafici: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostraDialogImpostazioniSalute() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permessi Richiesti")
            .setMessage("Per vedere la dashboard, autorizza SmokeShield in Health Connect.")
            .setPositiveButton("Apri Impostazioni") { _, _ ->
                try {
                    val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    val intentGenerico = Intent(android.provider.Settings.ACTION_SETTINGS)
                    startActivity(intentGenerico)
                }
            }
            .setNegativeButton("Torna Indietro") { _, _ -> findNavController().popBackStack() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}