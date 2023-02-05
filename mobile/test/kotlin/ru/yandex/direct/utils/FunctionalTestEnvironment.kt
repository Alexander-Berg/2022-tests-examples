// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import android.content.Intent
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportApi
import com.yandex.passport.api.PassportToken
import com.yandex.passport.api.PassportUid
import io.reactivex.Completable
import io.reactivex.Single
import ru.yandex.direct.Configuration
import ru.yandex.direct.db.DbHelper
import ru.yandex.direct.interactor.auth.PassportInteractor
import ru.yandex.direct.newui.error.resolution.DefaultResolution

open class FunctionalTestEnvironment : ViewEnvironment() {
    val accountName = "accountName"

    val authToken = "authToken"

    private val passportAccount = mock<PassportAccount> {
        on { primaryDisplayName } doReturn accountName
    }

    val passportApi = mock<PassportApi> {
        on { getToken(any()) } doReturn PassportToken { authToken }
        on { getAccount(any<PassportUid>()) } doReturn passportAccount
    }

    val configuration = mock<Configuration>()
    val dbHelper = mock<DbHelper>()

    val defaultErrorResolution = DefaultResolution().after(TestingErrorResolution())

    val passportInteractor = object : PassportInteractor(
        passportApi,
        configuration,
        dbHelper,
        context,
        scheduler,
        mock()
    ) {
        override fun performLogout() = Completable.fromAction(this::doPerformLogout)

        override fun performTokenRefresh() = Single.just(false)

        override fun createAccountManagerIntent() = mock<Intent>()
    }
}
