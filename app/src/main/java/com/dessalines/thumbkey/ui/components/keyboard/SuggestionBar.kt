package com.dessalines.thumbkey.ui.components.keyboard

import android.content.Context
import android.text.InputType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dessalines.thumbkey.IMEService
import kotlinx.coroutines.delay
import java.util.Locale

private const val SUGGESTION_PREFS = "suggestion_preferences"
private const val SUGGESTIONS_ENABLED = "suggestions_enabled"
private val WORD_PATTERN = Regex("[A-Za-z']+$")

object SuggestionPreferences {
    fun enabled(context: Context): Boolean =
        context
            .getSharedPreferences(SUGGESTION_PREFS, Context.MODE_PRIVATE)
            .getBoolean(SUGGESTIONS_ENABLED, true)

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(SUGGESTION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SUGGESTIONS_ENABLED, enabled)
            .apply()
    }
}

private object LocalSuggestionEngine {
    private val commonWords =
        """
        about
        above
        absolutely
        actually
        add
        added
        after
        again
        all
        almost
        already
        also
        always
        amazing
        and
        android
        animation
        another
        answer
        any
        anything
        app
        application
        are
        around
        ask
        asked
        awesome
        back
        background
        bad
        bar
        because
        become
        been
        before
        best
        better
        between
        big
        bird
        birds
        black
        blue
        border
        both
        build
        button
        called
        can
        change
        changes
        clean
        clear
        clipboard
        close
        code
        color
        colors
        come
        commit
        complete
        completion
        computer
        control
        controls
        cool
        correct
        correction
        create
        created
        current
        cursor
        custom
        data
        day
        default
        definitely
        delete
        device
        dictionary
        different
        disable
        disabled
        does
        easy
        edit
        editor
        email
        emoji
        enable
        enabled
        english
        enough
        enter
        especially
        even
        every
        everything
        exactly
        example
        feature
        features
        feel
        feels
        file
        files
        finally
        find
        first
        fix
        fixed
        font
        found
        friend
        friends
        full
        fun
        funny
        fuzzy
        game
        games
        get
        give
        go
        gold
        good
        gradient
        great
        guess
        happen
        happened
        happy
        hard
        have
        hello
        help
        here
        hey
        hide
        how
        idea
        image
        images
        input
        install
        installed
        issue
        just
        keep
        keyboard
        key
        keys
        keywi
        kind
        know
        language
        last
        layer
        learn
        learning
        left
        less
        light
        like
        little
        live
        load
        loaded
        local
        long
        look
        love
        lovely
        made
        make
        many
        match
        matching
        maybe
        mean
        means
        message
        messagease
        middle
        might
        more
        most
        move
        moved
        much
        name
        need
        needed
        never
        new
        next
        nice
        night
        nope
        nothing
        number
        of
        offline
        okay
        old
        on
        online
        only
        opacity
        open
        option
        options
        other
        output
        overlay
        part
        persist
        persistent
        phone
        photo
        photos
        picture
        pink
        place
        play
        please
        point
        position
        possible
        possibly
        prefer
        preset
        pretty
        preview
        private
        probably
        problem
        public
        purple
        question
        quick
        rainbow
        read
        reading
        really
        reason
        red
        remember
        remove
        removed
        right
        run
        running
        same
        samsung
        save
        saved
        screen
        setting
        settings
        shiny
        short
        should
        show
        side
        simple
        single
        size
        slow
        small
        software
        something
        sometimes
        space
        spell
        spelling
        start
        still
        stop
        stops
        strip
        style
        suggestion
        suggestions
        sure
        swipe
        system
        take
        tap
        test
        testing
        text
        than
        thank
        thanks
        that
        the
        their
        them
        then
        there
        these
        thing
        things
        think
        this
        thought
        through
        thumbkey
        time
        today
        together
        toggle
        top
        touch
        transparent
        transparency
        try
        tried
        trying
        type
        typing
        under
        understand
        update
        use
        used
        using
        version
        very
        want
        wanted
        water
        way
        we
        week
        well
        what
        when
        where
        which
        while
        white
        why
        width
        will
        wish
        with
        word
        words
        work
        working
        works
        world
        worse
        would
        write
        writing
        yeah
        year
        yes
        yesterday
        you
        your
        """.trimIndent()
            .lineSequence()
            .map { it.trim().lowercase(Locale.US) }
            .filter { it.length > 1 }
            .distinct()
            .toList()

    fun suggest(
        prefix: String,
        limit: Int = 3,
    ): List<String> {
        if (prefix.length < 2) return emptyList()
        val normalized = prefix.lowercase(Locale.US)
        val matches =
            commonWords
                .asSequence()
                .filter { it.startsWith(normalized) && it != normalized }
                .take(limit)
                .toList()
        return if (prefix.firstOrNull()?.isUpperCase() == true) {
            matches.map { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }
        } else {
            matches
        }
    }
}

@Composable
fun SuggestionBar(ime: IMEService) {
    var enabled by remember { mutableStateOf(SuggestionPreferences.enabled(ime)) }
    var prefix by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<String>()) }

    val inputType = ime.currentInputEditorInfo?.inputType ?: 0
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    val privateField =
        variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

    LaunchedEffect(enabled, privateField) {
        while (enabled && !privateField) {
            val beforeCursor =
                ime.currentInputConnection
                    ?.getTextBeforeCursor(64, 0)
                    ?.toString()
                    .orEmpty()
            val nextPrefix = WORD_PATTERN.find(beforeCursor)?.value.orEmpty()
            if (nextPrefix != prefix) {
                prefix = nextPrefix
                suggestions = LocalSuggestionEngine.suggest(nextPrefix)
            }
            delay(80)
        }
        if (!enabled || privateField) {
            prefix = ""
            suggestions = emptyList()
        }
    }

    val displayed =
        when (suggestions.size) {
            0, 1 -> suggestions
            2 -> listOf(suggestions[1], suggestions[0])
            else -> listOf(suggestions[1], suggestions[0], suggestions[2])
        }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.42f else 0.24f),
        modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            displayed.forEachIndexed { index, suggestion ->
                val isBest = (displayed.size > 1 && index == 1) || displayed.size == 1
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color =
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = if (isBest) 0.82f else 0.58f,
                        ),
                    border =
                        if (isBest) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.62f))
                        } else {
                            null
                        },
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable {
                                val currentPrefix = prefix
                                if (currentPrefix.isNotEmpty()) {
                                    ime.currentInputConnection?.deleteSurroundingText(currentPrefix.length, 0)
                                    ime.currentInputConnection?.commitText("$suggestion ", 1)
                                    prefix = ""
                                    suggestions = emptyList()
                                }
                            },
                ) {
                    Text(
                        text = suggestion,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isBest) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            if (displayed.isEmpty()) {
                Text(
                    text = if (enabled && !privateField) "suggestions" else "",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f)
                    } else {
                        Color.Transparent
                    },
                modifier =
                    Modifier.clickable {
                        enabled = !enabled
                        SuggestionPreferences.setEnabled(ime, enabled)
                    },
            ) {
                Text(
                    text = if (enabled) "✨" else "○",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                )
            }
        }
    }
}
