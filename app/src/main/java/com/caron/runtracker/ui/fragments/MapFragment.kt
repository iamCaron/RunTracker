package com.caron.runtracker.ui.fragments

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.View

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.caron.runtracker.R
import com.caron.runtracker.domain.Polylines
import com.caron.runtracker.other.Constants

class MapFragment : Fragment(R.layout.fragment_map) {

    var map: GoogleMap?=null
    lateinit var args : MapFragmentArgs
    lateinit var polylines: Polylines

    private val callback = OnMapReadyCallback { googleMap ->

        map=googleMap
        addAllPolylines(polylines)
        zoomToSeeWholeTrack(polylines)


    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args =MapFragmentArgs.fromBundle(requireArguments())

        polylines = args.polylines


        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragmentView) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun addAllPolylines(polylines: Polylines){
        for(polyline in polylines.polylineList){
            val polylineOptions= PolylineOptions()
                .color(Constants.POLYLINE_COLOR)
                .width(Constants.POLYLINE_WIDTH)
                .addAll(polyline.pathPointList)
            map?.addPolyline(polylineOptions)

        }
    }
    private fun zoomToSeeWholeTrack(polylines: Polylines){
        val bounds = LatLngBounds.builder()

        for(polyline in polylines.polylineList){
            for (pos in polyline.pathPointList){
                bounds.include(pos)
            }
        }


        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(
            bounds.build(),5

        ))
    }
}