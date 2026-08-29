package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedGradient(
    val id: String,
    val name: String,
    val angleDegrees: Float,
    val stops: List<KeyboardGradientStop>,
    val builtIn: Boolean = false,
) {
    fun toBackdrop(): KeyboardBackdrop = KeyboardBackdrop(angleDegrees = angleDegrees, stops = stops)
}

object GradientLibrary {
    private const val PREFS_NAME = "keywi_gradient_library"
    private const val KEY_GRADIENTS = "gradients_json"

    val builtIns: List<SavedGradient> =
        listOf(
            SavedGradient(
                id = "builtin_birdie_rainbow",
                name = "Birdie Rainbow",
                angleDegrees = BackdropThemePreferences.stateForPreset(BackdropPreset.BIRDIE_RAINBOW).angleDegrees,
                stops = BackdropThemePreferences.stateForPreset(BackdropPreset.BIRDIE_RAINBOW).stops,
                builtIn = true,
            ),
            SavedGradient(
                id = "builtin_sinebow",
                name = "Sinebow",
                angleDegrees = BackdropThemePreferences.stateForPreset(BackdropPreset.SINEBOW).angleDegrees,
                stops = BackdropThemePreferences.stateForPreset(BackdropPreset.SINEBOW).stops,
                builtIn = true,
            ),
        )

    fun load(context: Context): List<SavedGradient> {
        val encoded = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_GRADIENTS, null)
        val custom = encoded?.let(::decodeList).orEmpty()
        return builtIns + custom
    }

    fun saveCustom(
        context: Context,
        gradient: SavedGradient,
    ): SavedGradient {
        val saved = gradient.copy(id = gradient.id.ifBlank { UUID.randomUUID().toString() }, builtIn = false)
        val custom = load(context).filterNot { it.builtIn }.toMutableList()
        val index = custom.indexOfFirst { it.id == saved.id }
        if (index >= 0) custom[index] = saved else custom += saved
        persist(context, custom)
        return saved
    }

    fun delete(
        context: Context,
        id: String,
    ) {
        persist(context, load(context).filterNot { it.builtIn || it.id == id })
    }

    fun exportJson(context: Context): String = encodeList(load(context).filterNot { it.builtIn }).toString(2)

    fun importJson(
        context: Context,
        json: String,
    ): Int {
        val imported = decodeList(json).map { it.copy(id = UUID.randomUUID().toString(), builtIn = false) }
        val custom = load(context).filterNot { it.builtIn } + imported
        persist(context, custom)
        return imported.size
    }

    private fun persist(
        context: Context,
        gradients: List<SavedGradient>,
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            ).edit()
            .putString(KEY_GRADIENTS, encodeList(gradients).toString())
            .apply()
    }

    private fun encodeList(gradients: List<SavedGradient>): JSONArray =
        JSONArray().apply {
            gradients.forEach { gradient ->
                put(
                    JSONObject().apply {
                        put("id", gradient.id)
                        put("name", gradient.name)
                        put("type", "linear")
                        put("angle", gradient.angleDegrees.toDouble())
                        put(
                            "stops",
                            JSONArray().apply {
                                gradient.stops.forEach { stop ->
                                    put(
                                        JSONObject().apply {
                                            put("position", stop.position.toDouble())
                                            put("argb", stop.color.toArgb())
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
            }
        }

    private fun decodeList(json: String): List<SavedGradient> =
        runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val stopsJson = item.getJSONArray("stops")
                    val stops =
                        buildList {
                            for (stopIndex in 0 until stopsJson.length()) {
                                val stop = stopsJson.getJSONObject(stopIndex)
                                add(
                                    KeyboardGradientStop(
                                        position = stop.getDouble("position").toFloat().coerceIn(0f, 1f),
                                        color = Color(stop.getInt("argb")),
                                    ),
                                )
                            }
                        }.sortedBy { it.position }
                    if (stops.size >= 2) {
                        add(
                            SavedGradient(
                                id = item.optString("id", UUID.randomUUID().toString()),
                                name = item.optString("name", "Imported gradient"),
                                angleDegrees = item.optDouble("angle", 0.0).toFloat(),
                                stops = stops,
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
}
