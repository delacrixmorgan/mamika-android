package com.delacrixmorgan.mamika.common

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent

/**
 * GestureDetector
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class GestureDetector(private val context: Context, private val listener: SwipeGesture) : GestureDetector.OnGestureListener {
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (Math.abs(velocityY) < SWIPE_VELOCITY_THRESHOLD || Math.abs(e2.y - e1.y) < SWIPE_THRESHOLD) {
            return false
        }

        if (e2.y - e1.y > 0) {
            this.listener.onSwipeDown()
        } else {
            this.listener.onSwipeUp()
        }
        return true
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        val widthPixels = this.context.resources.displayMetrics.widthPixels

        if (e.x < widthPixels / 2) {
            this.listener.onLeftTap()
        } else {
            this.listener.onRightTap()
        }

        this.listener.onTap()
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        this.listener.onLongPressed()
    }

    override fun onShowPress(e: MotionEvent?) = Unit
    override fun onDown(e: MotionEvent?): Boolean = true
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean = false
}