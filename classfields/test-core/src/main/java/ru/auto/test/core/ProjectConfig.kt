package ru.auto.test.core

import io.kotest.core.config.AbstractProjectConfig


/**
 * This class appears to be unsused, but it is kotlintest's way to make tests setup.
 *
 * @author themishkun on 16/11/2018.
 */
object ProjectConfig : AbstractProjectConfig() {
    override fun beforeAll() {
        RxTestAndroid.RxCustomizer.setupRxJavaSchedulers()
    }
}
