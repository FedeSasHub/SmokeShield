package com.example.smokeshield

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.smokeshield.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CrisisViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[CrisisViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Gestione del click sul pulsante SOS
        binding.btnSos.setOnClickListener {

            // Aggiungiamo la terza opzione all'array
            val options = arrayOf(
                getString(R.string.diff_easy),
                getString(R.string.diff_hard),
                getString(R.string.diff_test)
            )

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.select_difficulty))
                .setItems(options) { _, which ->
                    // Usiamo 'when' per gestire le 3 casistiche
                    val minutes = when (which) {
                        0 -> 5  // Indice 0: Facile
                        1 -> 10 // Indice 1: Difficile
                        else -> 1 // Indice 2: Test Orale
                    }

                    val bundle = bundleOf("minutes" to minutes)
                    findNavController().navigate(R.id.action_homeFragment_to_quizFragment, bundle)
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