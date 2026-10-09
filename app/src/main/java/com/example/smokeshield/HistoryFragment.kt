package com.example.smokeshield

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smokeshield.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrisiStorico(val data: String, val livello: String, val durata: Int, val punteggio: Int, val dateObj: Date)

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val listaCrisi = mutableListOf<CrisiStorico>()
    private lateinit var adapter: StoricoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackProfile.setOnClickListener {
            findNavController().popBackStack()
        }

        adapter = StoricoAdapter(listaCrisi)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        caricaStoricoECalcolaStatistiche()
    }

    private fun caricaStoricoECalcolaStatistiche() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("history")
            .get()
            .addOnSuccessListener { snapshot ->
                listaCrisi.clear()

                var minutiTotali = 0
                var recordFacile = 0
                var recordMedio = 0
                var recordDifficile = 0

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                for (doc in snapshot.documents) {
                    val dataString = doc.getString("data") ?: "Data ignota"
                    val punteggio = doc.getLong("punteggio")?.toInt() ?: 0
                    val durata = doc.getLong("durata_minuti")?.toInt() ?: 0

                    val livelloGrezzo = doc.getString("livello") ?: when {
                        durata <= 1 -> "Facile"
                        durata <= 5 -> "Medio"
                        else -> "Difficile"
                    }
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

                    listaCrisi.add(CrisiStorico(dataString, livelloNormalizzato, durata, punteggio, dateObj))
                }

                listaCrisi.sortByDescending { it.dateObj }

                val numeroCrisi = listaCrisi.size
                binding.tvStatsCrises.text = "Crisi superate: $numeroCrisi"
                binding.tvStatsTime.text = "Tempo totale: $minutiTotali min"
                binding.tvStatsRecord.text = "Record - Facile: $recordFacile | Medio: $recordMedio | Difficile: $recordDifficile"

                adapter.notifyDataSetChanged()

                if (listaCrisi.isEmpty()) {
                    Toast.makeText(requireContext(), "Non ci sono ancora crisi registrate", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nel caricamento storico", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class StoricoAdapter(private val dataset: List<CrisiStorico>) :
        RecyclerView.Adapter<StoricoAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvData: TextView = view.findViewById(R.id.tv_item_data)
            val tvDettagli: TextView = view.findViewById(R.id.tv_item_dettagli)
            val tvPunteggio: TextView = view.findViewById(R.id.tv_item_punteggio)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_crisi_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = dataset[position]
            holder.tvData.text = item.data
            holder.tvDettagli.text = "Livello: ${item.livello} • Durata: ${item.durata} min"

            if (item.punteggio > 0) {
                holder.tvPunteggio.text = "+${item.punteggio} pt"
            } else {
                holder.tvPunteggio.text = "${item.punteggio} pt"
            }
        }

        override fun getItemCount() = dataset.size
    }
}