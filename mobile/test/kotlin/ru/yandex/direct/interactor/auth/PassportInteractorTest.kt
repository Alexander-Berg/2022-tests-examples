// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.auth

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportApi
import com.yandex.passport.api.PassportToken
import com.yandex.passport.api.PassportUid
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import ru.yandex.direct.Configuration
import ru.yandex.direct.db.DbHelper

class PassportInteractorTest {
    @Test
    fun hasAuthData_true_ifHasPassportUid() {
        runAuthDataTest(passportUid, true)
    }

    @Test
    fun hasAuthData_false_ifHasNoPassportUid() {
        runAuthDataTest(null, false)
    }

    @Test
    fun tryRetrieveAuthToken_shouldGetTokenFromPassport() {
        Environment().apply {
            val observer = TestObserver<Any>()
            interactor.tryRetrieveAuthToken().subscribe(observer)
            scheduler.triggerActions()
            verify(passportApi).getToken(passportUid)
        }
    }

    private fun runAuthDataTest(passportUid: PassportUid?, expectedResult: Boolean) {
        Environment().apply {
            whenever(configuration.passportUid).doReturn(passportUid)
            val observer = TestObserver<Boolean>()
            interactor.isLoggedIn.subscribe(observer)
            scheduler.triggerActions()
            observer.assertValues(expectedResult)
        }
    }

    private companion object {
        val passportUid = PassportUid.Factory.from(1)
    }

    private class Environment {
        val primaryDisplayName = "primaryDisplayName"
        val passportUid = PassportUid.Factory.from(1)
        val passportAccount = mock<PassportAccount> {
            on { primaryDisplayName } doReturn primaryDisplayName
        }
        val passportToken = mock<PassportToken>()
        val scheduler = TestScheduler()
        val configuration = mock<Configuration> {
            on { passportUid } doReturn PassportUid.Factory.from(1)
        }
        val dbHelper = mock<DbHelper>()
        val passportApi = mock<PassportApi> {
            on { getAccount(passportUid) } doReturn passportAccount
            on { getToken(passportUid) } doReturn passportToken
        }
        val interactor = PassportInteractor(passportApi, configuration,
                dbHelper, mock(), scheduler, mock())
    }
}
