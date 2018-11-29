package com.delacrixmorgan.mamika.common

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.View

/**
 * AutoFitTextureView
 * mamika-android
 *
 * Created by Delacrix Morgan on 29/11/2018.
 * Copyright (c) 2018 licensed under a Creative Commons Attribution-ShareAlike 4.0 International License.
 */

class AutoFitTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    /**
     * A [TextureView] that can be adjusted to a specified aspect ratio.
     */

    private var ratioWidth = 0
    private var ratioHeight = 0

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        this.ratioWidth = width
        this.ratioHeight = height

        this.requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)

        if (this.ratioWidth == 0 || this.ratioHeight == 0) {
            this.setMeasuredDimension(width, height)
        } else {
            if (this.width < ((this.height * this.ratioWidth) / this.ratioHeight)) {
                this.setMeasuredDimension(width, (width * this.ratioHeight) / this.ratioWidth)
            } else {
                this.setMeasuredDimension((height * this.ratioWidth) / this.ratioHeight, height)
            }
        }
    }
}