package com.example.collabora

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerSubTask: RecyclerView
    private lateinit var subTaskAdapter: SubTaskAdapter

    private val subTaskList = ArrayList<SubTask>()

    private val memberNames = ArrayList<String>()
    private val memberIds = ArrayList<String>()

    private var currentUserRole = "member"

    // GLOBAL VARIABLE 😄🔥
    private var currentUserId = ""
    private var currentTaskStatus = "Menunggu"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        db = FirebaseFirestore.getInstance()

        currentUserId =
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid ?: ""

        // Get initial status from intent
        currentTaskStatus = intent.getStringExtra("taskStatus") ?: "Menunggu"

        loadTeamMembers()
        checkUserRole()

        // ==============================
        // INIT VIEW
        // ==============================
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnComplete = findViewById<MaterialButton>(R.id.btnComplete)

        // STEP 1 😄🔥: INIT BTN DELETE
        val btnDeleteTask = findViewById<MaterialButton>(R.id.btnDeleteTask)

        val tvDetailTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailDesc = findViewById<TextView>(R.id.tvDetailDesc)

        recyclerSubTask = findViewById(R.id.recyclerSubTask)

        val btnAddSubTask = findViewById<MaterialButton>(R.id.btnAddSubTask)

        subTaskAdapter =
            SubTaskAdapter(
                subTaskList,
                currentTaskStatus == "Selesai",
                currentUserId,
                { subTask ->
                    val isAdmin = currentUserRole == "admin"
                    val isAssignedUser = subTask.assignedToId == currentUserId

                    if (isAdmin || isAssignedUser) {
                        updateSubTaskStatus(subTask)
                    } else {
                        Toast.makeText(this, "Kamu tidak punya akses", Toast.LENGTH_SHORT).show()
                    }
                },
                { subTask ->
                    // Callback untuk edit link lampiran subtask 😄🔥
                    showEditSubTaskLinkDialog(subTask)
                }
            )
        recyclerSubTask.layoutManager = LinearLayoutManager(this)
        recyclerSubTask.adapter = subTaskAdapter

        // LOAD REALTIME SUBTASK
        loadSubTasks()
        listenTaskRealtime()

        // ==============================
        // GET INTENT
        // ==============================
        val taskId = intent.getStringExtra("taskId")
        val title = intent.getStringExtra("taskTitle")
        val desc = intent.getStringExtra("taskDescription")
        val status = intent.getStringExtra("taskStatus")

        // ==============================
        // SET DATA
        // ==============================
        tvDetailTitle.text = title
        tvDetailDesc.text = desc

        // LOAD REALTIME SUBTASK
        loadSubTasks()
        listenTaskRealtime()

        // ==============================
        // BUTTON STATUS & UI DINAMIS
        // ==============================
        when (status) {
            "Menunggu" -> {
                btnComplete.text = "Kerjakan"
            }
            "Proses" -> {
                btnComplete.text = "Sudah Selesai"
            }
            "Selesai" -> {
                // BONUS 😄🔥: UPDATE UI KARENA DELETE PINDAH TOMBOL
                btnComplete.text = "Tugas Selesai 🎉"
                btnComplete.isEnabled = false
                btnComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#9CA3AF")
                )
            }
        }

        // ==============================
        // BACK BUTTON
        // ==============================
        btnBack.setOnClickListener {
            finish()
        }

        // ==============================
        // ADD SUBTASK BUTTON
        // ==============================
        btnAddSubTask.setOnClickListener {
            showAddSubTaskDialog()
        }

        // ==============================
        // STEP 4 😄🔥: CLICK LISTENER DELETE
        // ==============================
        btnDeleteTask.setOnClickListener {

            if (taskId == null) {

                Toast.makeText(
                    this,
                    "Task tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            db.collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Task berhasil dihapus 🗑️",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Gagal menghapus task",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // ==============================
        // LOGIKA KLIK TOMBOL (UPDATE ONLY)
        // ==============================
        btnComplete.setOnClickListener {
            if (taskId == null) {
                Toast.makeText(this, "Task tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gunakan currentTaskStatus agar dinamis 😄🔥
            val newStatus = if (currentTaskStatus == "Menunggu") "Proses" else "Selesai"

            db.collection("tasks").document(taskId)
                .update("status", newStatus)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        if (newStatus == "Proses") "Task mulai dikerjakan 🚀" else "Task berhasil diselesaikan 🎉",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Kita tidak finish() agar user bisa melihat perubahannya langsung di halaman detail
                    // Tapi jika user ingin kembali ke dashboard, dia bisa pakai tombol back
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal update task", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateAttachmentLinksUI(links: List<String>) {
        val layout = findViewById<android.widget.LinearLayout>(R.id.layoutAttachmentLinks)
        val btnAdd = findViewById<TextView>(R.id.tvAddAttachmentLink)
        layout.removeAllViews()

        if (links.isEmpty()) {
            val emptyTv = TextView(this)
            emptyTv.text = "Belum ada lampiran"
            emptyTv.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
            layout.addView(emptyTv)
        } else {
            for (link in links) {
                val linkTv = TextView(this)
                linkTv.text = "🔗 $link"
                linkTv.setTextColor(android.graphics.Color.parseColor("#1E9E75"))
                linkTv.setPadding(0, 8, 0, 8)
                linkTv.setOnClickListener {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                        startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Link tidak valid", Toast.LENGTH_SHORT).show()
                    }
                }
                layout.addView(linkTv)
            }
        }

        // Tampilkan tombol tambah hanya jika status belum Selesai 😄🔥
        if (currentTaskStatus != "Selesai") {
            btnAdd.visibility = View.VISIBLE
            btnAdd.setOnClickListener {
                showEditTaskLinksDialog(links)
            }
        } else {
            btnAdd.visibility = View.GONE
        }
    }

    private fun showEditTaskLinksDialog(currentLinks: List<String>) {
        val taskId = intent.getStringExtra("taskId") ?: return
        val editText = android.widget.EditText(this)
        editText.setText(currentLinks.joinToString(", "))
        editText.hint = "Masukkan link, pisahkan dengan koma"

        android.app.AlertDialog.Builder(this)
            .setTitle("Edit Link Lampiran")
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val input = editText.text.toString().trim()
                val newLinks = if (input.isNotEmpty()) {
                    input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }

                db.collection("tasks").document(taskId)
                    .update("attachmentLinks", newLinks)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Link berhasil diperbarui 🎉", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditSubTaskLinkDialog(subTask: SubTask) {
        val editText = android.widget.EditText(this)
        editText.setText(subTask.attachmentLink)
        editText.hint = "Masukkan link lampiran"

        android.app.AlertDialog.Builder(this)
            .setTitle("Edit Lampiran Sub Task")
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val newLink = editText.text.toString().trim()
                db.collection("sub_tasks").document(subTask.subTaskId)
                    .update("attachmentLink", newLink)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Lampiran sub task diperbarui 🎉", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun checkUserRole() {

        val sharedPref =
            getSharedPreferences(
                "CollaboraPrefs",
                MODE_PRIVATE
            )

        val currentTeamId =
            sharedPref.getString(
                "currentTeamId",
                ""
            ) ?: ""

        db.collection("team_members")
            .whereEqualTo(
                "teamId",
                currentTeamId
            )
            .whereEqualTo(
                "userId",
                currentUserId
            )
            .get()
            .addOnSuccessListener { snapshots ->

                if (!snapshots.isEmpty) {

                    val role =
                        snapshots.documents[0]
                            .getString("role")
                            ?: "member"

                    currentUserRole = role

                    // Update UI setelah role didapatkan 😄🔥
                    updateAdminButtonsVisibility()
                }
            }
    }

    private fun updateAdminButtonsVisibility() {
        val btnAddSubTask = findViewById<MaterialButton>(R.id.btnAddSubTask)
        val btnDeleteTask = findViewById<MaterialButton>(R.id.btnDeleteTask)

        if (currentUserRole == "admin") {
            if (currentTaskStatus == "Selesai") {
                btnAddSubTask.visibility = View.GONE
                btnDeleteTask.visibility = View.VISIBLE
            } else {
                btnAddSubTask.visibility = View.VISIBLE
                btnDeleteTask.visibility = View.VISIBLE
            }
        } else {
            btnAddSubTask.visibility = View.GONE
            btnDeleteTask.visibility = View.GONE
        }
    }

    // ==============================
    // DIALOG FUNCTION
    // ==============================
    private fun showAddSubTaskDialog() {

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // ==============================
        // INPUT SUB TASK
        // ==============================
        val editText = android.widget.EditText(this)
        editText.hint = "Contoh: Buat Home Screen"
        layout.addView(editText)

        // ==============================
        // DROPDOWN MEMBER
        // ==============================
        val spinner = android.widget.Spinner(this)
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            memberNames
        )
        spinner.adapter = adapter
        layout.addView(spinner)

        // ==============================
        // DIALOG
        // ==============================
        android.app.AlertDialog.Builder(this)
            .setTitle("Tambah Sub Task")
            .setView(layout)
            .setPositiveButton("Tambah") { _, _ ->

                val subTaskText = editText.text.toString().trim()

                if (subTaskText.isEmpty()) {
                    Toast.makeText(this, "Nama sub task tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedPosition = spinner.selectedItemPosition

                if (memberIds.isEmpty() || selectedPosition == -1) {
                    Toast.makeText(this, "Anggota team belum dimuat atau tidak ada", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val assignedToId = memberIds[selectedPosition]
                val assignedToName = memberNames[selectedPosition]

                val taskId = intent.getStringExtra("taskId") ?: ""
                val sharedPref = getSharedPreferences("CollaboraPrefs", MODE_PRIVATE)
                val currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""
                val subTaskId = java.util.UUID.randomUUID().toString()

                val subTaskData = hashMapOf(
                    "subTaskId" to subTaskId,
                    "taskId" to taskId,
                    "teamId" to currentTeamId,
                    "subTaskText" to subTaskText,
                    "assignedToId" to assignedToId,
                    "assignedToName" to assignedToName,
                    "status" to "Menunggu",
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("sub_tasks")
                    .document(subTaskId)
                    .set(subTaskData)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Sub task berhasil ditambahkan 🎉",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Gagal menambahkan sub task",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ==============================
    // LOAD TEAM MEMBERS
    // ==============================
    private fun loadTeamMembers() {

        val sharedPref = getSharedPreferences("CollaboraPrefs", MODE_PRIVATE)
        val currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""

        db.collection("team_members")
            .whereEqualTo("teamId", currentTeamId)
            .get()
            .addOnSuccessListener { snapshots ->

                memberNames.clear()
                memberIds.clear()

                for (document in snapshots.documents) {

                    val userId = document.getString("userId") ?: ""

                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val userName = userDoc.getString("name") ?: "Unknown User"

                            memberIds.add(userId)
                            memberNames.add(userName)
                        }
                }
            }
    }

    // ==============================
    // LOAD SUBTASKS
    // ==============================
    private fun loadSubTasks() {

        val taskId = intent.getStringExtra("taskId") ?: ""

        db.collection("sub_tasks")
            .whereEqualTo("taskId", taskId)
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Gagal memuat sub task",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val tvEmptySubTask = findViewById<TextView>(R.id.tvEmptySubTask)

                    val btnComplete =
                        findViewById<MaterialButton>(
                            R.id.btnComplete
                        )

                    subTaskList.clear()

                    for (document in snapshots.documents) {

                        val subTask = document.toObject(SubTask::class.java)

                        if (subTask != null) {
                            subTaskList.add(subTask)
                        }
                    }

                    subTaskAdapter.notifyDataSetChanged()

                    if (subTaskList.isNotEmpty()) {

                        btnComplete.visibility =
                            View.GONE

                    } else {

                        btnComplete.visibility =
                            View.VISIBLE
                    }

                    if (subTaskList.isEmpty()) {
                        tvEmptySubTask.visibility = View.VISIBLE
                    } else {
                        tvEmptySubTask.visibility = View.GONE
                    }
                }
            }
    }

    private fun listenTaskRealtime() {

        val taskId =
            intent.getStringExtra(
                "taskId"
            ) ?: ""

        val btnComplete =
            findViewById<MaterialButton>(
                R.id.btnComplete
            )

        db.collection("tasks")
            .document(taskId)
            .addSnapshotListener { snapshot, error ->

                if (error != null ||
                    snapshot == null ||
                    !snapshot.exists()
                ) {
                    return@addSnapshotListener
                }

                val status =
                    snapshot.getString(
                        "status"
                    ) ?: "Menunggu"
                
                currentTaskStatus = status
                
                // DATA ATTACHMENT 😄🔥
                val attachmentLinks = snapshot.get("attachmentLinks") as? List<String> ?: emptyList()
                updateAttachmentLinksUI(attachmentLinks)

                // LOCK ADAPTER 😄🔥: Kunci semua sub-tugas jika tugas utama Selesai
                subTaskAdapter.isTaskLocked = (status == "Selesai")
                subTaskAdapter.notifyDataSetChanged()

                when (status) {

                    "Menunggu" -> {
                        btnComplete.text = "Kerjakan"
                        btnComplete.isEnabled = true
                        btnComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#1E9E75")
                        )
                    }

                    "Proses" -> {
                        btnComplete.text = "Sudah Selesai"
                        btnComplete.isEnabled = true
                        btnComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#1E9E75")
                        )
                    }

                    "Selesai" -> {
                        btnComplete.text = "Tugas Selesai 🎉"
                        btnComplete.isEnabled = false
                        btnComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#9CA3AF")
                        )
                    }
                }

                // Update visibility tombol admin berdasarkan role DAN status 😄🔥
                updateAdminButtonsVisibility()
            }
    }

    private fun updateSubTaskStatus(
        subTask: SubTask
    ) {

        val newStatus = subTask.status

        db.collection("sub_tasks")
            .document(subTask.subTaskId)
            .update(
                "status",
                newStatus
            )
            .addOnSuccessListener {

                checkTaskCompletion()

                Toast.makeText(
                    this,
                    "Status updated → $newStatus",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Gagal update status",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun checkTaskCompletion() {

        val taskId =
            intent.getStringExtra(
                "taskId"
            ) ?: ""

        db.collection("sub_tasks")
            .whereEqualTo(
                "taskId",
                taskId
            )
            .get()
            .addOnSuccessListener { snapshots ->

                if (snapshots.isEmpty) return@addOnSuccessListener

                var totalSubTask = 0
                var completedSubTask = 0

                for (document in snapshots.documents) {

                    totalSubTask++

                    val status =
                        document.getString(
                            "status"
                        ) ?: "Menunggu"

                    if (status == "Selesai") {

                        completedSubTask++
                    }
                }

                val newTaskStatus =
                    when {

                        completedSubTask == 0 ->
                            "Menunggu"

                        completedSubTask ==
                                totalSubTask ->
                            "Selesai"

                        else ->
                            "Proses"
                    }

                db.collection("tasks")
                    .document(taskId)
                    .update(
                        "status",
                        newTaskStatus
                    )
            }
    }
}