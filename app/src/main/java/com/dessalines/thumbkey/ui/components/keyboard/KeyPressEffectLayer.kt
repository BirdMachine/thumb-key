package com.dessalines.thumbkey.ui.components.keyboard

import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders a fresh media drawable for every [trigger]. AnimatedImageDrawable is deliberately
 * configured with repeatCount = 0 so GIF/WebP animations play once and stop.
 */
@Composable
fun KeyPressEffectLayer(
    mediaUri: String?,
    trigger: Int,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    if (mediaUri.isNullOrBlank() || trigger <= 0) return

    val context = LocalContext.current
    val uri = remember(mediaUri) { runCatching { Uri.parse(mediaUri) }.getOrNull() }
    var finished by remember(mediaUri, trigger) { mutableStateOf(false) }

    val drawable =
        remember(context, mediaUri, trigger) {
            if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeDrawable(source)
                }.getOrNull()
            } else {
                null
            }
        }

    if (drawable == null || finished) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && drawable is AnimatedImageDrawable) {
        DisposableEffect(drawable, trigger) {
            drawable.repeatCount = 0
            val callback =
                object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(animatedDrawable: android.graphics.drawable.Drawable?) {
                        finished = true
                    }
                }
            drawable.registerAnimationCallback(callback)
            drawable.start()
            onDispose {
                drawable.unregisterAnimationCallback(callback)
                drawable.stop()
            }
        }
    }

    AndroidView(
        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .alpha(opacity.coerceIn(0f, 1f)),
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            if (imageView.drawable !== drawable) {
                imageView.setImageDrawable(drawable)
            }
        },
    )
}
