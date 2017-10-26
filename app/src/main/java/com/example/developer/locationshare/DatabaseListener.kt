package com.example.developer.locationshare

import com.example.developer.locationshare.model.UserLocation
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.lang.ref.WeakReference


class DatabaseListener(mapsActivity: MapsActivity) : ValueEventListener {

    private var weakReference: WeakReference<MapsActivity> = WeakReference(mapsActivity)

    override fun onCancelled(p0: DatabaseError?) {
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        for (postSnapshot in dataSnapshot.children) {
            val value = postSnapshot.getValue(UserLocation::class.java)
            val key = postSnapshot.key
            if (value != null) {
                if (weakReference.get()?.userLocation?.email != value.email) {
                    weakReference.get()?.arrayUsers?.put(key, value)
                    weakReference.get()?.arrayMarkers?.get(key)?.remove()
                    if (value.isActive) {
                        weakReference.get()?.arrayMarkers?.set(key, weakReference.get()?.addMarker(value))
                    }
                }
            }
        }
    }

}