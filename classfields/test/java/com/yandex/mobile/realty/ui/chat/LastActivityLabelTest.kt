package com.yandex.mobile.realty.ui.chat

import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.adapter.Formatter
import com.yandex.mobile.realty.ui.presenter.getChatUserLastActivityDisplayString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

/**
 * @author rogovalex on 27/01/2021.
 */
@RunWith(RobolectricTestRunner::class)
class LastActivityLabelTest : RobolectricTest() {

    @Test
    fun todayLabel() {
        val today = Date()
        val formattedTime = Formatter.TIME.format(today)
        assertEquals(
            "Был(а) сегодня в $formattedTime",
            today.getChatUserLastActivityDisplayString()
        )
    }

    @Test
    fun yesterdayLabel() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.time
        val formattedTime = Formatter.TIME.format(yesterday)
        assertEquals(
            "Был(а) вчера в $formattedTime",
            yesterday.getChatUserLastActivityDisplayString()
        )
    }

    @Test
    fun withinYearLabel() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val withinYear = calendar.time
        val formattedTime = Formatter.DAY_OF_MONTH.format(withinYear)
        assertEquals("Был(а) $formattedTime", withinYear.getChatUserLastActivityDisplayString())
    }

    @Test
    fun longAgoLabel() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        val longAgo = calendar.time
        assertEquals("Был(а) очень давно", longAgo.getChatUserLastActivityDisplayString())
    }
}
