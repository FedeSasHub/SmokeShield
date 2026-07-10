package com.example.smokeshield

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smokeshield.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Variabili per Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Mostriamo l'email dell'utente
        binding.tvEmailDisplay.text = auth.currentUser?.email ?: "Email sconosciuta"

        // 2. Recupero Dati da Firestore (Motivazione)
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
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Errore nel caricamento dati", Toast.LENGTH_SHORT).show()
                }
        }

        // 3. IL TUO CODICE ORIGINALE: Vai alle Impostazioni
        binding.fabSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        // 4. NUOVO CODICE: Logout
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}