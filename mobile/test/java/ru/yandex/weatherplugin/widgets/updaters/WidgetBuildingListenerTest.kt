// Copyright 2021 Yandex LLC. All rights reserved.

package ru.yandex.weatherplugin.widgets.updaters

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.robolectric.RobolectricTestRunner
import ru.yandex.weatherplugin.widgets.data.WeatherWidgetConfig
import ru.yandex.weatherplugin.widgets.data.WidgetState
import ru.yandex.weatherplugin.widgets.updaters.nowcast.WidgetViewDataUpdater
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Unit test for WeatherWidgetBuildingListener.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetBuildingListenerTest {
    private val imagesCount = 9
    private val strategy = mock<UpdateViewsStrategy>()
    private val service = Executors.newCachedThreadPool()
    private val config = mock<WeatherWidgetConfig>()
    private lateinit var underTest: WeatherWidgetBuildingListener

    @Before
    fun initTests() {
        underTest = spy(WeatherWidgetBuildingListener(imagesCount, null, config, strategy, WidgetState.EMPTY))
    }

    @Test
    fun successCallbackTest() {
        testListener {
            underTest.onImageLoadedSuccess(mock(), mock(), 0)
        }
    }

    @Test
    fun errorCallbackTest() {
        testListener {
            underTest.onImageLoadedError()
        }
    }

    @Test
    fun finishCallbackTest() {
        testListener {
            underTest.onStepFinish()
        }
    }

    @Test
    fun finishImmediatelyTest() {
        val task = service.submit(underTest)

        underTest.releaseUpdating()
        task.get(3, TimeUnit.SECONDS)

        verify(underTest).doUpdate()
    }

    private fun testListener(action: () -> Unit) {
        val task = service.submit(underTest)
        val dataUpdater = mock<WidgetViewDataUpdater> {
            on { updateWidget(any()) } doAnswer {
                for (i in 1..imagesCount) action()
            }
        }
        dataUpdater.updateWidget(false)

        task.get(3, TimeUnit.SECONDS)
        verify(underTest).doUpdate()
        verify(underTest, times(imagesCount)).onStepFinish()
    }
}
