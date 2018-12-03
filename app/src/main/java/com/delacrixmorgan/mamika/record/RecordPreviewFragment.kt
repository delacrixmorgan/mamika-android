package com.delacrixmorgan.mamika.record

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.delacrixmorgan.mamika.R
import com.delacrixmorgan.mamika.common.FileType
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_record_preview.*
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler
import nl.bravobit.ffmpeg.FFmpeg
import java.io.File

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
    private lateinit var bandwidthMeter: DefaultBandwidthMeter
    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    private var videoUrl: String = ""
    private var isGalleryVideo: Boolean = false
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var paletteFilePath = "/$FILENAME_FFMPEG_PALATTE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.ffmpeg = FFmpeg.getInstance(this.context)
        this.paletteFilePath = "${this.context?.filesDir}/$FILENAME_FFMPEG_PALATTE"

        this.bandwidthMeter = DefaultBandwidthMeter()
        this.dataSourceFactory = DefaultDataSourceFactory(this.context, Util.getUserAgent(this.context, this.activity?.packageName), this.bandwidthMeter as TransferListener)

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
        generatePalette()

        this.generateButton.setOnClickListener {
            generatePalette()
        }
    }

    override fun onStart() {
        super.onStart()
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
        val videoTrackSelectionFactory: TrackSelection.Factory = AdaptiveTrackSelection.Factory(this.bandwidthMeter)
        val mediaSource = ExtractorMediaSource.Factory(this.dataSourceFactory).createMediaSource(Uri.parse(videoUrl))

        this.trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        this.simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this.context, this.trackSelector)

        this.simpleExoPlayer?.prepare(mediaSource)
        this.simpleExoPlayer?.playWhenReady = true
        this.simpleExoPlayer?.repeatMode = Player.REPEAT_MODE_ALL

        this.playerView.player = this.simpleExoPlayer
        this.playerView.useController = false
        this.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

    private fun releasePlayer() {
        if (this.simpleExoPlayer == null) return

        this.simpleExoPlayer?.stop()
        this.simpleExoPlayer?.release()

        this.simpleExoPlayer = null
        this.trackSelector = null
    }

    private fun generatePalette() {
        if (!this.ffmpeg.isSupported) {
            Snackbar.make(this@RecordPreviewFragment.parentViewGroup, getString(R.string.record_capture_message_not_supported), Snackbar.LENGTH_SHORT).show()
            return
        }

        val filters = "fps=15,scale=300:-1:flags=lanczos,palettegen"
        val file = File(this.paletteFilePath)
        if (!file.exists()) file.createNewFile()

        val command = arrayOf("-y", "-v", "warning", "-i", this.videoUrl, "-vf", filters, this.paletteFilePath)

//        val command = arrayOf("-y", "-i", this.videoUrl, "-vf", filters, palette)
//        val command = arrayOf("-i", this.videoUrl, "-vf", "palettegen=max_colors=24", palette)
        //ffmpeg -y -ss 30 -t 3 -i input.flv \
        //-vf fps=10,scale=320:-1:flags=lanczos,palettegen palette.png

        this.ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
            override fun onStart() {
                this@RecordPreviewFragment.loadingViewGroup.visibility = View.VISIBLE
            }

            override fun onProgress(message: String?) {
                super.onProgress(message)
            }

            override fun onSuccess(message: String?) {
                generateGif()
            }

            override fun onFailure(message: String?) {
                this@RecordPreviewFragment.loadingViewGroup.visibility = View.GONE
                Snackbar.make(this@RecordPreviewFragment.parentViewGroup, getString(R.string.record_capture_message_trim_fail), Snackbar.LENGTH_SHORT).show()
            }

            override fun onFinish() = Unit
        })
    }

    private fun generateGif() {
        val context = this.context ?: return
        var isConversionSuccessful = false

        val outputFile = context.getVideoFilePath(FileType.GIF)

        val filters = "fps=15,scale=320:-1:flags=lanczos"
        val command = arrayOf("-v", "warning", "-stats", "-i", this.videoUrl, "-i", this.paletteFilePath, "-lavfi", "$filters [x]; [x][1:v] paletteuse", "-y", outputFile)

        this.ffmpeg.execute(command, object : ExecuteBinaryResponseHandler() {
            override fun onStart() {
                this@RecordPreviewFragment.loadingViewGroup.visibility = View.VISIBLE

                val file = File(this@RecordPreviewFragment.videoUrl)
                Log.i("RecordPreviewFragment", "totalSpace: ${file.totalSpace}")
            }

            override fun onProgress(message: String?) {
                Log.i("RecordPreviewFragment", message)
            }

            override fun onSuccess(message: String?) {
                isConversionSuccessful = true
            }

            override fun onFailure(message: String?) {
                isConversionSuccessful = false
                this@RecordPreviewFragment.loadingViewGroup.visibility = View.GONE
                Snackbar.make(this@RecordPreviewFragment.parentViewGroup, getString(R.string.record_capture_message_trim_fail), Snackbar.LENGTH_SHORT).show()
            }

            override fun onFinish() {
                if (isConversionSuccessful) {
                    this@RecordPreviewFragment.loadingViewGroup.visibility = View.GONE
                    launchPreviewFragment(outputFile)
                }
            }
        })
    }

    private fun launchPreviewFragment(outputFile: String) {

    }
}