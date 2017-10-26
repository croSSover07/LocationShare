package com.example.developer.locationshare.model

import java.util.*

class UserLocation(
//        TODO: Форматируй код, отступы не должны быть рандомными
//        var id :String,
//        var name: String,
//        var email: String,
//        var latitude: Double,
//        var longitude: Double,
//        var isActive:Boolean
        var id: String = "",
        var name: String = "",
        var email: String = "email",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var isActive: Boolean = false
) {
//    TODO: Можно описать через default параметры
//    constructor() : this("id", "name", "email", 0.0, 0.0, false)


//    override fun hashCode(): Int = email.hashCode()+id.hashCode()
//  TODO: У класса Objects есть утилитный метод hash для создания hash code.
    override fun hashCode(): Int = Objects.hash(id, name, email, latitude, longitude, isActive)

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