package com.khronodragon.android.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation

object ImageUtils {
    lateinit var resources: Resources
}

class CircleTransformation : Transformation {
    override fun key() = "circle"

    override fun transform(source: Bitmap?): Bitmap {
        val image = RoundedBitmapDrawableFactory.create(ImageUtils.resources, source)
        image.isCircular = true
        image.cornerRadius = maxOf(source!!.width, source.height) / 2f

        return image.bitmap!!
    }
}

fun ImageView.displayRoundImage(url: String) {
    Picasso.get()
            .load(url)
            .transform(CircleTransformation())
            .into(this)
}