package com.example.developer.locationshare

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var googleApiClient: GoogleApiClient
    val RC_SIGN_IN = 100
    private var auth: FirebaseAuth? = null
    lateinit var user: FirebaseUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val d = "1068796887191-1rokjkgmahn430683ice156ej0mao852.apps.googleusercontent.com"
        val reqIdtoke = resources.getString(R.string.default_web_client_id)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(reqIdtoke)
//                .requestServerAuthCode(d)
                .requestEmail()
                .build()
        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
        auth = FirebaseAuth.getInstance()

        var buttonGoogle = findViewById<View>(R.id.button_signGoogle).setOnClickListener(this)
        var buttonData = findViewById<Button>(R.id.button_data).setOnClickListener(this)
    }

    public override fun onStart() {
        super.onStart()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_signGoogle -> signInGoogle()
        }
    }

//region signInByEmailPass comment
//    private fun signInByEmailPass() {
//        var email = (findViewById<EditText>(R.id.editText_email)).text.toString()
//        var pass = (findViewById<EditText>(R.id.editText_pass)).text.toString()
//        auth?.signInWithEmailAndPassword(email, pass)
//                ?.addOnCompleteListener(this) { task ->
//                    if (task.isSuccessful) {
//                        user = auth!!.currentUser!!
//                    } else {
//                        Toast.makeText(this@MainActivity, "Authentication failed.",
//                                Toast.LENGTH_SHORT).show()
//                    }
//                }
//    }
// endregion

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
            intent.putExtra("display_name", acct?.displayName.toString())
            intent.putExtra("email", acct?.email.toString())
            intent.putExtra("token", acct?.idToken.toString())
            val token = acct?.idToken
            googleApiClient.clearDefaultAccountAndReconnect()

            startActivity(intent)
        } else {
            // Signed out, show unauthenticated UI.
            //   updateUI(false)
        }
    }
}
