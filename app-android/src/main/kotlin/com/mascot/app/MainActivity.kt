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

        // 状态文本
        val statusText = TextView(this).apply {
            text = if (isAccessibilityServiceEnabled(this)) "✅ 无障碍服务已开启" else "❌ 无障碍服务未开启"
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }
        layout.addView(statusText)

        // 显示悬浮球按钮
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

        // 隐藏悬浮球按钮
        val hideButton = Button(this).apply {
            text = "隐藏悬浮球"
            setOnClickListener {
                PetAccessibilityService.instance?.removePet()
            }
        }
        layout.addView(hideButton)

        // 角色管理（占位）
        val roleButton = Button(this).apply {
            text = "角色管理（即将推出）"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(roleButton)

        // 手势设置（占位）
        val gestureButton = Button(this).apply {
            text = "手势设置（即将推出）"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(gestureButton)

        // 更多设置（占位）
        val moreButton = Button(this).apply {
            text = "更多设置（即将推出）"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "功能开发中", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(moreButton)

        // 无障碍设置
        val accessibilityButton = Button(this).apply {
            text = "无障碍设置"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(accessibilityButton)

        // 设置桥接
        setupBridge()
    }

    override fun onResume() {
        super.onResume()
        // 重新设置桥接
        setupBridge()
        // 刷新状态文本（直接重建或忽略，本Demo简单处理）
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

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(context.packageName) == true
    }
}
