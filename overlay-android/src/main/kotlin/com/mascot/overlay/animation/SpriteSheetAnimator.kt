package com.mascot.overlay.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlin.math.max

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
    private var playing = false
    private var view: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var animationRunnable: Runnable? = null

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
        stopAnimation()
        currentRow = row
        currentFrame = row * cols
        playing = true
        updateView()
        startAnimation(loop)
    }

    private fun startAnimation(loop: Boolean) {
        val runnable = object : Runnable {
            override fun run() {
                if (!playing) return
                currentFrame++
                if (currentFrame >= (currentRow + 1) * cols) {
                    if (loop) {
                        currentFrame = currentRow * cols
                    } else {
                        stopAnimation()
                        return
                    }
                }
                updateView()
                handler.postDelayed(this, (1000 / targetFps).toLong())
            }
        }
        animationRunnable = runnable
        handler.postDelayed(runnable, (1000 / targetFps).toLong())
    }

    private fun stopAnimation() {
        playing = false
        animationRunnable?.let { handler.removeCallbacks(it) }
        animationRunnable = null
    }

    private fun updateView() {
        view?.invalidate()
    }

    fun draw(canvas: Canvas, targetRect: Rect) {
        if (currentFrame < frames.size) {
            canvas.drawBitmap(frames[currentFrame], null, targetRect, null)
        }
    }

    fun getFrameWidth(): Int = frameWidth
    fun getFrameHeight(): Int = frameHeight
}
