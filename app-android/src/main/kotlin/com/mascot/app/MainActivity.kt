package com.mascot.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        textView.text = "桌面萌宠启动成功"
        textView.textSize = 24f
        setContentView(textView)
    }
}
