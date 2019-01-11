package com.delacrixmorgan.mamika.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.delacrixmorgan.mamika.R

/**
 * SettingCreditDetailFragment
 * mamika-android
 *
 * Created by Delacrix Morgan on 11/01/2019.
 * Copyright (c) 2019 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class SettingCreditDetailFragment : Fragment() {
    companion object {
        fun newInstance() = SettingCreditDetailFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_setting_credit_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLayouts()
        setupListeners()
    }

    private fun setupLayouts() {

    }

    private fun setupListeners() {

    }
}