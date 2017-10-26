package com.example.developer.locationshare.model

class UserLocation(
        var id: String = "",
        var name: String = "",
        var email: String = "email",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var isActive: Boolean = false
)