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
        val mapsActivity: MapsActivity = weakReference.get() ?: return
        dataSnapshot.children
                .mapNotNull { it.getValue(UserLocation::class.java) }
                .forEach { mapsActivity.dataChanged(it) }
    }

}