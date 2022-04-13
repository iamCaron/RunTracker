package com.caron.runtracker.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.caron.runtracker.R
import com.caron.runtracker.other.Constants
import com.caron.runtracker.ui.MainActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
//declares how long the dependencies inside this service module will live
@InstallIn(ServiceComponent::class)
object ServiceModule {


    //for the lifetime of the service there is only going to be one instance of this
    @ServiceScoped
    @Provides
    fun provideFusedLocationProviderClient(
            @ApplicationContext app:Context
    )=FusedLocationProviderClient(app)

    @ServiceScoped
    @Provides
    fun provideMainActivityPendingIntent(@ApplicationContext app:Context)= PendingIntent.getActivity(
    app,
    0,
    Intent(app, MainActivity::class.java).also {
        it.action = Constants.ACTION_SHOW_TRACKING_FRAGMENT

    },PendingIntent.FLAG_UPDATE_CURRENT)

    @ServiceScoped
    @Provides
    fun provideBaseNotificationBuilder(
            @ApplicationContext app:Context,
            pendingIntent:PendingIntent)=NotificationCompat.Builder(app, Constants.NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle("Run Tracker")
            .setContentText("00:00:00")
            .setContentIntent(pendingIntent)

}