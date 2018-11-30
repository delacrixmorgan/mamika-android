package com.delacrixmorgan.mamika

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.delacrixmorgan.mamika.record.RecordActivity
import com.delacrixmorgan.mamika.record.hasPermissionsGranted
import kotlinx.android.synthetic.main.activity_main.*

/**
 * MainActivity
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class MainActivity : AppCompatActivity() {
    companion object {
        val VIDEO_PERMISSIONS = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        const val EXTRA_PERMISSION_MISSING = "permissionMissing"

        private const val REQUEST_CAMERA: Int = 1
        private const val REQUEST_VIDEO_PERMISSIONS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launchRecordActivity()

        this.recordButton.setOnClickListener {
            launchRecordActivity()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CAMERA -> {
                data?.getBooleanExtra(EXTRA_PERMISSION_MISSING, true)?.let {
                    if (resultCode == Activity.RESULT_CANCELED) {
                        requestVideoPermissions()
                    }
                }
            }
        }
    }

    private fun launchRecordActivity() {
        if (hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            val recordIntent = RecordActivity.newLaunchIntent(this)
            startActivityForResult(recordIntent, REQUEST_CAMERA)
        } else {
            requestVideoPermissions()
        }
    }

    //region Permissions
    private fun shouldShowRequestPermissionRationale(permissions: Array<String>) =
            permissions.any {
                ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            }

    private fun requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.message_record_capture_permission_required))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        } else {
            ActivityCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.size == VIDEO_PERMISSIONS.size) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        return
                    }
                }
                launchRecordActivity()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    //endregion
}