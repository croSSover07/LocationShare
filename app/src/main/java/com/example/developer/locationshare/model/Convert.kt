package com.example.developer.locationshare.model

import com.google.android.gms.maps.model.LatLng


class Convert {
    companion object {
        fun toLatLng(latLng: String): LatLng {
            val array = latLng.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return LatLng(array[0].toDouble(), array[1].toDouble())
        }
    }
}