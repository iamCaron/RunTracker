package com.caron.runtracker.ui.viewModels

import androidx.lifecycle.ViewModel
import com.caron.runtracker.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel
@Inject constructor(
    val mainRepository: MainRepository
    ):ViewModel() {


    val totalTimeRun = mainRepository.getTotalTimeInMillis()
    val totalDistance = mainRepository.getTotalDistance()
    val totalCaloriesBurned = mainRepository.getTotalCaloriesBurned()
    val totalAvgSpeed = mainRepository.getTotalAverageSpeed()

    val runsSortedByDate = mainRepository.getAllRunsSortedByDate()

}