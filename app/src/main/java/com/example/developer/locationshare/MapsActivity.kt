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
    var googleMap: GoogleMap? = null
    var locationRequest: LocationRequest? = null
    var googleApiClient: GoogleApiClient? = null
    var marker: Marker? = null

    lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        authToDatabase(intent.getStringExtra("token"))
        buildGoogleApiClient()
        googleApiClient!!.connect()

    }

    fun authToDatabase(token: String) {
        var cred: AuthCredential = GoogleAuthProvider.getCredential(token, null)
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

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        } else {
            // Show rationale and request permission.
        }

        googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        //googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        createMarkers()
    }

    fun createMarkers() {
        for (item in UsersDataSingleton.arrayUsers) {
            addMarker(item.value)
        }
    }

    fun addMarker(user: User) {
        val a = user.latLng.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        googleMap?.addMarker(MarkerOptions()
                .position(LatLng(a[0].toDouble(), a[1].toDouble()))
                .title(user.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
    }


    override fun onConnected(bundle: Bundle?) {
        locationRequest = LocationRequest()
        locationRequest!!.interval = 10
        locationRequest!!.fastestInterval = 10
        locationRequest!!.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        // locationRequest!!.smallestDisplacement = 0.1F
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
        //remove previously placed Marker
        if (marker != null) {
            marker!!.remove()
        }
        val latLng = LatLng(location.latitude, location.longitude)
        setData(latLng)
        //place marker where user just clicked
//        marker = googleMap?.addMarker(MarkerOptions()
//                .position(latLng)
//                .title("Current Location")
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))

        val cameraPosition = CameraPosition.Builder()
                .target(latLng).zoom(5f).build()

        googleMap?.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition))
    }

    private fun setData(latLng: LatLng) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.reference
        val email = intent.getStringExtra("email").toString()
        val displayName = intent.getStringExtra("display_name").toString()
        val latLng = resources.getString(R.string.latLng, latLng.latitude.toString(), latLng.longitude.toString())
        val user=User(displayName, email, latLng)
        myRef.child("users").child(user.hashCode().toString()).setValue(user)
    }

    private fun getArrayLatLng() {
        val database = FirebaseDatabase.getInstance()
        //TODO
        database.reference.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
//TODO
//                val b = snapshot.value.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                val c = LatLng(b[0].toDouble(), b[1].toDouble())
//                val d = c.toString()
//                Log.i("LOL", d)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    private fun toLatLng(value: String): LatLng {
        val b = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return LatLng(b[0].toDouble(), b[1].toDouble())
    }

    private fun readData() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.reference.child("users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {//TODO
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    // TODO: handle the post
                    val value = postSnapshot.getValue(User::class.java)
                    val key = postSnapshot.key
                    if (value != null) {
                        UsersDataSingleton.arrayUsers.put(key, value)
                    }
                }
            }
        })
    }

}
