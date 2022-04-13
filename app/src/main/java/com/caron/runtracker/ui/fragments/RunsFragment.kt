package com.caron.runtracker.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.caron.runtracker.R
import com.caron.runtracker.adapters.OnItemClickListener
import com.caron.runtracker.adapters.RunAdapter
import com.caron.runtracker.db.Run
import com.caron.runtracker.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.caron.runtracker.other.SortType
import com.caron.runtracker.other.Utility
import com.caron.runtracker.ui.viewModels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_runs.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber


@AndroidEntryPoint
class RunsFragment:Fragment(R.layout.fragment_runs) , EasyPermissions.PermissionCallbacks {

    //because dagger manages all the view model factories behind the scenes it will provide the right view model
    private val viewModel: MainViewModel by viewModels()

    private lateinit var runAdapter: RunAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissions()
        setupRecyclerView()

        when(viewModel.sortType){
            SortType.DATE-> spFilter.setSelection(0)
            SortType.RUNNING_TIME-> spFilter.setSelection(1)
            SortType.DISTANCE-> spFilter.setSelection(2)
            SortType.AVERAGE_SPEED-> spFilter.setSelection(3)
            SortType.CALORIES_BURNED-> spFilter.setSelection(4)

        }

        spFilter.onItemSelectedListener=object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
               when(position){
                   0 -> viewModel.sortRuns(SortType.DATE)
                   1 -> viewModel.sortRuns(SortType.RUNNING_TIME)
                   2 -> viewModel.sortRuns(SortType.DISTANCE)
                   3 -> viewModel.sortRuns(SortType.AVERAGE_SPEED)
                   4 -> viewModel.sortRuns(SortType.CALORIES_BURNED)
               }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        viewModel.runs.observe(viewLifecycleOwner, Observer {
            for (run in it) {
                Timber.d(run.toString())
            }
            runAdapter.submitList(it)
        })


        fab.setOnClickListener {
            findNavController().navigate(R.id.action_runsFragment_to_trackingFragment)
        }
    }

    private fun setupRecyclerView()=rvRuns.apply {
        runAdapter = RunAdapter(recyclerItemOnClickImplementation())
        adapter=runAdapter
        layoutManager=LinearLayoutManager(requireContext())
    }

    private fun recyclerItemOnClickImplementation(): OnItemClickListener {
        return object : OnItemClickListener {
            override fun onItemClicked(run: Run) {



                findNavController().navigate(RunsFragmentDirections.actionRunsFragmentToRunDetailsFragment(run))
            }

        }
    }

    private fun requestPermissions(){
        if(Utility.hasLocationPermission(requireContext())){
            return
        }

        if (Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){

            EasyPermissions.requestPermissions(
                    this,
                    "Location Permission is Required",
                    REQUEST_CODE_LOCATION_PERMISSION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)
        }else{

            EasyPermissions.requestPermissions(
                    this,
                    "Location Permission is Required",
                    REQUEST_CODE_LOCATION_PERMISSION,
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