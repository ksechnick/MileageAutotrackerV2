/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sechnick.mileage_autotracker.triptracker

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sechnick.mileage_autotracker.R
import com.google.android.material.snackbar.Snackbar
import com.sechnick.mileage_autotracker.MainActivity
import com.sechnick.mileage_autotracker.service.TrackingService
import com.sechnick.mileage_autotracker.database.MileageDatabase
import com.sechnick.mileage_autotracker.databinding.FragmentTripTrackerBinding
import kotlinx.android.synthetic.main.fragment_trip_tracker.*

/**
 * A fragment with buttons to record start and end times for sleep, which are saved in
 * a database. Cumulative data is displayed in a simple scrollable TextView.
 * (Because we have not learned about RecyclerView yet.)
 * The Clear button will clear all data from the database.
 */
class TripTrackerFragment : Fragment() {

    /**
     * Called when the Fragment is ready to display content to the screen.
     *
     * This function uses DataBindingUtil to inflate R.layout.fragment_active_trip.
     *
     * It is also responsible for passing the [TripTrackerViewModel] to the
     * [FragmentSleepTrackerBinding] generated by DataBinding. This will allow DataBinding
     * to use the [LiveData] on our ViewModel.
     */
    lateinit var tripTrackerViewModel : TripTrackerViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentTripTrackerBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_trip_tracker, container, false)

        val application = requireNotNull(this.activity).application

        // Create an instance of the ViewModel Factory.
        val dataSource = MileageDatabase.getInstance(application).mileageDatabaseDao
        val viewModelFactory = TripTrackerViewModelFactory(dataSource, application)

        // Get a reference to the ViewModel associated with this fragment.
        tripTrackerViewModel =
                ViewModelProviders.of(
                        this, viewModelFactory).get(TripTrackerViewModel::class.java)

        // To use the View Model with data binding, you have to explicitly
        // give the binding object a reference to it.
        binding.tripTrackerViewModel= tripTrackerViewModel

        val adapter = RecordedTripAdapter(RecordedTripListener { nightId ->
            //Toast.makeText(context, "${nightId}", Toast.LENGTH_LONG).show()
            tripTrackerViewModel.onSleepNightClicked(nightId)
        })
        binding.sleepList.adapter = adapter


        tripTrackerViewModel.trips.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.addHeaderAndSubmitList(it)
            }
        })

        // Specify the current activity as the lifecycle owner of the binding.
        // This is necessary so that the binding can observe LiveData updates.
        binding.setLifecycleOwner(this)

        // Add an Observer on the state variable for showing a Snackbar message
        // when the CLEAR button is pressed.
        tripTrackerViewModel.showSnackBarEvent.observe(this, Observer {
            if (it == true) { // Observed state is true.
                Snackbar.make(
                        activity!!.findViewById(android.R.id.content),
                        getString(R.string.cleared_message),
                        Snackbar.LENGTH_SHORT // How long to display the message.
                ).show()
                // Reset state to make sure the toast is only shown once, even if the device
                // has a configuration change.
                tripTrackerViewModel.doneShowingSnackbar()
            }
        })

        // Add an Observer on the state variable for Navigating when STOP button is pressed.
        tripTrackerViewModel.navigateToSleepQuality.observe(this, Observer { trip ->
            trip?.let {
                // We need to get the navController from this, because button is not ready, and it
                // just has to be a view. For some reason, this only matters if we hit stop again
                // after using the back button, not if we hit stop and choose a quality.
                // Also, in the Navigation Editor, for Quality -> Tracker, check "Inclusive" for
                // popping the stack to get the correct behavior if we press stop multiple times
                // followed by back.
                // Also: https://stackoverflow.com/questions/28929637/difference-and-uses-of-oncreate-oncreateview-and-onactivitycreated-in-fra
                this.findNavController().navigate(
                        TripTrackerFragmentDirections
                                .actionSleepTrackerFragmentToSleepQualityFragment(trip.tripId))
                // Reset state to make sure we only navigate once, even if the device
                // has a configuration change.
                tripTrackerViewModel.doneNavigating()
            }
        })

        // Add an Observer on the state variable for Navigating when and item is clicked.
        tripTrackerViewModel.navigateToSleepDetail.observe(this, Observer { trip ->
            trip?.let {

                this.findNavController().navigate(
                        TripTrackerFragmentDirections
                                .actionSleepTrackerFragmentToSleepDetailFragment(trip))
                tripTrackerViewModel.onSleepDetailNavigated()
            }
        })

        // Add an Observer on the state variable for Navigating when and item is clicked.
        TripTrackerViewModel.requestLocationPermission.observe(this, Observer {
            if (it == true) { // Observed state is true.

                checkPermissionForLocation()
            }
        })

        // Add an Observer on the state variable for Navigating when and item is clicked.
        tripTrackerViewModel.locationSnackbarText.observe(this, Observer {
            if (tripTrackerViewModel.locationSnackbarText.value == "nothing yet") {
                Snackbar.make(
                        activity!!.findViewById(android.R.id.content),
                        tripTrackerViewModel.locationSnackbarText.value!!,
                        Snackbar.LENGTH_SHORT // How long to display the message.
                ).show()
                // Reset state to make sure the toast is only shown once, even if the device
                // has a configuration change.
            }
        })

        checkPermissionForLocation()

        val manager = LinearLayoutManager(activity)

        binding.sleepList.layoutManager = manager

        return binding.root
    }







    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        Log.d("permissions", "requesting permissions at fragments")

        if (requestCode == TripTrackerViewModel.REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                TripTrackerViewModel.locationPermissionGranted()
            } else {
                Toast.makeText(this.context, "Permission Denied", Toast.LENGTH_SHORT).show()
                TripTrackerViewModel.locationPermissionNotGranted()
            }
        }
    }

    fun checkPermissionForLocation(): Boolean {

        Log.d("permissions", "starting location permission check")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (context!!.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                //TODO("could context ever be null?")
                TripTrackerViewModel.locationPermissionGranted()
                Log.d("permissions", "location permission check passed")
                true
            } else {
                // Show the permission request
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        TripTrackerViewModel.REQUEST_PERMISSION_LOCATION)
                TripTrackerViewModel.locationPermissionNotGranted()
                Log.d("permissions", "location permission check failed")
                false
            }
        } else {
            TripTrackerViewModel.locationPermissionGranted()
            Log.d("permissions", "too old to request permissions")
            true
        }
    }



}
