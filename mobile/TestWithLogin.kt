package com.yandex.mail.perftests.runner

import com.yandex.perftests.tests.TestBaseImpl
import org.junit.After

open class TestWithLogin(testClass: Class<*>) : TestBaseImpl(testClass) {

    @After
    fun removeAccounts() = utils().removeAccounts()
}
