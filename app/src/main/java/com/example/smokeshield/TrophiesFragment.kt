package com.example.smokeshield

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smokeshield.databinding.FragmentTrophiesBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TrofeoItem(val giorniRichiesti: Int, val icona: String, val titolo: String, val descrizione: String)

class TrophiesFragment : Fragment() {

    private var _binding: FragmentTrophiesBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val listaTrofei = listOf(
        TrofeoItem(1, "🩸", "Sangue Pulito", "Il monossido di carbonio nel tuo sangue è sceso a livelli normali."),
        TrofeoItem(3, "🫁", "Respiro Libero", "I tubi bronchiali iniziano a rilassarsi, rendendo più facile respirare."),
        TrofeoItem(7, "👅", "Sensi Risvegliati", "Le terminazioni nervose si rigenerano: olfatto e gusto migliorano."),
        TrofeoItem(14, "🫀", "Cuore Forte", "La circolazione sanguigna e la funzionalità polmonare sono in netto miglioramento."),
        TrofeoItem(30, "🏃", "Rinascita", "Tosse e fiato corto diminuiscono del 30%. Inizia la vera disintossicazione."),
        TrofeoItem(90, "🛡️", "Scudo di Ferro", "Il rischio di infarto ha iniziato a crollare drasticamente."),
        TrofeoItem(180, "🌬️", "Polmoni Nuovi", "Le ciglia polmonari si sono rigenerate. Infezioni e tosse sono un lontano ricordo."),
        TrofeoItem(365, "👑", "Traguardo d'Oro", "Il rischio di malattie cardiache è sceso alla metà rispetto a un fumatore.")
    )

    private var giorniSenzaFumo = 0L
    private lateinit var adapter: TrophiesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrophiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackToProfile.setOnClickListener {
            findNavController().popBackStack()
        }

        adapter = TrophiesAdapter(listaTrofei)
        binding.rvTrophiesList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTrophiesList.adapter = adapter

        calcolaGiorniUtente()
    }

    private fun calcolaGiorniUtente() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val dataSmettoStr = doc.getString("data_smetto")
                    if (!dataSmettoStr.isNullOrEmpty()) {
                        try {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val dataSmetto = sdf.parse(dataSmettoStr) ?: Date()
                            val diffMillis = Date().time - dataSmetto.time
                            giorniSenzaFumo = TimeUnit.MILLISECONDS.toDays(diffMillis)
                            if (giorniSenzaFumo < 0) giorniSenzaFumo = 0

                            adapter.notifyDataSetChanged()
                        } catch (e: Exception) {
                        }
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class TrophiesAdapter(private val dataset: List<TrofeoItem>) :
        RecyclerView.Adapter<TrophiesAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.card_trofeo)
            val tvIcona: TextView = view.findViewById(R.id.tv_trofeo_icona)
            val tvTitolo: TextView = view.findViewById(R.id.tv_trofeo_titolo)
            val tvDesc: TextView = view.findViewById(R.id.tv_trofeo_descrizione)
            val chipStatus: Chip = view.findViewById(R.id.chip_status)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trofeo, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = dataset[position]
            holder.tvIcona.text = item.icona
            holder.tvTitolo.text = item.titolo
            holder.tvDesc.text = item.descrizione

            if (giorniSenzaFumo >= item.giorniRichiesti) {
                holder.card.setCardBackgroundColor(Color.WHITE)
                holder.card.alpha = 1.0f
                holder.chipStatus.text = "Sbloccato!"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_dark)
                holder.chipStatus.setTextColor(Color.WHITE)
            } else {
                holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                holder.card.alpha = 0.5f
                holder.chipStatus.text = "Sblocca a ${item.giorniRichiesti} gg"
                holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
                holder.chipStatus.setTextColor(Color.WHITE)
            }
        }

        override fun getItemCount() = dataset.size
    }
}