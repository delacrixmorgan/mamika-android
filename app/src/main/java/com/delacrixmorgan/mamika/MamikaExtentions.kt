package com.delacrixmorgan.mamika

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

/**
 * MamikaExtensions
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

fun View.performHapticContextClick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    } else {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

fun MotionEvent.calculateFingerSpacing(): Float {
    val x = getX(0) - getX(1)
    val y = getY(0) - getY(1)

    return Math.sqrt((x * x + y * y).toDouble()).toFloat()
}