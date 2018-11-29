package com.delacrixmorgan.mamika.record

import android.util.SparseIntArray
import android.view.Surface
import androidx.fragment.app.Fragment

/**
 * RecordCaptureFragment
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class RecordCaptureFragment : Fragment() {
    companion object {
        const val REQUEST_GALLERY_PICK = 1

        private const val CAMERA_FRONT = "1"
        private const val CAMERA_BACK = "0"
        private const val MAX_DURATION_IN_MILLISECONDS: Long = 30 * 1000

        private const val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
        private const val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
        private val DEFAULT_ORIENTATIONS = SparseIntArray().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }

        private val INVERSE_ORIENTATIONS = SparseIntArray().apply {
            append(Surface.ROTATION_0, 270)
            append(Surface.ROTATION_90, 180)
            append(Surface.ROTATION_180, 90)
            append(Surface.ROTATION_270, 0)
        }

        fun newInstance(): RecordCaptureFragment = RecordCaptureFragment()
    }
}