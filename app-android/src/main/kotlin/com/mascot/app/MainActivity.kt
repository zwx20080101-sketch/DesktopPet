package com.mascot.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.mascot.overlay.PetAccessibilityService
import com.mascot.overlay.bridge.ServiceBridge

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var mainLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val scrollView = ScrollView(this)
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 100)
        }
        scrollView.addView(mainLayout)
        setContentView(scrollView)

        setupBridge()
        showGuideOrStatus()
    }

    private fun setupBridge() {
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
    }

    private fun showGuideOrStatus() {
        mainLayout.removeAllViews()

        if (!isAccessibilityServiceEnabled(this) || !isIgnoringBatteryOptimizations() || !prefs.getBoolean("app_details_opened", false)) {
            showGuidePage()
        } else {
            showReadyPage()
        }
    }

    private fun showGuidePage() {
        val title = TextView(this).apply {
            text = "欢迎使用桌面萌宠"
            textSize = 28f
        }
        mainLayout.addView(title)

        val desc = TextView(this).apply {
            text = "为了正常运行，需要完成以下设置：\n\n1. 开启无障碍服务\n2. 允许忽略电池优化\n3. 允许自启动/后台弹出\n\n请按顺序操作。"
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }
        mainLayout.addView(desc)

        val startButton = Button(this).apply {
            text = "开始设置"
            setOnClickListener {
                startNextStep()
            }
        }
        mainLayout.addView(startButton)
    }

    private fun showReadyPage() {
        val title = TextView(this).apply {
            text = "✅ 全部就绪"
            textSize = 28f
        }
        mainLayout.addView(title)

        val status = TextView(this).apply {
            text = "悬浮球已准备就绪"
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }
        mainLayout.addView(status)

        val showButton = Button(this).apply {
            text = "显示悬浮球"
            setOnClickListener {
                val service = PetAccessibilityService.instance
                if (service != null) {
                    service.showPet()
                } else {
                    Toast.makeText(this@MainActivity, "服务未连接，请重新开启无障碍服务", Toast.LENGTH_SHORT).show()
                }
            }
        }
        mainLayout.addView(showButton)
    }

    private fun startNextStep() {
        when {
            !isAccessibilityServiceEnabled(this) -> {
                Toast.makeText(this, "请开启无障碍服务", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            !isIgnoringBatteryOptimizations() -> {
                requestIgnoreBatteryOptimizations()
            }
            !prefs.getBoolean("app_details_opened", false) -> {
                prefs.edit().putBoolean("app_details_opened", true).apply()
                Toast.makeText(this, "请在应用详情中允许自启动和后台弹出", Toast.LENGTH_SHORT).show()
                openAppDetails()
            }
            else -> {
                showReadyPage()
            }
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

    override fun onResume() {
        super.onResume()
        setupBridge()
        showGuideOrStatus()
    }
}
