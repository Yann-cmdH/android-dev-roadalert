package com.roadalert.cameroun.ui.countdown

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CountdownRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = 0x26FFFFFF.toInt()
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = 0xFFFFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    private val oval = RectF()

    var progress: Float = 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 14f / 2f + 4f
        oval.set(padding, padding, width - padding, height - padding)
        canvas.drawOval(oval, backgroundPaint)
        val sweepAngle = 360f * progress
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)
    }
}