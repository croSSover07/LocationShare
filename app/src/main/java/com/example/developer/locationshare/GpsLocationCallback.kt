package com.example.developer.locationshare

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import java.lang.ref.WeakReference


class GpsLocationCallback(mapsActivity: MapsActivity) : LocationCallback() {

    private var weakReference: WeakReference<MapsActivity> = WeakReference(mapsActivity)

    override fun onLocationResult(locationResult: LocationResult) {
        super.onLocationResult(locationResult)

        val mapsActivity = weakReference.get() ?: return
        mapsActivity.locationChanged(locationResult.lastLocation)
    }
}