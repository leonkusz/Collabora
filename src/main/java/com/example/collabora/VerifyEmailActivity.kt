package com.example.collabora

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        auth = FirebaseAuth.getInstance()

        val btnOpenGmail = findViewById<MaterialButton>(R.id.btnOpenGmail)
        val btnAlreadyVerified = findViewById<MaterialButton>(R.id.btnAlreadyVerified)
        val btnResendEmail = findViewById<MaterialButton>(R.id.btnResendEmail)

        // 1. BUKA GMAIL
        btnOpenGmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://mail.google.com")
            startActivity(intent)
        }

        // 2. CEK APAKAH EMAIL SUDAH VERIFIED
        btnAlreadyVerified.setOnClickListener {
            val user = auth.currentUser

            if (user == null) {
                Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@setOnClickListener
            }

            // Ubah tombol jadi loading
            btnAlreadyVerified.isEnabled = false
            btnAlreadyVerified.text = "Mengecek..."

            // Reload data terbaru dari server Firebase
            user.reload().addOnCompleteListener { task ->
                // Kembalikan tombol ke semula
                btnAlreadyVerified.isEnabled = true
                btnAlreadyVerified.text = "Saya Sudah Verifikasi"

                if (task.isSuccessful) {
                    // Cek apakah sekarang sudah verified
                    if (user.isEmailVerified) {
                        val inviteCode = intent.getStringExtra("inviteCode")
                        Toast.makeText(this, "Email berhasil diverifikasi 🎉", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this, DashboardActivity::class.java)
                        if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)

                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Email belum diverifikasi. Cek Inbox/Spam!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Gagal mengecek: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. RESEND EMAIL VERIFICATION
        btnResendEmail.setOnClickListener {
            val user = auth.currentUser

            if (user == null) {
                Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnResendEmail.isEnabled = false
            btnResendEmail.text = "Mengirim ulang..."

            user.sendEmailVerification().addOnCompleteListener { task ->
                btnResendEmail.isEnabled = true
                btnResendEmail.text = "Kirim Ulang Email"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Email verifikasi berhasil dikirim ulang 📩", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal mengirim ulang: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}