package ru.yandex.market.internal.rx

import io.reactivex.Single
import org.junit.Test

class SinglesTest {

    @Test
    fun `Return error single when validation failed`() {
        Single.just("")
            .compose(Singles.validate { false })
            .test()
            .assertError(IllegalArgumentException::class.java)
    }

    @Test
    fun `Return value when validation succeed`() {
        Single.just("")
            .compose(Singles.validate { true })
            .test()
            .assertValue("")
    }

    @Test
    fun `Return error single when inverse validation succeed`() {
        Single.just("")
            .compose(Singles.validateNot { false })
            .test()
            .assertValue("")
    }

    @Test
    fun `Return error single when inverse validation failed`() {
        Single.just("")
            .compose(Singles.validateNot { true })
            .test()
            .assertError(IllegalArgumentException::class.java)
    }
}