package com.caron.runtracker.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.caron.runtracker.R
import com.caron.runtracker.domain.Polyline
import com.caron.runtracker.domain.Polylines
import com.caron.runtracker.other.Constants.ACTION_PAUSE_SERVICE
import com.caron.runtracker.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.caron.runtracker.other.Constants.ACTION_STOP_SERVICE
import com.caron.runtracker.other.Constants.FASTEST_LOCATION_INTERVAL
import com.caron.runtracker.other.Constants.LOCATION_UPDATE_INTERVAL
import com.caron.runtracker.other.Constants.NOTIFICATION_CHANNEL_ID
import com.caron.runtracker.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.caron.runtracker.other.Constants.NOTIFICATION_ID
import com.caron.runtracker.other.Constants.TIMER_UPDATE_INTERVAL
import com.caron.runtracker.other.Utility
import com.caron.runtracker.other.Utility.calculateDistanceBetweenTwoPoints
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject



@AndroidEntryPoint
class TrackingService:LifecycleService() {

    var isFirstRun = true
    var serviceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val timeRunInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var baseNotificationBuilder:NotificationCompat.Builder

    lateinit var currentNotificationBuilder: NotificationCompat.Builder

    //variable to calculate current distance
    private var prevPoint:LatLng?=null


    private var prevAltitude:Float?=null

    @set:Inject
    private var weight=80f

    companion object {
        var startAltitude:Float? = null
        val timeRunInMillis=MutableLiveData<Long>()
        val distanceCovered=MutableLiveData<Float>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
        val currentSpeed=MutableLiveData<Float>()
        val topSpeed=MutableLiveData<Float>()
        val altitude=MutableLiveData<Float>()
        val elevationGain=MutableLiveData<Float>()
        val caloriesBurned=MutableLiveData<Float>()

    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        distanceCovered.postValue(0.0F)
        pathPoints.postValue(Polylines(mutableListOf()))
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
        currentSpeed.postValue(0F)
        topSpeed.postValue(0F)
        elevationGain.postValue(0F)
        altitude.postValue(0F)
        caloriesBurned.postValue(0F)
        startAltitude =null
        prevAltitude=null
    }

    override fun onCreate() {
        super.onCreate()
        currentNotificationBuilder=baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        isTracking.observe(this, Observer {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)

            if(!it){
                prevPoint=null
            }

        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

     
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForeGroundService()
                        isFirstRun = false
                    } else {
                        Timber.d("Resuming service....")
                        startTimer()
                    }
                    Timber.d("started or resumed service")

                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("paused service")
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("stopped service")
                    killService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled=false
    //time between starting a run and pausing
    private var lapTime=0L
    //total time of all laps
    private var timeRun=0L
    //timestamp of when we started the timer
    private var timeStarted=0L
    private var lastSecondTimestamp=0L

    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true

        //stopwatch  implementation
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                // time difference between now and timeStarted
                lapTime = System.currentTimeMillis() - timeStarted
                // post the new lapTime
                timeRunInMillis.postValue(timeRun + lapTime)
                if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }

            //when tracking is paused add the lap to total time run
            timeRun += lapTime
        }
    }

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerEnabled=false
    }

    private fun updateNotificationTrackingState(isTracking:Boolean){

        val notificationTest=if (isTracking) "Pause" else "Resume"

        val pendingIntent= if (isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }

            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
        }else{
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }

            PendingIntent.getService(this,2,resumeIntent, FLAG_UPDATE_CURRENT)

        }

        val notificationManager =getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        currentNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible=true
            set(currentNotificationBuilder,ArrayList<NotificationCompat.Action>())
        }

        if(!serviceKilled) {
            currentNotificationBuilder = baseNotificationBuilder
                    .addAction(R.drawable.ic_pause_black_24dp, notificationTest, pendingIntent)

            notificationManager.notify(NOTIFICATION_ID, currentNotificationBuilder.build())
        }


    }

    private fun killService(){
        serviceKilled=true
        isFirstRun=true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }


    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (Utility.hasLocationPermission(this)) {
                val request = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                        request,
                locationCallback,
                Looper.getMainLooper())
            }
        }else{
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }


    val locationCallback=object:LocationCallback(){
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if(isTracking.value!!){
                result?.locations?.let{ locations ->
                    for(location in locations){
                        addPathPoint(location)

                        val currentAltitude=location.altitude.toFloat()

                        altitude.postValue(currentAltitude)

                        //record the start altitude only at the start of run
                        if (startAltitude ==null){
                            startAltitude =currentAltitude
                        }

                        prevAltitude?.let {
                            if (currentAltitude>it){
                                elevationGain.postValue(elevationGain.value!!+(currentAltitude-it))
                            }
                        }

                        prevAltitude=currentAltitude

                        currentSpeed.postValue(location.speed)

                        if(location.speed > topSpeed.value!!){
                            topSpeed.postValue(location.speed)
                        }


                        val currentPoint=LatLng(location.latitude,location.longitude)



                        prevPoint?.let {
                            distanceCovered.postValue(
                                    calculateDistanceBetweenTwoPoints(it,currentPoint)
                                            + distanceCovered.value!!)
                        }



                        prevPoint=currentPoint

                        distanceCovered.let {
                           caloriesBurned.postValue ((it.value!! / 1000f) * weight)
                        }

                    }

                }
            }
        }
    }


    //adds pathpoint to last polyline
    private fun addPathPoint(location: Location?){
        location?.let {


            val pos=LatLng(location.latitude,location.longitude)
            pathPoints.value?.apply {
                last()?.let{it.add(pos)}
                pathPoints.postValue(this)
            }
        }
    }

    // when we pause our activity and resume it again we need to add an empty list of polylines
    // before adding the coordinates again

    private fun addEmptyPolyline()= pathPoints.value?.apply {
        add(Polyline(mutableListOf()))
        pathPoints.postValue(this)
    }?: pathPoints.postValue(Polylines(mutableListOf()))

    private fun startForeGroundService(){

        startTimer()

        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }



        startForeground(NOTIFICATION_ID,baseNotificationBuilder.build())

        timeRunInSeconds.observe(this, Observer {
            if(!serviceKilled) {
                val notification = currentNotificationBuilder
                        .setContentText(Utility.getFormattedStopWatchTime(it * 1000))
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })

    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager:NotificationManager){
        val channel= NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }
}