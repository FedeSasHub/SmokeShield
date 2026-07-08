package com.example.smokeshield

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smokeshield.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    // Gli oggetti necessari per far apparire la schermata di Google
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()

        // 1. Se l'utente è già loggato, scavalca il login e vai alla Home
        if (auth.currentUser != null) {
            vaiAllaHome()
        }

        // 2. CONFIGURAZIONE GOOGLE SIGN-IN
        // Chiediamo a Google l'ID Token necessario per Firebase e l'email
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("50037665090-bleu65bcfs8uej80sj5cuevcrricn0sb.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // 3. IL MANAGER DEL RISULTATO DI GOOGLE
        // Questa funzione rimane in ascolto finché l'utente non sceglie il suo account Google
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google ha dato il via libera, prendiamo l'account
                val account = task.getResult(ApiException::class.java)!!
                // Passiamo il passaporto di Google a Firebase
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                binding.btnGoogleSignIn.isEnabled = true
                Toast.makeText(requireContext(), "Accesso Google annullato o fallito: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. LOGIN CLASSICO (Email e Password)
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Inserisci email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Bentornato!", Toast.LENGTH_SHORT).show()
                        vaiAllaHome()
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { regTask ->
                                binding.btnLogin.isEnabled = true
                                if (regTask.isSuccessful) {
                                    Toast.makeText(requireContext(), "Account creato con successo!", Toast.LENGTH_SHORT).show()
                                    vaiAllaHome()
                                } else {
                                    Toast.makeText(requireContext(), "Errore: ${regTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                }
        }

        // 5. CLICK SUL BOTTONE DI GOOGLE
        binding.btnGoogleSignIn.setOnClickListener {
            binding.btnGoogleSignIn.isEnabled = false
            // Disconnettiamo preventivamente il vecchio account memorizzato per far riapparire il selettore
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                // Lanciamo la schermata di Google
                googleSignInLauncher.launch(signInIntent)
            }
        }

        return binding.root
    }

    // 6. SCAMBIO TOKEN: GOOGLE -> FIREBASE
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.btnGoogleSignIn.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Accesso Google riuscito!", Toast.LENGTH_SHORT).show()
                    vaiAllaHome()
                } else {
                    Toast.makeText(requireContext(), "Errore Firebase Google: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun vaiAllaHome() {
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}