package com.example.developer.locationshare.model

import com.google.android.gms.maps.model.Marker


object UsersDataSingleton {
    val arrayUsers: HashMap<String, User> = hashMapOf()
    val arrayMarkers: HashMap<String, Marker?> = hashMapOf()
}