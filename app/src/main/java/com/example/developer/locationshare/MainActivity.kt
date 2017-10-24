package com.example.developer.locationshare

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.squareup.leakcanary.LeakCanary


class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private val RC_SIGN_IN = 100
    }

    private lateinit var googleApiClient: GoogleApiClient
    private var auth: FirebaseAuth? = null

    private lateinit var txtInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        LeakCanary.install(this@MainActivity.application)
        setContentView(R.layout.activity_main)
        initValues()

    }

    private fun initValues() {

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(resources.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        auth = FirebaseAuth.getInstance()

        findViewById<View>(R.id.button_signGoogle).setOnClickListener(this)
        txtInfo = findViewById(R.id.textView_info)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_signGoogle -> signInGoogle()
        }
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
            val acct = result.signInAccount
            val intent = Intent(this@MainActivity, MapsActivity::class.java)
                    .putExtra(MapsActivity.EXTRA_DISPLAY_NAME, acct?.displayName.toString())
                    .putExtra(MapsActivity.EXTRA_EMAIL, acct?.email.toString())
                    .putExtra(MapsActivity.EXTRA_TOKEN, acct?.idToken.toString())
            if (checkGpsStatus(this)) {
                startActivity(intent)
            } else {
                txtInfo.text = getString(R.string.error_message_gps_disabled)
            }

        } else {
            txtInfo.text = getString(R.string.error_message_to_continue)
        }
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
        R.id.menu_change_user -> {
            googleApiClient.clearDefaultAccountAndReconnect()
            signInGoogle()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
