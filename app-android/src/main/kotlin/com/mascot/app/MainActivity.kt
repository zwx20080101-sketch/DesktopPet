package com.mascot.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
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

        val hint = TextView(this).apply {
            text = "请先开启无障碍服务，然后返回桌面即可看到悬浮萌宠。"
            textSize = 16f
            setPadding(0, 40, 0, 40)
        }
        layout.addView(hint)

        val button = Button(this).apply {
            text = "开启无障碍服务"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
        layout.addView(button)

        setContentView(layout)
    }
}
