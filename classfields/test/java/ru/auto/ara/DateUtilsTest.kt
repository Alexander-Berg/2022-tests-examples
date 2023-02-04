@file:Suppress("ForbiddenImport") // really need this one for robolectric
package ru.auto.ara

import android.app.Application
import android.content.res.Resources
import com.yandex.mobile.verticalcore.utils.AppHelper
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.draft.mergeByDay
import ru.auto.core_ui.util.isToday
import ru.auto.data.model.data.offer.DailyCounter
import java.util.*
import kotlin.math.roundToInt

/**
 * @author aleien on 23.11.18.
 */

@RunWith(AllureRunner::class) class DateUtilsTest {

    private val mockApp: Application = mock()
    private val mockResources: Resources = mock()

    @Before
    fun presetAppHelper() {
        whenever(mockApp.resources).thenReturn(mockResources)
        whenever(mockApp.applicationContext).thenReturn(mockApp)
        AppHelper.setupApp(mockApp)
    }

    @Test
    fun `when inserted is full result is inserted`() {
        fun generateRandom(): List<DailyCounter> {
            val data = getEmptyCounters(TWO_WEEKS)
            data.forEach { it.views = ((Math.random() * 10) + 1).roundToInt() }

            data.forEach { print(it) }
            return data
        }

        val notEmptyViewsData = generateRandom()
        val result = twoWeeksDummyData.mergeByDay(notEmptyViewsData)

        assertThat(result.size).isEqualTo(TWO_WEEKS)
        assertThat(result).isSortedAccordingTo { o1, o2 ->
            when {
                o1.date == o2.date -> 0
                o1.date?.before(o2.date) == true -> -1
                o1.date?.after(o2.date) == true -> 1
                else -> 0
            }
        }

        assertThat(result.lastOrNull()?.date?.isToday()).isEqualTo(true)
        assertThat(result).allMatch { it.views > 0 }

    }

    @Test
    fun `when inserted is empty result is original`() {
        val result = twoWeeksDummyData.mergeByDay(emptyList())
        assertThat(result.size).isEqualTo(TWO_WEEKS)
        assertThat(result).isSortedAccordingTo { o1, o2 ->
            when {
                o1.date == o2.date -> 0
                o1.date?.before(o2.date) == true -> -1
                o1.date?.after(o2.date) == true -> 1
                else -> 0
            }
        }

        assertThat(result.lastOrNull()?.date?.isToday()).isEqualTo(true)
    }

    @Test
    fun `when inserted data is somewhat empty, result is merge`() {
        fun generateRandom(): List<DailyCounter> {
            val data = getEmptyCounters(TWO_WEEKS)
            data.forEach { it.views = (Math.random() * 10).roundToInt() }

            data.forEach { print(it) }
            return data
        }

        val notEmptyViewsData = generateRandom()
        val notEmptyPositions =
            notEmptyViewsData.mapIndexedNotNull { index: Int, dailyCounter: DailyCounter ->
                if (dailyCounter.views > 0) {
                    index
                } else {
                    null
                }
            }
        val result = twoWeeksDummyData.mergeByDay(notEmptyViewsData)

        assertThat(result.size).isEqualTo(TWO_WEEKS)
        assertThat(result).isSortedAccordingTo { o1, o2 ->
            when {
                o1.date == o2.date -> 0
                o1.date?.before(o2.date) == true -> -1
                o1.date?.after(o2.date) == true -> 1
                else -> 0
            }
        }

        assertThat(result.lastOrNull()?.date?.isToday()).isEqualTo(true)
        result.forEachIndexed { index, dailyCounter ->
            if (notEmptyPositions.contains(index)) assertThat(dailyCounter.views > 0).isTrue()
        }
    }

    companion object {
        private const val TWO_WEEKS = 14
        private val twoWeeksDummyData = getEmptyCounters(TWO_WEEKS)

        private fun getEmptyCounters(days: Int): List<DailyCounter> {
            val addedCounters = mutableListOf<DailyCounter>()

            for (i in 0 until days) {
                val time = Calendar.getInstance().apply { add(Calendar.DATE, -i) }.time
                addedCounters.add(DailyCounter(time, 0, 0, 0))
            }

            return addedCounters.apply { reverse() }
        }
    }
}
