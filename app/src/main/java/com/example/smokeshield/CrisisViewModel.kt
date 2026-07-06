package com.example.smokeshield

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CrisisViewModel : ViewModel() {

    // MutableLiveData è modificabile dal ViewModel
    private val _stressLevel = MutableLiveData<Int>(0)

    // LiveData pubblico che la View può solo leggere, non modificare (Sicurezza!)
    val stressLevel: LiveData<Int> get() = _stressLevel

    fun increaseStress() {
        // Prende il valore attuale, se è nullo usa 0, e aggiunge 1
        _stressLevel.value = (_stressLevel.value ?: 0) + 1
    }
}