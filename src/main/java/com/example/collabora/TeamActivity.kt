package com.example.collabora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class TeamActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val heartbeatHandler   = Handler(Looper.getMainLooper())
    private val HEARTBEAT_INTERVAL = 10_000L
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            writeLastActive(System.currentTimeMillis())
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL)
        }
    }

    private lateinit var tvTeamName: TextView
    private lateinit var tvInviteCode: TextView
    private lateinit var tvMemberCount: TextView
    private lateinit var layoutMembers: LinearLayout

    private var currentUserId = ""
    private var userRoleInTeam = "member"
    private var inviteCode = ""
    private var teamName = ""
    private var currentTeamId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_team)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        val sharedPref = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
        currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""

        tvTeamName = findViewById(R.id.tvTeamName)
        tvInviteCode = findViewById(R.id.tvInviteCode)
        tvMemberCount = findViewById(R.id.tvMemberCount)
        layoutMembers = findViewById(R.id.layoutMembers)

        val cardTeamChat = findViewById<CardView>(R.id.cardTeamChat)
        cardTeamChat.setOnClickListener {
            startActivity(Intent(this, TeamChatActivity::class.java))
        }

        findViewById<android.widget.ImageView>(R.id.btnShareInvite).setOnClickListener {
            shareInviteLink()
        }

        findViewById<CardView>(R.id.btnDestructiveAction).setOnClickListener {
            if (userRoleInTeam == "admin") {
                showDeleteTeamDialog()
            } else {
                showLeaveTeamDialog()
            }
        }

        loadTeamData()
        setupNavigation()
    }

    private fun shareInviteLink() {
        if (inviteCode.isEmpty()) return

        // Ubah ke HTTPS agar bisa diklik di WhatsApp/Telegram 😄🔥
        val inviteLink = "https://collabora.app/invite?code=$inviteCode"
        val shareText = "Ayo bergabung dengan tim $teamName di Collabora! Klik link ini untuk masuk: $inviteLink"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Undangan Tim")
        startActivity(shareIntent)
    }

    override fun onStart() {
        super.onStart()
        writeLastActive(System.currentTimeMillis())
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL)
    }

    override fun onStop() {
        super.onStop()
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        // writeLastActive(0L) // DIHAPUS agar dot hijau tetap menyala 😄🔥
    }

    private fun writeLastActive(time: Long) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("lastActive", time)
            .addOnFailureListener {
                db.collection("users").document(uid)
                    .set(mapOf("lastActive" to time), SetOptions.merge())
            }
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navCalendar).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
        }
        findViewById<CardView>(R.id.btnAddTask).setOnClickListener {
            startActivity(Intent(this, CreateTaskActivity::class.java))
        }
    }

    private fun loadTeamData() {
        if (currentTeamId.isEmpty()) return

        val btnDestructive = findViewById<CardView>(R.id.btnDestructiveAction)
        val tvDestructive  = findViewById<TextView>(R.id.tvDestructiveAction)

        db.collection("teams").document(currentTeamId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    teamName = document.getString("teamName") ?: "-"
                    inviteCode = document.getString("inviteCode") ?: ""
                    
                    tvTeamName.text = teamName
                    tvInviteCode.text = "Code: $inviteCode"
                }
            }

        db.collection("team_members")
            .whereEqualTo("teamId", currentTeamId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Gagal memuat anggota", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // CEK APAKAH KAMU DI-KICK 😄🔥
                    val isStillMember = snapshots.documents.any { it.getString("userId") == currentUserId }
                    if (!isStillMember) {
                        resetTeamAndGoToDashboard()
                        Toast.makeText(this, "Akses tim dicabut", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    layoutMembers.removeAllViews()
                    tvMemberCount.text = "${snapshots.size()} Anggota"

                    // Temukan role saya dulu di tim ini 😄🔥
                    userRoleInTeam = snapshots.documents.find { it.getString("userId") == currentUserId }
                        ?.getString("role") ?: "member"

                    // Update Tombol Keluar/Hapus Tim berdasarkan role 😄🔥
                    btnDestructive.visibility = android.view.View.VISIBLE
                    if (userRoleInTeam == "admin") {
                        tvDestructive.text = "Hapus Tim"
                    } else {
                        tvDestructive.text = "Keluar Tim"
                    }

                    val sortedDocs = snapshots.documents.sortedByDescending { it.getString("role") == "admin" }

                    for (doc in sortedDocs) {
                        val memberDocId = doc.id
                        val userId = doc.getString("userId") ?: "-"
                        val role = doc.getString("role") ?: "member"
                        
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { userDoc ->
                                val userName = userDoc.getString("name") ?: "Unknown User"
                                val photoUrl = userDoc.getString("photoUrl") ?: ""
                                
                                val memberItemView = layoutInflater.inflate(R.layout.item_member_manage, layoutMembers, false)
                                
                                val tvInitial = memberItemView.findViewById<TextView>(R.id.tvMemberInitial)
                                val ivPhoto = memberItemView.findViewById<ImageView>(R.id.ivMemberPhoto)
                                val tvName = memberItemView.findViewById<TextView>(R.id.tvMemberName)
                                val tvRole = memberItemView.findViewById<TextView>(R.id.tvMemberRole)
                                val btnManage = memberItemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnManageRole)
                                val btnKick = memberItemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnKickMember)
                                
                                if (photoUrl.isNotEmpty()) {
                                    tvInitial.visibility = View.GONE
                                    ivPhoto.visibility = View.VISIBLE
                                    Glide.with(this@TeamActivity)
                                        .load(photoUrl)
                                        .into(ivPhoto)
                                } else {
                                    tvInitial.visibility = View.VISIBLE
                                    ivPhoto.visibility = View.GONE
                                    tvInitial.text = userName.take(2).uppercase()
                                }

                                if (userId == currentUserId) {
                                    tvName.text = "$userName (Saya)"
                                } else {
                                    tvName.text = userName
                                }
                                tvRole.text = if (role == "admin") "👑 Admin" else "Member"
                                
                                // ACTION: Hanya admin yang bisa kelola anggota lain (bukan dirinya sendiri) 😄🔥
                                if (userRoleInTeam == "admin" && userId != currentUserId) {
                                    btnManage.visibility = android.view.View.VISIBLE
                                    btnManage.setOnClickListener {
                                        showChangeRoleDialog(memberDocId, userName, role)
                                    }

                                    // Kick Button: Hanya muncul jika yang dikelola adalah "member" 😄🔥
                                    if (role == "member") {
                                        btnKick.visibility = android.view.View.VISIBLE
                                        btnKick.setOnClickListener {
                                            showKickMemberDialog(memberDocId, userName)
                                        }
                                    } else {
                                        btnKick.visibility = android.view.View.GONE
                                    }

                                } else {
                                    btnManage.visibility = android.view.View.GONE
                                    btnKick.visibility   = android.view.View.GONE
                                }

                                layoutMembers.addView(memberItemView, if (role == "admin") 0 else -1)
                            }
                    }
                }
            }
    }

    private fun showKickMemberDialog(docId: String, name: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Keluarkan Anggota")
            .setMessage("Apakah kamu yakin ingin mengeluarkan $name dari tim?")
            .setPositiveButton("Ya, Keluarkan") { _, _ ->
                db.collection("team_members").document(docId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "$name telah dikeluarkan", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showLeaveTeamDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Keluar Tim")
            .setMessage("Apakah kamu yakin ingin keluar dari tim $teamName?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                leaveTeam()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun leaveTeam() {
        // Kita gunakan query agar lebih pasti (antisipasi ID dokumen lama) 😄🔥
        db.collection("team_members")
            .whereEqualTo("teamId", currentTeamId)
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    doc.reference.delete()
                }
                resetTeamAndGoToDashboard()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal keluar tim", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteTeamDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Hapus Tim")
            .setMessage("PERINGATAN: Menghapus tim akan mengeluarkan semua anggota dan menghapus semua data tim. Apakah kamu yakin?")
            .setPositiveButton("Ya, Hapus Permanen") { _, _ ->
                deleteTeam()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteTeam() {
        // 1. Hapus semua member
        db.collection("team_members").whereEqualTo("teamId", currentTeamId).get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    doc.reference.delete()
                }
                
                // 2. Hapus dokumen tim itu sendiri
                db.collection("teams").document(currentTeamId).delete()
                    .addOnSuccessListener {
                        resetTeamAndGoToDashboard()
                    }
            }
    }

    private fun resetTeamAndGoToDashboard() {
        val sharedPref = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("currentTeamId").commit()
        
        Toast.makeText(this, "Berhasil!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showChangeRoleDialog(docId: String, name: String, currentRole: String) {
        val newRole = if (currentRole == "admin") "member" else "admin"
        val actionText = if (newRole == "admin") "Jadikan Admin" else "Jadikan Member Biasa"

        android.app.AlertDialog.Builder(this)
            .setTitle("Kelola Peran")
            .setMessage("Apakah kamu sudah yakin ingin mengubah peran $name menjadi $newRole?")
            .setPositiveButton(actionText) { _, _ ->
                updateMemberRole(docId, newRole)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateMemberRole(docId: String, newRole: String) {
        db.collection("team_members").document(docId)
            .update("role", newRole)
            .addOnSuccessListener {
                Toast.makeText(this, "Peran berhasil diperbarui 🎉", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui peran", Toast.LENGTH_SHORT).show()
            }
    }
}
