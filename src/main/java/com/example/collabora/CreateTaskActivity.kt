package com.example.collabora

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context // STEP 1 😄🔥
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class CreateTaskActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    // ==============================
    // PENTING 😄🔥: GLOBAL VARIABLE
    // ==============================
    private var selectedCalendar: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        db = FirebaseFirestore.getInstance()

        // ==============================
        // INIT VIEW
        // ==============================

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val etTaskTitle = findViewById<TextInputEditText>(R.id.etTaskTitle)
        val etTaskDesc = findViewById<TextInputEditText>(R.id.etTaskDesc)
        val etDeadline = findViewById<TextInputEditText>(R.id.etDeadline)
        val etAttachmentLink = findViewById<TextInputEditText>(R.id.etAttachmentLink)
        val radioPriority = findViewById<RadioGroup>(R.id.radioPriority)
        val btnSaveTask = findViewById<MaterialButton>(R.id.btnSaveTask)

        // ==============================
        // BACK BUTTON
        // ==============================

        btnBack.setOnClickListener {
            finish()
        }

        // ==============================
        // DATE & TIME PICKER (DENGAN VALIDASI MASA LALU)
        // ==============================

        etDeadline.setOnClickListener {

            val calendar = Calendar.getInstance()

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->

                    val timePickerDialog = TimePickerDialog(
                        this,
                        { _, selectedHour, selectedMinute ->

                            // Assign nilai ke Global Variable
                            selectedCalendar = Calendar.getInstance()

                            selectedCalendar?.set(
                                selectedYear,
                                selectedMonth,
                                selectedDay,
                                selectedHour,
                                selectedMinute
                            )

                            // ==============================
                            // CEK APAKAH WAKTU YANG DIPILIH < WAKTU SEKARANG
                            // ==============================
                            if (selectedCalendar!!.timeInMillis < System.currentTimeMillis()) {

                                // Jika waktu masa lalu, tolak input dan munculkan notifikasi
                                Toast.makeText(
                                    this,
                                    "Input tanggal dan waktu yang benar",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Reset global variable karena input tidak valid
                                selectedCalendar = null
                                etDeadline.setText("")

                            } else {

                                // Jika waktu valid, format dan tampilkan di EditText
                                val dateFormat = SimpleDateFormat(
                                    "dd MMMM yyyy • HH:mm",
                                    Locale("id", "ID")
                                )

                                etDeadline.setText(
                                    dateFormat.format(selectedCalendar!!.time)
                                )
                            }
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    )

                    timePickerDialog.show()
                },
                year,
                month,
                day
            )

            // LAPIS KEAMANAN 1: Disable tanggal sebelum hari ini di UI Kalender
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()

            datePickerDialog.show()
        }

        // ==============================
        // SAVE TASK
        // ==============================

        btnSaveTask.setOnClickListener {

            val title = etTaskTitle.text.toString().trim()
            val description = etTaskDesc.text.toString().trim()
            val deadline = etDeadline.text.toString().trim()
            val linksInput = etAttachmentLink.text.toString().trim()

            // Konversi string ke list, pisahkan pakai koma 😄🔥
            val attachmentLinks = if (linksInput.isNotEmpty()) {
                linksInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            // VALIDASI
            if (title.isEmpty() || description.isEmpty() || deadline.isEmpty() || selectedCalendar == null) {
                Toast.makeText(
                    this,
                    "Semua data tugas harus diisi dengan benar",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // PRIORITAS
            val selectedPriorityId = radioPriority.checkedRadioButtonId

            if (selectedPriorityId == -1) {
                Toast.makeText(
                    this,
                    "Pilih prioritas tugas",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val selectedRadioButton = findViewById<RadioButton>(selectedPriorityId)
            val priority = selectedRadioButton.text.toString()

            // TASK ID
            val taskId = UUID.randomUUID().toString()

            // STEP 2 😄🔥: GET CURRENT TEAM ID
            val sharedPref = getSharedPreferences("CollaboraPrefs", Context.MODE_PRIVATE)
            val currentTeamId = sharedPref.getString("currentTeamId", "") ?: ""

            // DATA TASK
            val taskData = hashMapOf(
                "taskId" to taskId,
                "teamId" to currentTeamId,
                "title" to title,
                "description" to description,
                "deadline" to deadline,
                "attachmentLinks" to attachmentLinks, // PLURAL 😄🔥
                "priority" to priority,
                "status" to "Menunggu",
                "createdAt" to System.currentTimeMillis()
            )

            // SAVE FIREBASE
            db.collection("tasks")
                .document(taskId)
                .set(taskData)
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Tugas berhasil dibuat 🎉",
                        Toast.LENGTH_SHORT
                    ).show()

                    // ==============================
                    // SCHEDULE REMINDER 😄🔥
                    // ==============================
                    val reminderIntent = Intent(this, ReminderReceiver::class.java)
                    reminderIntent.putExtra("taskTitle", title)

                    val pendingIntent = PendingIntent.getBroadcast(
                        this,
                        System.currentTimeMillis().toInt(),
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

                    // Reminder 1 menit sebelum deadline 😄🔥
                    val reminderTime = selectedCalendar!!.timeInMillis - (60 * 1000)

                    if (reminderTime > System.currentTimeMillis()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    }

                    // Kembali ke Dashboard
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }

                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Gagal menyimpan tugas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}