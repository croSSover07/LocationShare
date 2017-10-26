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
        val location = locationResult.lastLocation
        val latLng = LatLng(location.latitude, location.longitude)

        weakReference.get()?.setData(latLng, true)

        val cameraPosition = CameraPosition.Builder()
                .target(latLng).zoom(MapsActivity.DEFAULT_ZOOM).build()

        weakReference.get()?.map?.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition))
    }
}