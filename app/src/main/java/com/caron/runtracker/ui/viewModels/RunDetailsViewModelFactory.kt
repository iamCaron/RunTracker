package com.caron.runtracker.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.caron.runtracker.db.Run
import com.caron.runtracker.repositories.MainRepository

class RunDetailsViewModelFactory(val mainRepository: MainRepository, val run: Run): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunDetailsViewModel::class.java)) {
            return RunDetailsViewModel(mainRepository,run) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
