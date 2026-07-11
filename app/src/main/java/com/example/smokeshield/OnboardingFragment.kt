package com.example.smokeshield

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smokeshield.databinding.FragmentOnboardingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnSalvaDati.setOnClickListener {
            val anni = binding.etAnni.text.toString().trim()
            val sigarette = binding.etSigarette.text.toString().trim()
            val motivo = binding.etMotivo.text.toString().trim()

            if (anni.isEmpty() || sigarette.isEmpty() || motivo.isEmpty()) {
                Toast.makeText(requireContext(), "Compila tutti i campi per favore!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSalvaDati.isEnabled = false

            val userData = hashMapOf(
                "anni_fumo" to anni.toInt(),
                "sigarette_giorno" to sigarette.toInt(),
                "motivazione" to motivo,
                "email" to (auth.currentUser?.email ?: "Sconosciuta")
            )

            val userId = auth.currentUser?.uid

            if (userId != null) {
                db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        findNavController().navigate(R.id.action_onboardingFragment_to_homeFragment)
                    }
                    .addOnFailureListener {
                        binding.btnSalvaDati.isEnabled = true
                        Toast.makeText(requireContext(), "Errore nel salvataggio", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Errore: Utente non trovato", Toast.LENGTH_SHORT).show()
                binding.btnSalvaDati.isEnabled = true
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}