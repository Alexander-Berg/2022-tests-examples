package ru.auto.ara

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.Listener
import io.kotest.core.spec.IsolationMode
import io.kotest.extensions.allure.AllureTestReporter

/**
 * This class appears to be unsused, but it is kotlintest's way to make tests setup.
 *
 * @author themishkun on 16/11/2018.
 */
object ProjectConfig : AbstractProjectConfig() {
    override val isolationMode: IsolationMode = IsolationMode.InstancePerLeaf
    override fun listeners(): List<Listener> = listOf(AllureTestReporter())
    override fun beforeAll() {
        RxTest.RxSetupper.setupRxJavaSchedulers()
    }
}
