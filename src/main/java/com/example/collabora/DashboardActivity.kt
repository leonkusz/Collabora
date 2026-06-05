package com.example.collabora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    private lateinit var chipSemua: TextView
    private lateinit var chipMenunggu: TextView
    private lateinit var chipProses: TextView
    private lateinit var chipSelesai: TextView

    private val taskList         = ArrayList<Task>()
    private val filteredTaskList = ArrayList<Task>()
    private var currentFilter    = "Semua"

    private var doubleBackToExitPressedOnce = false

    private var tvProfileInitial: TextView? = null
    private var ivProfilePhoto: ImageView? = null
    private var tvGreetingName: TextView? = null
    private var tvDashboardStats: TextView? = null
    private var tvHighlightTitle: TextView? = null
    private var tvHighlightProgress: TextView? = null
    private var tvHighlightDeadline: TextView? = null

    // ─────────────────────────────────────────────
    // HEARTBEAT SYSTEM 😄🔥
    // ─────────────────────────────────────────────
    private val heartbeatHandler   = Handler(Looper.getMainLooper())
    private val HEARTBEAT_INTERVAL = 10_000L // 10 detik sekali update
    private val ONLINE_THRESHOLD   = 25_000L // 25 detik tanpa update = offline

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            writeLastActive(System.currentTimeMillis())
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL)
        }
    }

    // ─────────────────────────────────────────────
    // LISTENER REGISTRATION 😄🔥
    // ─────────────────────────────────────────────
    private var teamMembersListener : ListenerRegistration? = null
    private var tasksListener       : ListenerRegistration? = null
    private var statsListener       : ListenerRegistration? = null
    private var profileListener     : ListenerRegistration? = null
    private val memberListeners     = mutableMapOf<String, ListenerRegistration>()
    private val memberDataMap       = mutableMapOf<String, MemberData>()

    data class MemberData(
        val name: String,
        val initial: String,
        val online: Boolean,
        val userId: String,
        val photoUrl: String = ""
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        db.collection("teams")
            .whereEqualTo("ownerId", currentUser.uid)
            .get()
            .addOnSuccessListener { ownerDocuments ->
                if (!ownerDocuments.isEmpty) {
                    val teamId = ownerDocuments.documents[0].getString("teamId") ?: ""
                    saveCurrentTeamId(teamId)

                    setContentView(R.layout.activity_dashboard)
                    setupMainDashboard()
                    
                    // HANDLE DEEP LINK INVITE 😄🔥
                    handleInviteCode()
                } else {
                    db.collection("team_members")
                        .whereEqualTo("userId", currentUser.uid)
                        .get()
                        .addOnSuccessListener { memberDocuments ->
                            if (!memberDocuments.isEmpty) {
                                val teamId = memberDocuments.documents[0].getString("teamId") ?: ""
                                saveCurrentTeamId(teamId)

                                setContentView(R.layout.activity_dashboard)
                                setupMainDashboard()
                            } else {
                                setContentView(R.layout.activity_dashboard_empty)
                                setupEmptyDashboard()
                            }
                            
                            // HANDLE DEEP LINK INVITE 😄🔥
                            handleInviteCode()
                        }
                }
            }
    }

    private fun handleInviteCode() {
        val inviteCode = intent.getStringExtra("inviteCode") ?: return
        
        // Cari tim berdasarkan kode ini
        db.collection("teams").whereEqualTo("inviteCode", inviteCode).get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    val teamDoc = snapshots.documents[0]
                    val teamId = teamDoc.getString("teamId") ?: ""
                    val teamName = teamDoc.getString("teamName") ?: "Tim"
                    
                    showJoinConfirmationDialog(teamId, teamName)
                }
            }
    }

    private fun showJoinConfirmationDialog(teamId: String, teamName: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Undangan Tim")
            .setMessage("Apakah Anda ingin bergabung dengan Tim $teamName?")
            .setPositiveButton("Ya, Gabung") { _, _ ->
                joinTeam(teamId)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun joinTeam(teamId: String) {
        val currentUser = auth.currentUser ?: return
        
        val memberData = hashMapOf(
            "teamId" to teamId,
            "userId" to currentUser.uid,
            "role" to "member",
            "joinedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("team_members")
            .document("${teamId}_${currentUser.uid}") // ID Konsisten 😄🔥
            .set(memberData)
            .addOnSuccessListener {
                saveCurrentTeamId(teamId)
                Toast.makeText(this, "Berhasil bergabung ke tim! 🎉", Toast.LENGTH_SHORT).show()
                
                // Refresh activity agar UI dashboard utama muncul
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }

    private fun saveCurrentTeamId(teamId: String) {
        val sharedPref = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("currentTeamId", teamId).commit()
    }

    // ─────────────────────────────────────────────
    // onStart — Jalankan Heartbeat Kembali 😄🔥
    // ─────────────────────────────────────────────
    override fun onStart() {
        super.onStart()
        writeLastActive(System.currentTimeMillis())
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL)
    }

    // ─────────────────────────────────────────────
    // onStop — Pertahankan Status Saat Ganti Page 😄🔥
    // ─────────────────────────────────────────────
    override fun onStop() {
        super.onStop()
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        
        // JANGAN set 0L di sini agar saat ganti page dot hijau tidak mati!
        // Dot akan mati otomatis jika tidak ada heartbeat selama 25 detik (user keluar app)
        
        // SIMPAN WAKTU TERAKHIR AKTIF 😄🔥
        val sharedPref = getSharedPreferences("CollaboraSession", Context.MODE_PRIVATE)
        sharedPref.edit().putLong("last_active_time", System.currentTimeMillis()).apply()
    }

    // ─────────────────────────────────────────────
    // TULIS KELUAR AMAN — Cabut Semua Listener Saat App Close
    // ─────────────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        teamMembersListener?.remove().also { teamMembersListener = null }
        tasksListener?.remove().also       { tasksListener = null }
        statsListener?.remove().also       { statsListener = null }
        profileListener?.remove().also     { profileListener = null }
        // Cabut semua listener member realtime
        memberListeners.values.forEach { it.remove() }
        memberListeners.clear()
    }

    // ─────────────────────────────────────────────
    // TULIS LAST ACTIVE KE FIRESTORE 😄🔥
    // ─────────────────────────────────────────────
    private fun writeLastActive(time: Long) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("lastActive", time)
            .addOnFailureListener {
                db.collection("users").document(uid)
                    .set(mapOf("lastActive" to time), SetOptions.merge())
            }
    }

    // ─────────────────────────────────────────────
    // CEK ONLINE STATUS 😄🔥
    // ─────────────────────────────────────────────
    private fun isUserOnline(lastActive: Long): Boolean {
        val diff   = System.currentTimeMillis() - lastActive
        return diff < ONLINE_THRESHOLD
    }

    // ─────────────────────────────────────────────
    // EMPTY DASHBOARD SETUP
    // ─────────────────────────────────────────────
    private fun setupEmptyDashboard() {
        val btnCreateTeam = findViewById<MaterialButton>(R.id.btnCreateTeam)
        val btnJoinTeam   = findViewById<MaterialButton>(R.id.btnJoinTeam)
        val btnAddTask    = findViewById<CardView>(R.id.btnAddTask)

        tvGreetingName   = findViewById(R.id.tvGreetingName)
        tvProfileInitial = findViewById(R.id.tvProfileInitial)
        ivProfilePhoto   = findViewById(R.id.ivProfilePhoto)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "User"
                    val photoUrl = doc.getString("photoUrl") ?: ""

                    tvGreetingName?.text = "Halo, $name 👋"

                    if (photoUrl.isNotEmpty()) {
                        tvProfileInitial?.visibility = View.GONE
                        ivProfilePhoto?.visibility = View.VISIBLE
                        ivProfilePhoto?.let { Glide.with(this@DashboardActivity).load(photoUrl).into(it) }
                    } else {
                        tvProfileInitial?.visibility = View.VISIBLE
                        ivProfilePhoto?.visibility = View.GONE
                        tvProfileInitial?.text = if (name.length >= 2) name.take(2).uppercase() else name.uppercase()
                    }
                }
        }

        btnCreateTeam.setOnClickListener {
            startActivity(Intent(this, CreateTeamActivity::class.java))
        }
        btnJoinTeam.setOnClickListener {
            startActivity(Intent(this, JoinTeamActivity::class.java))
        }
        btnAddTask.setOnClickListener {
            Toast.makeText(this, "Buat atau join team terlebih dahulu 😄", Toast.LENGTH_SHORT).show()
        }

        // NAVBAR LOCK 😄🔥
        findViewById<LinearLayout>(R.id.navCalendar).setOnClickListener {
            Toast.makeText(this, "Anda belum create/join tim 😄", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.navTeam).setOnClickListener {
            Toast.makeText(this, "Anda belum create/join tim 😄", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Anda belum create/join tim 😄", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────
    // MAIN DASHBOARD SETUP 😄🔥
    // ─────────────────────────────────────────────
    private fun setupMainDashboard() {
        val btnAddTask = findViewById<CardView>(R.id.btnAddTask)

        tvProfileInitial    = findViewById(R.id.tvProfileInitial)
        ivProfilePhoto      = findViewById(R.id.ivProfilePhoto)
        tvGreetingName      = findViewById(R.id.tvGreetingName)
        tvDashboardStats    = findViewById(R.id.tvDashboardStats)
        tvHighlightTitle    = findViewById(R.id.tvHighlightTitle)
        tvHighlightProgress = findViewById(R.id.tvHighlightProgress)
        tvHighlightDeadline = findViewById(R.id.tvHighlightDeadline)

        chipSemua    = findViewById(R.id.chipSemua)
        chipMenunggu = findViewById(R.id.chipMenunggu)
        chipProses   = findViewById(R.id.chipProses)
        chipSelesai  = findViewById(R.id.chipSelesai)

        recyclerTasks = findViewById(R.id.recyclerTasks)

        taskAdapter = TaskAdapter(filteredTaskList) { task ->
            val intent = Intent(this, TaskDetailActivity::class.java).apply {
                putExtra("taskId",          task.taskId)
                putExtra("taskTitle",       task.title)
                putExtra("taskDescription", task.description)
                putExtra("taskStatus",      task.status)
                putExtra("taskPriority",    task.priority)
                putExtra("taskDeadline",    task.deadline)
                // Hapus putExtra attachmentLink karena sekarang realtime list 😄🔥
            }
            startActivity(intent)
        }

        recyclerTasks.layoutManager = LinearLayoutManager(this)
        recyclerTasks.adapter       = taskAdapter

        // Jalankan semua penangkap data Firestore
        loadTasks()
        loadDashboardRealtime()
        loadTeamMembersRealtime()

        chipSemua.setOnClickListener    { currentFilter = "Semua";    filterTasks() }
        chipMenunggu.setOnClickListener { currentFilter = "Menunggu"; filterTasks() }
        chipProses.setOnClickListener   { currentFilter = "Proses";   filterTasks() }
        chipSelesai.setOnClickListener  { currentFilter = "Selesai";  filterTasks() }

        btnAddTask.setOnClickListener {
            startActivity(Intent(this, CreateTaskActivity::class.java))
        }

        setupNavigation()
    }

    // ─────────────────────────────────────────────
    // LOAD TEAM MEMBERS REALTIME 😄🔥
    // ─────────────────────────────────────────────
    private fun loadTeamMembersRealtime() {
        val sharedPref    = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
        val currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""
        val layoutTeamMembers = findViewById<LinearLayout>(R.id.layoutTeamMembers)

        if (currentTeamId.isEmpty()) return

        teamMembersListener = db.collection("team_members")
            .whereEqualTo("teamId", currentTeamId)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val newUserIds = snapshots.documents.mapNotNull { it.getString("userId") }

                // CEK APAKAH KAMU DI-KICK 😄🔥
                val myUid = auth.currentUser?.uid
                if (myUid != null && myUid !in newUserIds) {
                    // KAMU DI-KICK ATAU TIM DIHAPUS! 😱
                    val pref = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
                    pref.edit().remove("currentTeamId").commit()
                    
                    Toast.makeText(this, "Akses tim dicabut (di-kick atau tim dihapus)", Toast.LENGTH_LONG).show()
                    
                    // Refresh Activity agar masuk ke Dashboard Empty
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return@addSnapshotListener
                }

                // 1. Hapus listener untuk user yang sudah tidak di tim
                val iterator = memberListeners.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.key !in newUserIds) {
                        entry.value.remove()
                        iterator.remove()
                        memberDataMap.remove(entry.key)
                    }
                }

                // 2. Tambah listener untuk user baru
                for (userId in newUserIds) {
                    if (!memberListeners.containsKey(userId)) {
                        val listener = db.collection("users").document(userId)
                            .addSnapshotListener { userDoc, _ ->
                                if (userDoc != null && userDoc.exists()) {
                                    val name       = userDoc.getString("name") ?: "User"
                                    val initial    = name.take(2).uppercase()
                                    val lastActive = userDoc.getLong("lastActive") ?: 0L
                                    val online     = isUserOnline(lastActive)
                                    val photoUrl   = userDoc.getString("photoUrl") ?: ""

                                    memberDataMap[userId] = MemberData(name, initial, online, userId, photoUrl)
                                    runOnUiThread { renderTeamMembers(layoutTeamMembers) }
                                }
                            }
                        if (listener != null) memberListeners[userId] = listener
                    }
                }
            }
    }

    private fun renderTeamMembers(container: LinearLayout) {
        container.removeAllViews()
        // Urutkan agar yang online di depan
        val sortedMembers = memberDataMap.values.sortedWith(
            compareByDescending<MemberData> { it.online }.thenBy { it.name }
        )

        val myUid = auth.currentUser?.uid

        for (member in sortedMembers) {
            val memberView = layoutInflater.inflate(R.layout.item_team_member, container, false)
            val tvInitial = memberView.findViewById<TextView>(R.id.tvMemberInitial)
            val ivPhoto = memberView.findViewById<ImageView>(R.id.ivMemberPhoto)
            
            if (member.photoUrl.isNotEmpty()) {
                tvInitial.visibility = View.GONE
                ivPhoto.visibility = View.VISIBLE
                Glide.with(this@DashboardActivity)
                    .load(member.photoUrl)
                    .into(ivPhoto)
            } else {
                tvInitial.visibility = View.VISIBLE
                ivPhoto.visibility = View.GONE
                tvInitial.text = member.initial
            }

            val tvName = memberView.findViewById<TextView>(R.id.tvMemberName)
            if (member.userId == myUid) {
                tvName.text = "Saya"
            } else {
                tvName.text = member.name
            }

            val dot = memberView.findViewById<View>(R.id.viewOnlineDot)
            dot.visibility = if (member.online) View.VISIBLE else View.GONE
            container.addView(memberView)
        }
    }


    // ─────────────────────────────────────────────
    // LOAD TASKS REALTIME 😄🔥
    // ─────────────────────────────────────────────
    private fun loadTasks() {
        val sharedPref    = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
        val currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""

        tasksListener = db.collection("tasks")
            .whereEqualTo("teamId", currentTeamId)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                taskList.clear()
                for (document in snapshots) {
                    taskList.add(document.toObject(Task::class.java))
                }
                filterTasks()
            }
    }

    // ─────────────────────────────────────────────
    // LOAD DASHBOARD STATS + HIGHLIGHT TASK 😄🔥
    // ─────────────────────────────────────────────
    private fun loadDashboardRealtime() {
        val currentUser   = auth.currentUser ?: return
        val sharedPref    = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
        val currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""

        profileListener = db.collection("users").document(currentUser.uid)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val photoUrl = document.getString("photoUrl") ?: ""

                    tvGreetingName?.text = "Halo, $name 👋"

                    if (photoUrl.isNotEmpty()) {
                        tvProfileInitial?.visibility = View.GONE
                        ivProfilePhoto?.visibility = View.VISIBLE
                        ivProfilePhoto?.let { Glide.with(this@DashboardActivity).load(photoUrl).into(it) }
                    } else {
                        tvProfileInitial?.visibility = View.VISIBLE
                        ivProfilePhoto?.visibility = View.GONE
                        tvProfileInitial?.text = if (name.length >= 2) name.take(2).uppercase() else name.uppercase()
                    }
                }
            }

        statsListener = db.collection("tasks")
            .whereEqualTo("teamId", currentTeamId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val totalTasks  = snapshots.size()
                var activeCount = 0

                for (document in snapshots.documents) {
                    if (document.getString("status") != "Selesai") activeCount++
                }

                tvDashboardStats?.text = "$totalTasks task · $activeCount sedang berjalan"

                if (snapshots.documents.isNotEmpty()) {
                    val latestTask = snapshots.documents[0]
                    val title      = latestTask.getString("title")    ?: "-"
                    val deadline   = latestTask.getString("deadline")  ?: "-"
                    val taskId     = latestTask.getString("taskId")    ?: ""

                    tvHighlightTitle?.text    = title
                    tvHighlightDeadline?.text = "⏰ Deadline $deadline"
                    tvHighlightDeadline?.visibility = View.VISIBLE

                    if (taskId.isNotEmpty()) {
                        db.collection("sub_tasks")
                            .whereEqualTo("taskId", taskId)
                            .addSnapshotListener { subSnaps, _ ->
                                if (subSnaps == null) {
                                    tvHighlightProgress?.text = "0%"
                                    return@addSnapshotListener
                                }

                                val totalSub = subSnaps.size()
                                if (totalSub == 0) {
                                    val s = latestTask.getString("status") ?: "Menunggu"
                                    tvHighlightProgress?.text = if (s == "Selesai") "100%" else "0%"
                                    tvHighlightProgress?.visibility = View.VISIBLE
                                    return@addSnapshotListener
                                }

                                var done = 0
                                for (subDoc in subSnaps.documents) {
                                    if (subDoc.getString("status") == "Selesai") done++
                                }

                                val pct = (done.toDouble() / totalSub) * 100
                                tvHighlightProgress?.text = "${pct.toInt()}%"
                                tvHighlightProgress?.visibility = View.VISIBLE
                            }
                    } else {
                        tvHighlightProgress?.text = "0%"
                    }
                } else {
                    // JIKA TIDAK ADA TUGAS 😄🔥
                    tvHighlightTitle?.text = "TIDAK ADA TUGAS SAAT INI"
                    tvHighlightDeadline?.visibility = View.GONE
                    tvHighlightProgress?.visibility = View.GONE
                }
            }
    }

    // ─────────────────────────────────────────────
    // FILTER TASKS
    // ─────────────────────────────────────────────
    private fun filterTasks() {
        filteredTaskList.clear()
        if (currentFilter == "Semua") {
            filteredTaskList.addAll(taskList)
        } else {
            filteredTaskList.addAll(taskList.filter { it.status == currentFilter })
        }
        updateChipUI()
        taskAdapter.notifyDataSetChanged()
    }

    private fun updateChipUI() {
        val chips        = listOf(chipSemua, chipMenunggu, chipProses, chipSelesai)
        val activeCard   = android.graphics.Color.parseColor("#1E9E75")
        val inactiveCard = android.graphics.Color.parseColor("#FFFFFF")
        val activeText   = android.graphics.Color.parseColor("#FFFFFF")
        val inactiveText = android.graphics.Color.parseColor("#6B9480")

        for (chip in chips) {
            val parent = chip.parent as CardView
            if (chip.text.toString() == currentFilter) {
                parent.setCardBackgroundColor(activeCard)
                chip.setTextColor(activeText)
            } else {
                parent.setCardBackgroundColor(inactiveCard)
                chip.setTextColor(inactiveText)
            }
        }
    }

    // ─────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────
    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.navCalendar).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navTeam).setOnClickListener {
            startActivity(Intent(this, TeamActivity::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }

    // ─────────────────────────────────────────────
    // DOUBLE BACK EXIT
    // ─────────────────────────────────────────────
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Tekan tombol kembali sekali lagi untuk keluar aplikasi", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }
}
