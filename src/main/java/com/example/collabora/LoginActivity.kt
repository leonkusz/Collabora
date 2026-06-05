package com.example.collabora

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        sharedPreferences =
            getSharedPreferences("CollaboraSession", MODE_PRIVATE)

        val etEmail =
            findViewById<TextInputEditText>(R.id.etEmail)

        val etPassword =
            findViewById<TextInputEditText>(R.id.etPassword)

        val tvRegisterLink =
            findViewById<TextView>(R.id.tvRegisterLink)

        val btnLogin =
            findViewById<MaterialButton>(R.id.btnLogin)

        // PINDAH REGISTER
        tvRegisterLink.setOnClickListener {

            val intent =
                Intent(this, RegisterActivity::class.java)

            startActivity(intent)
        }

        // LOGIN FIREBASE
        btnLogin.setOnClickListener {

            val email =
                etEmail.text.toString().trim()

            val password =
                etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    this,
                    "Email dan password harus diisi",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Kunci tombol agar tidak dobel klik 😄🔥
            btnLogin.isEnabled = false
            btnLogin.text = "Masuk..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful()) {
                        val user = auth.currentUser
                        val inviteCode = intent.getStringExtra("inviteCode")

                        if (user != null) {
                            if (user.isEmailVerified) {
                                // JIKA SUDAH VERIFIKASI -> KE DASHBOARD
                                Toast.makeText(this, "Login berhasil 🎉", Toast.LENGTH_SHORT).show()

                                // SET WAKTU AKTIF PERTAMA 😄🔥
                                val sessionPref = getSharedPreferences("CollaboraSession", Context.MODE_PRIVATE)
                                sessionPref.edit().putLong("last_active_time", System.currentTimeMillis()).apply()

                                val intent = Intent(this, DashboardActivity::class.java)
                                if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                // JIKA BELUM VERIFIKASI -> KE HALAMAN VERIFIKASI
                                btnLogin.isEnabled = true
                                btnLogin.text = "Masuk"

                                Toast.makeText(this, "Email belum diverifikasi. Silakan cek inbox Anda 📩", Toast.LENGTH_LONG).show()

                                val intent = Intent(this, VerifyEmailActivity::class.java)
                                if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)
                                startActivity(intent)
                            }
                        }
                    } else {
                        // KEMBALIKAN TOMBOL JIKA GAGAL 😄🔥
                        btnLogin.isEnabled = true
                        btnLogin.text = "Masuk"

                        val errorMessage = task.exception?.message ?: "Email atau password salah"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
