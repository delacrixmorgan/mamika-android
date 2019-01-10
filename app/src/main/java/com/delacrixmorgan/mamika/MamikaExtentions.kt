package com.delacrixmorgan.mamika

import android.content.Context
import android.content.Intent
import android.net.Uri
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

fun Context.launchPlayStore(packageName: String) {
    val url = "https://play.google.com/store/apps/details?id=$packageName"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}

fun Context.launchWebsite(url: String) {
    val intent = Intent(Intent.ACTION_VIEW)

    intent.data = Uri.parse(url)
    startActivity(intent)
}

fun Context.shareAppIntent(message: String) {
    val intent = Intent(Intent.ACTION_SEND)

    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, message)

    startActivity(Intent.createChooser(intent, "Tell a GIF buddy"))
}