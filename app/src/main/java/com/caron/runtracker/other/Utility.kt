package com.caron.runtracker.other

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import com.caron.runtracker.domain.Polyline
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit


object Utility {

    fun hasLocationPermission(context:Context)=
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
            EasyPermissions.hasPermissions(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }else{
            EasyPermissions.hasPermissions(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }


    fun getFormattedStopWatchTime(ms: Long, includeMillis: Boolean = false): String {
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        if(!includeMillis) {
            return "${if(hours < 10) "0" else ""}$hours:" +
                    "${if(minutes < 10) "0" else ""}$minutes:" +
                    "${if(seconds < 10) "0" else ""}$seconds"
        }
        milliseconds -= TimeUnit.SECONDS.toMillis(seconds)
        milliseconds /= 10
        return "${if(hours < 10) "0" else ""}$hours:" +
                "${if(minutes < 10) "0" else ""}$minutes:" +
                "${if(seconds < 10) "0" else ""}$seconds:" +
                "${if(milliseconds < 10) "0" else ""}$milliseconds"
    }

    fun calculatePolylineLength(polyline: Polyline):Float{

        var distance=0f

        for(i in 0..polyline.pathPointList.size-2){
            val pos1 = polyline.pathPointList[i]
            val pos2 = polyline.pathPointList[i + 1]

            distance+= calculateDistanceBetweenTwoPoints(pos1,pos2)
        }
        return distance
    }

    fun calculateDistanceBetweenTwoPoints(pos1:LatLng,pos2:LatLng):Float{
        val result = FloatArray(1)

        Location.distanceBetween(
            pos1.latitude,
            pos1.longitude,
            pos2.latitude,
            pos2.longitude,
            result
        )

        return result[0]
    }

    fun getFormattedDistance(distance:Float):String{

        if(distance>=1000) {
            return ((Math.round(distance  * 10.0) / 10.0)/100.0).toString() +" kms"
        }else
        return (Math.round(distance  * 100.0) / 100.0).toString() +" m"

    }


    fun getFormattedSpeed(speed:Float):String{

        return (Math.round(speed  * 100.0) / 100.0).toString() +" kmph"

    }

    fun getFormattedCalories(calories:Float):String{
        if(calories>=1000) {
            return ((Math.round(calories  * 10.0) / 10.0)/100.0).toString() +" kcal"
        }else
        return (Math.round(calories  * 100.0) / 100.0).toString() +" cal"

    }


    fun getFormattedAltitude(distance:Float):String{

        if(distance>=1000) {
            return ((Math.round(distance  * 10.0) / 10.0)/100.0).toString() +" kms"
        }else
            return (Math.round(distance  * 100.0) / 100.0).toString() +" m"
    }

}