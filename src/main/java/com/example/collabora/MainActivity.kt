package com.example.collabora

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Sembunyikan Action Bar untuk Splash Screen yang bersih
        supportActionBar?.hide()
        
        // Tampilkan layout Splash Screen
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        
        sharedPreferences = getSharedPreferences("CollaboraSession", MODE_PRIVATE)

        // Delay 2 detik untuk efek Splash Screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 2000)
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        
        // CATCH DEEP LINK 😄🔥
        val inviteCode = intent.data?.getQueryParameter("code")
        
        val lastActiveTime = sharedPreferences.getLong("last_active_time", 0)
        val currentTime = System.currentTimeMillis()
        val oneMinute = 60 * 1000

        // ====================================
        // USER LOGIN & BELUM TIMEOUT
        // ====================================
        if (currentUser != null && currentUser.isEmailVerified && (currentTime - lastActiveTime < oneMinute)) {
            // Jika sudah login & sudah verifikasi email & belum 1 menit -> Ke Dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)
            startActivity(intent)
            finish()
        } else {
            // Jika lebih dari 1 menit atau belum login -> Sign Out & Ke Onboarding 😄🔥
            auth.signOut()
            val intent = Intent(this, OnboardingActivity::class.java)
            if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)
            startActivity(intent)
            finish()
        }
    }
}
