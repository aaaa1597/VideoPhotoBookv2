package com.tks.videophotobook

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class DoubleTapGuideView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val blinkAnimator = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
        duration = 1000L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = TimeInterpolator { input ->
            when {
                input < 0.25f -> 1f - (input / 0.25f)   /* 0〜0.25秒で1→0 */
                input < 0.5f -> (input - 0.25f) / 0.25f/* 0.25〜0.5秒で0→1 */
                else -> 1f                     /* 0.5〜1秒はずっと表示（だんまり） */
            }
        }
        addUpdateListener {
            alpha = it.animatedValue as Float
        }
    }

    init {
        setImageResource(R.drawable.arrow_left)
        rotation = 315f /* 左下を向ける(右上から時計回りに315度) */
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        blinkAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        blinkAnimator.end()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        postInvalidateDelayed(33L)
    }
}