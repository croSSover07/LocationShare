package com.example.developer.locationshare.model

class UserLocation(
        var id :String,
        var name: String,
        var email: String,
        var latitude: Double,
        var longitude: Double,
        var isActive:Boolean
) {
    constructor() : this("id", "name", "email", 0.0, 0.0, false)

    override fun hashCode(): Int = email.hashCode()+id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserLocation

        if (name != other.name) return false
        if (email != other.email) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (isActive != other.isActive) return false
        if (id != other.id) return false

        return true
    }
}