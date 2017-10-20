package com.example.developer.locationshare.model


class User() {
    lateinit var name: String
    lateinit var email: String
    lateinit var latLng: String
    var isActive: Boolean = false

    constructor(name: String, email: String, latLng: String   , isActive: Boolean) : this() {
        this.name = name
        this.email = email
        this.latLng = latLng
        this.isActive = isActive
    }

    override fun hashCode(): Int {
        email.hashCode()
        return email.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (name != other.name) return false
        if (email != other.email) return false
        if (latLng != other.latLng) return false
        if (isActive != other.isActive) return false

        return true
    }
}