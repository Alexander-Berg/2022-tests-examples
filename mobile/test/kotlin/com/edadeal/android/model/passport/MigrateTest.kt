package com.edadeal.android.model.passport

import com.edadeal.android.helpers.ResponseFactory
import com.edadeal.android.helpers.StatefulMock
import com.edadeal.android.metrics.MigrationDescription
import com.edadeal.android.metrics.MigrationException
import com.edadeal.android.model.ApiException
import com.edadeal.protobuf.usr.v1.UserInfo
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.authsdk.YandexAuthAccount
import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.PassportToken
import com.yandex.passport.api.PassportUid
import io.reactivex.Single
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.internal.verification.Times

class MigrateTest : MigrationDelegateTest() {

    private fun prepare(
        socialUid: String? = "1",
        amContains: List<Long> = listOf(1L),
        migrationErrorCode: Int? = null,
        meErrorCode: Int? = null
    ): StatefulMock<Boolean> {
        whenever(prefs.isAuthorized).then { true }
        whenever(prefs.mockAmAccountAsEmpty).then { false }
        whenever(launchHelper.yandexKit.getAccounts()).then { emptyList<YandexAuthAccount>() }
        whenever(passportApi.passportContext).then { passportContext }
        whenever(passportApi.getCurrentUid()).then { null }
        whenever(passportApi.getToken(any(), any())).then {
            val passportToken: PassportToken = mock()
            whenever(passportToken.value).then { "passport_token" }
            passportToken
        }
        whenever(launchHelper.duid).then { "duid" }

        whenever(launchHelper.socialUid).then { socialUid }
        whenever(passportApi.getAccounts()).then {
            amContains.map { uuid ->
                val passportAccount: PassportAccount = mock()
                whenever(passportAccount.uid).then { PassportUid.Factory.from(uuid) }
                passportAccount
            }
        }
        whenever(userApi.migrate(any())).then {
            val value = if (migrationErrorCode == null) ResponseFactory.success() else ResponseFactory.error(migrationErrorCode)
            Single.just(value)
        }
        whenever(launchDelegate.loadMe()).then {
            if (meErrorCode != null) Single.error(ApiException(meErrorCode)) else Single.just(UserInfo(
                "", "", "", false, "", "", "", ""
            ))
        }
        return StatefulMock(false, { prefs::amAllowed.set(ArgumentMatchers.anyBoolean()) }, { prefs.amAllowed })
    }

    @Test
    fun `Migration failed when social UID is empty`() {
        val amAllowed = prepare("", listOf(1L))

        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateError(MigrationException(MigrationDescription.EmptyPUID))
        assert(!amAllowed.value)
    }

    @Test
    fun `Migration failed when AM not contains uid`() {
        val amAllowed = prepare("1", listOf(2L))
        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateError(MigrationException(MigrationDescription.AccountNotFound))
        assert(!amAllowed.value)
    }

    @Test
    fun `Migration failed when migration request failed`() {
        val amAllowed = prepare(migrationErrorCode = 404)
        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateError(MigrationException(ApiException(404), MigrationException.ErrorStep.Migrate))
        assert(!amAllowed.value)
    }

    @Test
    fun `Migration failed when me request failed`() {
        val amAllowed = prepare(meErrorCode = 404)
        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateError(MigrationException(ApiException(404), MigrationException.ErrorStep.Me))
        assert(!amAllowed.value)
    }

    @Test
    fun `Migration succeed when http requests were correct`() {
        val amAllowed = prepare()
        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateSuccess(amAllowed)
        verify(passportApi, Times(1)).setCurrentUid(any())
    }
}
