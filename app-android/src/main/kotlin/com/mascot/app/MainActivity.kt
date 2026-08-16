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
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60,80,60,80) }
        scroll.addView(layout); setContentView(scroll)

        val status = TextView(this).apply { text = if (isAccessibilityOn()) "✅ 无障碍已开启" else "❌ 无障碍未开启"; textSize = 16f; setPadding(0,30,0,30) }
        layout.addView(status)

        val showBtn = Button(this).apply { text = "显示悬浮球"; setOnClickListener { PetAccessibilityService.instance?.showPet() ?: Toast.makeText(this@MainActivity,"服务未连接",Toast.LENGTH_SHORT).show() } }
        layout.addView(showBtn)

        val hideBtn = Button(this).apply { text = "隐藏悬浮球"; setOnClickListener { PetAccessibilityService.instance?.removePet() } }
        layout.addView(hideBtn)

        val settingsBtn = Button(this).apply { text = "无障碍设置"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
        layout.addView(settingsBtn)

        // 设置桥接
        PetAccessibilityService.instance?.bridge = object : ServiceBridge {
            override fun openMainApp() {
                val i = Intent(this@MainActivity, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) }
                startActivity(i)
            }
        }
    }

    override fun onResume() { super.onResume(); PetAccessibilityService.instance?.bridge = object : ServiceBridge {
        override fun openMainApp() {
            val i = Intent(this@MainActivity, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) }
            startActivity(i)
        }
    } }

    private fun isAccessibilityOn(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.contains(packageName) == true
    }
}
