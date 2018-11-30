package com.delacrixmorgan.mamika.record

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.delacrixmorgan.mamika.R
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
import kotlinx.android.synthetic.main.fragment_record_preview.*

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

        fun newInstance(videoUrl: String, isGalleryVideo: Boolean): RecordPreviewFragment {
            val fragment = RecordPreviewFragment()
            val args = Bundle()

            args.putString(ARG_RECORD_PREVIEW_VIDEO_URL, videoUrl)
            args.putBoolean(ARG_RECORD_PREVIEW_IS_GALLERY_VIDEO, isGalleryVideo)

            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var bandwidthMeter: DefaultBandwidthMeter
    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    private var trackSelector: DefaultTrackSelector? = null
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var isGalleryVideo: Boolean = false
    private var videoUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        if (this.simpleExoPlayer == null) {
            return
        }

        this.simpleExoPlayer?.stop()
        this.simpleExoPlayer?.release()

        this.simpleExoPlayer = null
        this.trackSelector = null
    }
}