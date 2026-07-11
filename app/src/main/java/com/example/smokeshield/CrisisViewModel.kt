package com.example.smokeshield

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CrisisViewModel : ViewModel() {

    private val _stressLevel = MutableLiveData<Int>(0)
    val stressLevel: LiveData<Int> get() = _stressLevel

    fun increaseStress() {
        _stressLevel.value = (_stressLevel.value ?: 0) + 1
    }
}