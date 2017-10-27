package com.example.developer.locationshare

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat.checkSelfPermission
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
        val REQUEST_PERMISSION_CODE = 1
        val KEY_GOOGLE_SIGN_IN_ACCOUNT = "googleSignInAccount"
        val PROVIDERS_CHANGED_ACTION_FILTER = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private var googleSignInAccount: GoogleSignInAccount? = null

    private val mapUserIdMarker: HashMap<String, Marker> = hashMapOf()

    private var databaseRef = FirebaseDatabase.getInstance().reference.child("users")
    private lateinit var googleApiClient: GoogleApiClient
    private var firebaseAuth: FirebaseAuth? = null

    private var gpsLocationCallback = GpsLocationCallback(this)
    private var databaseValueListener = DatabaseListener(this)

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
        LeakCanary.install(application)

        initGoogleClient()
        if (savedInstanceState != null) {
            googleSignInAccount = (savedInstanceState.get(KEY_GOOGLE_SIGN_IN_ACCOUNT) as GoogleSignInAccount)
        }
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest().apply {
            interval = INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }

    override fun onResume() {
        super.onResume()

        registerReceiver(gpsLocationReceiver, PROVIDERS_CHANGED_ACTION_FILTER)

        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            val token = googleSignInAccount?.idToken
            if (token != null) {
                mapUserIdMarker.clear()

                authToDatabase(token)

                if (checkGpsStatus(this)) {
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, gpsLocationCallback, null)
                } else {
                    showGPSAlertDialog(this)
                }
            } else {
                signInGoogle()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_PERMISSION_CODE)
        }
    }


    override fun onPause() {
        super.onPause()

        unregisterReceiver(gpsLocationReceiver)

        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            if (googleSignInAccount != null) {
                databaseRef.removeEventListener(databaseValueListener)
                updateCurrentUserDataOnDatabase(isActive = false)
                fusedLocationProviderClient.removeLocationUpdates(gpsLocationCallback)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults.firstOrNull() != PERMISSION_GRANTED) {
                    finish()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun authToDatabase(token: String) {
        val cred: AuthCredential = GoogleAuthProvider.getCredential(token, null)

        FirebaseAuth.getInstance().signInWithCredential(cred).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                databaseRef.addValueEventListener(databaseValueListener)
            } else {
                Toast.makeText(this@MapsActivity, getString(R.string.error_auth_failed), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initGoogleClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun signInGoogle() {
        startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(googleApiClient), RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            handleSignInResult(Auth.GoogleSignInApi.getSignInResultFromIntent(data))
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            if (result.signInAccount != null) {
                googleSignInAccount = result.signInAccount
            }
        } else {
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_GOOGLE_SIGN_IN_ACCOUNT, googleSignInAccount)
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

    private fun addMarker(userLocation: UserLocation): Marker = map.addMarker(MarkerOptions()
            .position(LatLng(userLocation.latitude, userLocation.longitude))
            .title(userLocation.displayName)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))

    fun updateCurrentUserDataOnDatabase(latitude: Double? = null, longitude: Double? = null, isActive: Boolean) {
        if (latitude != null && longitude != null) {
            val googleSignInAccount = googleSignInAccount
            if (googleSignInAccount != null) {
                databaseRef.child(googleSignInAccount.id).setValue(UserLocation(googleSignInAccount.id.toString(),
                        googleSignInAccount.displayName.toString(),
                        googleSignInAccount.email.toString(),
                        latitude,
                        longitude,
                        isActive))
            }
        } else {
            databaseRef.child(googleSignInAccount?.id).child("active").setValue(isActive)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
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

    fun dataChanged(value: UserLocation) {
        if (googleSignInAccount?.email != value.email) {
            mapUserIdMarker[value.id]?.remove()
            if (value.isActive) {
                mapUserIdMarker[value.id] = addMarker(value)
            }
        }
    }
}