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

import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.sechnick.mileage_autotracker.R
import com.sechnick.mileage_autotracker.convertDurationToFormatted
import com.sechnick.mileage_autotracker.convertNumericQualityToString
import com.sechnick.mileage_autotracker.database.RecordedTrip


@BindingAdapter("tripDurationFormatted")
fun TextView.setTripDurationFormatted(item: RecordedTrip?) {
    item?.let {
        text = convertDurationToFormatted(item.startTimeMilli, item.endTimeMilli, context.resources)
    }
}

@BindingAdapter("tripQualityString")
fun TextView.setTripQualityString(item: RecordedTrip?) {
    item?.let {
        text = convertNumericQualityToString(item.vehicleId, context.resources)
    }

}


@BindingAdapter("tripImage")
fun ImageView.setTripImage(item: RecordedTrip?) {
    item?.let {
        setImageResource(when (item.vehicleId) {
            0 -> R.drawable.ic_sleep_0
            1 -> R.drawable.ic_sleep_1
            2 -> R.drawable.ic_sleep_2

            3 -> R.drawable.ic_sleep_3

            4 -> R.drawable.ic_sleep_4
            5 -> R.drawable.ic_sleep_5
            else -> R.drawable.ic_sleep_active
        })
    }
}

@BindingAdapter("customEnabled")
fun Button.setEnabled(item: Boolean) {
    Log.d("data binding", "button visibility =$item")
    isEnabled = item
}
