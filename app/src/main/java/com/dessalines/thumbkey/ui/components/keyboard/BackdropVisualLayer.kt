package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@RequiresApi(Build.VERSION_CODES.P)
private fun decodeModernBackdrop(
    context: Context,
    uri: Uri,
): Drawable {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeDrawable(source).also { drawable ->
        if (drawable is AnimatedImageDrawable) {
            drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            drawable.start()
        }
    }
}

@Composable
fun BackdropVisualLayer(
    state: BackdropThemeState,
    modifier: Modifier = Modifier,
) {
    when (state.mode) {
        BackdropMode.COLORFUL -> {
            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .alpha(state.opacity)
                        .keyboardGradientBackground(state.toBackdrop()),
            )
        }

        BackdropMode.IMAGE,
        BackdropMode.GIF,
        -> {
            val context = LocalContext.current
            val uri = state.mediaUri?.let(Uri::parse)
            val decodedDrawable =
                remember(context, state.mediaUri) {
                    if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        runCatching { decodeModernBackdrop(context, uri) }.getOrNull()
                    } else {
                        null
                    }
                }

            AndroidView(
                modifier =
                    modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .alpha(state.opacity),
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { imageView ->
                    when {
                        uri == null -> {
                            imageView.tag = null
                            imageView.setImageDrawable(null)
                        }

                        decodedDrawable != null -> {
                            if (imageView.drawable !== decodedDrawable) {
                                imageView.setImageDrawable(decodedDrawable)
                            }
                            imageView.tag = state.mediaUri
                        }

                        imageView.tag != state.mediaUri -> {
                            imageView.setImageURI(uri)
                            imageView.tag = state.mediaUri
                        }
                    }
                },
            )
        }

        BackdropMode.NONE -> {}
    }
}
