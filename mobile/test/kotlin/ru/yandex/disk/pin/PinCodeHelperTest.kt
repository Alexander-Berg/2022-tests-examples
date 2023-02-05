package ru.yandex.disk.pin

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.auth.AccountManager
import ru.yandex.auth.YandexAccount
import ru.yandex.disk.CredentialsManager
import ru.yandex.disk.storage.MockSharedPreferences
import ru.yandex.disk.strictmode.StrictModeManager
import rx.Completable
import rx.Single
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

private const val PIN = "5555"

class PinCodeHelperTest {
    private val accountManager = mock<AccountManager>()
    private val passportPinStorage = MockSharedPreferences()
    private val credentialsManager = mock<CredentialsManager>()
    private val strictModeManager = mock<StrictModeManager>()
    private val globalPreferences = MockSharedPreferences()
    private val helper = PinCodeHelper(accountManager, passportPinStorage, credentialsManager,
        strictModeManager, globalPreferences, mock(), Schedulers.immediate(), Schedulers.immediate())

    @Test
    fun `should update all pins from random account if active account is absent`() {
        val yandexAccount1 = mock<YandexAccount>()
        val yandexAccount2 = mock<YandexAccount> {
            on { readPinCode() } doReturn PIN
        }
        whenever(accountManager.accountList).thenReturn(Single.just(listOf(yandexAccount1, yandexAccount2)))
        whenever(accountManager.writePinCodeIfChanged(any(), any())).thenReturn(Completable.complete())

        val subscriber = TestSubscriber.create<PinCode>()
        helper.updateAllPinsFromRandomAccountAsCompletable().subscribe(subscriber)

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)
        subscriber.assertCompleted()
        verify(accountManager).writePinCodeIfChanged(yandexAccount1, PIN)
    }
}
