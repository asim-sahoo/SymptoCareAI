package com.example.symptocareai

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.concurrent.TimeUnit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.FitnessOptions

class Home : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var fitnessOptions: FitnessOptions
    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        val email = intent.getStringExtra("email")
        val displayName = intent.getStringExtra("name")

        findViewById<TextView>(R.id.textView).text = email + "\n" + displayName

        // Initialize fitnessOptions
        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .build()

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        val hasPermissions = GoogleSignIn.hasPermissions(account, fitnessOptions)

        if (!hasPermissions) {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        } else {
            // Permission already granted, read step count data
            readStepCountData(account)
        }

        // Sign out button
        findViewById<Button>(R.id.googleSignOutButton).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, Login::class.java))
        }
    }

    // Handle permission result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Permission granted, read step count data
                val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
                readStepCountData(account)
            } else {
                // Permission denied
            }
        }
    }

    // Function to read step count data
    private fun readStepCountData(account: GoogleSignInAccount) {
        val historyClient = Fitness.getHistoryClient(this, account)

        val currentTimeMillis = System.currentTimeMillis()
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000 // 7 days in milliseconds

        val startTime = currentTimeMillis - sevenDaysInMillis
        val endTime = currentTimeMillis

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .bucketByTime(1, TimeUnit.DAYS)  // You can change the bucketing interval
            .build()

        historyClient
            .readData(readRequest)
            .addOnSuccessListener { response ->
                // Process the aggregated fitness data
                if (response.buckets.isNotEmpty()) {
                    for (bucket in response.buckets) {
                        for (dataSet in bucket.dataSets) {
                            for (dataPoint in dataSet.dataPoints) {
                                val stepCount = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                                displayStepCount(stepCount) // Display step count in TextView
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure
            }
    }

    // Display step count in TextView
    private fun displayStepCount(stepCount: Int) {
        val stepCountTextView = findViewById<TextView>(R.id.stepCountValueTextView)
        stepCountTextView.text = "Step Count: $stepCount"
    }
}
