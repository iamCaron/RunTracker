package com.caron.runtracker.repositories

import com.caron.runtracker.db.Run
import com.caron.runtracker.db.RunDao
import javax.inject.Inject


class MainRepository
@Inject constructor(val runDao: RunDao){

    suspend fun insertRun(run: Run)=runDao.insertRun(run)

    suspend fun deleteRun(run: Run)=runDao.deleteRun(run)

    fun getRunById(runId:Int)=runDao.getRun(runId)

    fun getAllRunsSortedByDate()=runDao.getAllRunsSortedByDate()

    fun getAllRunsSortedByDistance()=runDao.getAllRunsSortedByDistance()

    fun getAllRunsSortedByTimeInMillis()=runDao.getAllRunsSortedByTimeInMillis()

    fun getAllRunsSortedByAvgSpeed()=runDao.getAllRunsSortedByAverageSpeed()

    fun getAllRunsSortedByCaloriesBurned()=runDao.getAllRunsSortedByCaloriesBurned()

    fun getTotalAverageSpeed()=runDao.getTotalAverageSpeed()

    fun getTotalDistance()=runDao.getTotalDistance()

    fun getTotalCaloriesBurned()=runDao.getTotalCaloriesBurned()

    fun getTotalTimeInMillis()=runDao.getTotalTimeInMillis()



}