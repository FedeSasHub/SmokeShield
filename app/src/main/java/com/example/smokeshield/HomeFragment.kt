package com.example.smokeshield

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.smokeshield.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
class HomeFragment : Fragment() {

    // Variabili per il binding e il ViewModel
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

        // IL COMANDO DI NAVIGAZIONE
        binding.btnSos.setOnClickListener {
            // Controlla la mappa (nav_graph) e usa l'azione generata in automatico
            findNavController().navigate(R.id.action_homeFragment_to_quizFragment)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Pulizia per evitare sprechi di memoria
        _binding = null
    }
}