package dev.ambr.ui.theme

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/** Checks every colour token in tokens.json against [DarkColors] and [OutdoorColors]. */
class TokenParityTest {

    private val properties: Map<String, (AmbrColors) -> Color> = mapOf(
        "ground" to { it.ground },
        "surface" to { it.surface },
        "surface-2" to { it.surface2 },
        "glass" to { it.glass },
        "glass-strong" to { it.glassStrong },
        "line" to { it.line },
        "line-strong" to { it.lineStrong },
        "ink" to { it.ink },
        "ink-2" to { it.inkSecondary },
        "ink-3" to { it.inkTertiary },
        "accent" to { it.accent },
        "accent-pressed" to { it.accentPressed },
        "on-accent" to { it.onAccent },
        "accent-soft" to { it.accentSoft },
        "fog" to { it.fog },
        "fog-hatch" to { it.fogHatch },
        "ok" to { it.ok },
        "warn" to { it.warn },
        "err" to { it.err },
        "rec" to { it.rec },
        "focus" to { it.focus },
    )

    private val themes = mapOf("dark" to DarkColors, "outdoor" to OutdoorColors)

    private fun loadTokens(): JSONObject {
        val stream = javaClass.classLoader!!.getResourceAsStream("tokens.json")
            ?: error("tokens.json not found on the test classpath")
        return JSONObject(stream.bufferedReader().use { it.readText() })
    }

    /** Token name -> (theme id -> raw value), for every colour token. */
    private fun colourTokens(): Map<String, Map<String, String>> {
        val tokens = loadTokens().getJSONObject("color").getJSONArray("tokens")
        return (0 until tokens.length()).associate { i ->
            val token = tokens.getJSONObject(i)
            val value = token.getJSONObject("value")
            token.getString("name") to value.keys().asSequence().associateWith { value.getString(it) }
        }
    }

    private fun channels(c: Color): IntArray =
        floatArrayOf(c.red, c.green, c.blue, c.alpha).map { (it * 255).roundToInt() }.toIntArray()

    private val rgbaPattern = Regex("""rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*([\d.]+)\s*\)""")
    private val aliasPattern = Regex("""\{([a-z0-9-]+)\}""")

    @Test
    fun everyTokenHasAProperty() {
        val tokenNames = colourTokens().keys
        assertEquals("tokens without a Kotlin property", emptySet<String>(), tokenNames - properties.keys)
        assertEquals("Kotlin properties without a token", emptySet<String>(), properties.keys - tokenNames)
    }

    @Test
    fun coloursMatchTokens() {
        val tokens = colourTokens()
        var checked = 0
        for ((name, values) in tokens) {
            val property = properties[name] ?: continue
            for ((themeId, colors) in themes) {
                var raw = checkNotNull(values[themeId]) { "token '$name' has no '$themeId' value" }
                aliasPattern.matchEntire(raw)?.let { alias ->
                    raw = tokens.getValue(alias.groupValues[1]).getValue(themeId)
                }
                val actual = property(colors)
                val where = "$themeId/$name"
                when {
                    raw.startsWith("#") -> {
                        val expected = Color(0xFF000000 or raw.substring(1).toLong(16))
                        assertEquals("$where: expected $raw", expected, actual)
                    }
                    rgbaPattern.matches(raw) -> {
                        val g = rgbaPattern.matchEntire(raw)!!.groupValues
                        val expected = intArrayOf(
                            g[1].toInt(), g[2].toInt(), g[3].toInt(), (g[4].toFloat() * 255).roundToInt(),
                        )
                        val got = channels(actual)
                        for (ch in 0..3) {
                            assertTrue(
                                "$where: expected $raw, got ${got.toList()} (channel $ch off by more than 1)",
                                abs(expected[ch] - got[ch]) <= 1,
                            )
                        }
                    }
                    else -> fail("$where: unsupported colour value '$raw'")
                }
                checked++
            }
        }
        assertEquals("every colour token checked in both themes", tokens.size * themes.size, checked)
    }
}
