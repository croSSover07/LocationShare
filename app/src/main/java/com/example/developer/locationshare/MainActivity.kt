package com.example.developer.locationshare

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private val RC_SIGN_IN = 100
    }

    private lateinit var googleApiClient: GoogleApiClient
    private var auth: FirebaseAuth? = null

    private lateinit var txtInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                    .putExtra("display_name", acct?.displayName.toString())
                    .putExtra("email", acct?.email.toString())
                    .putExtra("token", acct?.idToken.toString())
            startActivity(intent)
        } else {
            txtInfo.text = getString(R.string.message_to_continue)
        }
    }
}
