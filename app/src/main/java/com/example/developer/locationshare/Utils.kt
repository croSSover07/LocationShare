package com.example.developer.locationshare

import android.content.Context
import android.location.LocationManager


fun checkGpsStatus(context: Context): Boolean =
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)
