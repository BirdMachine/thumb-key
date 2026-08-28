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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
            AndroidView(
                modifier = modifier.fillMaxSize().alpha(state.opacity),
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { imageView ->
                    if (uri == null) {
                        imageView.setImageDrawable(null)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        runCatching {
                            decodeModernBackdrop(context, uri)
                        }.onSuccess { drawable ->
                            imageView.setImageDrawable(drawable)
                        }.onFailure {
                            imageView.setImageURI(uri)
                        }
                    } else {
                        imageView.setImageURI(uri)
                    }
                },
            )
        }

        BackdropMode.NONE -> {}
    }
}
