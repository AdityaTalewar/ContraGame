package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.DifficultyTier
import com.example.game.DynamicDifficultyManager
import com.example.game.StageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Retro Strike", appName)
    }

    @Test
    fun `test stage catalog loads all stages`() {
        val stage1 = StageCatalog.getStage(0)
        assertEquals("STAGE 1", stage1.name)
        assertNotNull(stage1.bossType)

        val stage2 = StageCatalog.getStage(1)
        assertEquals("STAGE 2", stage2.name)

        val stage3 = StageCatalog.getStage(2)
        assertEquals("STAGE 3", stage3.name)
    }

    @Test
    fun `test dynamic difficulty adjustment escalation`() {
        val dda = DynamicDifficultyManager()
        assertEquals(DifficultyTier.BALANCED, dda.currentTier)

        // Simulating consecutive kill streaks
        for (i in 1..12) {
            dda.recordEnemyKilled()
        }
        assertEquals(DifficultyTier.VETERAN, dda.currentTier)

        for (i in 1..12) {
            dda.recordEnemyKilled()
        }
        assertEquals(DifficultyTier.INSANE, dda.currentTier)
    }
}
