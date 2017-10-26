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
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.squareup.leakcanary.LeakCanary


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        val RC_SIGN_IN = 100
        val DEFAULT_ZOOM = 16f
        val INTERVAL = 10L
        val FASTEST_INTERVAL = 10L
    }

    lateinit var map: GoogleMap
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationRequest: LocationRequest

    lateinit var userLocation: UserLocation
    private var currentGoogleAcc: GoogleSignInAccount? = null
    val arrayUsers: HashMap<String, UserLocation> = hashMapOf()
    val arrayMarkers: HashMap<String, Marker?> = hashMapOf()

    private var databaseRef = FirebaseDatabase.getInstance().reference.child("users")
    private lateinit var googleApiClient: GoogleApiClient
    private var auth: FirebaseAuth? = null

    private var locationCallback = GpsLocationCallback(this)
    private var valueListener = DatabaseListener(this)

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
        registerReceiver(gpsLocationReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (currentGoogleAcc != null) {
                initLocalUser()
                connectToDatabase()
                if (checkGpsStatus(this)) {
                    initMapsValues()
                    fusedLocationProviderClient?.requestLocationUpdates(locationRequest, locationCallback, null)
                } else {
                    showGPSAlertDialog(this)
                }
            } else {
                signInGoogle()
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    1)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gpsLocationReceiver)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (currentGoogleAcc != null) {
                databaseRef.removeEventListener(valueListener)
                setData(LatLng(userLocation.latitude, userLocation.longitude), false)
                if (fusedLocationProviderClient != null) {
                    fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
                }
            }
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

    private fun authToDatabase(token: String) {
        val cred: AuthCredential = GoogleAuthProvider.getCredential(token, null)
        FirebaseAuth.getInstance()?.signInWithCredential(cred)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        databaseRef.addValueEventListener(valueListener)
                    } else {
                        Toast.makeText(this@MapsActivity, getString(R.string.error_auth_failed), Toast.LENGTH_SHORT).show()
                        finish()
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
    }

    private fun initLocalUser() {
        arrayMarkers.clear()
        arrayUsers.clear()
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
        } else {
            finish()
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

    fun addMarker(userLocation: UserLocation): Marker? = map.addMarker(MarkerOptions()
            .position(LatLng(userLocation.latitude, userLocation.longitude))
            .title(userLocation.name)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))

    fun setData(latLng: LatLng, isActive: Boolean) {
        userLocation.isActive = isActive
        userLocation.latitude = latLng.latitude
        userLocation.longitude = latLng.longitude

        databaseRef.child(userLocation.hashCode().toString()).setValue(userLocation)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings?.isZoomControlsEnabled = true
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