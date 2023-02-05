package com.yandex.mail.settings

import android.media.Ringtone
import android.net.Uri
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.mail.BaseMailApplication
import com.yandex.mail.account.MailProvider
import com.yandex.mail.container.AccountInfoContainer
import com.yandex.mail.entity.AccountType
import com.yandex.mail.metrica.YandexMailMetrica
import com.yandex.mail.model.AccountModel
import com.yandex.mail.model.CacheTrimModel
import com.yandex.mail.model.GeneralSettingsModel
import com.yandex.mail.model.StorageModel
import com.yandex.mail.pin.PinCode
import com.yandex.mail.pin.PinCodeModel
import com.yandex.mail.runners.UnitTestRunner
import com.yandex.mail.settings.entry_settings.EntrySettingsPresenter
import com.yandex.mail.settings.entry_settings.EntrySettingsPresenterConfig
import com.yandex.mail.settings.entry_settings.EntrySettingsView
import com.yandex.mail.storage.StubSharedPreferences
import com.yandex.mail.util.ActionTimeTracker
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers.trampoline
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import java.util.concurrent.TimeUnit

@RunWith(UnitTestRunner::class)
class EntrySettingsPresenterTest {

    lateinit var timeScheduler: TestScheduler

    val baseMailApplication = mock<BaseMailApplication>()

    val accountModel = mock<AccountModel>()

    val settingsModel = mock<GeneralSettingsModel>()

    val storageModel = mock<StorageModel>()

    val cacheTrimModel = mock<CacheTrimModel>()

    val pinCodeModel = mock<PinCodeModel>()

    val pinCode = mock<PinCode>()

    val generalSettings = mock<GeneralSettings>()

    val generalSettingsEditor = mock<GeneralSettingsEditor>()

    val metrica = mock<YandexMailMetrica>()

    val view = mock<EntrySettingsView>()

    val actionTimeTracker = mock<ActionTimeTracker>()

    val simpleStorage = SimpleStorageImpl(StubSharedPreferences(), metrica)

    lateinit var entrySettingsPresenter: EntrySettingsPresenter

    lateinit var containerList: List<AccountInfoContainer>

    private val container1 = AccountInfoContainer.create(
        1,
        "managerName",
        "managerType",
        true,
        true,
        true,
        AccountType.LOGIN,
        MailProvider.YANDEX,
        false,
        "name1",
        "email1",
        true
    )

    @Before
    fun beforeEachTest() {
        containerList = emptyList()

        timeScheduler = TestScheduler()

        whenever(accountModel.observeAccountsInfoWithMails()).thenReturn(Flowable.just(emptyList<AccountInfoContainer>()))
        whenever(accountModel.accountsInfoWithMails).thenReturn(Single.just(emptyList<AccountInfoContainer>()))
        whenever(generalSettings.edit()).thenReturn(generalSettingsEditor)
        whenever(generalSettingsEditor.setFingerprintAuthEnabled(anyBoolean())).thenReturn(generalSettingsEditor)
        whenever(settingsModel.generalSettings).thenReturn(generalSettings)

        whenever(storageModel.storageUsedBytes).thenReturn(Single.just(0L))
        whenever(cacheTrimModel.clearCacheForAllAccounts()).thenReturn(Single.just(0L))

        whenever(pinCodeModel.readPin()).thenReturn(Single.just(pinCode))

        entrySettingsPresenter = createEntrySettingsPresenter()
    }

    @Test
    fun `loadFragmentData should invoke onInfoLoaded`() {
        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.loadFragmentData()

        verify(view).onInfoLoaded(emptyList<AccountInfoContainer>())
    }

    @Test
    fun `loadFragmentData should invoke onInfoLoaded with right list`() {
        mockAccountModelWithOneItem()

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.loadFragmentData()

        verify(view).onInfoLoaded(containerList)
    }

    @Test
    fun `checkAccountLogged should invoke onAccountChecked if logged`() {
        whenever(accountModel.isAccountLogged(eq(1))).thenReturn(true)
        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.loadFragmentData()

        entrySettingsPresenter.checkAccountLogged(1)

        verify(view).onAccountChecked()
    }

    @Test
    fun `checkAccountLogged should call refresh token if not logged`() {
        whenever(accountModel.isAccountLogged(1)).thenReturn(false)
        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.loadFragmentData()

        entrySettingsPresenter.checkAccountLogged(1)

        verify(view, never()).onAccountChecked()
        verify(accountModel).checkAccountInPassport(1)
    }

    @Test
    fun `putSwipeAction should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setSwipeAction(any())).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putSwipeAction(SwipeAction.DELETE)

        verify(generalSettingsEditor).setSwipeAction(SwipeAction.DELETE)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putSendingDelaySeconds should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setSendingDelaySeconds(anyInt())).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putSendingDelaySeconds(5)

        verify(generalSettingsEditor).setSendingDelaySeconds(5)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putCompactMode should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setCompactModeEnabled(true)).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putCompactMode(true)

        verify(generalSettingsEditor).setCompactModeEnabled(true)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putAuthorizeByFingerprint should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setFingerprintAuthEnabled(true)).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putAuthorizeByFingerprint(true)

        verify(generalSettingsEditor).setFingerprintAuthEnabled(true)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putNotificationVibrationEnabled should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setNotificationVibrationEnabled(true)).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putNotificationVibrationEnabled(true)

        verify(generalSettingsEditor).setNotificationVibrationEnabled(true)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putDoNotDisturbEnabled should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setDoNotDisturbEnabled(true)).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putDoNotDisturbEnabled(true)

        verify(generalSettingsEditor).setDoNotDisturbEnabled(true)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putDoNotDisturbTimeFrom should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setDoNotDisturbTimeFrom(eq(10), eq(10))).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putDoNotDisturbTimeFrom(Pair(10, 10))

        verify(generalSettingsEditor).setDoNotDisturbTimeFrom(10, 10)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putDoNotDisturbTimeTo should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setDoNotDisturbTimeTo(eq(10), eq(10))).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putDoNotDisturbTimeTo(Pair(10, 10))

        verify(generalSettingsEditor).setDoNotDisturbTimeTo(10, 10)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putNotificationBeepEnabled should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setNotificationBeepEnabled(true)).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putNotificationBeepEnabled(true)

        verify(generalSettingsEditor).setNotificationBeepEnabled(true)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putNotificationBeep should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setNotificationBeep(any())).thenReturn(generalSettingsEditor)

        val notificationBeep = mock<Uri>()
        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putNotificationBeep(notificationBeep)

        verify(generalSettingsEditor).setNotificationBeep(notificationBeep)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putAdShown should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setAdShown(true)).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putAdShown(true)

        verify(generalSettingsEditor).setAdShown(true)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `putNewYearMode should call generalSettingsEditor and apply`() {
        whenever(generalSettingsEditor.setNewYearModeEnabled(true)).thenReturn(generalSettingsEditor)

        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.putNewYearMode(true)

        verify(generalSettingsEditor).setNewYearModeEnabled(true)
        verify(generalSettingsEditor).apply()
    }

    @Test
    fun `loadCurrentRingtone should call onCurrentRingtoneLoaded`() {
        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.loadFragmentData()
        entrySettingsPresenter.loadCurrentRingtone()

        // getRingtone() is static so I can't mock it. Just check that method being called.
        verify(view).onCurrentRingtoneLoaded(any<Ringtone>())
    }

    @Test
    fun `loadCurrentPinCode should pass to view with shouldSet = true`() {
        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.loadFragmentData()
        entrySettingsPresenter.loadCurrentPinCode(true)

        verify(view).onCurrentPinCodeLoaded(pinCode, true)
    }

    @Test
    fun `loadCurrentPinCode should pass to view with shouldSet = false`() {
        entrySettingsPresenter.onBindView(view)
        entrySettingsPresenter.loadFragmentData()
        entrySettingsPresenter.loadCurrentPinCode(false)

        verify(view).onCurrentPinCodeLoaded(pinCode, false)
    }

    @Test
    fun `clearCache should dismiss progress`() {
        entrySettingsPresenter.onBindView(view)
        verify(view).dismissCacheClearingProgress()
        clearInvocations(view)

        entrySettingsPresenter.clearCache()
        timeScheduler.advanceTimeBy(3, TimeUnit.SECONDS)

        verify(view).dismissCacheClearingProgress()
    }

    @Test
    fun `clearCache should invoke showCacheClearingProgress before cache clearing`() {
        val orderedView = inOrder(view)
        entrySettingsPresenter.onBindView(view)

        entrySettingsPresenter.clearCache()
        timeScheduler.advanceTimeBy(3, TimeUnit.SECONDS)

        orderedView.verify(view).showCacheClearingProgress()

        verify(cacheTrimModel).clearCacheForAllAccounts()
        orderedView.verify(view).dismissCacheClearingProgress()
    }

    private fun mockAccountModelWithOneItem() {
        containerList = listOf(container1)

        whenever(accountModel.accountsInfoWithMails).thenReturn(Single.just(containerList))
    }

    private fun createTestPresenterConfig() = EntrySettingsPresenterConfig(
        ioScheduler = trampoline(),
        uiScheduler = trampoline(),
        timeScheduler = timeScheduler
    )

    private fun createEntrySettingsPresenter() = EntrySettingsPresenter(
        baseMailApplication,
        accountModel,
        settingsModel,
        cacheTrimModel,
        pinCodeModel,
        metrica,
        createTestPresenterConfig(),
        simpleStorage
    )
}
