package com.mascot.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.mascot.overlay.PetAccessibilityService
import android.os.PowerManager

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // 构建简洁状态页
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

        // 点击状态文字可手动重新检查
        statusText.setOnClickListener {
            startAutoGuide()
        }

        scrollView.addView(layout)
        setContentView(scrollView)

        // 启动自动引导
        startAutoGuide()
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置返回后，继续自动引导
        startAutoGuide()
    }

    private fun startAutoGuide() {
        // 如果已经完成引导，只更新状态并尝试显示悬浮球
        if (prefs.getBoolean("guide_done", false)) {
            updateStatusAndShowPet()
            return
        }

        // 第一步：检查无障碍服务
        if (!isAccessibilityServiceEnabled(this)) {
            statusText.text = "请开启无障碍服务"
            openAccessibilitySettings()
            return
        }

        // 第二步：检查电池优化
        if (!isIgnoringBatteryOptimizations()) {
            statusText.text = "请允许忽略电池优化"
            requestIgnoreBatteryOptimizations()
            return
        }

        // 第三步：引导自启动/后台弹出（只跳转一次）
        if (!prefs.getBoolean("app_details_opened", false)) {
            prefs.edit().putBoolean("app_details_opened", true).apply()
            statusText.text = "请允许自启动和后台弹出"
            openAppDetails()
            return
        }

        // 全部完成
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
                // 服务可能还没连接，稍后会自动显示
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
                    // 部分设备没有该对话框，跳转电池优化列表
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
