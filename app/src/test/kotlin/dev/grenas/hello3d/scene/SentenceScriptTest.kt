package dev.grenas.hello3d.scene

import dev.grenas.hello3d.data.Profile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SentenceScriptTest {

    private val profile = Profile(
        name = "Pedro",
        location = "Houston, TX",
        languages = listOf("Kotlin", "Rust", "TypeScript"),
    )

    @Test
    fun `intro types name, scrambles first language, types location`() {
        val script = SentenceScript.build(profile)
        val typed = script.intro.filterIsInstance<Step.Type>()
        assertEquals("my name is Pedro", typed[0].text)
        assertEquals(SentenceScript.LINE_NAME, typed[0].line)
        assertEquals(SentenceScript.NAME_LABEL.length, typed[0].emphasisFrom)
        assertEquals("I program in ", typed[1].text)
        assertEquals("based in Houston, TX", typed[2].text)

        val scrambles = script.intro.filterIsInstance<Step.Scramble>()
        assertEquals(listOf("I program in Kotlin"), scrambles.map { it.text })
    }

    @Test
    fun `loop cycles every language and returns to the first`() {
        val loop = SentenceScript.build(profile).loop.filterIsInstance<Step.Scramble>()
        assertEquals(
            listOf("I program in Rust", "I program in TypeScript", "I program in Kotlin"),
            loop.map { it.text },
        )
    }

    @Test
    fun `blank fields fall back or are skipped`() {
        val script = SentenceScript.build(Profile(name = "  ", location = "", languages = emptyList()))
        val typed = script.intro.filterIsInstance<Step.Type>()
        assertEquals(listOf("my name is Developer"), typed.map { it.text })
        assertTrue(script.intro.none { it is Step.Scramble })
        assertTrue(script.loop.isEmpty())
    }

    @Test
    fun `single language has no loop`() {
        assertTrue(SentenceScript.build(profile.copy(languages = listOf("Go"))).loop.isEmpty())
    }

    @Test
    fun `play settles every line with no scramble left and the cursor off`() = runBlocking {
        val sink = RecordingSink()
        SentenceScript.play(
            script = SentenceScript.build(profile),
            sink = sink,
            alphabet = "01",
            random = Random(1),
            loops = 1,
            delay = {},
        )
        assertEquals("my name is Pedro", sink.get(0).text)
        assertEquals("I program in Kotlin", sink.get(1).text)
        assertEquals("based in Houston, TX", sink.get(2).text)
        for (line in 0 until SentenceScript.LINE_COUNT) {
            val state = sink.get(line)
            assertTrue(state.cells.all { it.visible && it.scramble == null })
            assertFalse(state.cursor)
        }
        val emphasised = sink.get(0).cells.filter { it.emphasis }.map { it.char }.joinToString("")
        assertEquals("Pedro", emphasised)
    }

    @Test
    fun `scramble resolves left to right and keeps the label`() = runBlocking {
        val sink = RecordingSink()
        SentenceScript.play(
            script = SentenceScript.build(profile),
            sink = sink,
            alphabet = "ABC",
            random = Random(7),
            loops = 1,
            delay = {},
        )
        val label = SentenceScript.LANGUAGES_LABEL
        val languageStates = sink.history
            .filter { (line, _) -> line == SentenceScript.LINE_LANGUAGES }
            .map { it.second }
        assertTrue(languageStates.any { state -> state.cells.any { it.scramble != null } })
        for (state in languageStates) {
            if (state.cells.size <= label.length) continue
            if (state.cells.all { it.visible }) {
                assertTrue(state.cells.take(label.length).all { it.scramble == null })
            }
            val tail = state.cells.drop(label.length).filter { it.char != ' ' }
            val firstScrambled = tail.indexOfFirst { it.scramble != null }
            if (firstScrambled >= 0) {
                assertTrue(tail.drop(firstScrambled).all { it.scramble != null })
            }
        }
    }

    @Test
    fun `sentence reads naturally`() {
        assertEquals(
            "Hello World, my name is Pedro. I program in Kotlin, Rust and TypeScript. Based in Houston, TX.",
            SentenceScript.sentence(profile),
        )
        assertEquals(
            "Hello World, my name is Developer.",
            SentenceScript.sentence(Profile(name = "", location = " ", languages = emptyList())),
        )
    }

    private class RecordingSink : LineSink {
        private val lines = MutableList(SentenceScript.LINE_COUNT) { LineState() }
        val history = mutableListOf<Pair<Int, LineState>>()
        override fun get(line: Int) = lines[line]
        override fun set(line: Int, state: LineState) {
            lines[line] = state
            history += line to state
        }
    }
}
