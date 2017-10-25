package com.example.developer.locationshare

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.developer.locationshare.model.UserLocation
import com.example.developer.locationshare.model.UsersDataSingleton
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
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
import com.squareup.leakcanary.LeakCanary


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        val RC_SIGN_IN = 100
        val DEFAULT_ZOOM = 16f
        val INTERVAL = 10L
        val FASTEST_INTERVAL = 10L
    }

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private lateinit var userLocation: UserLocation
    private var currentGoogleAcc: GoogleSignInAccount? = null

    private var databaseRef = FirebaseDatabase.getInstance().reference
    private lateinit var googleApiClient: GoogleApiClient
    private var auth: FirebaseAuth? = null

    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            val latLng = LatLng(location.latitude, location.longitude)

            setData(latLng, true)

            val cameraPosition = CameraPosition.Builder()
                    .target(latLng).zoom(DEFAULT_ZOOM).build()

            map.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition))
        }
    }

    private var gpsLocationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                if (!checkGpsStatus(context)) {
                    showGPSAlertDialog(context)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this@MapsActivity.application)

        initGoogleClient()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (currentGoogleAcc != null) {
                this.registerReceiver(gpsLocationReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
                if (checkGpsStatus(this)) {
                    initLocalUserAndConnectDB()
                    if (UsersDataSingleton.ARRAY_USERS.isNotEmpty()) {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
                    } else {
                        initMapsValues()
                    }
                } else {
                    showGPSAlertDialog(this)
                }
            } else {
                signInGoogle()
            }
        } else {   // Show rationale and request permission.
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    finish()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        block()
    }

    override fun onPause() {
        super.onPause()
        if (UsersDataSingleton.ARRAY_USERS.isNotEmpty()) {
            block()
        }
    }

    private fun authToDatabase(token: String) {
        val cred: AuthCredential = GoogleAuthProvider.getCredential(token, null)
        FirebaseAuth.getInstance()?.signInWithCredential(cred)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        readData()
                    } else {
                        Toast.makeText(this@MapsActivity, getString(R.string.error_auth_failed), Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun initGoogleClient() {

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(resources.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        auth = FirebaseAuth.getInstance()
    }

    private fun initMapsValues() {

        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)

        locationRequest = LocationRequest()
        with(locationRequest) {
            interval = INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {// Show rationale and request permission.
        }
    }

    private fun initLocalUserAndConnectDB() {
        UsersDataSingleton.clear()
        val copy = currentGoogleAcc
        if (copy != null) {
            userLocation = UserLocation(
                    copy.id.toString(),
                    copy.displayName.toString(),
                    copy.email.toString(),
                    0.0,
                    0.0,
                    true)
        }
        connectToDatabase()
    }

    private fun connectToDatabase() {
        authToDatabase(currentGoogleAcc?.idToken.toString())
    }

    private fun signInGoogle() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            if (result.signInAccount != null) {
                currentGoogleAcc = result.signInAccount
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_log_out -> {
            googleApiClient.clearDefaultAccountAndReconnect()
            signInGoogle()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun readData() {
        databaseRef.child("users")
                .addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    val value = postSnapshot.getValue(UserLocation::class.java)
                    val key = postSnapshot.key
                    if (value != null) {
                        if (userLocation.email != value.email) {
                            UsersDataSingleton.ARRAY_USERS.put(key, value)
                            UsersDataSingleton.arrayMarkers[key]?.remove()
                            if (value.isActive) {
                                UsersDataSingleton.arrayMarkers[key] = addMarker(value)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun addMarker(userLocation: UserLocation): Marker? = map.addMarker(MarkerOptions()
            .position(LatLng(userLocation.latitude, userLocation.longitude))
            .title(userLocation.name)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))

    private fun setData(latLng: LatLng, isActive: Boolean) {
        val databaseReference = databaseRef
        userLocation.isActive = isActive
        userLocation.latitude = latLng.latitude
        userLocation.longitude = latLng.longitude

        databaseReference.child("users").child(userLocation.hashCode().toString()).setValue(userLocation)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        } else {// Show rationale and request permission.
        }
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings?.isZoomControlsEnabled = true
    }

    private fun block() {
        setData(LatLng(userLocation.latitude, userLocation.longitude), false)
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        unregisterReceiver(gpsLocationReceiver)
    }

    private fun showGPSAlertDialog(context: Context) {
        AlertDialog.Builder((context as MapsActivity))
                .setMessage(getString(R.string.message_gps_turn_on))
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    finish()
                })
                .setCancelable(false)
                .show()
    }
}