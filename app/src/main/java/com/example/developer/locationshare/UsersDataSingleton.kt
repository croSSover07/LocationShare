package com.example.developer.locationshare

import com.example.developer.locationshare.model.User
import com.google.android.gms.maps.model.Marker


object UsersDataSingleton {
    val arrayUsers: HashMap<String, User> = hashMapOf()
    val arrayMarkers: HashMap<String, Marker?> = hashMapOf()
}