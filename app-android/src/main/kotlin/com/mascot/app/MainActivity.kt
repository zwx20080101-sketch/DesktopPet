package com.mascot.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.mascot.overlay.PetAccessibilityService

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 80, 50, 80)
        }

        val title = TextView(this).apply {
            text = "桌面萌宠"
            textSize = 28f
        }
        layout.addView(title)

        statusText = TextView(this).apply {
            text = "检测中..."
            textSize = 16f
            setPadding(0, 30, 0, 20)
        }
        layout.addView(statusText)

        // 显示悬浮球按钮
        val showPetButton = Button(this).apply {
            text = "显示悬浮球"
            setOnClickListener {
                val service = PetAccessibilityService.instance
                if (service != null) {
                    service.showPet()
                    updateStatus()
                } else {
                    Toast.makeText(this@MainActivity, "服务未运行，请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(showPetButton)

        // 开启无障碍服务按钮
        val openAccessibilityButton = Button(this).apply {
            text = "1. 开启无障碍服务"
            setOnClickListener {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "无法打开设置", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(openAccessibilityButton)

        // 电池优化设置按钮
        val batteryButton = Button(this).apply {
            text = "2. 电池优化设为不限制"
            setOnClickListener {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    openAppDetails()
                }
            }
        }
        layout.addView(batteryButton)

        // 应用详情/自启动按钮
        val appDetailsButton = Button(this).apply {
            text = "3. 允许自启动/后台弹出"
            setOnClickListener {
                openAppDetails()
            }
        }
        layout.addView(appDetailsButton)

        // 使用说明
        val hint = TextView(this).apply {
            text = "\n设置完成后，请按 Home 键回到桌面。\n如果悬浮球未显示，请重新打开本应用并点击“显示悬浮球”。"
            textSize = 14f
            setPadding(0, 30, 0, 0)
        }
        layout.addView(hint)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = isAccessibilityServiceEnabled(this)
        if (enabled) {
            statusText.text = "✅ 无障碍服务已开启"
        } else {
            statusText.text = "❌ 无障碍服务未开启，请点击下方按钮开启"
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

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(context.packageName) == true
    }
}
