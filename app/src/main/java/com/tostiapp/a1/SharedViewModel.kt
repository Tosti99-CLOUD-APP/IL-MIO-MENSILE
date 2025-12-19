package com.tostiapp.a1

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _backgroundChanged = MutableLiveData<Unit>()
    val backgroundChanged: LiveData<Unit> = _backgroundChanged

    fun notifyBackgroundChanged() {
        _backgroundChanged.value = Unit
    }
}