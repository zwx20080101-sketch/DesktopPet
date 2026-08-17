package com.mascot.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.mascot.overlay.bridge.ServiceBridge
import com.mascot.overlay.service.PetAccessibilityService

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 80)
        }
        scrollView.addView(layout)
        setContentView(scrollView)

        // 标题，与状态分开，避免遮挡
        val titleText = TextView(this).apply {
            text = "🐾 桌面萌宠"
            textSize = 28f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(titleText)

        val statusText = TextView(this).apply {
            text = if (isAccessibilityOn()) "✅ 无障碍服务已开启" else "❌ 无障碍服务未开启"
            textSize = 16f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(statusText)

        // 功能按钮
        addButton(layout, "显示悬浮球") {
            val service = PetAccessibilityService.instance
            if (service != null) {
                service.showPet()
            } else {
                Toast.makeText(this@MainActivity, "服务未连接，请先开启无障碍服务", Toast.LENGTH_SHORT).show()
            }
        }

        addButton(layout, "隐藏悬浮球") {
            PetAccessibilityService.instance?.removePet()
        }

        addButton(layout, "角色管理（即将推出）") {
            Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
        }

        addButton(layout, "手势设置（即将推出）") {
            Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
        }

        addButton(layout, "更多设置（即将推出）") {
            Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
        }

        addButton(layout, "无障碍设置") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        setupBridge()
    }

    override fun onResume() {
        super.onResume()
        setupBridge()
    }

    private fun addButton(layout: LinearLayout, text: String, onClick: () -> Unit) {
        val button = Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
        layout.addView(button)
    }

    private fun setupBridge() {
        PetAccessibilityService.instance?.bridge = object : ServiceBridge {
            override fun openMainApp() {
                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }
    }

    private fun isAccessibilityOn(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(packageName) == true
    }
}
