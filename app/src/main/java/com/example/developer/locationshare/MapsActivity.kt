package com.example.developer.locationshare

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.example.developer.locationshare.model.User
import com.example.developer.locationshare.model.UsersDataSingleton
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.lang.ref.WeakReference


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var user: User

    private var weakRefActivity = WeakReference(this@MapsActivity)

    private lateinit var locationRequest: LocationRequest

    private var lastKnownLocation: Location? = null

    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            val latLng = LatLng(location.latitude, location.longitude)
            setData(latLng, true)

            val cameraPosition = CameraPosition.Builder()
                    .target(latLng).zoom(Constant.DEFAULT_ZOOM).build()
            weakRefActivity.get()?.map?.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        initValues()
    }


    private fun initValues() {
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val email = intent.getStringExtra("email").toString()
        val displayName = intent.getStringExtra("display_name").toString()
        user = User(displayName, email, "", true)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest()
        locationRequest.interval = Constant.interval
        locationRequest.fastestInterval = Constant.fastestInterval
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    override fun onStart() {
        super.onStart()
        authToDatabase(intent.getStringExtra("token"))
    }

    private fun authToDatabase(token: String) {
        val cred: AuthCredential = GoogleAuthProvider.getCredential(token, null)
        FirebaseAuth.getInstance()?.signInWithCredential(cred)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        readData()
                    } else {
                        Toast.makeText(this@MapsActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun readData() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.reference.child("users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    val value = postSnapshot.getValue(User::class.java)
                    val key = postSnapshot.key
                    if (value != null) {
                        UsersDataSingleton.arrayUsers.put(key, value)
                        UsersDataSingleton.arrayMarkers[key]?.remove()
                        if (value.isActive) {
                            UsersDataSingleton.arrayMarkers[key] = addMarker(value)
                        }
                    }
                }
            }
        })
    }

    private fun addMarker(user: User): Marker? {
        val arrayOfLatAndLng = user.latLng.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return map.addMarker(MarkerOptions()
                .position(LatLng(arrayOfLatAndLng[0].toDouble(), arrayOfLatAndLng[1].toDouble()))
                .title(user.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
    }

    private fun setData(latLng: LatLng, isActive: Boolean) {
        val databaseReference = FirebaseDatabase.getInstance().reference
        weakRefActivity.get()?.user?.isActive = isActive
        weakRefActivity.get()?.user?.latLng = resources.getString(R.string.latLng, latLng.latitude.toString(), latLng.longitude.toString())

        databaseReference.child("users").child(weakRefActivity.get()?.user?.hashCode().toString()).setValue(weakRefActivity.get()?.user)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        } else {
            // Show rationale and request permission.
        }
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings?.isZoomControlsEnabled = true
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, weakRefActivity.get()?.locationCallback, null)
        } else {
            // Show rationale and request permission.
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        toExit()
    }

    override fun onPause() {
        super.onPause()
        toExit()
    }

    private fun toExit() {
        setData(Convert.toLatLng(user.latLng), false)
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}
