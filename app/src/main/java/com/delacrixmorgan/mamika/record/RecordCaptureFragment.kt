package com.delacrixmorgan.mamika.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.delacrixmorgan.mamika.R

/**
 * RecordCaptureFragment
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class RecordCaptureFragment : Fragment() {
    companion object {
        private const val REQUEST_GALLERY_PICK = 1
        private const val MAX_DURATION_IN_MILLISECONDS: Long = 30 * 1000

        fun newInstance(): RecordCaptureFragment = RecordCaptureFragment()
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record_capture, container, false)
    }
}