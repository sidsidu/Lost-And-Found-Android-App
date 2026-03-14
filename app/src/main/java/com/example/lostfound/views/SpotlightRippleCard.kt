package com.example.lostfound.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.Interpolator
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.lostfound.R

class SpotlightRippleCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    // --- 1. PHYSICS CONSTANTS (From React) ---
    private val MINIMUM_PRESS_MS = 300L
    private val INITIAL_ORIGIN_SCALE = 0.2f
    private val PADDING = 10f
    private val SOFT_EDGE_CONTAINER_RATIO = 0.35f
    
    // Standard ripple color (defaults to white overlay for Spotlight)
    private var rippleColor = ContextCompat.getColor(context, R.color.ripple_color)
    private var maxRippleAlpha = 38 // ~15% opacity logic requested in Demo
    private var currentRippleAlpha = 0f

    // Standard Android fast-out slow-in curve
    private val standardEasing: Interpolator = Interpolator { input ->
        val t = input - 1.0f
        t * t * t * t * t + 1.0f
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private var rippleRadius = 0f
    private var maxRadius = 0f
    private var rippleX = 0f
    private var rippleY = 0f

    private var growAnimator: ValueAnimator? = null
    private var isPressing = false

    init {
        // Required for custom drawing in ViewGroups
        setWillNotDraw(false)
        radius = resources.getDimension(R.dimen.card_radius_large) 
        cardElevation = resources.getDimension(R.dimen.card_elevation)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateMaxRadius(w.toFloat(), h.toFloat())
    }

    private fun calculateMaxRadius(width: Float, height: Float) {
        val maxDim = maxOf(height, width)
        val softEdgeSize = maxOf(SOFT_EDGE_CONTAINER_RATIO * maxDim, 75f)
        val hypotenuse = Math.hypot(width.toDouble(), height.toDouble()).toFloat()
        maxRadius = hypotenuse + PADDING + softEdgeSize
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressing = true
                rippleX = event.x
                rippleY = event.y
                startRippleAnimation()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressing = false
                endRippleAnimation()
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun startRippleAnimation() {
        growAnimator?.cancel()
        
        // Dynamic duration matching React logic (min 400, max 1000)
        val dynDuration = (Math.hypot(width.toDouble(), height.toDouble()) * 1.5).toLong()
            .coerceIn(400L, 1000L)

        // Set Paint properties
        ripplePaint.color = rippleColor
        ripplePaint.alpha = maxRippleAlpha
        currentRippleAlpha = maxRippleAlpha.toFloat()
        
        // Exact gradient translation from React
        // background: "radial-gradient(closest-side, currentColor max(calc(100% - 70px), 65%), transparent 100%)"
        val colors = intArrayOf(
            rippleColor,
            rippleColor,
            Color.TRANSPARENT
        )
        // Estimate gradient stops for the soft edge
        val stops = floatArrayOf(0f, 0.65f, 1f)

        growAnimator = ValueAnimator.ofFloat(0f, maxRadius).apply {
            duration = dynDuration
            interpolator = standardEasing
            addUpdateListener { animator ->
                rippleRadius = animator.animatedValue as Float
                
                // Update radial gradient to match current radius
                if (rippleRadius > 0) {
                   ripplePaint.shader = RadialGradient(
                        rippleX, rippleY, rippleRadius,
                        colors, stops,
                        Shader.TileMode.CLAMP
                    )
                }
                
                invalidate()
            }
        }
        growAnimator?.start()
    }

    private fun endRippleAnimation() {
        // Fade out
        val fadeOutAnimator = ValueAnimator.ofFloat(currentRippleAlpha, 0f).apply {
            duration = MINIMUM_PRESS_MS
            addUpdateListener { animator ->
                ripplePaint.alpha = (animator.animatedValue as Float).toInt()
                invalidate()
            }
        }
        fadeOutAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw Spotlight Ripple
        if (rippleRadius > 0 && ripplePaint.alpha > 0) {
            canvas.drawCircle(rippleX, rippleY, rippleRadius, ripplePaint)
        }
    }
}
