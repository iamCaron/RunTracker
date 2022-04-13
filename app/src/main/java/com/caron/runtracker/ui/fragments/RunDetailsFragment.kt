package com.caron.runtracker.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.caron.runtracker.R
import com.caron.runtracker.other.Utility
import kotlinx.android.synthetic.main.fragment_run_details.*
import androidx.navigation.fragment.findNavController
import com.caron.runtracker.db.Run
import com.caron.runtracker.domain.Polylines
import com.caron.runtracker.repositories.MainRepository
import com.caron.runtracker.ui.viewModels.RunDetailsViewModel
import com.caron.runtracker.ui.viewModels.RunDetailsViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.tvDistanceCovered
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RunDetailsFragment: Fragment(R.layout.fragment_run_details),View.OnClickListener {


    lateinit var args: RunDetailsFragmentArgs
    lateinit var run: Run
    lateinit var polylines: Polylines

    @Inject
    lateinit var repository: MainRepository

    private val viewModel: RunDetailsViewModel by viewModels{ RunDetailsViewModelFactory(repository,run) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

         args =RunDetailsFragmentArgs.fromBundle(requireArguments())
         run=args.run


        viewModel.liveRun.observe(viewLifecycleOwner, Observer { liveRun ->
            {
                run=liveRun
            }
        })




         polylines=run.polylineList
        Timber.d(polylines.toString())

        tvDistanceCovered.text= Utility.getFormattedDistance(run.distanceInMeters.toFloat())
        tvRunTime.text= Utility.getFormattedStopWatchTime(run.timeInMillis)
        tvCalories.text= Utility.getFormattedCalories(run.caloriesBurned)
        tvAverageSpeed.text= Utility.getFormattedSpeed(run.avgSpeedInKMH)
        tvMaxSpeed.text= Utility.getFormattedSpeed(run.maxSpeedInKMH)
        tvElevationGain.text= Utility.getFormattedAltitude(run.elevationGain)
        tvAltitude.text= Utility.getFormattedAltitude(run.startAltitude)

        mapImageView.setOnClickListener(this)
        btnDeleteRun.setOnClickListener(this)

        Glide.with(this).load(run.img).into(mapImageView)





    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.mapImageView->
                    findNavController().navigate(
                        RunDetailsFragmentDirections.actionRunDetailsFragmentToMapsFragment(
                            polylines
                        )
                    )
            R.id.btnDeleteRun-> {

               showDeleteRunDialog()
            }

        }
    }

    private fun showDeleteRunDialog(){

        DeleteRunDialog().apply {
            setYesListener {
                deleteRunAndNavigate()

            }
        }.show(parentFragmentManager, CANCEL_TRACKING_DIALOG_TAG)

    }

    private fun deleteRunAndNavigate(){
        viewModel.deleteRun()
        findNavController().navigate(RunDetailsFragmentDirections.actionRunDetailsFragmentToRunsFragment())
    }



}