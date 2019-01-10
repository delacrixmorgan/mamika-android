package com.delacrixmorgan.mamika.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.delacrixmorgan.mamika.BuildConfig
import com.delacrixmorgan.mamika.R
import kotlinx.android.synthetic.main.fragment_setting_list.*

/**
 * SettingListFragment
 * mamika-android
 *
 * Created by Delacrix Morgan on 10/01/2019.
 * Copyright (c) 2019 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class SettingListFragment : Fragment() {
    companion object {
        fun newInstance() = SettingListFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_setting_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLayouts()
        setupListeners()
    }

    private fun setupLayouts() {
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE

        this.buildNumberTextView.text = getString(R.string.message_build_version_name, versionName, versionCode)
    }

    private fun setupListeners() {
        this.kingscupViewGroup.setOnClickListener {

        }

        this.squarkViewGroup.setOnClickListener {

        }

        this.sourceCodeButton.setOnClickListener {

        }

        this.creditButton.setOnClickListener {

        }

        this.supportButton.setOnClickListener {

        }

        this.shareButton.setOnClickListener {

        }

        this.backButton.setOnClickListener {
            this.activity?.supportFragmentManager?.popBackStack()
        }
    }
}