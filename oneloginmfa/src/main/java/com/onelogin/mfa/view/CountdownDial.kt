package com.onelogin.mfa.view

import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat.getColor
import com.onelogin.mfa.R
import com.onelogin.mfa.model.Factor
import kotlinx.coroutines.*

class CountdownDial @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(
    context,
    attrs
) {
    private val countdownPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mainRect: RectF = RectF()
    private var centerRect: RectF = RectF()
    private var strokeWidth: Int = 10

    private var progress: Double = 0.toDouble()
    private var isViewVisible: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        initAttributes(attrs)
    }

    override fun onDraw(canvas: Canvas) {
        val currentPosition = (progress.toFloat() * 360)

        mainRect.left = 1f
        mainRect.top = 1f
        mainRect.right = (width - 1).toFloat()
        mainRect.bottom = (height - 1).toFloat()
        centerRect.left = mainRect.left + strokeWidth
        centerRect.top = mainRect.top + strokeWidth
        centerRect.right = mainRect.right - strokeWidth
        centerRect.bottom = mainRect.bottom - strokeWidth

        // Draw progress background
        canvas.drawOval(mainRect, backgroundPaint)

        // Draw countdown
        canvas.drawArc(mainRect, -90f, currentPosition, true, countdownPaint)

        // Draw inner
        canvas.drawOval(centerRect, centerPaint)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        isViewVisible = visibility == VISIBLE
    }

    fun setProgress(factor: Factor, duration: Long = 100) {
        scope.launch {
            while (isViewVisible) {
                updateProgress(factor.getTimerInMillis().toDouble() / factor.getPeriodInMillis())
                delay(duration)
            }
        }
    }

    /**
     * Set countdown progress [0, 1]
     **/
    private fun updateProgress(progress: Double) {
        if (progress < 0 || progress > 1)
            return

        this.progress = progress
        invalidate()
    }

    private fun initAttributes(attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CountdownDial)

        backgroundPaint.color = attributes.getColor(
            R.styleable.CountdownDial_countdown_background_color,
            getColor(context, R.color.onelogin_mfa_progress_background)
        )

        countdownPaint.strokeCap = Paint.Cap.SQUARE
        countdownPaint.color = attributes.getColor(
            R.styleable.CountdownDial_countdown_paint_color,
            getColor(context, R.color.onelogin_mfa_blue)
        )

        val color = if (isDarkThemeOn()) {
            android.R.color.background_dark
        } else {
            android.R.color.background_light
        }

        centerPaint.color = attributes.getColor(
            R.styleable.CountdownDial_countdown_center_color,
            getColor(context, color)
        )

        attributes.recycle()
    }

    private fun isDarkThemeOn(): Boolean {
        return context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }

}
