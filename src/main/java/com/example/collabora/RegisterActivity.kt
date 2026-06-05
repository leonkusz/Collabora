package com.example.collabora

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Reset error dari validasi sebelumnya
            tilConfirmPassword.error = null

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                tilConfirmPassword.error = "* Password minimal 8 karakter"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                tilConfirmPassword.error = "* Password harus sama"
                return@setOnClickListener
            }

            // CATCH INVITE CODE 😄🔥
            val inviteCode = intent.getStringExtra("inviteCode")

            // Kunci tombol agar tidak di-spam klik
            btnRegister.isEnabled = false
            btnRegister.text = "Mendaftarkan..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val userData = hashMapOf(
                                "userId" to user.uid,
                                "name" to name,
                                "email" to email,
                                "lastActive" to System.currentTimeMillis(),
                                "createdAt" to Timestamp.now()
                            )

                            // Simpan ke Firestore
                            db.collection("users")
                                .document(user.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    // Jika berhasil simpan data, kirim email verifikasi
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { verifyTask ->
                                            btnRegister.isEnabled = true
                                            btnRegister.text = "Daftar"

                                            if (verifyTask.isSuccessful) {
                                                Toast.makeText(this, "Email verifikasi telah dikirim 📩", Toast.LENGTH_LONG).show()

                                                // ==========================================
                                                // BARIS auth.signOut() SUDAH DIHAPUS DI SINI 😄🔥
                                                // Sesi login dipertahankan agar VerifyEmailActivity tidak null!
                                                // ==========================================

                                                val intent = Intent(this, VerifyEmailActivity::class.java)
                                                if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                Toast.makeText(this, "Gagal mengirim verifikasi: ${verifyTask.exception?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    // INI PENYELAMATNYA 😄🔥
                                    // Jika gagal simpan ke database (karena koneksi/rules), kembalikan tombol
                                    btnRegister.isEnabled = true
                                    btnRegister.text = "Daftar"
                                    Toast.makeText(this, "Gagal menyimpan data akun: ${e.message}", Toast.LENGTH_LONG).show()

                                    // Hapus auth user yang terlanjur terbuat agar tidak nyangkut
                                    user.delete()
                                }
                        }
                    } else {
                        // Jika dari awal auth sudah gagal (misal email sudah terdaftar)
                        btnRegister.isEnabled = true
                        btnRegister.text = "Daftar"
                        Toast.makeText(this, "Register gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}