package com.example.fixedearthanchordemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CompassArrowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentAngle = 0f

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val arrowPath = Path()

    /**
     * Set the direction the arrow points to, in degrees.
     * 0 = up (forward), positive = clockwise.
     * Quantized to 15-degree increments.
     */
    fun setDirection(degrees: Float) {
        val normalized = ((degrees % 360) + 360) % 360
        val quantized = (Math.round(normalized / 15f) * 15).toFloat()
        if (quantized != currentAngle) {
            currentAngle = quantized
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) * 0.85f

        // Draw outer circle
        canvas.drawCircle(cx, cy, radius, circlePaint)

        // Draw arrow rotated to point toward target
        canvas.save()
        canvas.rotate(currentAngle, cx, cy)

        arrowPath.reset()
        val tipY = cy - radius * 0.75f
        val baseY = cy + radius * 0.3f
        val notchY = cy + radius * 0.05f
        val halfWidth = radius * 0.28f

        arrowPath.moveTo(cx, tipY)
        arrowPath.lineTo(cx - halfWidth, baseY)
        arrowPath.lineTo(cx, notchY)
        arrowPath.lineTo(cx + halfWidth, baseY)
        arrowPath.close()

        canvas.drawPath(arrowPath, arrowPaint)
        canvas.drawPath(arrowPath, outlinePaint)

        canvas.restore()
    }
}
