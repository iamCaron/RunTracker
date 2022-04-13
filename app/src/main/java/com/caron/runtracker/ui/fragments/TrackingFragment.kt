 package com.caron.runtracker.ui.fragments

 import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.caron.runtracker.R
import com.caron.runtracker.db.Run
import com.caron.runtracker.domain.Polylines
import com.caron.runtracker.other.Constants
import com.caron.runtracker.other.Constants.ACTION_PAUSE_SERVICE
import com.caron.runtracker.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.caron.runtracker.other.Constants.ACTION_STOP_SERVICE
import com.caron.runtracker.other.Constants.MAP_ZOOM
import com.caron.runtracker.other.Constants.POLYLINE_COLOR
import com.caron.runtracker.other.Constants.POLYLINE_WIDTH
import com.caron.runtracker.other.Utility
import com.caron.runtracker.other.Utility.getFormattedAltitude
import com.caron.runtracker.other.Utility.getFormattedCalories
import com.caron.runtracker.other.Utility.getFormattedDistance
import com.caron.runtracker.other.Utility.getFormattedSpeed

import com.caron.runtracker.services.TrackingService
import com.caron.runtracker.ui.viewModels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.lang.Math.round
import java.util.*
import javax.inject.Inject

 const val CANCEL_TRACKING_DIALOG_TAG="CancelDialog"

 @AndroidEntryPoint
class TrackingFragment:Fragment(R.layout.fragment_tracking), EasyPermissions.PermissionCallbacks {

    //map view used in the ui fragment instead of map fragment because we already have a fragment ,
    // this gives performance benefits but we have to manage the maps lifecycle as opposed to map fragment

    private val viewModel: MainViewModel by viewModels()

    private var isTracking=false
    private var pathPoints= Polylines(mutableListOf())


    private var map:GoogleMap?=null

    private var currentTimeInMillis=0L

    private var menu: Menu?=null

     @set:Inject
     private var weight=80f


    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        btnToggleRun.setOnClickListener {
            toggleRun() }

        if (savedInstanceState!=null){
            val cancelTrackingDialog=parentFragmentManager.findFragmentByTag(
                CANCEL_TRACKING_DIALOG_TAG
            )
            as CancelTrackingDialog?
            cancelTrackingDialog?.setYesListener {
                stopRun()
            }
        }

        btnFinishRun.setOnClickListener{
            if (pathPoints.polylineList[0].isNotEmpty()) {
                zoomToSeeWholeTrack()
                endRunAndSaveToDb()
            }else{
                stopRun()
            }
        }

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync{
            map=it
            if (Utility.hasLocationPermission(requireContext())) {
                it.isMyLocationEnabled = true
            }else {
              requestPermissions()
            }
            addAllPolylines()
        }

        subscribeToObservers()

    }

    private fun subscribeToObservers(){
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })
        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints=it
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            currentTimeInMillis=it
            val formattedTime= Utility.getFormattedStopWatchTime(currentTimeInMillis,true)
            tvRunTime.text=formattedTime
        })

        TrackingService.distanceCovered.observe(viewLifecycleOwner, Observer {
            tvDistanceCovered.text=getFormattedDistance(it)
        })

        TrackingService.currentSpeed.observe(viewLifecycleOwner, Observer {
            tvCurrentSpeed.text= getFormattedSpeed(it)
        })

        TrackingService.topSpeed.observe(viewLifecycleOwner, Observer {
            tvMaxSpeed.text= getFormattedSpeed(it)
        })

        TrackingService.altitude.observe(viewLifecycleOwner, Observer {
            tvAltitude.text= getFormattedAltitude(it)
        })

        TrackingService.elevationGain.observe(viewLifecycleOwner, Observer {
            tvElevationGain.text= getFormattedAltitude(it)
        })

        TrackingService.caloriesBurned.observe(viewLifecycleOwner, Observer {
            tvCalories.text= getFormattedCalories(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        setHasOptionsMenu(true)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu,menu)
        this.menu=menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if(currentTimeInMillis>0L){
            this.menu?.getItem(0)?.isVisible=true

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCancelTrackingDialog(){

        CancelTrackingDialog().apply {
            setYesListener {
                stopRun()
                Timber.d("cancelling")
            }
        }.show(parentFragmentManager, CANCEL_TRACKING_DIALOG_TAG)

    }

    private fun stopRun(){
        tvRunTime.text="00:00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runsFragment)

    }

    private fun toggleRun(){
        if (isTracking){
            menu?.getItem(0)?.isVisible=true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }else{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking:Boolean){
        this.isTracking=isTracking
        if(!isTracking && currentTimeInMillis > 0L){
            btnToggleRun.text="Start"
            btnFinishRun.visibility=View.VISIBLE

        }else if (isTracking){
            btnToggleRun.text="Stop"
            menu?.getItem(0)?.isVisible=true

            btnFinishRun.visibility=View.GONE
        }
    }

    private fun moveCameraToUser(){
        if (pathPoints.isNotEmpty()&& pathPoints?.last()?.isNotEmpty() ?:false){

            map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            pathPoints.last()
                                    ?.last(),
                                    MAP_ZOOM)
            )

        }
    }

    private fun endRunAndSaveToDb(){


            map?.snapshot { bmp ->
                var distanceInMeters = 0
                for (polyline in pathPoints.polylineList) {
                    distanceInMeters += Utility.calculatePolylineLength(polyline).toInt()
                }

                val avgSpeed = round((distanceInMeters / 1000f) / (currentTimeInMillis / 1000f / 60 / 60) * 10) / 10f
                val dateTimestamp = Calendar.getInstance().timeInMillis
                val caloriesBurned = ((distanceInMeters / 1000f) * weight)
                val maxSpeed= TrackingService.topSpeed.value
                val startAltitude= TrackingService.startAltitude
                val elevationGain= TrackingService.elevationGain.value
                val polylines= Polylines(pathPoints.polylineList)
                val run = Run(bmp,
                        dateTimestamp,
                        avgSpeed,maxSpeed!!,
                        distanceInMeters,
                        currentTimeInMillis,
                        caloriesBurned,
                        startAltitude!!,
                        elevationGain!!,
                       polylines)


                Timber.d((viewModel.insertRun(run).toString()))

                Snackbar.make(requireActivity().findViewById(R.id.rootView),
                        "Run Saved Successfully",
                        Snackbar.LENGTH_LONG).show()

                stopRun()
            }



    }



    private fun zoomToSeeWholeTrack(){
        val bounds = LatLngBounds.builder()

        for(polyline in pathPoints.polylineList){
            for (pos in polyline.pathPointList){
                bounds.include(pos)
            }
        }


        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                (mapView.height * 0.05f).toInt()
        ))
    }

    //on orientation change activity is destroyed and data is lost
    //this function gets data from viewmodel and redraws polylines
    private fun addAllPolylines(){
        for(polyline in pathPoints.polylineList){
            val polylineOptions=PolylineOptions()
                    .color(POLYLINE_COLOR)
                    .width(POLYLINE_WIDTH)
                    .addAll(polyline.pathPointList)
            map?.addPolyline(polylineOptions)

        }
    }

     //connects last two points and draws line on map
    private fun addLatestPolyline(){
        if (pathPoints.isNotEmpty()&& pathPoints.last()?.pathPointList?.size!!  >1){
            val preLastLatLng = pathPoints.last()!!.pathPointList[pathPoints.last()!!.pathPointList.size-2]
            val lastLatLng= pathPoints.last()!!.last()
            val polylineOptions=PolylineOptions()
                    .color(POLYLINE_COLOR)
                    .width(POLYLINE_WIDTH)
                    .add(preLastLatLng)
                    .add(lastLatLng)

            map?.addPolyline(polylineOptions)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    private fun sendCommandToService(action:String)=
            Intent(requireContext(), TrackingService::class.java).also {
                it.action=action
                requireContext().startService(it)
            }

     private fun requestPermissions(){


         if (Build.VERSION.SDK_INT< Build.VERSION_CODES.Q){

             EasyPermissions.requestPermissions(
                     this,
                     "Location Permission is Required",
                     Constants.REQUEST_CODE_LOCATION_PERMISSION,
                     Manifest.permission.ACCESS_FINE_LOCATION,
                     Manifest.permission.ACCESS_COARSE_LOCATION)
         }else{

             EasyPermissions.requestPermissions(
                     this,
                     "Location Permission is Required",
                     Constants.REQUEST_CODE_LOCATION_PERMISSION,
                     Manifest.permission.ACCESS_FINE_LOCATION,
                     Manifest.permission.ACCESS_COARSE_LOCATION,
                     Manifest.permission.ACCESS_BACKGROUND_LOCATION)
         }
     }

     override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

     }


     override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
         if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
             AppSettingsDialog.Builder(this).build().show()
         }else{
             requestPermissions()
         }
     }

     override fun onRequestPermissionsResult(requestCode: Int,
                                             permissions: Array<out String>,
                                             grantResults: IntArray) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults)
         EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)

     }
}