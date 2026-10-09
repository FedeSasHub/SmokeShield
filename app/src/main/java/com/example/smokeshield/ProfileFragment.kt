package com.example.smokeshield

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smokeshield.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        caricaDatiUtente()

        binding.btnTrophies.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_trophiesFragment)
        }

        binding.btnHealthDashboard.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_healthFragment)
        }

        binding.btnImpostaAbitudini.setOnClickListener {
            mostraDialogAbitudini()
        }

        binding.btnVediStorico.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_historyFragment)
        }

        binding.fabSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Impostazioni in arrivo", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun caricaDatiUtente() {
        val utenteLoggato = auth.currentUser
        if (utenteLoggato != null) {
            binding.tvEmailDisplay.text = utenteLoggato.email ?: "Email non disponibile"
            db.collection("users").document(utenteLoggato.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        binding.tvMotivoDisplay.text = document.getString("motivazione") ?: "Nessuna motivazione inserita"
                        calcolaRisparmio(document)
                    }
                }
        }
    }

    private fun calcolaRisparmio(doc: DocumentSnapshot) {
        val dataSmettoStr = doc.getString("data_smetto")
        val quantitaGiorno = doc.getLong("abitudine_quantita") ?: 0L
        val quantitaPacchetto = doc.getLong("abitudine_sigarette_pacchetto") ?: 20L
        val prezzoPacchetto = doc.getDouble("abitudine_prezzo") ?: 0.0

        if (dataSmettoStr.isNullOrEmpty() || quantitaGiorno == 0L) {
            binding.tvRisparmioSoldi.text = "--- €"
            binding.tvGiorniSenza.text = "-"
            binding.tvSigaretteEvitate.text = "-"
            return
        }

        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dataSmetto = sdf.parse(dataSmettoStr) ?: Date()
            val diffMillis = Date().time - dataSmetto.time
            var giorniPassati = TimeUnit.MILLISECONDS.toDays(diffMillis)
            if (giorniPassati < 0) giorniPassati = 0

            val sigaretteEvitate = giorniPassati * quantitaGiorno
            val pacchettiEvitati = sigaretteEvitate.toDouble() / quantitaPacchetto.toDouble()
            val soldiRisparmiati = pacchettiEvitati * prezzoPacchetto

            binding.tvGiorniSenza.text = giorniPassati.toString()
            binding.tvSigaretteEvitate.text = sigaretteEvitate.toString()
            binding.tvRisparmioSoldi.text = String.format(Locale.getDefault(), "%.2f €", soldiRisparmiati)

        } catch (e: Exception) {
            binding.tvRisparmioSoldi.text = "Err"
        }
    }

    private fun mostraDialogAbitudini() {
        val uid = auth.currentUser?.uid ?: return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_abitudini, null)

        val etTipo = dialogView.findViewById<AutoCompleteTextView>(R.id.et_tipo_fumo)
        val etQuantita = dialogView.findViewById<TextInputEditText>(R.id.et_quantita)
        val tilQuantitaPacchetto = dialogView.findViewById<TextInputLayout>(R.id.til_quantita_pacchetto)
        val etQuantitaPacchetto = dialogView.findViewById<TextInputEditText>(R.id.et_quantita_pacchetto)
        val tilPrezzo = dialogView.findViewById<TextInputLayout>(R.id.til_prezzo)
        val etPrezzo = dialogView.findViewById<TextInputEditText>(R.id.et_prezzo)
        val etDataSmetto = dialogView.findViewById<TextInputEditText>(R.id.et_data_smetto)

        val tipi = arrayOf("Sigarette", "Tabacco Trinciato", "IQOS / Terea")
        etTipo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipi))

        fun aggiornaTestiInBaseAlTipo(tipo: String) {
            if (tipo == "Tabacco Trinciato") {
                tilQuantitaPacchetto.hint = "Stima sigarette ricavate da una busta"
                tilPrezzo.hint = "Prezzo totale (Tabacco+Filtri+Cartine) €"
            } else {
                tilQuantitaPacchetto.hint = "Sigarette in un pacchetto"
                tilPrezzo.hint = "Prezzo di un pacchetto (€)"
            }
        }

        etTipo.setOnItemClickListener { parent, _, position, _ ->
            aggiornaTestiInBaseAlTipo(parent.getItemAtPosition(position).toString())
        }

        val calendar = Calendar.getInstance()
        etDataSmetto.setOnClickListener {
            DatePickerDialog(requireContext(), { _, anno, mese, giorno ->
                etDataSmetto.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", giorno, mese + 1, anno))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val tipoSalvato = doc.getString("abitudine_tipo") ?: "Sigarette"
                etTipo.setText(tipoSalvato, false)
                aggiornaTestiInBaseAlTipo(tipoSalvato)
                etQuantita.setText(doc.getLong("abitudine_quantita")?.toString() ?: "")
                etQuantitaPacchetto.setText(doc.getLong("abitudine_sigarette_pacchetto")?.toString() ?: "20")
                etPrezzo.setText(doc.getDouble("abitudine_prezzo")?.toString() ?: "")
                etDataSmetto.setText(doc.getString("data_smetto") ?: "")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val tipo = etTipo.text.toString()
                val quantita = etQuantita.text.toString().toIntOrNull() ?: 0
                val quantitaPacchetto = etQuantitaPacchetto.text.toString().toIntOrNull() ?: 20
                val prezzo = etPrezzo.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

                db.collection("users").document(uid).set(mapOf(
                    "abitudine_tipo" to tipo,
                    "abitudine_quantita" to quantita,
                    "abitudine_sigarette_pacchetto" to quantitaPacchetto,
                    "abitudine_prezzo" to prezzo,
                    "data_smetto" to etDataSmetto.text.toString()
                ), SetOptions.merge()).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Abitudini salvate!", Toast.LENGTH_SHORT).show()

                    requireContext().getSharedPreferences("SmokeShieldPrefs", Context.MODE_PRIVATE)
                        .edit().putInt("ultimo_trofeo_notificato", 0).apply()

                    caricaDatiUtente()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}