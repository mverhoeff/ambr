package dev.ambr.coverage

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ambr.capture.Preset
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverageEngineSmokeTest {
    @Test
    fun createVersionDestroy() {
        val engine = CoverageEngine.create(Preset.ROOM)
        assertEquals("ambr-native 1", CoverageEngine.version())
        engine.reset()
        engine.destroy()
    }
}
