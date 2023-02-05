package ru.yandex.yandexmaps.common.utils.rx

import io.reactivex.Observable
import org.junit.Test
import ru.yandex.yandexmaps.common.utils.extensions.rx.distinctLastEmitted

class ObservableDistinctLastEmittedTest {

    @Test
    fun intWithThreshold() {
        Observable.range(1, 100)
            .distinctLastEmitted { lastEmitted, current -> Math.abs(lastEmitted - current) < 20 }
            .test()
            .assertValues(1, 21, 41, 61, 81)
    }

    @Test
    fun intWithThresholdFused() {
        Observable.range(101, 100)
            .map { it - 100 }
            .distinctLastEmitted { lastEmitted, current -> Math.abs(lastEmitted - current) < 20 }
            .test()
            .assertValues(1, 21, 41, 61, 81)
    }
}
