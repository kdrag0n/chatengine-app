package com.khronodragon.android.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
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
        image.intrinsicHeight
        image.isCircular = true
        image.cornerRadius = maxOf(source!!.width, source.height) / 2f

        val bitmap = Bitmap.createBitmap(image.intrinsicWidth, image.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        image.setBounds(0, 0, canvas.width, canvas.height)
        image.draw(canvas)

        source.recycle()
        return bitmap
    }
}

fun ImageView.displayRoundImage(url: String) {
    Picasso.get()
            .load(url)
            .transform(CircleTransformation())
            .into(this)
}