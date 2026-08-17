package com.mascot.app

import android.app.Activity
import android.content.Context
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
            setPadding(60, 80, 60, 80)
        }
        scrollView.addView(layout)
        setContentView(scrollView)

        val statusText = TextView(this).apply {
            text = if (isAccessibilityServiceEnabled(this)) "✅ 无障碍服务已开启" else "❌ 无障碍服务未开启"
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }
        layout.addView(statusText)

        val showButton = Button(this).apply {
            text = "显示悬浮球"
            setOnClickListener {
                val service = PetAccessibilityService.instance
                if (service != null) {
                    service.showPet()
                } else {
                    Toast.makeText(this@MainActivity, "服务未连接，请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(showButton)

        val hideButton = Button(this).apply {
            text = "隐藏悬浮球"
            setOnClickListener {
                PetAccessibilityService.instance?.removePet()
            }
        }
        layout.addView(hideButton)

        val roleButton = Button(this).apply {
            text = "角色管理（即将推出）"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(roleButton)

        val gestureButton = Button(this).apply {
            text = "手势设置（即将推出）"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(gestureButton)

        val moreButton = Button(this).apply {
            text = "更多设置（即将推出）"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(moreButton)

        val accessibilityButton = Button(this).apply {
            text = "无障碍设置"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(accessibilityButton)

        // 设置桥接
        PetAccessibilityService.instance?.bridge = object : ServiceBridge {
            override fun openMainApp() {
                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 重新设置桥接
        PetAccessibilityService.instance?.bridge = object : ServiceBridge {
            override fun openMainApp() {
                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }
        // 刷新状态
        val statusText = findViewById<TextView>(0)
        if (statusText != null) {
            statusText.text = if (isAccessibilityServiceEnabled(this)) "✅ 无障碍服务已开启" else "❌ 无障碍服务未开启"
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
