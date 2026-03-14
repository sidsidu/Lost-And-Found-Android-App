package com.example.lostfound.activities

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lostfound.R
import com.example.lostfound.databinding.ActivitySpotlightDemoBinding

class SpotlightDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpotlightDemoBinding
    private var isUnlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpotlightDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractiveCard()
    }

    private fun setupInteractiveCard() {
        binding.spotlightCard.setOnClickListener {
            toggleSecurityState()
        }
    }

    private fun toggleSecurityState() {
        isUnlocked = !isUnlocked

        val startColorLock = ContextCompat.getColor(
            this,
            if (isUnlocked) R.color.scan_denied else R.color.scan_granted
        )
        val endColorLock = ContextCompat.getColor(
            this,
            if (isUnlocked) R.color.scan_granted else R.color.scan_denied
        )

        val startColorIcon = ContextCompat.getColor(
            this,
            if (isUnlocked) R.color.scan_idle else R.color.primary_variant
        )
        val endColorIcon = ContextCompat.getColor(
            this,
            if (isUnlocked) R.color.primary_variant else R.color.scan_idle
        )

        val lockBgStart = ContextCompat.getColor(this, if (isUnlocked) R.color.scan_denied_bg else R.color.scan_granted_bg)
        val lockBgEnd = ContextCompat.getColor(this, if (isUnlocked) R.color.scan_granted_bg else R.color.scan_denied_bg)

        // Animate Colors
        ValueAnimator.ofObject(ArgbEvaluator(), startColorLock, endColorLock).apply {
            duration = 500
            addUpdateListener { animator ->
                binding.ivLockIcon.setColorFilter(animator.animatedValue as Int)
            }
            start()
        }

        ValueAnimator.ofObject(ArgbEvaluator(), lockBgStart, lockBgEnd).apply {
            duration = 500
            addUpdateListener { animator ->
               // Fallback: Tint background drawable (Assuming it's a shape drawable API > 21)
               binding.ivLockIcon.background.setTint(animator.animatedValue as Int)
            }
            start()
        }

        ValueAnimator.ofObject(ArgbEvaluator(), startColorIcon, endColorIcon).apply {
            duration = 700
            addUpdateListener { animator ->
                binding.ivFingerprint.setColorFilter(animator.animatedValue as Int)
            }
            start()
        }

        // Animate Alpha (Glows)
        binding.fingerprintGlow.animate()
            .alpha(if (isUnlocked) 0.4f else 0.0f)
            .setDuration(700)
            .start()

        // Animate Scale (Fingerprint popping out)
        binding.ivFingerprint.animate()
            .scaleX(if (isUnlocked) 1.1f else 1.0f)
            .scaleY(if (isUnlocked) 1.1f else 1.0f)
            .setDuration(700)
            .start()

        // Toggle text states
        binding.tvStatusMessage.text = if (isUnlocked) "Access Granted" else "Touch to Authorize"
        
        // Use standard fallback lock/unlock resources (assuming app has basic vectors, or use default)
        binding.ivLockIcon.setImageResource(
            if (isUnlocked) android.R.drawable.ic_secure else android.R.drawable.ic_partial_secure
        )
    }
}
