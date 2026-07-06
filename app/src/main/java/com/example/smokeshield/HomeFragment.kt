package com.example.smokeshield

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.smokeshield.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    // Variabili per il binding e il ViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CrisisViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 1. Inizializza il ViewModel
        viewModel = ViewModelProvider(this)[CrisisViewModel::class.java]

        // 2. Gonfia il layout usando il Data Binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // 3. Collega il ViewModel all'XML e imposta il ciclo di vita
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // 4. Cosa succede al click del bottone? Chiama la logica nel ViewModel!
        binding.btnAumentaStress.setOnClickListener {
            viewModel.increaseStress()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Pulizia per evitare sprechi di memoria
        _binding = null
    }
}