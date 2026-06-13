package com.desktoppet.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class PetDrawView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var state = "idle"
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var blinkFrame = 0

    fun setState(s: String) { state = s }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        // Body
        paint.color = when (state) {
            "eating" -> Color.parseColor("#FFB347")
            "drinking" -> Color.parseColor("#87CEEB")
            "sleeping" -> Color.parseColor("#B0C4DE")
            else -> Color.parseColor("#FF9ECD")
        }
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cx - 40f, cy - 30f, cx + 40f, cy + 50f, 30f, 30f, paint)

        // Head
        paint.color = paint.color
        canvas.drawCircle(cx, cy - 55f, 42f, paint)

        // Eyes
        paint.color = Color.WHITE
        canvas.drawCircle(cx - 14f, cy - 58f, 12f, paint)
        canvas.drawCircle(cx + 14f, cy - 58f, 12f, paint)

        paint.color = Color.parseColor("#333333")
        if (state == "sleeping") {
            // Closed eyes - ZZZ
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(cx - 22f, cy - 58f, cx - 6f, cy - 58f, paint)
            canvas.drawLine(cx + 6f, cy - 58f, cx + 22f, cy - 58f, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 18f
            paint.color = Color.parseColor("#8888AA")
            canvas.drawText("z z z", cx + 30f, cy - 80f, paint)
        } else {
            canvas.drawCircle(cx - 14f, cy - 58f, 6f, paint)
            canvas.drawCircle(cx + 14f, cy - 58f, 6f, paint)
            // Shine
            paint.color = Color.WHITE
            canvas.drawCircle(cx - 11f, cy - 61f, 2f, paint)
            canvas.drawCircle(cx + 17f, cy - 61f, 2f, paint)
        }

        // Mouth
        paint.color = Color.parseColor("#FF6B8A")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        val path = Path()
        path.moveTo(cx - 10f, cy - 45f)
        path.quadTo(cx, cy - 38f, cx + 10f, cy - 45f)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        // Cheeks
        paint.color = Color.parseColor("#FFB6C1")
        paint.alpha = 150
        canvas.drawCircle(cx - 30f, cy - 50f, 10f, paint)
        canvas.drawCircle(cx + 30f, cy - 50f, 10f, paint)
        paint.alpha = 255

        // Ears
        paint.color = Color.parseColor("#FF9ECD")
        canvas.drawCircle(cx - 35f, cy - 90f, 15f, paint)
        canvas.drawCircle(cx + 35f, cy - 90f, 15f, paint)
        paint.color = Color.parseColor("#FFB6C1")
        canvas.drawCircle(cx - 35f, cy - 90f, 8f, paint)
        canvas.drawCircle(cx + 35f, cy - 90f, 8f, paint)

        // State accessories
        when (state) {
            "eating" -> {
                paint.color = Color.parseColor("#8B4513")
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(cx - 5f, cy + 20f, cx + 5f, cy + 55f, 3f, 3f, paint)
                paint.color = Color.parseColor("#FF6B35")
                canvas.drawCircle(cx, cy + 18f, 10f, paint)
            }
            "drinking" -> {
                paint.color = Color.parseColor("#4FC3F7")
                canvas.drawRoundRect(cx + 20f, cy + 10f, cx + 45f, cy + 50f, 5f, 5f, paint)
                paint.color = Color.WHITE
                paint.alpha = 100
                canvas.drawRoundRect(cx + 25f, cy + 15f, cx + 32f, cy + 45f, 3f, 3f, paint)
                paint.alpha = 255
            }
            else -> {}
        }

        // Tail
        paint.color = Color.parseColor("#FF9ECD")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.strokeCap = Paint.Cap.ROUND
        val tail = Path()
        tail.moveTo(cx + 38f, cy + 30f)
        tail.quadTo(cx + 70f, cy + 10f, cx + 60f, cy - 10f)
        canvas.drawPath(tail, paint)
        paint.style = Paint.Style.FILL
    }
}
