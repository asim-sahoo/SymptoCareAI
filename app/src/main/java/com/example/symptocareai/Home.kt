package com.example.symptocareai

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import androidx.recyclerview.widget.LinearLayoutManager
import com.example.symptocareai.databinding.ActivityHomeBinding
import org.json.JSONException
import org.json.JSONObject
import android.widget.EditText
import org.json.JSONArray

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

import okhttp3.*

class Home : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var messageList: MutableList<Message>
    private lateinit var messageAdapter: MessageAdapter
    private val client = OkHttpClient()
    private lateinit var binding: ActivityHomeBinding

    // WebSocket related variables
    private lateinit var webSocket: WebSocket
    private val webSocketUrl = "wss://public.backend.medisearch.io:443/ws/medichat/api"
    private var api_key: String = "API_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()

        // Initialize GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)

        messageList = mutableListOf()

        // Setup recycler view
        messageAdapter = MessageAdapter(messageList)
        binding.recyclerView.adapter = messageAdapter
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        binding.recyclerView.layoutManager = llm

        val messageEditText: EditText = findViewById(R.id.message_edit_text)

        binding.sendBtn.setOnClickListener {
            val userQuery = messageEditText.text.toString().trim()
            if (userQuery.isNotEmpty()) {
                // Display user's query in the right_chat_view
                addToChat(userQuery, Message.SENT_BY_ME)
                messageEditText.text.clear()

                // Send user's query to the WebSocket
                sendWebSocketMessage(userQuery)
            }
        }

        // Sign out button
        findViewById<Button>(R.id.googleSignOutButton).setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut() // Also sign out from Google Sign-In

            // Clear activity stack and start Login activity
            val loginIntent = Intent(this, Login::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
        }

        // Initialize WebSocket
        val request = Request.Builder().url(webSocketUrl).build()
        webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // WebSocket connection is established, you can send your initial data here if needed
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Handle incoming WebSocket messages here
                try {
                    val jsonData = JSONObject(text)

                    if (jsonData.getString("event") == "articles") {
                        val articlesArray = jsonData.getJSONArray("articles")

                        // Create a StringBuilder to store the article information
                        val articleInfo = StringBuilder()

                        // Iterate through the articles
                        for (i in 0 until articlesArray.length()) {
                            val articleObject = articlesArray.getJSONObject(i)
                            val title = articleObject.getString("title")
                            val url = articleObject.getString("url")

                            // Append the article information to the StringBuilder
                            articleInfo.append("Title: $title\n")
                            articleInfo.append("URL: $url\n\n")
                        }

                        runOnUiThread {
                            addToChat("Articles:\n$articleInfo", Message.SENT_BY_BOT)
                        }
                    } else if (jsonData.getString("event") == "llm_response") {
                        val response = jsonData.getString("message")
                        runOnUiThread {
                            addToChat("LLM Response:\n$response", Message.SENT_BY_BOT)
                        }
                    } else if (jsonData.getString("event") == "error") {
                        runOnUiThread {
                            addToChat("Got error", Message.SENT_BY_BOT)
                        }
                    }

//                    runOnUiThread {
//                        addToChat(jsonData.toString(), Message.SENT_BY_BOT)
//                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }


        })
    }

    private fun sendWebSocketMessage(message: String) {
        // Construct your WebSocket message here and send it
        val apiSettings = JSONObject()
        apiSettings.put("language", "English")

        val apiChatContent = JSONObject()
        apiChatContent.put("event", "user_message")
        apiChatContent.put("conversation", JSONArray().put(message))
        apiChatContent.put("settings", apiSettings)
        apiChatContent.put("key", api_key)
        apiChatContent.put("id", generateID())

        webSocket.send(apiChatContent.toString())
    }

    private fun addToChat(message: String, sentBy: String) {
        runOnUiThread {
            if (sentBy == Message.SENT_BY_ME) {
                messageList.add(Message(message, sentBy))
                messageAdapter.notifyItemInserted(messageList.size - 1)
                binding.recyclerView.smoothScrollToPosition(messageList.size - 1)
            } else {
                // Display responses in the left_chat_view
                messageList.add(Message(message, sentBy))
                messageAdapter.notifyItemInserted(messageList.size - 1)
                binding.recyclerView.smoothScrollToPosition(messageList.size - 1)
            }
        }
    }

    fun generateID(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val id = StringBuilder()

        for (i in 0 until 32) {
            val randomIndex = (characters.indices).random()
            id.append(characters[randomIndex])
        }

        return id.toString()
    }

    private fun addResponse(response: String) {
        addToChat(response, Message.SENT_BY_BOT)
    }
}