package com.caron.runtracker.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caron.runtracker.db.Run
import com.caron.runtracker.repositories.MainRepository
import kotlinx.coroutines.launch


class  RunDetailsViewModel
 constructor(
     val mainRepository: MainRepository,
     val run: Run
): ViewModel() {

        var liveRun=mainRepository.getRunById(run.id!!)




         fun deleteRun(){
            viewModelScope.launch {
                mainRepository.deleteRun(run)
            }
        }




}