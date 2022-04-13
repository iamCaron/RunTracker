package com.caron.runtracker.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
open class Polylines(var polylineList : MutableList<Polyline>):Parcelable {


    fun last(): Polyline?{

        if (polylineList.isNotEmpty()) {
            return polylineList.last()
        }
        return null

    }

    fun add(polyline: Polyline){
        polylineList.add(polyline)
    }

    fun isNotEmpty():Boolean{
        return polylineList.isNotEmpty()
    }
}