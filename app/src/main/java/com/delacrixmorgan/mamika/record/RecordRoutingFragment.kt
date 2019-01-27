package com.delacrixmorgan.mamika.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.delacrixmorgan.mamika.R
import kotlinx.android.synthetic.main.fragment_base_routing.*

/**
 * RecordRoutingFragment
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class RecordRoutingFragment : Fragment() {
    companion object {
        fun newInstance() = RecordRoutingFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_base_routing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragment = RecordCaptureFragment.newInstance()
        if (savedInstanceState == null) {
            this.activity?.apply {
                supportFragmentManager.beginTransaction()
                        .replace(this.routingContainer.id, fragment, fragment.javaClass.simpleName)
                        .commit()
            }
        }
    }
}