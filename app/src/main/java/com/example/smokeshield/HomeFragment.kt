package com.example.smokeshield

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.smokeshield.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.btnSos.setOnClickListener {
            val options = arrayOf(
                "Facile (1 min)",
                "Medio (5 min)",
                "Difficile (10 min)"
            )

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.select_difficulty))
                .setItems(options) { _, which ->
                    val minutes = when (which) {
                        0 -> 1
                        1 -> 5
                        else -> 10
                    }

                    val bundle = bundleOf("minutes" to minutes)
                    findNavController().navigate(R.id.action_homeFragment_to_quizFragment, bundle)
                }
                .show()
        }

        binding.btnGotoProfile?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        binding.btnGotoSettings?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }

        binding.btnGotoHealth?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_healthFragment)
        }

        binding.btnGotoTrophies?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trophiesFragment)
        }

        binding.btnTutorial.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Come funziona l'app")
                .setMessage("Premi il grande tasto rosso SOS ogni volta che senti il forte desiderio di fumare.\n\nScegli la difficoltà in base all'intensità della crisi. Completare il minigioco ti aiuterà a distrarre la mente finché il desiderio non sarà passato!")
                .setPositiveButton("Ho capito") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}