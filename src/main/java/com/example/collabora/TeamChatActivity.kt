package com.example.collabora

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeamChatActivity : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: CardView
    private lateinit var btnBack: TextView

    private lateinit var layoutEmptyChat: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var currentTeamId = ""

    private lateinit var chatAdapter: ChatAdapter

    private val chatList =
        ArrayList<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_chat)

        // =========================
        // FIREBASE & PREFS INIT 😄🔥
        // =========================

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val sharedPref =
            getSharedPreferences(
                "CollaboraPrefs",
                Context.MODE_PRIVATE
            )

        currentTeamId =
            sharedPref.getString(
                "currentTeamId",
                ""
            ) ?: ""

        // =========================
        // INIT VIEW
        // =========================

        recyclerChat =
            findViewById(R.id.recyclerChat)

        etMessage =
            findViewById(R.id.etMessage)

        btnSend =
            findViewById(R.id.btnSend)

        btnBack =
            findViewById(R.id.btnBack)

        layoutEmptyChat =
            findViewById(R.id.layoutEmptyChat)

        // =========================
        // BACK BUTTON
        // =========================

        btnBack.setOnClickListener {
            finish()
        }

        // =========================
        // CHAT ADAPTER 😄🔥
        // =========================

        // STEP 1 😄🔥: UPDATE CHAT ADAPTER INIT
        chatAdapter =
            ChatAdapter(chatList) { chat ->

                showDeleteDialog(chat)

            }


        recyclerChat.layoutManager =
            LinearLayoutManager(this)

        recyclerChat.adapter =
            chatAdapter

        // =========================
        // PANGGIL REALTIME CHAT 😄🔥
        // =========================

        loadRealtimeChat()
        loadTeamName()

        // =========================
        // SEND BUTTON
        // =========================

        btnSend.setOnClickListener {

            val message =
                etMessage.text.toString().trim()

            if (message.isEmpty()) {
                return@setOnClickListener
            }

            val currentUser =
                auth.currentUser
                    ?: return@setOnClickListener

            // =========================
            // GET USER NAME
            // =========================

            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { userDocument ->

                    val userName =
                        userDocument.getString("name")
                            ?: "Unknown"

                    // =========================
                    // CHAT DATA
                    // =========================

                    val chatData =
                        hashMapOf(

                            "message" to message,

                            "senderId" to currentUser.uid,

                            "senderName" to userName,

                            "teamId" to currentTeamId,

                            "timestamp" to System.currentTimeMillis()
                        )

                    // =========================
                    // SAVE TO FIRESTORE
                    // =========================

                    db.collection("team_chats")
                        .add(chatData)

                    etMessage.text.clear()
                }
        }
    }

    // =========================
    // REALTIME CHAT FUNCTION 😄🔥
    // =========================

    private fun loadRealtimeChat() {

        db.collection("team_chats")
            .whereEqualTo(
                "teamId",
                currentTeamId
            )
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null) {

                    chatList.clear()

                    for (document in snapshots.documents) {

                        val chat =
                            document.toObject(
                                ChatMessage::class.java
                            )

                        if (chat != null) {

                            val updatedChat =
                                chat.copy(
                                    messageId = document.id
                                )

                            chatList.add(updatedChat)
                        }
                    }

                    chatAdapter.notifyDataSetChanged()

                    if (chatList.isEmpty()) {

                        layoutEmptyChat.visibility =
                            android.view.View.VISIBLE

                    } else {

                        layoutEmptyChat.visibility =
                            android.view.View.GONE
                    }

                    recyclerChat.scrollToPosition(
                        chatList.size - 1
                    )
                }
            }
    }

    // =========================
    // STEP 2 😄🔥: BUAT DELETE DIALOG
    // =========================

    private fun showDeleteDialog(
        chat: ChatMessage
    ) {

        android.app.AlertDialog.Builder(this)
            .setTitle("Hapus Pesan")
            .setMessage(
                "Yakin ingin menghapus pesan ini?"
            )

            .setPositiveButton("Hapus") { _, _ ->

                deleteMessage(chat)
            }

            .setNegativeButton("Batal", null)

            .show()
    }

    // Fungsi kosong sementara supaya Android Studio tidak error merah 😄🔥
    private fun deleteMessage(
        chat: ChatMessage
    ) {

        db.collection("team_chats")
            .document(chat.messageId)

            .update(

                mapOf(

                    "message" to
                            "Pesan telah dihapus"
                )
            )
    }

    private fun loadTeamName() {
        if (currentTeamId.isEmpty()) return

        db.collection("teams").document(currentTeamId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val teamName = document.getString("teamName") ?: "-"
                    findViewById<TextView>(R.id.tvTeamName).text = teamName
                }
            }
    }
}