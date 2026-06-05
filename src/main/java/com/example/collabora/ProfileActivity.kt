package com.example.collabora

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

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

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uploadImageToCloudinary(uri)
            }
        }

    private var profileListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        listenUserData()
        setupNavigation()

        findViewById<CardView>(R.id.cardProfilePhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<CardView>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // ─────────────────────────────────────────────
    // LISTEN USER DATA REALTIME
    // ─────────────────────────────────────────────
    private fun listenUserData() {
        val currentUser = auth.currentUser ?: return

        profileListener = db.collection("users")
            .document(currentUser.uid)
            .addSnapshotListener { document, _ ->

                if (document != null && document.exists()) {
                    val name     = document.getString("name")     ?: "User"
                    val email    = document.getString("email")    ?: "-"
                    val photoUrl = document.getString("photoUrl") ?: ""

                    findViewById<TextView>(R.id.tvProfileName).text  = name
                    findViewById<TextView>(R.id.tvProfileEmail).text = email

                    val tvInitial = findViewById<TextView>(R.id.tvProfileInitialLarge)
                    val ivPhoto   = findViewById<ImageView>(R.id.ivProfilePhoto)

                    if (photoUrl.isNotEmpty()) {
                        tvInitial.visibility = View.GONE
                        ivPhoto.visibility   = View.VISIBLE
                        Glide.with(this@ProfileActivity)
                            .load(photoUrl)
                            .into(ivPhoto)
                    } else {
                        tvInitial.visibility = View.VISIBLE
                        ivPhoto.visibility   = View.GONE
                        tvInitial.text = if (name.length >= 2) name.take(2).uppercase() else name.uppercase()
                    }
                }
            }
    }

    // ─────────────────────────────────────────────
    // UPLOAD KE CLOUDINARY
    // ─────────────────────────────────────────────
    private fun uploadImageToCloudinary(uri: android.net.Uri) {
        val cloudName = "dwnotrifi"
        val uploadPreset = "collabora_upload"

        // Kita gunakan OkHttpClient standar
        val client = OkHttpClient()

        try {
            val file = File(cacheDir, "temp_profile.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            runOnUiThread {
                Toast.makeText(this, "Mengunggah foto ke Cloudinary...", Toast.LENGTH_SHORT).show()
            }

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.e("CLOUDINARY", "Upload gagal: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Upload gagal: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()

                    Log.d("CLOUDINARY", "Code: ${response.code}")
                    Log.d("CLOUDINARY", "Body: $responseData")

                    if (response.isSuccessful && responseData != null) {
                        try {
                            val json = JSONObject(responseData)
                            val url = json.getString("secure_url")

                            Log.d("CLOUDINARY", "URL berhasil: $url")

                            savePhotoUrlToFirestore(url)

                        } catch (e: Exception) {
                            Log.e("CLOUDINARY", "Parse error: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(
                                    this@ProfileActivity,
                                    "Gagal baca response: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Log.e("CLOUDINARY", "Server error: ${response.code}")
                        runOnUiThread {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Server error: ${response.code}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            })

        } catch (e: Exception) {
            Log.e("CLOUDINARY", "Exception: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // SIMPAN URL KE FIRESTORE
    // Pakai .set() + merge() bukan .update()
    // ─────────────────────────────────────────────
    private fun savePhotoUrlToFirestore(url: String) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .set(mapOf("photoUrl" to url), SetOptions.merge())
            .addOnSuccessListener {
                Log.d("CLOUDINARY", "photoUrl tersimpan: $url")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Foto profil diperbarui! ✅",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("CLOUDINARY", "Gagal simpan Firestore: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Gagal simpan: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // ─────────────────────────────────────────────
    // HEARTBEAT
    // ─────────────────────────────────────────────
    override fun onStart() {
        super.onStart()
        writeLastActive(System.currentTimeMillis())
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL)
    }

    override fun onStop() {
        super.onStop()
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
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

    // ─────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────
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
        findViewById<LinearLayout>(R.id.navTeam).setOnClickListener {
            startActivity(Intent(this, TeamActivity::class.java))
            finish()
            overridePendingTransition(0, 0)
        }
        findViewById<CardView>(R.id.btnAddTask).setOnClickListener {
            startActivity(Intent(this, CreateTaskActivity::class.java))
        }
    }

    // ─────────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        profileListener?.remove()
    }
}