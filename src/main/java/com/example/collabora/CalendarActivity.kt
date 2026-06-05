package com.example.collabora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

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

    private lateinit var tvMonthYear: TextView
    private lateinit var recyclerCalendar: RecyclerView
    private lateinit var recyclerDayTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    
    private val calendar = Calendar.getInstance()
    private val taskList = ArrayList<Task>()
    private val dayTaskList = ArrayList<Task>()
    private val deadlineDates = HashSet<String>()
    private var selectedDay = ""
    
    private var kickListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_calendar)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()
        
        listenForKick()

        tvMonthYear = findViewById(R.id.tvMonthYear)
        recyclerCalendar = findViewById(R.id.recyclerCalendar)
        recyclerDayTasks = findViewById(R.id.recyclerDayTasks)

        // Set default selected day to today
        selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()

        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            selectedDay = "" // Reset selection when month changes
            updateCalendar()
        }

        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            selectedDay = "" // Reset selection when month changes
            updateCalendar()
        }

        setupRecyclerViews()
        loadTasks()
        setupNavigation()
    }

    private fun setupRecyclerViews() {
        recyclerCalendar.layoutManager = GridLayoutManager(this, 7)
        
        taskAdapter = TaskAdapter(dayTaskList) { task ->
            val intent = Intent(this, TaskDetailActivity::class.java).apply {
                putExtra("taskId",          task.taskId)
                putExtra("taskTitle",       task.title)
                putExtra("taskDescription", task.description)
                putExtra("taskStatus",      task.status)
                putExtra("taskPriority",    task.priority)
                putExtra("taskDeadline",    task.deadline)
                // Hapus attachmentLink tunggal 😄🔥
            }
            startActivity(intent)
        }
        recyclerDayTasks.layoutManager = LinearLayoutManager(this)
        recyclerDayTasks.adapter = taskAdapter
    }

    private fun loadTasks() {
        val sharedPref = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
        val currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""

        db.collection("tasks")
            .whereEqualTo("teamId", currentTeamId)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    taskList.clear()
                    deadlineDates.clear()
                    
                    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                    
                    for (doc in snapshots) {
                        val task = doc.toObject(Task::class.java)
                        taskList.add(task)
                        
                        try {
                            val dateStr = task.deadline.split("•")[0].trim()
                            val date = sdf.parse(dateStr)
                            if (date != null) {
                                val cal = Calendar.getInstance()
                                cal.time = date
                                val d = cal.get(Calendar.DAY_OF_MONTH)
                                val m = cal.get(Calendar.MONTH)
                                val y = cal.get(Calendar.YEAR)
                                deadlineDates.add("$d-$m-$y")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    updateCalendar()
                }
            }
    }

    private fun updateCalendar() {
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvMonthYear.text = monthYearFormat.format(calendar.time)

        val days = ArrayList<String>()
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val maxDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until firstDayOfWeek) {
            days.add("")
        }

        val currentMonthDeadlines = HashSet<String>()
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        for (i in 1..maxDayOfMonth) {
            days.add(i.toString())
            if (deadlineDates.contains("$i-$month-$year")) {
                currentMonthDeadlines.add(i.toString())
            }
        }

        recyclerCalendar.adapter = CalendarAdapter(days, currentMonthDeadlines, selectedDay) { day ->
            selectedDay = day
            showTasksForDate(day)
        }
        
        if (selectedDay.isNotEmpty()) {
            showTasksForDate(selectedDay)
        }
    }

    private fun showTasksForDate(day: String) {
        dayTaskList.clear()
        if (day.isEmpty()) {
            taskAdapter.notifyDataSetChanged()
            return
        }

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, day.toInt())
        val targetDateStr = sdf.format(cal.time)
        
        dayTaskList.addAll(taskList.filter { it.deadline.startsWith(targetDateStr) })
        taskAdapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        writeLastActive(System.currentTimeMillis())
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL)
    }

    override fun onStop() {
        super.onStop()
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        // writeLastActive(0L) // DIHAPUS agar dot hijau tidak mati saat ganti page 😄🔥
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

    private fun listenForKick() {
        val uid = auth.currentUser?.uid ?: return
        val sharedPref = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
        val currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""
        
        if (currentTeamId.isEmpty()) return

        kickListener = db.collection("team_members")
            .whereEqualTo("teamId", currentTeamId)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val isStillMember = snapshots.documents.any { it.getString("userId") == uid }
                    if (!isStillMember) {
                        // KICKED!! 😄🔥
                        sharedPref.edit().remove("currentTeamId").commit()
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        kickListener?.remove()
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navTeam).setOnClickListener {
            startActivity(Intent(this, TeamActivity::class.java))
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
}
