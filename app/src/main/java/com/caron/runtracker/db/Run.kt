package com.caron.runtracker.db

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.caron.runtracker.domain.Polylines
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "running_table")
data class Run(
    var img: Bitmap?=null,
    var timestamp:Long=0L,
    var avgSpeedInKMH:Float=0f,
    var maxSpeedInKMH:Float=0f,
    var distanceInMeters:Int=0,
    var timeInMillis:Long=0L,
    var caloriesBurned:Float=0f,
    var startAltitude:Float=0f,
    var elevationGain:Float=0f,
    var polylineList: Polylines


):Parcelable {

    @PrimaryKey(autoGenerate = true)
    var id:Int?=null

}