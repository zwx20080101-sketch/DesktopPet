package com.mascot.overlay.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View

class SpriteSheetAnimator(
    context: Context,
    spriteSheetName: String,
    private val rows: Int,
    private val cols: Int,
    private val fps: Int = 6
) {
    private val spriteSheet: Bitmap = BitmapFactory.decodeStream(context.assets.open(spriteSheetName))
    private val frameWidth = spriteSheet.width / cols
    private val frameHeight = spriteSheet.height / rows
    private val frames = mutableListOf<Bitmap>()

    private var currentFrame = 0
    private var targetRow = 0
    private var isPlaying = false
    private var view: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isPlaying) return
            currentFrame++
            if (currentFrame >= (targetRow + 1) * cols) {
                currentFrame = targetRow * cols
            }
            view?.invalidate()
            handler.postDelayed(this, (1000 / fps).toLong())
        }
    }

    init {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val src = Rect(c * frameWidth, r * frameHeight, (c + 1) * frameWidth, (r + 1) * frameHeight)
                frames.add(Bitmap.createBitmap(spriteSheet, src.left, src.top, src.width(), src.height()))
            }
        }
    }

    fun attach(view: View) {
        this.view = view
    }

    fun playRow(row: Int) {
        stop()
        targetRow = row
        currentFrame = row * cols
        isPlaying = true
        view?.invalidate()
        handler.postDelayed(frameRunnable, (1000 / fps).toLong())
    }

    fun stop() {
        isPlaying = false
        handler.removeCallbacks(frameRunnable)
    }

    fun draw(canvas: Canvas, targetRect: Rect) {
        if (currentFrame < frames.size) {
            canvas.drawBitmap(frames[currentFrame], null, targetRect, null)
        }
    }
}
