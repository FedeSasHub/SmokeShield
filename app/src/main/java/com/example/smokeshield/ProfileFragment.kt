package com.example.smokeshield

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        binding.fabSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }

        return binding.root
    }

    private fun scaricaCronologiaECalcolaStatistiche(userId: String) {
        db.collection("users").document(userId).collection("history")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val listaCrisi = mutableListOf<HistoryItem>()

                var minutiTotali = 0

                // Variabili coi nuovi nomi
                var recordFacile = 0
                var recordMedio = 0
                var recordDifficile = 0

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                for (documento in querySnapshot.documents) {
                    val dataString = documento.getString("data") ?: "Data sconosciuta"
                    val punteggio = documento.getLong("punteggio")?.toInt() ?: 0
                    val durata = documento.getLong("durata_minuti")?.toInt() ?: 0

                    val livelloGrezzo = documento.getString("livello") ?: when {
                        durata <= 1 -> "Facile"
                        durata <= 5 -> "Medio"
                        else -> "Difficile"
                    }

                    // TRADUTTORE DEI VECCHI SALVATAGGI
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

                    val dateObj = try {
                        sdf.parse(dataString) ?: Date(0)
                    } catch (e: Exception) {
                        Date(0)
                    }

                    listaCrisi.add(HistoryItem(dataString, punteggio, dateObj, livelloNormalizzato))
                }

                listaCrisi.sortByDescending { it.dateObj }

                val numeroCrisi = listaCrisi.size
                binding.tvStatsCrises.text = "Crisi superate: $numeroCrisi"
                binding.tvStatsTime.text = "Tempo totale: $minutiTotali min"

                // Mostriamo i nuovi nomi!
                binding.tvStatsRecord.text = "Record - Facile: $recordFacile | Medio: $recordMedio | Difficile: $recordDifficile"

                val adapter = HistoryAdapter(listaCrisi)
                binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
                binding.rvHistory.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nel caricamento storico", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}