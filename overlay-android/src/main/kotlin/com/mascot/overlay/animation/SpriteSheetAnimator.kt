package com.mascot.overlay.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View

class SpriteSheetAnimator(
    context: Context,
    private val spriteSheetName: String,
    private val rows: Int,
    private val cols: Int,
    private val targetFps: Int = 12
) {
    private val spriteSheet: Bitmap = BitmapFactory.decodeStream(context.assets.open(spriteSheetName))
    private val frameWidth: Int = spriteSheet.width / cols
    private val frameHeight: Int = spriteSheet.height / rows
    private val frames = mutableListOf<Bitmap>()
    private var currentRow = 0
    private var currentFrame = 0
    private var isPlaying = false
    private var view: View? = null

    init {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val src = Rect(c * frameWidth, r * frameHeight, (c + 1) * frameWidth, (r + 1) * frameHeight)
                val bmp = Bitmap.createBitmap(spriteSheet, src.left, src.top, src.width(), src.height())
                frames.add(bmp)
            }
        }
    }

    fun attach(view: View) {
        this.view = view
    }

    fun playRow(row: Int, loop: Boolean = true) {
        currentRow = row
        currentFrame = row * cols
        isPlaying = true
        updateView()
        startAnimation(loop)
    }

    private fun startAnimation(loop: Boolean) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (!isPlaying) return
                currentFrame++
                if (currentFrame >= (currentRow + 1) * cols) {
                    currentFrame = currentRow * cols
                }
                updateView()
                handler.postDelayed(this, (1000 / targetFps).toLong())
            }
        }
        handler.postDelayed(runnable, (1000 / targetFps).toLong())
    }

    private fun updateView() {
        view?.invalidate()
    }

    fun draw(canvas: Canvas) {
        if (currentFrame < frames.size) {
            canvas.drawBitmap(frames[currentFrame], null, Rect(0, 0, frameWidth, frameHeight), null)
        }
    }

    fun getFrameWidth(): Int = frameWidth
    fun getFrameHeight(): Int = frameHeight
}
