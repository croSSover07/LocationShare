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

        val PROVIDERS_CHANGED_ACTION_FILTER = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    lateinit var map: GoogleMap
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationRequest: LocationRequest

    lateinit var userLocation: UserLocation
    //        TODO: Нейминг
    private var signInAccount: GoogleSignInAccount? = null
    //        TODO: Нейминг array users а хранит map user location
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
        // TODO: Зачем использовать метку? Зачем использовать this?
//        LeakCanary.install(this@MapsActivity.application)
        LeakCanary.install(application)

        initGoogleClient()
    }

    override fun onResume() {
        super.onResume()

//        TODO: Зачем создавать каждый раз новый объект фильтра.
//        registerReceiver(gpsLocationReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        registerReceiver(gpsLocationReceiver, PROVIDERS_CHANGED_ACTION_FILTER)

        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            if (signInAccount != null) {
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
//          TODO: Что за 1 ? Не нужно использовать литералы в коде
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 1)
        }
    }


    override fun onPause() {
        super.onPause()

        unregisterReceiver(gpsLocationReceiver)

        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {

            if (signInAccount != null) {
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
//          TODO: Что за 1 ? Не нужно использовать литералы в коде
            1 -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
//                } else {
//                    finish()
//                }
                // TODO: Упрощаем код.
                if (grantResults.firstOrNull() != PERMISSION_GRANTED) {
                    finish()
                }

//                return
            }
        // TODO: Стандартное поведение
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun authToDatabase(token: String) {
//      TODO: для чего проверки на not null ?
        val cred: AuthCredential = GoogleAuthProvider.getCredential(token, null)
//        FirebaseAuth.getInstance()?.signInWithCredential(cred)
//                ?.addOnCompleteListener(this) { task ->
//                    if (task.isSuccessful) {
//                        databaseRef.addValueEventListener(valueListener)
//                    } else {
//                        Toast.makeText(this@MapsActivity, getString(R.string.error_auth_failed), Toast.LENGTH_SHORT).show()
//                        finish()
//                    }
//                }
        FirebaseAuth.getInstance().signInWithCredential(cred).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                databaseRef.addValueEventListener(valueListener)
            } else {
                Toast.makeText(this@MapsActivity, getString(R.string.error_auth_failed), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    //  TODO: Пустая строка в начале метода
    private fun initGoogleClient() {

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                TODO: Лишний вызов resources
//                .requestIdToken(resources.getString(R.string.default_web_client_id))
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        auth = FirebaseAuth.getInstance()
    }

    //  TODO: Пустая строка в начале метода
    private fun initMapsValues() {
        //  TODO: Инициализировать карту не нужно при каждом вызове onResume
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)

        //  TODO: Инициализировать location request также можно один раз.
//        locationRequest = LocationRequest()
//        with(locationRequest) {
//            interval = INTERVAL
//            fastestInterval = FASTEST_INTERVAL
//            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
//        }
//        TODO: Более упрощенный код
        locationRequest = LocationRequest().apply {
            interval = INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

//        TODO: Можно инициализовароть 1 раз как late init var и использовать инстанс в дальнейшем не создавая новый
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initLocalUser() {
        arrayMarkers.clear()
        arrayUsers.clear()

//        TODO: Нейминг
//        val copy = signInAccount
//        if (copy != null) {
//            userLocation = UserLocation(
//                    copy.id.toString(),
//                    copy.displayName.toString(),
//                    copy.email.toString(),
//                    0.0,
//                    0.0,
//                    true)
//        }
        val account = signInAccount
        if (account != null) {
            userLocation = UserLocation(
                    account.id.toString(),
                    account.displayName.toString(),
                    account.email.toString(),
                    0.0,
                    0.0,
                    true)
        }
    }

    private fun connectToDatabase() {
        //  TODO: Так делать не стоит, если аккаунт будет null или же idToken аккаунта будет null вызов будет authToDatabase("null").
        authToDatabase(signInAccount?.idToken.toString())
    }

    //    TODO: Этот метод не нужно выполнять при каждом пересоздании активити. В onSaveInstanceState мы можем сохранить инстанс залогиненного юзера.
    private fun signInGoogle() {
//        TODO: Упрощаем код
//        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
//        startActivityForResult(signInIntent, RC_SIGN_IN)
        startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(googleApiClient), RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
//            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
//            handleSignInResult(result)

            // TODO: Упрощаем код.
            handleSignInResult(Auth.GoogleSignInApi.getSignInResultFromIntent(data))
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            if (result.signInAccount != null) {
                signInAccount = result.signInAccount
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

    //  TODO: Очень странный метод. Я посмотрел все его вызовы и там всегда создается LatLng из latitude и longitude что бы потом обратно разделить их
//  TODO: Раздели обновление параметров UserLocation и обновление записи в базе данных.
    fun setData(latLng: LatLng, isActive: Boolean) {
        userLocation.isActive = isActive
        userLocation.latitude = latLng.latitude
        userLocation.longitude = latLng.longitude

        // TODO: Я бы лучше использовал userLocation.id в качестве ключа вместо hashCode()
        databaseRef.child(userLocation.hashCode().toString()).setValue(userLocation)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            map.isMyLocationEnabled = true
//        }
        // TODO: бесполезное условие, достаточно просто присвоить переменной значение результата сравнения.
        // Для уменьшения кода, выполнены статические импорты метода checkSelfPermission и переменных ACCESS_FINE_LOCATION и PERMISSION_GRANTED
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
}