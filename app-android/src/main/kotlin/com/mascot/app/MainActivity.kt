package com.mascot.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.os.PowerManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.mascot.overlay.PetAccessibilityService
import com.mascot.overlay.bridge.ServiceBridge

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 100)
        }

        val title = TextView(this).apply {
            text = "桌面萌宠"
            textSize = 30f
        }
        layout.addView(title)

        statusText = TextView(this).apply {
            text = "正在检查权限..."
            textSize = 16f
            setPadding(0, 40, 0, 0)
        }
        layout.addView(statusText)

        statusText.setOnClickListener {
            startAutoGuide()
        }

        scrollView.addView(layout)
        setContentView(scrollView)

        // 设置服务桥接
        PetAccessibilityService.instance?.bridge = object : ServiceBridge {
            override fun openMainApp() {
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            }

            override fun removeOverlay() {
                PetAccessibilityService.instance?.removePet()
            }
        }

        startAutoGuide()
    }

    override fun onResume() {
        super.onResume()
        startAutoGuide()
    }

    private fun startAutoGuide() {
        if (prefs.getBoolean("guide_done", false)) {
            updateStatusAndShowPet()
            return
        }

        if (!isAccessibilityServiceEnabled(this)) {
            statusText.text = "请开启无障碍服务"
            openAccessibilitySettings()
            return
        }

        if (!isIgnoringBatteryOptimizations()) {
            statusText.text = "请允许忽略电池优化"
            requestIgnoreBatteryOptimizations()
            return
        }

        if (!prefs.getBoolean("app_details_opened", false)) {
            prefs.edit().putBoolean("app_details_opened", true).apply()
            statusText.text = "请允许自启动和后台弹出"
            openAppDetails()
            return
        }

        prefs.edit().putBoolean("guide_done", true).apply()
        updateStatusAndShowPet()
    }

    private fun updateStatusAndShowPet() {
        val enabled = isAccessibilityServiceEnabled(this)
        if (enabled) {
            statusText.text = "✅ 全部就绪，悬浮球已显示"
            val service = PetAccessibilityService.instance
            if (service != null) {
                service.showPet()
            } else {
                Toast.makeText(this, "服务连接中，请稍候", Toast.LENGTH_SHORT).show()
            }
        } else {
            statusText.text = "❌ 无障碍服务未开启，点击此处重新引导"
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (e2: Exception) {
                        openAppDetails()
                    }
                }
            }
        }
    }

    private fun openAppDetails() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "请手动进入系统设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        return true
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(context.packageName) == true
    }
}
