package dev.grenas.hello3d.data

import dev.grenas.hello3d.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageLogosTest {

    /** Languages with no official logo to show. */
    private val logoless = setOf("SQL", "Assembly")

    @Test
    fun `every curated language has a logo unless none exists`() {
        for (language in Languages.curated - logoless) {
            assertNotNull("$language should have a logo", LanguageLogos.forLanguage(language))
        }
        for (language in logoless) assertNull(LanguageLogos.forLanguage(language))
    }

    @Test
    fun `curated languages do not share a logo`() {
        val logos = (Languages.curated - logoless).map { LanguageLogos.forLanguage(it) }
        assertEquals(logos.size, logos.toSet().size)
    }

    @Test
    fun `matching ignores case, padding and common aliases`() {
        assertEquals(R.drawable.logo_kotlin, LanguageLogos.forLanguage("  KOTLIN "))
        assertEquals(R.drawable.logo_go, LanguageLogos.forLanguage("golang"))
        assertEquals(R.drawable.logo_cplusplus, LanguageLogos.forLanguage("cpp"))
        assertEquals(R.drawable.logo_csharp, LanguageLogos.forLanguage("C#"))
        assertEquals(R.drawable.logo_javascript, LanguageLogos.forLanguage("js"))
        assertEquals(R.drawable.logo_objectivec, LanguageLogos.forLanguage("Objective-C"))
    }

    @Test
    fun `C is not confused with its descendants`() {
        assertEquals(R.drawable.logo_c, LanguageLogos.forLanguage("C"))
    }

    @Test
    fun `unknown languages have no logo`() {
        assertNull(LanguageLogos.forLanguage("Brainfuck"))
        assertNull(LanguageLogos.forLanguage(""))
    }
}
