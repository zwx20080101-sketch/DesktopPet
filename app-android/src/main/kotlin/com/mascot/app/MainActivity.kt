package com.mascot.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.mascot.overlay.PetAccessibilityService

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            setPadding(0, 40, 0, 40)
        }
        layout.addView(statusText)

        val openAccessibilityButton = Button(this).apply {
            text = "开启无障碍服务"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
        layout.addView(openAccessibilityButton)

        val showPetButton = Button(this).apply {
            text = "显示悬浮球"
            setOnClickListener {
                val service = PetAccessibilityService.instance
                if (service != null) {
                    service.showPet()
                    updateStatus()
                } else {
                    statusText.text = "服务未运行，请先开启无障碍服务"
                }
            }
        }
        layout.addView(showPetButton)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = isAccessibilityServiceEnabled(this)
        if (enabled) {
            statusText.text = "无障碍服务已开启，可以显示悬浮球"
        } else {
            statusText.text = "无障碍服务未开启，请点击下方按钮开启"
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
