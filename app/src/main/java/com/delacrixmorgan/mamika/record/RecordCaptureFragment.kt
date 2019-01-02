package com.delacrixmorgan.mamika.record

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.HandlerThread
import android.view.*
import androidx.fragment.app.Fragment
import com.delacrixmorgan.mamika.MainActivity.Companion.VIDEO_PERMISSIONS
import com.delacrixmorgan.mamika.R
import com.delacrixmorgan.mamika.calculateFingerSpacing
import com.delacrixmorgan.mamika.common.FileType
import com.delacrixmorgan.mamika.common.GestureListener
import com.delacrixmorgan.mamika.common.PermissionsUtils
import com.delacrixmorgan.mamika.common.SwipeGesture
import com.delacrixmorgan.mamika.performHapticContextClick
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_record_capture.*
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * RecordCaptureFragment
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class RecordCaptureFragment : Fragment(), SwipeGesture {
    companion object {
        private const val REQUEST_GALLERY_PICK = 1

        const val MAX_DURATION_IN_MILLISECONDS: Long = 30 * 1000
        const val EXTRA_PERMISSION_MISSING = "permissionMissing"

        fun newInstance(): RecordCaptureFragment = RecordCaptureFragment()
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            mamikaCameraDevice = cameraDevice

            startPreview()
            this@RecordCaptureFragment.activity?.configureTransform(videoSize, textureView.width, textureView.height)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            mamikaCameraDevice = null

            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            mamikaCameraDevice = null

            cameraDevice.close()
            this@RecordCaptureFragment.activity?.finish()
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            this@RecordCaptureFragment.activity?.configureTransform(videoSize, width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
    }

    private val countDownTimer = object : CountDownTimer(MAX_DURATION_IN_MILLISECONDS, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val elapseTime: Int = ((MAX_DURATION_IN_MILLISECONDS - millisUntilFinished) / 1000).toInt() + 1
            this@RecordCaptureFragment.progressBar.progress = elapseTime
        }

        override fun onFinish() {
            stopRecordingVideo()
            this@RecordCaptureFragment.progressBar.progress = 0
        }
    }

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraManager = this.activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record_capture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = this.activity ?: return
        val isFlashSupported = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        gestureDetector = GestureDetector(activity, GestureListener(this))

        this.galleryButton.setOnClickListener {
            launchGalleryVideoPickerIntent()
        }

        this.recordButton.setOnTouchListener { _, event ->
            this.flashButton.visibility = View.VISIBLE
            this.galleryButton.visibility = View.INVISIBLE

            gestureDetector.onTouchEvent(event)
            panToZoom(event)
            captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
            true
        }

        this.flashButton.setOnClickListener {
            if (isFlashSupported) {
                toggleCameraFlash()
            }
        }

        this.switchButton.setOnClickListener {
            if (PermissionsUtils.isPermissionGranted(activity, PermissionsUtils.Permission.CAMERA) && !isRecordingVideo) {
                switchCameraOutputs()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_GALLERY_PICK) {
            if (resultCode == Activity.RESULT_OK) {
                val videoUri = data?.data ?: return
                val videoUrl = this.context?.getFilePathFromVideoURI(videoUri) ?: ""
                val mediaPlayer = MediaPlayer.create(this.context, videoUri)

                if (mediaPlayer.duration <= MAX_DURATION_IN_MILLISECONDS) {
                    launchPreviewFragment(videoUrl, true)
                } else {
                    trimVideoDuration(videoUrl)
                }
                mediaPlayer.release()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (this.textureView.isAvailable) {
            openCamera()
            this.flashButton.visibility = View.INVISIBLE
            this.galleryButton.visibility = View.VISIBLE
        } else {
            this.textureView.surfaceTextureListener = surfaceTextureListener
            this.textureView.setOnTouchListener { _, event ->
                if (event.pointerCount == 2) {
                    pinchToZoom(event)
                }
                captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                true
            }
        }
    }

    override fun onPause() {
        closeCamera()
        stopRecordingVideo()
        stopBackgroundThread()

        super.onPause()
    }

    //endregion

    //region LaunchFragments
    private fun launchGalleryVideoPickerIntent() {
        val galleryVideoIntent = Intent()
        galleryVideoIntent.type = "video/*"
        galleryVideoIntent.action = Intent.ACTION_PICK

        startActivityForResult(
                Intent.createChooser(
                        galleryVideoIntent,
                        getString(R.string.record_capture_title_gallery_select)
                ), REQUEST_GALLERY_PICK
        )
    }

    private fun launchPreviewFragment(videoUrl: String, isGalleryVideo: Boolean = false) {
        val previewFragment = RecordPreviewFragment.newInstance(videoUrl, isGalleryVideo)
        this.activity?.apply {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.routingContainer, previewFragment, previewFragment.javaClass.simpleName)
                    .addToBackStack(previewFragment.javaClass.simpleName)
                    .commit()
        }
    }
    //endregion

    //region Gallery
    private fun trimVideoDuration(videoUrl: String) {
        val context = this.context ?: return
        var isTrimSuccessful = false
        val ffmpeg = FFmpeg.getInstance(context)
        val outputFile = context.getVideoFilePath(FileType.MP4)
        val maxDurationInSeconds = MAX_DURATION_IN_MILLISECONDS / 1000
        val command =
                arrayOf("-i", videoUrl, "-ss", "00:00:00", "-codec", "copy", "-t", "$maxDurationInSeconds", outputFile)

        if (ffmpeg.isSupported) {
            ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
                override fun onStart() {
                    this@RecordCaptureFragment.loadingViewGroup.visibility = View.VISIBLE
                }

                override fun onSuccess(message: String?) {
                    isTrimSuccessful = true
                }

                override fun onFailure(message: String?) {
                    isTrimSuccessful = false
                    this@RecordCaptureFragment.loadingViewGroup.visibility = View.GONE
                    Snackbar.make(
                            this@RecordCaptureFragment.parentViewGroup,
                            getString(R.string.record_capture_message_trim_fail),
                            Snackbar.LENGTH_SHORT
                    ).show()
                }

                override fun onFinish() {
                    if (isTrimSuccessful) {
                        this@RecordCaptureFragment.loadingViewGroup.visibility = View.GONE
                        launchPreviewFragment(outputFile, true)
                    }
                }
            })
        } else {
            Snackbar.make(
                    this@RecordCaptureFragment.parentViewGroup,
                    getString(R.string.record_capture_message_not_supported),
                    Snackbar.LENGTH_SHORT
            ).show()
        }
    }
    //endregion

    //region BackgroundThread
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(this.tag)
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    //endregion

    //region Camera Zoom
    private fun pinchToZoom(event: MotionEvent) {
        val characteristics = cameraManager.getCameraCharacteristics(cameraDirection)
        val sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val currentFingerSpacing: Float = event.calculateFingerSpacing()

        if (fingerSpacing != 0F) {
            var zoomSensitivity = 0.05F

            // Prevent Over Zoom-In
            if (currentFingerSpacing > fingerSpacing) {
                if ((maximumZoomLevel - zoomLevel) <= zoomSensitivity) {
                    zoomSensitivity = maximumZoomLevel - zoomLevel
                }
                zoomLevel += zoomSensitivity

            } else {
                // Prevent Over Zoom-Out
                if (currentFingerSpacing < fingerSpacing) {
                    if ((zoomLevel - zoomSensitivity) < 1f) {
                        zoomSensitivity = zoomLevel - 1f
                    }
                    zoomLevel -= zoomSensitivity
                }
            }

            val ratio = 1 / zoomLevel
            val croppedWidth = sensorRect.width() - Math.round(sensorRect.width() * ratio)
            val croppedHeight = sensorRect.height() - Math.round(sensorRect.height() * ratio)

            zoomRect = Rect(
                    croppedWidth / 2,
                    croppedHeight / 2,
                    sensorRect.width() - croppedWidth / 2,
                    sensorRect.height() - croppedHeight / 2
            )
            previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        }
        fingerSpacing = currentFingerSpacing
    }

    private fun panToZoom(event: MotionEvent) {
        val characteristics = cameraManager.getCameraCharacteristics(cameraDirection)
        val sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                anchorPosition = event.rawY
                maximumRawY = maximumZoomLevel * anchorPosition

                if (!isRecordingVideo) {
                    toggleRecordVideo()
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isRecordingVideo) {
                    toggleRecordVideo()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                zoomLevel = maximumZoomLevel - (event.rawY / anchorPosition) * maximumZoomLevel

                val ratio = 1 / zoomLevel
                val croppedWidth = sensorRect.width() - Math.round(sensorRect.width() * ratio)
                val croppedHeight = sensorRect.height() - Math.round(sensorRect.height() * ratio)

                zoomRect = Rect(
                        croppedWidth / 2,
                        croppedHeight / 2,
                        sensorRect.width() - croppedWidth / 2,
                        sensorRect.height() - croppedHeight / 2
                )
                previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            }
        }
    }
    //endregion

    //region SwipeGesture
    override fun onTap() {
        toggleRecordVideo()
    }
    //endregion

    //region Camera
    @SuppressLint("MissingPermission")
    @Throws(RuntimeException::class)
    private fun openCamera() {
        val activity = this.activity
        if (activity == null || activity.isFinishing) return

        if (!activity.hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_PERMISSION_MISSING, true)

            activity.setResult(Activity.RESULT_CANCELED, resultIntent)
            activity.finish()
            return
        }

        if (!this.textureView.isAvailable) {
            return
        }

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Snackbar.make(
                        this.parentViewGroup,
                        getString(R.string.message_record_capture_timeout_exception_error),
                        Snackbar.LENGTH_SHORT
                ).show()
                return
            }

            val cameraId = cameraDirection
            val characteristics = cameraManager.getCameraCharacteristics(cameraDirection)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            maximumZoomLevel = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)

            if (map == null) {
                Snackbar.make(
                        this.parentViewGroup,
                        getString(R.string.message_record_capture_preview_sizes_exception_error),
                        Snackbar.LENGTH_SHORT
                ).show()
                return
            }

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(videoSize.width, videoSize.height)
            } else {
                textureView.setAspectRatio(videoSize.height, videoSize.width)
            }

            activity.configureTransform(videoSize, this.textureView.width, this.textureView.height)
            mediaRecorder = MediaRecorder()
            cameraManager.openCamera(cameraId, this.stateCallback, null)

        } catch (e: NullPointerException) {
            Snackbar.make(this.parentViewGroup, getString(R.string.message_record_capture_camera_api_not_supported_exception_error), Snackbar.LENGTH_SHORT).show()
        } catch (e: InterruptedException) {
            Snackbar.make(this.parentViewGroup, getString(R.string.message_record_capture_interrupted_exception_error), Snackbar.LENGTH_SHORT).show()
        } catch (e: CameraAccessException) {
            Snackbar.make(this.parentViewGroup, getString(R.string.message_record_capture_unable_access_camera_exception_error), Snackbar.LENGTH_SHORT).show()
            activity.finish()
        }
    }

    @Throws(RuntimeException::class)
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            closePreviewSession()
            mamikaCameraDevice?.close()
            mamikaCameraDevice = null

            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: InterruptedException) {
            Snackbar.make(
                    this.parentViewGroup,
                    getString(R.string.message_record_capture_interrupted_exception_error),
                    Snackbar.LENGTH_SHORT
            ).show()
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startPreview() {
        try {
            if (mamikaCameraDevice == null || !this.textureView.isAvailable) return
            closePreviewSession()

            val texture = this.textureView.surfaceTexture
            texture.setDefaultBufferSize(videoSize.width, videoSize.height)

            val previewSurface = Surface(texture)

            previewRequestBuilder = mamikaCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            previewRequestBuilder.addTarget(previewSurface)

            mamikaCameraDevice?.createCaptureSession(listOf(previewSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    updatePreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Snackbar.make(
                            this@RecordCaptureFragment.parentViewGroup,
                            getString(R.string.message_record_capture_session_exception_error),
                            Snackbar.LENGTH_SHORT
                    ).show()
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Snackbar.make(
                    this.parentViewGroup,
                    getString(R.string.message_record_capture_camera_access_exception_error),
                    Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun updatePreview() {
        try {
            if (mamikaCameraDevice == null) return
            HandlerThread(this.tag).start()

            previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Snackbar.make(
                    this.parentViewGroup,
                    getString(R.string.message_record_capture_camera_access_exception_error),
                    Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleCameraFlash() {
        if (cameraDirection == CAMERA_BACK) {
            if (isFlashOn) {
                this.flashButton.setImageResource(R.drawable.ic_flash_on)
                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
            } else {
                this.flashButton.setImageResource(R.drawable.ic_flash_off)
                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
            }

            isFlashOn = !isFlashOn
            captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
        }
    }

    @SuppressLint("MissingPermission")
    @Throws(Exception::class)
    private fun switchCameraOutputs() {
        when (cameraDirection) {
            CAMERA_BACK -> {
                if (isFlashOn) {
                    isFlashOn = !isFlashOn
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
                    captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                }
                cameraDirection = CAMERA_FRONT
            }
            CAMERA_FRONT -> {
                cameraDirection = CAMERA_BACK
            }
        }

        cameraOpenCloseLock.release()
        mamikaCameraDevice?.close()

        if (cameraManager.cameraIdList.contains(cameraDirection)) {
            cameraManager.openCamera(cameraDirection, this.stateCallback, null)
            val characteristics = cameraManager.getCameraCharacteristics(cameraDirection)
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        }
    }
    //endregion

    //region Record Controls
    private fun toggleRecordVideo() {
        this.recordButton.performHapticContextClick()

        if (isRecordingVideo) {
            stopRecordingVideo()
            this.progressBar.visibility = View.GONE
        } else {
            this.progressBar.visibility = View.VISIBLE
            this.recordButton.isEnabled = false
            startRecordingVideo()
        }
    }

    @Throws(IOException::class)
    private fun setUpMediaRecorder() {
        val cameraActivity = this.activity ?: return

        if (videoAbsolutePath.isNullOrEmpty()) {
            videoAbsolutePath = this.context?.getVideoFilePath(FileType.MP4)
        }

        val rotation = cameraActivity.windowManager.defaultDisplay.rotation
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES -> mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
            SENSOR_ORIENTATION_INVERSE_DEGREES -> mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
        }

        val cameraProfile = CamcorderProfile.get(cameraDirection.toInt(), CamcorderProfile.QUALITY_HIGH)

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            setProfile(cameraProfile)
            setOutputFile(videoAbsolutePath)

            setMaxDuration(MAX_DURATION_IN_MILLISECONDS.toInt())
            setOnInfoListener { _, info, _ ->
                if (info == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecordingVideo()
                }
            }

            setVideoSize(videoSize.width, videoSize.height)
            prepare()
        }
    }

    private fun startRecordingVideo() {
        if (mamikaCameraDevice == null || !this.textureView.isAvailable) return

        try {
            closePreviewSession()
            setUpMediaRecorder()

            this.textureView.surfaceTexture.setDefaultBufferSize(videoSize.width, videoSize.height)

            val previewSurface = Surface(this.textureView.surfaceTexture)
            val recorderSurface = mediaRecorder?.surface ?: return
            val surfaces = arrayListOf(previewSurface, recorderSurface)

            val cameraDevice = mamikaCameraDevice ?: return
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
            }

            cameraDevice.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    with(this@RecordCaptureFragment) {
                        captureSession = cameraCaptureSession
                        updatePreview()

                        activity?.runOnUiThread {
                            val mediaRecorder = mediaRecorder ?: return@runOnUiThread

                            mediaRecorder.start()
                            countDownTimer.start()

                            isRecordingVideo = true
                            recordButton.isEnabled = true
                            recordButton.setImageResource(R.drawable.ic_camera_stop)
                        }
                    }
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Snackbar.make(this@RecordCaptureFragment.parentViewGroup, getString(R.string.message_record_capture_session_exception_error), Snackbar.LENGTH_SHORT).show()
                }
            }, backgroundHandler)

        } catch (e: CameraAccessException) {
            Snackbar.make(this.parentViewGroup, getString(R.string.message_record_capture_camera_access_exception_error), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun stopRecordingVideo() {
        if (this.progressBar.progress > 1) {
            this.recordButton.setImageResource(0)
            this.flashButton.setImageResource(R.drawable.ic_flash_on)

            this.progressBar.progress = 0

            try {
                isRecordingVideo = false
                this.countDownTimer.cancel()

                captureSession?.stopRepeating()
                captureSession?.abortCaptures()

                mediaRecorder?.apply {
                    stop()
                    reset()
                }

                if (videoAbsolutePath != "") {
                    this.launchPreviewFragment(videoAbsolutePath.toString())
                }

                videoAbsolutePath = null
                startPreview()

            } catch (e: RuntimeException) {
                Snackbar.make(this.parentViewGroup, getString(R.string.message_record_capture_runtime_exception_error), Snackbar.LENGTH_SHORT).show()
                this.activity?.finish()
            } catch (e: CameraAccessException) {
                Snackbar.make(this.parentViewGroup, getString(R.string.message_record_capture_camera_access_exception_error), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }
    //endregion
}