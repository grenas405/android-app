package dev.grenas.hello3d.scene

import dev.grenas.hello3d.data.Profile
import kotlin.random.Random

/** One character slot on a 3D text line. */
data class GlyphCell(
    val char: Char,
    val visible: Boolean = true,
    /** Rendered in the accent material (the personal parts of the sentence). */
    val emphasis: Boolean = false,
    /** While non-null, this random character is shown instead of [char] (matrix scramble). */
    val scramble: Char? = null,
)

/** The state of one line: its cells and whether the typing cursor is shown. */
data class LineState(val cells: List<GlyphCell> = emptyList(), val cursor: Boolean = false) {
    val text: String get() = buildString { cells.forEach { append(it.char) } }
    fun reveal(index: Int): LineState =
        copy(cells = cells.mapIndexed { i, cell -> if (i == index) cell.copy(visible = true) else cell })
}

/** Where the script writes line states (the Compose-backed [LineBoard] in the app, a fake in tests). */
interface LineSink {
    fun get(line: Int): LineState
    fun set(line: Int, state: LineState)
}

sealed interface Step {
    /** Typewriter: reveal [text] one glyph at a time with a cursor. Cells from [emphasisFrom] are accented. */
    data class Type(val line: Int, val text: String, val emphasisFrom: Int, val charMillis: Long = 45) : Step

    /** Matrix scramble: cells beyond the common prefix with the current text cycle random glyphs, then settle left to right. */
    data class Scramble(val line: Int, val text: String, val emphasisFrom: Int, val durationMillis: Long = 780) : Step

    data class Hold(val millis: Long) : Step
}

/** [intro] plays once; [loop] repeats forever (cycling languages). */
data class Script(val intro: List<Step>, val loop: List<Step>)

/**
 * Pure choreography for "Hello World, my name is …" — no Android or Compose types, so it is unit
 * testable. [play] drives a [LineSink] with suspending delays that tests can replace.
 */
object SentenceScript {

    const val LINE_NAME = 0
    const val LINE_LANGUAGES = 1
    const val LINE_LOCATION = 2
    const val LINE_COUNT = 3

    const val NAME_LABEL = "my name is "
    const val LANGUAGES_LABEL = "I program in "
    const val LOCATION_LABEL = "based in "

    const val FALLBACK_NAME = "Developer"
    const val HOLD_LANGUAGE_MILLIS = 1_900L
    const val CURSOR_LINGER_MILLIS = 380L
    const val SCRAMBLE_TICK_MILLIS = 60L

    fun build(profile: Profile): Script {
        val name = profile.name.trim().ifBlank { FALLBACK_NAME }
        val location = profile.location.trim()
        val languages = profile.languages.map { it.trim() }.filter { it.isNotBlank() }

        val intro = buildList {
            add(Step.Type(LINE_NAME, NAME_LABEL + name, NAME_LABEL.length))
            add(Step.Hold(450))
            if (languages.isNotEmpty()) {
                add(Step.Type(LINE_LANGUAGES, LANGUAGES_LABEL, LANGUAGES_LABEL.length))
                add(Step.Scramble(LINE_LANGUAGES, LANGUAGES_LABEL + languages.first(), LANGUAGES_LABEL.length))
                add(Step.Hold(650))
            }
            if (location.isNotEmpty()) {
                add(Step.Type(LINE_LOCATION, LOCATION_LABEL + location, LOCATION_LABEL.length))
            }
            add(Step.Hold(HOLD_LANGUAGE_MILLIS))
        }
        val loop = if (languages.size > 1) {
            buildList {
                for (i in 1..languages.size) {
                    val language = languages[i % languages.size]
                    add(Step.Scramble(LINE_LANGUAGES, LANGUAGES_LABEL + language, LANGUAGES_LABEL.length))
                    add(Step.Hold(HOLD_LANGUAGE_MILLIS))
                }
            }
        } else {
            emptyList()
        }
        return Script(intro, loop)
    }

    /** Every character the lines can show, so the glyph atlas can be warmed before typing starts. */
    fun charactersFor(profile: Profile): Set<Char> =
        (NAME_LABEL + profile.name + LANGUAGES_LABEL + profile.languages.joinToString("") +
            LOCATION_LABEL + profile.location + FALLBACK_NAME).toSet()

    /** Plain-text version of the animated sentence, for accessibility. */
    fun sentence(profile: Profile): String {
        val name = profile.name.trim().ifBlank { FALLBACK_NAME }
        val languages = profile.languages.filter { it.isNotBlank() }
        val spoken = when (languages.size) {
            0 -> null
            1 -> languages[0]
            else -> languages.dropLast(1).joinToString(", ") + " and " + languages.last()
        }
        return buildString {
            append("Hello World, my name is ").append(name).append('.')
            spoken?.let { append(" I program in ").append(it).append('.') }
            profile.location.trim().takeIf { it.isNotEmpty() }?.let { append(" Based in ").append(it).append('.') }
        }
    }

    /**
     * Runs [script] against [sink]. [loops] bounds the loop section (tests pass a small number; the
     * app passes [Int.MAX_VALUE]). [delay] is injectable so tests run instantly.
     */
    suspend fun play(
        script: Script,
        sink: LineSink,
        alphabet: String,
        random: Random = Random.Default,
        loops: Int = Int.MAX_VALUE,
        delay: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
    ) {
        script.intro.forEach { perform(it, sink, alphabet, random, delay) }
        var remaining = loops
        while (remaining > 0 && script.loop.isNotEmpty()) {
            script.loop.forEach { perform(it, sink, alphabet, random, delay) }
            remaining--
        }
    }

    private suspend fun perform(
        step: Step,
        sink: LineSink,
        alphabet: String,
        random: Random,
        delay: suspend (Long) -> Unit,
    ) {
        when (step) {
            is Step.Hold -> delay(step.millis)

            is Step.Type -> {
                val cells = step.text.mapIndexed { i, c ->
                    GlyphCell(c, visible = false, emphasis = i >= step.emphasisFrom)
                }
                sink.set(step.line, LineState(cells, cursor = true))
                for (i in cells.indices) {
                    sink.set(step.line, sink.get(step.line).reveal(i))
                    delay(if (cells[i].char == ' ') step.charMillis / 2 else step.charMillis)
                }
                delay(CURSOR_LINGER_MILLIS)
                sink.set(step.line, sink.get(step.line).copy(cursor = false))
            }

            is Step.Scramble -> {
                val current = sink.get(step.line)
                val keep = if (current.cells.all { it.visible }) commonPrefix(current.text, step.text) else 0
                val target = step.text.mapIndexed { i, c ->
                    if (i < keep) current.cells[i].copy(emphasis = i >= step.emphasisFrom, scramble = null)
                    else GlyphCell(c, visible = true, emphasis = i >= step.emphasisFrom)
                }
                val scrambling = step.text.length - keep
                if (scrambling > 0) {
                    val ticks = (step.durationMillis / SCRAMBLE_TICK_MILLIS).toInt().coerceAtLeast(1)
                    for (tick in 0 until ticks) {
                        val resolvedUpTo = keep + (scrambling * tick) / ticks
                        sink.set(step.line, LineState(target.mapIndexed { i, cell ->
                            when {
                                i < resolvedUpTo || cell.char == ' ' -> cell
                                else -> cell.copy(scramble = randomGlyph(alphabet, random, avoid = cell.char))
                            }
                        }))
                        delay(SCRAMBLE_TICK_MILLIS)
                    }
                }
                sink.set(step.line, LineState(target))
            }
        }
    }

    private fun commonPrefix(a: String, b: String): Int {
        var i = 0
        while (i < a.length && i < b.length && a[i] == b[i]) i++
        return i
    }

    private fun randomGlyph(alphabet: String, random: Random, avoid: Char): Char {
        if (alphabet.isEmpty()) return avoid
        val pick = alphabet[random.nextInt(alphabet.length)]
        return if (pick == avoid && alphabet.length > 1) alphabet[(alphabet.indexOf(pick) + 1) % alphabet.length] else pick
    }
}
