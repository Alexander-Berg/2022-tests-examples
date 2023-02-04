package ru.auto.feature.diff_counters.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.feature.diff_counters.data.model.OfferCounters
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AllureRobolectricRunner::class)
class OfferCountersRepositoryTest {

    @Test
    fun `should save and restore counters`() {
        val expected = OfferCounters(
            offerId = "abcd",
            viewsCount = 11,
            searchPosition = 21
        )
        val prefs = InstrumentationRegistry.getInstrumentation().context.getSharedPreferences("test", Context.MODE_PRIVATE)
        val repository = OfferCountersRepository(prefs)
        val counters = repository.getCounters().toBlocking().value()
        assertNull(counters)
        repository.saveCounters(counters = expected).await()
        var actual = repository.getCounters().toBlocking().value()
        assertEquals(expected, actual)
        repository.clearCounters().await()
        actual = repository.getCounters().toBlocking().value()
        assertNull(actual)
    }
}
