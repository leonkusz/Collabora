package com.example.collabora

import android.content.Context // STEP 1 😄🔥
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class CreateTeamActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_team)

        // =========================
        // FIREBASE
        // =========================

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // =========================
        // COMPONENT
        // =========================

        val etTeamName =
            findViewById<TextInputEditText>(R.id.etTeamName)

        val etTeamDescription =
            findViewById<TextInputEditText>(R.id.etTeamDescription)

        val btnCreateTeam =
            findViewById<MaterialButton>(R.id.btnCreateTeam)

        val btnBack =
            findViewById<CardView>(R.id.btnBack)

        // =========================
        // BACK BUTTON
        // =========================

        btnBack.setOnClickListener {
            finish()
        }

        // =========================
        // CREATE TEAM
        // =========================

        btnCreateTeam.setOnClickListener {

            val teamName =
                etTeamName.text.toString().trim()

            val description =
                etTeamDescription.text.toString().trim()

            // VALIDATION

            if (teamName.isEmpty()) {

                Toast.makeText(
                    this,
                    "Nama team harus diisi",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // GET CURRENT USER

            val currentUser =
                auth.currentUser

            if (currentUser == null) {

                Toast.makeText(
                    this,
                    "User tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // GENERATE INVITE CODE

            val inviteCode =
                "COLL-" + Random.nextInt(1000, 9999)

            // DATA TEAM
            val teamId = db.collection("teams").document().id

            val teamData = hashMapOf(
                "teamId" to teamId, // SIMPAN ID ASLI 😄🔥
                "teamName" to teamName,
                "description" to description,
                "inviteCode" to inviteCode,
                "ownerId" to currentUser.uid,
                "createdAt" to Timestamp.now()
            )

            // SAVE TO FIRESTORE
            db.collection("teams")
                .document(teamId) // Pakai ID yang sudah kita buat agar sinkron
                .set(teamData)
                .addOnSuccessListener {

                    // =========================
                    // AUTO ADD OWNER TO TEAM MEMBERS 😄🔥
                    // =========================

                    val memberData = hashMapOf(
                        "teamId" to teamId,
                        "userId" to currentUser.uid,
                        "role" to "admin",
                        "joinedAt" to Timestamp.now()
                    )

                    db.collection("team_members")
                        .document("${teamId}_${currentUser.uid}") // ID Unik member
                        .set(memberData)

                    // =========================
                    // SAVE CURRENT TEAM ID
                    // =========================

                    val sharedPref =
                        getSharedPreferences(
                            "CollaboraPrefs",
                            Context.MODE_PRIVATE
                        )

                    sharedPref.edit()
                        .putString(
                            "currentTeamId",
                            teamId
                        )
                        .apply()

                    Toast.makeText(
                        this,
                        "Team berhasil dibuat 🎉",
                        Toast.LENGTH_SHORT
                    ).show()

                    // PINDAH KE DASHBOARD

                    val intent =
                        Intent(this, DashboardActivity::class.java)

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    finish()

                }
                .addOnFailureListener { e ->

                    Toast.makeText(
                        this,
                        "Gagal membuat team: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}