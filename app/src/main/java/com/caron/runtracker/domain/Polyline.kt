package com.caron.runtracker.domain

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.parcel.Parcelize

@Parcelize
class Polyline( var pathPointList : MutableList<LatLng>) :Parcelable{



    fun add(pathPoint:LatLng){
        pathPointList.add(pathPoint)
    }

    fun isNotEmpty():Boolean{
        return pathPointList.isNotEmpty()
    }
    fun last():LatLng{
        return pathPointList.last()
    }
}