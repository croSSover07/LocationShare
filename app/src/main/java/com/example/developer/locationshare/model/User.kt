package com.example.developer.locationshare.model


class User() {
    lateinit var name: String
    lateinit var email: String
    lateinit var latLng: String

    constructor(name: String, email: String, latLng: String) : this() {
        this.name = name
        this.email = email
        this.latLng = latLng
    }

    override fun hashCode(): Int {
        email.hashCode()
        return email.hashCode()
    }
}