package com.example.symptocareai

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.webkit.WebView
import com.example.symptocareai.databinding.ActivityHomeBinding

class Home : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityHomeBinding
    private lateinit var webViewChat: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()

        // Initialize GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)

        // Find the WebView by its ID
        webViewChat = findViewById(R.id.webViewChat)
        webViewChat.settings.javaScriptEnabled = true

        // Load the HTML file containing the WebSocket chat
        webViewChat.loadUrl("file:///android_asset/websocket.html")

        // Sign out button
        findViewById<Button>(R.id.googleSignOutButton).setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut() // Also sign out from Google Sign-In

            // Clear the activity stack and start the Login activity
            val loginIntent = Intent(this, Login::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
        }
    }
}
