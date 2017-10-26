package com.example.developer.locationshare

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import java.lang.ref.WeakReference


class GpsLocationCallback(mapsActivity: MapsActivity) : LocationCallback() {

    private var weakReference: WeakReference<MapsActivity> = WeakReference(mapsActivity)

    override fun onLocationResult(locationResult: LocationResult) {
        super.onLocationResult(locationResult)

        val mapsActivity = weakReference.get() ?: return

        val location = locationResult.lastLocation
        mapsActivity.setData(location.latitude, location.longitude, true)
        mapsActivity.updateCurrentUserDataOnDatabase()

        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude)).zoom(MapsActivity.DEFAULT_ZOOM).build()

        mapsActivity.map.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition))
    }
}