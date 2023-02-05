package ru.yandex.yandexmaps.utils.rx

import io.reactivex.Observable
import io.reactivex.functions.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.yandexmaps.utils.rx.Rx2Utils.doOnFirst

class Rx2UtilsTest {

    @Test
    @Throws(Exception::class)
    fun doOnFirst_worksOnlyForFirst() {
        val callCounter = intArrayOf(0)

        Observable.range(0, 10).compose(doOnFirst(Consumer<Int> { callCounter[0]++ })).subscribe()

        assertThat(callCounter[0]).isEqualTo(1)
    }

    @Test
    fun doOnFirst_worksWithMultipleSubscribes() {
        val callCounter = intArrayOf(0)
        val doOnFirstObservable = Observable.range(0, 10).compose(doOnFirst(Consumer<Int> { callCounter[0]++ }))

        doOnFirstObservable.subscribe()
        doOnFirstObservable.subscribe()

        assertThat(callCounter[0]).isEqualTo(2)
    }
}
