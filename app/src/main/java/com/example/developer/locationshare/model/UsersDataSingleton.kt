package com.example.developer.locationshare.model

import com.google.android.gms.maps.model.Marker


object UsersDataSingleton {
    val ARRAY_USERS: HashMap<String, UserLocation> = hashMapOf()
    val arrayMarkers: HashMap<String, Marker?> = hashMapOf()
    fun clear(){
        arrayMarkers.clear()
        ARRAY_USERS.clear()
    }
}