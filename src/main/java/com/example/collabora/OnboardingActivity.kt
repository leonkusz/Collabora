package com.example.collabora

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    // FUNCTION PERMISSION
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    1001
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // ==========================================
        // POPUP MUNCUL OTOMATIS SAAT HALAMAN DIBUKA 😄🔥
        // ==========================================
        requestNotificationPermission()

        // ==========================================
        // CATCH INVITE CODE 😄🔥
        // ==========================================
        val inviteCode = intent.getStringExtra("inviteCode")

        // 1. Inisialisasi elemen-elemen yang bisa diklik
        val tvSkip = findViewById<TextView>(R.id.tvSkip)
        val btnStart = findViewById<MaterialButton>(R.id.btnStart)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        viewPager = findViewById(R.id.viewPagerOnboarding)
        val layoutDots = findViewById<LinearLayout>(R.id.layoutDots)

        // Setup ViewPager2 😄🔥
        val onboardingItems = listOf(
            OnboardingAdapter.OnboardingItem(
                R.layout.illustration_tasks,
                "Kelola tugas tim\ndengan mudah",
                "Bagi tugas, pantau progress, dan koordinasi anggota kelompok — semua dalam satu tempat yang realtime."
            ),
            OnboardingAdapter.OnboardingItem(
                R.layout.illustration_calendar,
                "Kalender Tim\nRealtime",
                "Pantau jadwal dan deadline penting secara visual. Tidak ada lagi tugas yang terlewatkan!"
            ),
            OnboardingAdapter.OnboardingItem(
                R.layout.illustration_chat,
                "Diskusi &\nChat Interaktif",
                "Komunikasi lancar antar anggota tim dengan fitur chat realtime terintegrasi di setiap workspace."
            )
        )

        viewPager.adapter = OnboardingAdapter(onboardingItems)
        setupDots(onboardingItems.size, layoutDots)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position, layoutDots)
            }
        })

        // Auto Scroll Logic 😄🔥
        runnable = Runnable {
            var currentItem = viewPager.currentItem
            currentItem = (currentItem + 1) % onboardingItems.size
            viewPager.setCurrentItem(currentItem, true)
            handler.postDelayed(runnable, 2000)
        }
        handler.postDelayed(runnable, 2000)

        // 2. Beri Perintah saat diklik

        // Tombol "Lewati" Pojok Kanan Atas -> Langsung ke Login
        tvSkip.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)
            startActivity(intent)
            finish()
        }

        // Tombol "Mulai Sekarang" Tengah -> Langsung ke Daftar (Register)
        btnStart.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)
            startActivity(intent)
            finish()
        }

        // Link "Masuk" di Paling Bawah -> Langsung ke Login
        tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            if (inviteCode != null) intent.putExtra("inviteCode", inviteCode)
            startActivity(intent)
            finish()
        }
    }

    private fun setupDots(size: Int, container: LinearLayout) {
        container.removeAllViews()
        for (i in 0 until size) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(if (i == 0) 48 else 24, 24)
            params.setMargins(8, 0, 8, 0)
            dot.layoutParams = params
            dot.setBackgroundResource(R.drawable.bg_status_solid)
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(if (i == 0) "#1e9e75" else "#D1D5DB")
            )
            container.addView(dot)
        }
    }

    private fun updateDots(position: Int, container: LinearLayout) {
        for (i in 0 until container.childCount) {
            val dot = container.getChildAt(i)
            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = if (i == position) 48 else 24
            dot.layoutParams = params
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(if (i == position) "#1e9e75" else "#D1D5DB")
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}
