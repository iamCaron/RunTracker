package com.caron.runtracker.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: Run)

    @Delete
    suspend fun deleteRun(run: Run)

    @Query("SELECT * FROM running_table ORDER BY timestamp DESC")
    fun getAllRunsSortedByDate():LiveData<List<Run>>

    @Query("SELECT * FROM running_table ORDER BY timeInMillis DESC")
    fun getAllRunsSortedByTimeInMillis():LiveData<List<Run>>

    @Query("SELECT * FROM running_table ORDER BY caloriesBurned DESC")
    fun getAllRunsSortedByCaloriesBurned():LiveData<List<Run>>

    @Query("SELECT * FROM running_table ORDER BY avgSpeedInKMH DESC")
    fun getAllRunsSortedByAverageSpeed():LiveData<List<Run>>

    @Query("SELECT * FROM running_table ORDER BY distanceInMeters DESC")
    fun getAllRunsSortedByDistance():LiveData<List<Run>>

    @Query("SELECT SUM(timeInMillis) from running_table")
    fun getTotalTimeInMillis():LiveData<Long>

    @Query("SELECT SUM(caloriesBurned) from running_table")
    fun getTotalCaloriesBurned():LiveData<Int>


    @Query("SELECT SUM(distanceInMeters) from running_table")
    fun getTotalDistance():LiveData<Int>

    @Query("SELECT AVG(avgSpeedInKMH) from running_table")
    fun getTotalAverageSpeed():LiveData<Float>

    @Query("SELECT * FROM running_table WHERE id=:runId")
    fun getRun(runId:Int):LiveData<Run>
}