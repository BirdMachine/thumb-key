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
        context.getSharedPreferences(SUGGESTION_PREFS, Context.MODE_PRIVATE)
            .getBoolean(SUGGESTIONS_ENABLED, true)

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context.getSharedPreferences(SUGGESTION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SUGGESTIONS_ENABLED, enabled)
            .apply()
    }
}

private object LocalSuggestionEngine {
    private val commonWords =
        """
        the
        be
        to
        of
        and
        a
        in
        that
        have
        i
        it
        for
        not
        on
        with
        he
        as
        you
        do
        at
        this
        but
        his
        by
        from
        they
        we
        say
        her
        she
        or
        an
        will
        my
        one
        all
        would
        there
        their
        what
        so
        up
        out
        if
        about
        who
        get
        which
        go
        me
        when
        make
        can
        like
        time
        no
        just
        him
        know
        take
        people
        into
        year
        your
        good
        some
        could
        them
        see
        other
        than
        then
        now
        look
        only
        come
        its
        over
        think
        also
        back
        after
        use
        two
        how
        our
        work
        first
        well
        way
        even
        new
        want
        because
        these
        give
        day
        most
        us
        is
        are
        was
        were
        been
        being
        has
        had
        did
        does
        should
        may
        might
        must
        need
        really
        very
        much
        more
        less
        many
        few
        another
        every
        each
        both
        same
        different
        own
        such
        any
        something
        anything
        nothing
        everything
        someone
        anyone
        everyone
        where
        why
        while
        before
        again
        still
        already
        always
        never
        often
        sometimes
        maybe
        probably
        sure
        right
        left
        yes
        yeah
        nope
        okay
        ok
        please
        thanks
        thank
        hello
        hey
        hi
        love
        lovely
        awesome
        cool
        nice
        great
        amazing
        pretty
        beautiful
        happy
        fun
        funny
        weird
        little
        big
        small
        long
        short
        high
        low
        old
        young
        easy
        hard
        simple
        quick
        slow
        fast
        better
        best
        bad
        worse
        worst
        thing
        things
        stuff
        place
        part
        point
        problem
        question
        answer
        idea
        reason
        example
        kind
        type
        number
        name
        word
        words
        text
        message
        email
        phone
        computer
        keyboard
        key
        keys
        screen
        app
        application
        software
        code
        file
        files
        data
        system
        setting
        settings
        option
        options
        feature
        features
        update
        change
        changes
        build
        version
        test
        testing
        issue
        fix
        fixed
        working
        works
        make
        made
        create
        created
        add
        added
        remove
        removed
        show
        hide
        open
        close
        start
        stop
        keep
        save
        saved
        load
        loaded
        find
        found
        try
        tried
        trying
        help
        use
        using
        used
        type
        typing
        write
        writing
        read
        reading
        send
        sent
        call
        called
        talk
        talking
        tell
        told
        ask
        asked
        feel
        feels
        felt
        seem
        seems
        become
        became
        leave
        left
        put
        bring
        brought
        move
        moved
        turn
        turned
        run
        running
        play
        playing
        live
        living
        happen
        happened
        happen
        mean
        means
        thought
        remember
        forgot
        understand
        believe
        guess
        hope
        wish
        prefer
        enjoy
        like
        dislike
        want
        wanted
        need
        needed
        able
        possible
        possibly
        actually
        basically
        especially
        exactly
        finally
        definitely
        absolutely
        totally
        mostly
        almost
        enough
        together
        around
        across
        above
        below
        under
        between
        through
        inside
        outside
        here
        there
        today
        tomorrow
        yesterday
        morning
        afternoon
        evening
        night
        week
        month
        year
        minute
        hour
        second
        home
        house
        room
        car
        road
        city
        world
        friend
        friends
        family
        person
        people
        woman
        women
        man
        men
        child
        children
        dog
        cat
        bird
        birds
        food
        water
        coffee
        music
        game
        games
        movie
        movies
        book
        books
        picture
        image
        images
        photo
        photos
        color
        colors
        gradient
        rainbow
        gold
        black
        white
        red
        green
        blue
        pink
        purple
        orange
        yellow
        dark
        light
        clear
        clean
        local
        private
        public
        online
        offline
        current
        next
        last
        top
        bottom
        middle
        side
        center
        full
        single
        custom
        default
        enable
        enabled
        disable
        disabled
        suggestion
        suggestions
        complete
        completion
        autocomplete
        correct
        correction
        autocorrect
        fuzzy
        match
        matching
        learn
        learning
        language
        english
        spell
        spelling
        dictionary
        words
        input
        output
        cursor
        space
        enter
        delete
        backspace
        emoji
        clipboard
        layout
        theme
        style
        font
        border
        background
        transparent
        transparency
        alpha
        opacity
        shiny
        metallic
        layer
        overlay
        animation
        animate
        touch
        tap
        swipe
        gesture
        press
        button
        bar
        strip
        capsule
        preview
        editor
        edit
        control
        controls
        angle
        position
        width
        height
        size
        radius
        color
        stop
        stops
        preset
        custom
        persist
        persistent
        phone
        android
        github
        branch
        commit
        action
        actions
        workflow
        build
        apk
        install
        installed
        update
        device
        samsung
        messagease
        thumbkey
        keywi
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
            commonWords.asSequence()
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
            val beforeCursor = ime.currentInputConnection?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
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
                val isBest = displayed.size > 1 && index == 1 || displayed.size == 1
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
