package com.delacrixmorgan.mamika.record

import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.delacrixmorgan.mamika.R
import com.delacrixmorgan.mamika.common.FileType
import com.delacrixmorgan.mamika.saveFileExternally
import com.delacrixmorgan.mamika.setting.SettingListFragment
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_record_preview.*
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import java.io.File
import java.net.URLConnection

/**
 * RecordPreviewFragment
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class RecordPreviewFragment : Fragment() {
    companion object {
        const val ARG_RECORD_PREVIEW_VIDEO_URL = "RecordPreview.videoUrl"
        const val ARG_RECORD_PREVIEW_IS_GALLERY_VIDEO = "RecordPreview.isGalleryVideo"

        private const val FILENAME_FFMPEG_PALATTE = "palette.png"

        fun newInstance(videoUrl: String, isGalleryVideo: Boolean): RecordPreviewFragment {
            val fragment = RecordPreviewFragment()
            val args = Bundle()

            args.putString(ARG_RECORD_PREVIEW_VIDEO_URL, videoUrl)
            args.putBoolean(ARG_RECORD_PREVIEW_IS_GALLERY_VIDEO, isGalleryVideo)

            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var ffmpeg: FFmpeg
    private lateinit var outputFile: File
    private lateinit var bandwidthMeter: DefaultBandwidthMeter
    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    private var videoUrl: String = ""
    private var isVideoSaved = false
    private var isGalleryVideo: Boolean = false
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var paletteFilePath = "/$FILENAME_FFMPEG_PALATTE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.ffmpeg = FFmpeg.getInstance(this.context)
        this.paletteFilePath = "${this.context?.filesDir}/$FILENAME_FFMPEG_PALATTE"

        this.bandwidthMeter = DefaultBandwidthMeter()
        this.dataSourceFactory = DefaultDataSourceFactory(
                this.context,
                Util.getUserAgent(this.context, this.activity?.packageName),
                this.bandwidthMeter as TransferListener
        )

        this.arguments?.let {
            this.videoUrl = it.getString(ARG_RECORD_PREVIEW_VIDEO_URL) ?: ""
            this.isGalleryVideo = it.getBoolean(ARG_RECORD_PREVIEW_IS_GALLERY_VIDEO)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.progressBar.indeterminateDrawable.setColorFilter(ContextCompat.getColor(view.context, R.color.colorPrimary), PorterDuff.Mode.SRC_IN)

        generatePalette()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        initialiseVideoPlayer(this.videoUrl)
    }

    override fun onResume() {
        super.onResume()
        initialiseVideoPlayer(this.videoUrl)
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initialiseVideoPlayer(videoUrl: String) {
        val mediaSource = ExtractorMediaSource.Factory(this.dataSourceFactory).createMediaSource(Uri.parse(videoUrl))

        this.simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this.context, DefaultTrackSelector())
        this.simpleExoPlayer?.prepare(mediaSource)
        this.simpleExoPlayer?.volume = 0F
        this.simpleExoPlayer?.playWhenReady = true
        this.simpleExoPlayer?.repeatMode = Player.REPEAT_MODE_ALL

        this.playerView.player = this.simpleExoPlayer
        this.playerView.useController = false
        this.playerView.resizeMode = RESIZE_MODE_ZOOM
    }

    private fun releasePlayer() {
        if (this.simpleExoPlayer == null) return

        this.simpleExoPlayer?.stop()
        this.simpleExoPlayer?.release()

        this.simpleExoPlayer = null
        this.trackSelector = null
    }

    private fun setupListeners() {
        this.backButton.setOnClickListener {
            this.activity?.supportFragmentManager?.popBackStack()
        }

        this.sendButton.setOnClickListener {
            shareFile(this.outputFile)
        }

        this.retryButton.setOnClickListener {
            this.retryButton.visibility = View.INVISIBLE
            this.progressBar.visibility = View.VISIBLE
            generatePalette()
        }

        this.settingsButton?.setOnClickListener {
            launchSettingsFragment()
        }
    }

    private fun launchSettingsFragment() {
        val settingsFragment = SettingListFragment.newInstance()
        this.activity?.apply {
            supportFragmentManager.beginTransaction()
                    .add(R.id.routingContainer, settingsFragment, settingsFragment.javaClass.simpleName)
                    .addToBackStack(settingsFragment.javaClass.simpleName)
                    .commit()
        }
    }

    private fun generatePalette() {
        if (!this.ffmpeg.isSupported) {
            Snackbar.make(this@RecordPreviewFragment.parentViewGroup, getString(R.string.record_capture_message_not_supported), Snackbar.LENGTH_SHORT).show()
            return
        }

        val filters = "fps=15,scale=480:-1:flags=lanczos,palettegen"
        val paletteFile = File(this.paletteFilePath)
        if (!paletteFile.exists()) paletteFile.createNewFile()

        val command = arrayOf("-y", "-v", "warning", "-i", this.videoUrl, "-vf", filters, this.paletteFilePath)

        this.ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
            override fun onStart() {
                progressBar.visibility = View.VISIBLE
            }

            override fun onSuccess(message: String?) {
                generateGif()
            }

            override fun onFailure(message: String?) {
                progressBar.visibility = View.INVISIBLE
                retryButton.visibility = View.VISIBLE

                Snackbar.make(this@RecordPreviewFragment.parentViewGroup, getString(R.string.record_capture_message_trim_fail), Snackbar.LENGTH_SHORT).show()
            }

            override fun onFinish() = Unit
        })
    }

    private fun generateGif() {
        val context = this.context ?: return
        var isConversionSuccessful = false

        val outputFilePath = context.getVideoFilePath(FileType.GIF)

        val filters = "fps=15,scale=480:-1:flags=lanczos"
        val command = arrayOf("-v", "warning", "-stats", "-i", this.videoUrl, "-i", this.paletteFilePath, "-lavfi", "$filters [x]; [x][1:v] paletteuse", "-y", outputFilePath)

        this.ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
            override fun onStart() {
                progressBar.visibility = View.VISIBLE
                val file = File(this@RecordPreviewFragment.videoUrl)
                Log.i("RecordPreviewFragment", "totalSpace: ${file.totalSpace}")
            }

            override fun onProgress(message: String?) {
                Log.i("RecordPreviewFragment", "onProgress: $message")
            }

            override fun onSuccess(message: String?) {
                isConversionSuccessful = true
            }

            override fun onFailure(message: String?) {
                isConversionSuccessful = false
                retryButton.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
                Snackbar.make(this@RecordPreviewFragment.parentViewGroup, getString(R.string.record_capture_message_trim_fail), Snackbar.LENGTH_SHORT).show()
            }

            override fun onFinish() {
                if (isConversionSuccessful && isVisible) {
                    sendButton.visibility = View.VISIBLE
                    progressBar.visibility = View.INVISIBLE

                    outputFile = File(outputFilePath)

                    if (!outputFile.exists()) outputFile.createNewFile()
                    if (!isVideoSaved) {
                        context.saveFileExternally(outputFilePath)
                        isVideoSaved = true
                    }
                } else {
                    retryButton.visibility = View.VISIBLE
                    progressBar.visibility = View.INVISIBLE
                }
            }
        })
    }

    private fun shareFile(file: File) {
        val context = this.context ?: return
        val intentShareFile = Intent(Intent.ACTION_SEND)
        val fileProvider = FileProvider.getUriForFile(context, "${context.packageName}.fileProvider", file)

        intentShareFile.type = URLConnection.guessContentTypeFromName(file.name)

        intentShareFile.putExtra(Intent.EXTRA_STREAM, fileProvider)
        intentShareFile.putExtra(Intent.EXTRA_TEXT, "#ShotOnMamika️️")
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(Intent.createChooser(intentShareFile, "Share File"))
    }
}