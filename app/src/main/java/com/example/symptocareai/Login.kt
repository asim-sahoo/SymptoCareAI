package com.example.symptocareai

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication instance
        auth = FirebaseAuth.getInstance()

        // Set up Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Configure Google client ID
            .requestEmail() // Request user's email
            .build()

        // Create GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set click listener for Google Sign-In button
        findViewById<Button>(R.id.googleSignInButton).setOnClickListener {
            signInGoogle()
        }
    }

    private fun signInGoogle() {
        // Check if the user is already signed in and clear cached account if needed
        val signedInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (signedInAccount != null) {
            googleSignInClient.signOut()
        }

        // Launch the Google Sign-In intent
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            }
        } else {
            // Display error message if sign-in task fails
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        // Create Google Auth credentials using the account's ID token
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        // Sign in to Firebase with the provided credentials
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                // If sign-in is successful, navigate to the Home activity and pass user data
                val intent: Intent = Intent(this, Home::class.java)
                intent.putExtra("email", account.email)
                intent.putExtra("name", account.displayName)
                startActivity(intent)
            } else {
                // Display error message if Firebase authentication fails
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
