package com.example.developer.locationshare


import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.example.developer.locationshare.model.User
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
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


class MapsActivity : AppCompatActivity(),
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private var googleMap: GoogleMap? = null
    private lateinit var locationRequest: LocationRequest
    private var googleApiClient: GoogleApiClient? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        authToDatabase(intent.getStringExtra("token"))
        buildGoogleApiClient()
        googleApiClient!!.connect()
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

    override fun onResume() {
        super.onResume()

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @Synchronized private fun buildGoogleApiClient() {
        Toast.makeText(this, "buildGoogleApiClient", Toast.LENGTH_SHORT).show()
        googleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setUpMap()
    }


    private fun setUpMap() {
        val a = googleMap
        if (a != null) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                a.isMyLocationEnabled = true
            } else {
                // Show rationale and request permission.
            }
            a.mapType = GoogleMap.MAP_TYPE_NORMAL
            a.uiSettings?.isZoomControlsEnabled = true
            googleMap = a
        }
    }

    private fun addMarker(user: User): Marker? {
        val a = user.latLng.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
       return googleMap?.addMarker(MarkerOptions()
                .position(LatLng(a[0].toDouble(), a[1].toDouble()))
                .title(user.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
    }

    override fun onConnected(bundle: Bundle?) {
        locationRequest = LocationRequest()
        locationRequest.interval = 10
        locationRequest.fastestInterval = 10
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
        } else {
            // Show rationale and request permission.
        }

    }

    override fun onConnectionSuspended(i: Int) {
    }

    override fun onLocationChanged(location: Location) {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
        if (marker != null) {
            marker!!.remove()
        }

        val latLng = LatLng(location.latitude, location.longitude)
        setData(latLng, true)

        val cameraPosition = CameraPosition.Builder()
                .target(latLng).zoom(5f).build()
        googleMap?.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition))
    }

    private fun setData(latLng: LatLng, isActive: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.reference

        val email = intent.getStringExtra("email").toString()
        val displayName = intent.getStringExtra("display_name").toString()
        val latLng = resources.getString(R.string.latLng, latLng.latitude.toString(), latLng.longitude.toString())
        val user = User(displayName, email, latLng, isActive)

        myRef.child("users").child(user.hashCode().toString()).setValue(user)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    private fun readData() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.reference.child("users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {//TODO
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    val value = postSnapshot.getValue(User::class.java)
                    val key = postSnapshot.key
                    if (value != null) {
                        UsersDataSingleton.arrayUsers.put(key, value)
                        if (value.isActive) {
                            UsersDataSingleton.arrayMarkers[key]= addMarker(value)
                        } else {
                            UsersDataSingleton.arrayMarkers[key]?.remove()
                        }
                    }
                }
            }
        })
    }

}
