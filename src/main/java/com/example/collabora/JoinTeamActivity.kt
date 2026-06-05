package com.example.collabora

import android.content.Context
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

class JoinTeamActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_team)

        // ====================================
        // FIREBASE
        // ====================================

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ====================================
        // COMPONENT
        // ====================================

        val etInviteCode =
            findViewById<TextInputEditText>(R.id.etInviteCode)

        val btnJoinTeam =
            findViewById<MaterialButton>(R.id.btnJoinTeam)

        val btnBack =
            findViewById<CardView>(R.id.btnBack)

        // ====================================
        // BACK BUTTON
        // ====================================

        btnBack.setOnClickListener {
            finish()
        }

        // ====================================
        // JOIN TEAM
        // ====================================

        btnJoinTeam.setOnClickListener {

            val inviteCode =
                etInviteCode.text.toString()
                    .trim()
                    .uppercase()

            // VALIDASI
            if (inviteCode.isEmpty()) {

                Toast.makeText(
                    this,
                    "Invite code harus diisi",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

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

            // ====================================
            // CARI TEAM BERDASARKAN INVITE CODE
            // ====================================

            db.collection("teams")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .addOnSuccessListener { documents ->

                    // TEAM TIDAK DITEMUKAN
                    if (documents.isEmpty) {

                        Toast.makeText(
                            this,
                            "Invite code tidak valid",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@addOnSuccessListener
                    }

                    // TEAM DITEMUKAN
                    val teamDocument =
                        documents.documents[0]

                    val teamId =
                        teamDocument.id

                    // ====================================
                    // DUPLICATE CHECK 😄🔥
                    // ====================================

                    db.collection("team_members")
                        .whereEqualTo("teamId", teamId)
                        .whereEqualTo("userId", currentUser.uid)
                        .get()
                        .addOnSuccessListener { existingMember ->

                            // =========================
                            // USER SUDAH ADA DI TEAM
                            // =========================

                            if (!existingMember.isEmpty) {

                                Toast.makeText(
                                    this,
                                    "Kamu sudah bergabung di team ini",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@addOnSuccessListener
                            }

                            // =========================
                            // USER BELUM ADA
                            // LANJUT SAVE MEMBER
                            // =========================

                            val memberData = hashMapOf(

                                "teamId" to teamId,

                                "userId" to currentUser.uid,

                                "role" to "member",

                                "joinedAt" to Timestamp.now()
                            )

                            // ====================================
                            // SIMPAN MEMBER KE FIRESTORE
                            // ====================================

                            db.collection("team_members")
                                .document("${teamId}_${currentUser.uid}") // ID Konsisten 😄🔥
                                .set(memberData)
                                .addOnSuccessListener {

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
                                        "Berhasil join team 🎉",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // ====================================
                                    // PINDAH KE DASHBOARD
                                    // ====================================

                                    val intent =
                                        Intent(
                                            this,
                                            DashboardActivity::class.java
                                        )

                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                                    startActivity(intent)
                                    finish()
                                }

                                .addOnFailureListener { e ->

                                    Toast.makeText(
                                        this,
                                        "Gagal join team: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                }

                .addOnFailureListener { e ->

                    Toast.makeText(
                        this,
                        "Terjadi kesalahan: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}