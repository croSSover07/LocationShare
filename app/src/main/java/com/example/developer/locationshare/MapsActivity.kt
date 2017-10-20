package com.example.developer.locationshare

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.developer.locationshare.model.Convert.Companion.toLatLng
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
    private lateinit var googleApiClient: GoogleApiClient

    private lateinit var user: User

    private var exit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val email = intent.getStringExtra("email").toString()
        val displayName = intent.getStringExtra("display_name").toString()
        user = User(displayName, email, "", true)
    }

    override fun onStart() {
        super.onStart()

        buildGoogleApiClient()
        authToDatabase(intent.getStringExtra("token"))
        googleApiClient.connect()
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

        if (user.latLng.isNotEmpty()) {
            setData(toLatLng(user.latLng), true)
        }
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
        val copyGoogleMap = googleMap
        if (copyGoogleMap != null) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                copyGoogleMap.isMyLocationEnabled = true
            } else {
                // Show rationale and request permission.
            }
            copyGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            copyGoogleMap.uiSettings?.isZoomControlsEnabled = true
            googleMap = copyGoogleMap
        }
    }

    private fun addMarker(user: User): Marker? {
        val arrayOfLatAndLng = user.latLng.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return googleMap?.addMarker(MarkerOptions()
                .position(LatLng(arrayOfLatAndLng[0].toDouble(), arrayOfLatAndLng[1].toDouble()))
                .title(user.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
    }

    override fun onConnected(bundle: Bundle?) {
        locationRequest = LocationRequest()
        locationRequest.interval = Constant.interval
        locationRequest.fastestInterval = Constant.fastestInterval
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

        val latLng = LatLng(location.latitude, location.longitude)
        if (exit) {
            setData(latLng, false)
        } else {
            setData(latLng, true)
        }
        val cameraPosition = CameraPosition.Builder()
                .target(latLng).zoom(Constant.DEFAULT_ZOOM).build()
        googleMap?.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition))
    }

    private fun setData(latLng: LatLng, isActive: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.reference

        user.isActive = isActive
        user.latLng = resources.getString(R.string.latLng, latLng.latitude.toString(), latLng.longitude.toString())

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
                        UsersDataSingleton.arrayMarkers[key]?.remove()
                        if (value.isActive) {
                            UsersDataSingleton.arrayMarkers[key]= addMarker(value)
                        }
                    }
                }
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_log_out -> {
            toExit()
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        setData(toLatLng(user.latLng), false)
    }

    override fun onDestroy() {
        toExit()
        super.onDestroy()
    }

    override fun onBackPressed() {
        toExit()
        finishAffinity()
        System.exit(0)
    }

    fun toExit() {
        exit = true
        googleApiClient.clearDefaultAccountAndReconnect()
        setData(toLatLng(user.latLng), false)
    }
}
