package com.example.developer.locationshare.model


class User(
        var id :String,
        var name: String,
        var email: String,
        var latLng: String,
        var isActive:Boolean
) {
    constructor() : this("id","name", "email", "latLng",false)

    override fun hashCode(): Int = email.hashCode()+id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (name != other.name) return false
        if (email != other.email) return false
        if (latLng != other.latLng) return false
        if (isActive != other.isActive) return false
        if (id != other.id) return false

        return true
    }
}