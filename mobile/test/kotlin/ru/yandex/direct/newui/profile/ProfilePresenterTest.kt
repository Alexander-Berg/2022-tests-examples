// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.profile

import android.graphics.Bitmap
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.passport.api.PassportUid
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import ru.yandex.direct.R
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.clients.ClientInfo
import ru.yandex.direct.interactor.clients.AvatarInteractor
import ru.yandex.direct.interactor.clients.CurrentClientInteractor
import ru.yandex.direct.interactor.push.PushInteractor
import ru.yandex.direct.newui.base.ItemClickEvent
import ru.yandex.direct.newui.profile.items.ProfileAccountItem
import ru.yandex.direct.newui.profile.items.ProfileButtonItem
import ru.yandex.direct.newui.profile.items.ProfileInfoItem
import ru.yandex.direct.newui.profile.items.ProfileItem
import ru.yandex.direct.newui.profile.items.ProfileSeparatorItem
import ru.yandex.direct.repository.clients.Avatar
import ru.yandex.direct.repository.clients.AvatarRemoteRepository
import ru.yandex.direct.repository.clients.ClientsRemoteRepository
import ru.yandex.direct.utils.CurrencyInitializer
import ru.yandex.direct.utils.FunctionalTestEnvironment
import ru.yandex.direct.utils.SimpleLocalRepository
import java.net.URL

class ProfilePresenterTest {
    @Test
    fun presenter_shouldShowClientCorrectly_whenAttached() {
        Environment().apply {
            val clientInfo = ApiSampleData.clientInfo
            setClient(clientInfo)
            attach()
            verify(adapter).clearAll()
            verify(adapter).addAll(listOf(
                ProfileAccountItem("mdtester", R.string.dialog_log_out_ok, R.id.action_logout, false),
                ProfileSeparatorItem()
            ))
            verify(adapter).addAll(listOf<ProfileItem>(
                ProfileInfoItem.anemic(R.string.account_phone_title, "000"),
                ProfileInfoItem.anemic(R.string.account_email_title, "mdtester@yandex.ru"),
                ProfileInfoItem.anemic(R.string.account_currency_title, "RUB"),
                ProfileInfoItem.anemic(R.string.account_shared_account_title, "yes"),
                ProfileInfoItem.anemic(R.string.account_overdraft_limit_title, "0")
            ))
            verify(adapter).addAll(listOf(
                ProfileSeparatorItem(),
                ProfileButtonItem(R.string.push_settings_title, R.id.action_push_settings),
                ProfileButtonItem(R.string.app_password, R.id.action_app_password),
                ProfileButtonItem(R.string.profile_show_onboarding, R.id.action_show_onboarding),
                ProfileSeparatorItem(),
                ProfileButtonItem(R.string.side_menu_item_about, R.id.action_about),
                ProfileButtonItem(R.string.side_menu_item_feedback, R.id.action_feedback),
                ProfileButtonItem(R.string.side_menu_item_help, R.id.action_help)
            ))
        }
    }

    @Test
    fun presenter_shouldShowSubclientCorrectly_whenAttached() {
        Environment().apply {
            val clientInfo = ApiSampleData.subclientInfo
            setClient(clientInfo)
            whenever(configuration.isAgency).doReturn(false)
            attach()
            verify(adapter).clearAll()
            verify(adapter).addAll(listOf(
                    ProfileAccountItem("subuser-petr", R.string.dialog_log_out_ok, R.id.action_logout, false),
                    ProfileSeparatorItem()
            ))
            verify(adapter).addAll(listOf<ProfileItem>(
                    ProfileInfoItem.anemic(R.string.account_phone_title, "1234567890"),
                    ProfileInfoItem.anemic(R.string.account_email_title, "testestest@ya.ru"),
                    ProfileInfoItem.anemic(R.string.account_currency_title, "YND_FIXED"),
                    ProfileInfoItem.anemic(R.string.account_shared_account_title, "yes"),
                    ProfileInfoItem.anemic(R.string.account_overdraft_limit_title, "0")
            ))
            verify(adapter).addAll(listOf(
                    ProfileSeparatorItem(),
                    ProfileButtonItem(R.string.push_settings_title, R.id.action_push_settings),
                    ProfileButtonItem(R.string.app_password, R.id.action_app_password),
                    ProfileButtonItem(R.string.profile_show_onboarding, R.id.action_show_onboarding),
                    ProfileSeparatorItem(),
                    ProfileButtonItem(R.string.side_menu_item_about, R.id.action_about),
                    ProfileButtonItem(R.string.side_menu_item_feedback, R.id.action_feedback),
                    ProfileButtonItem(R.string.side_menu_item_help, R.id.action_help)
            ))
        }
    }

    @Test
    fun presenter_shouldShowSubclientForAgencyCorrectly_whenAttached() {
        Environment().apply {
            val clientInfo = ApiSampleData.subclientInfo
            setClient(clientInfo)
            attach()
            verify(adapter).clearAll()
            verify(adapter).addAll(listOf(
                ProfileAccountItem(accountName, R.string.dialog_log_out_ok, R.id.action_logout, true),
                ProfileSeparatorItem(),
                ProfileAccountItem("subuser-petr", R.string.profile_subclient_select_button, R.id.action_select_client, false),
                ProfileSeparatorItem()
            ))
            verify(adapter).addAll(listOf<ProfileItem>(
                ProfileInfoItem.clickable(R.string.account_phone_title, "1234567890", R.id.action_call_phone),
                ProfileInfoItem.clickable(R.string.account_email_title, "testestest@ya.ru", R.id.action_send_mail),
                ProfileInfoItem.anemic(R.string.account_currency_title, "YND_FIXED"),
                ProfileInfoItem.anemic(R.string.account_shared_account_title, "yes"),
                ProfileInfoItem.anemic(R.string.account_overdraft_limit_title, "0"),
                ProfileInfoItem.anemic(R.string.account_allow_edit_campaigns_title, "no"),
                ProfileInfoItem.anemic(R.string.account_allow_transfer_money_title, "no"),
                ProfileInfoItem.anemic(R.string.account_allow_import_xls_title, "no")
            ))
            verify(adapter).addAll(listOf(
                ProfileSeparatorItem(),
                ProfileButtonItem(R.string.app_password, R.id.action_app_password),
                ProfileButtonItem(R.string.profile_show_onboarding, R.id.action_show_onboarding),
                ProfileSeparatorItem(),
                ProfileButtonItem(R.string.side_menu_item_about, R.id.action_about),
                ProfileButtonItem(R.string.side_menu_item_feedback, R.id.action_feedback),
                ProfileButtonItem(R.string.side_menu_item_help, R.id.action_help)
            ))
        }
    }

    @Test
    fun presenter_shouldDisablePhoneAndMailButtons_whenEmpty() {
        Environment().apply {
            val clientInfo = ApiSampleData.subclientInfo.clone()
            clientInfo.email = ""
            clientInfo.phone = ""
            setClient(clientInfo)
            attach()
            verify(adapter).clearAll()
            verify(adapter).addAll(listOf(
                ProfileAccountItem(accountName, R.string.dialog_log_out_ok, R.id.action_logout, true),
                ProfileSeparatorItem(),
                ProfileAccountItem("subuser-petr", R.string.profile_subclient_select_button, R.id.action_select_client, false),
                ProfileSeparatorItem()
            ))
            verify(adapter).addAll(listOf<ProfileItem>(
                ProfileInfoItem.anemic(R.string.account_phone_title, ""),
                ProfileInfoItem.anemic(R.string.account_email_title, ""),
                ProfileInfoItem.anemic(R.string.account_currency_title, "YND_FIXED"),
                ProfileInfoItem.anemic(R.string.account_shared_account_title, "yes"),
                ProfileInfoItem.anemic(R.string.account_overdraft_limit_title, "0"),
                ProfileInfoItem.anemic(R.string.account_allow_edit_campaigns_title, "no"),
                ProfileInfoItem.anemic(R.string.account_allow_transfer_money_title, "no"),
                ProfileInfoItem.anemic(R.string.account_allow_import_xls_title, "no")
            ))
            verify(adapter).addAll(listOf(
                ProfileSeparatorItem(),
                ProfileButtonItem(R.string.app_password, R.id.action_app_password),
                ProfileButtonItem(R.string.profile_show_onboarding, R.id.action_show_onboarding),
                ProfileSeparatorItem(),
                ProfileButtonItem(R.string.side_menu_item_about, R.id.action_about),
                ProfileButtonItem(R.string.side_menu_item_feedback, R.id.action_feedback),
                ProfileButtonItem(R.string.side_menu_item_help, R.id.action_help)
            ))
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun presenter_shouldLoadAndShowAvatar_whenAttachedForSimpleUser() {
        Environment().apply {
            setClient(ApiSampleData.clientInfo)
            whenever(avatarRemoteRepo.fetch(any())).doReturn(avatarWithBitmap)
            attach()
            val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<ProfileItem>>
            verify(adapter, times(3)).addAll(captor.capture())
            assertThat((captor.allValues[0][0] as ProfileAccountItem).avatar).isEqualTo(avatarWithBitmap.bitmap)
        }
    }

    @Test
    fun presenter_shouldLoadAndShowEmptyAvatar_whenGotNullFromRepoForSimpleUser() {
        Environment().apply {
            setClient(ApiSampleData.clientInfo)
            whenever(avatarRemoteRepo.fetch(any())).doReturn(avatarWithoutBitmap)
            attach()
            val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<ProfileItem>>
            verify(adapter, times(3)).addAll(captor.capture())
            assertThat((captor.allValues[0][0] as ProfileAccountItem).avatar).isEqualTo(null)
        }
    }

    @Test
    fun presenter_shouldLoadAndShowAvatar_whenAttachedForAgency() {
        Environment().apply {
            setClient(ApiSampleData.subclientInfo)
            whenever(avatarRemoteRepo.fetch(any())).doReturn(avatarWithBitmap)
            attach()
            val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<ProfileItem>>
            verify(adapter, times(3)).addAll(captor.capture())
            for (i in 0..1) {
                assertThat((captor.allValues[0][0] as ProfileAccountItem).avatar).isEqualTo(avatarWithBitmap.bitmap)
            }
        }
    }

    @Test
    fun presenter_shouldLoadAndShowEmptyAvatar_whenGotNullFromRepoForAgency() {
        Environment().apply {
            setClient(ApiSampleData.subclientInfo)
            whenever(avatarRemoteRepo.fetch(any())).doReturn(avatarWithoutBitmap)
            attach()
            val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<ProfileItem>>
            verify(adapter, times(3)).addAll(captor.capture())
            for (i in 0..1) {
                assertThat((captor.allValues[0][0] as ProfileAccountItem).avatar).isEqualTo(null)
            }
        }
    }

    @Test
    fun presenter_shouldShowLogoutDialog_whenLogoutButtonPressed() {
        Environment().apply {
            setClient(ApiSampleData.clientInfo)
            val clicks = PublishSubject.create<ItemClickEvent<ProfileItem>>()
            whenever(adapter.clicks).doReturn(clicks)
            attach()
            clicks.onNext(ItemClickEvent(mock(), ProfileAccountItem(accountName, 0, R.id.action_logout, false)))
            scheduler.triggerActions()
            verify(view).showConfirmDialog(anyInt(), any(), eq(logoutMessage), anyOrNull())
        }
    }

    @Test
    fun presenter_shouldPerformLogoutCorrectly() {
        Environment().apply {
            setClient(ApiSampleData.clientInfo)
            attach()
            presenter.onLogoutDialogConfirm(view)
            scheduler.triggerActions()
            verify(configuration).currentClient = null
            verify(view).navigateToSplashActivity()
        }
    }

    @Test
    fun presenter_shouldNavigateToClientsList_onClientLoginClick() {
        Environment().apply {
            setClient(ApiSampleData.clientInfo)
            val clicks = PublishSubject.create<ItemClickEvent<ProfileItem>>()
            whenever(adapter.clicks).doReturn(clicks)
            attach()
            clicks.onNext(ItemClickEvent(mock(), ProfileAccountItem(accountName, 0, R.id.action_select_client, false)))
            scheduler.triggerActions()
            verify(view).navigateToAgencyClientsActivity()
        }
    }

    @Test
    fun presenter_shouldNavigateToEmailActivity_onEmailClick() {
        Environment().apply {
            setClient(ApiSampleData.subclientInfo)
            val clicks = PublishSubject.create<ItemClickEvent<ProfileItem>>()
            whenever(adapter.clicks).doReturn(clicks)
            attach()
            clicks.onNext(ItemClickEvent(mock(), ProfileAccountItem(accountName, 0, R.id.action_send_mail, false)))
            scheduler.triggerActions()
            verify(view).navigateToEmailActivity(ApiSampleData.subclientInfo.email!!)
        }
    }

    @Test
    fun presenter_shouldNavigateToPhoneActivity_onPhoneClick() {
        Environment().apply {
            setClient(ApiSampleData.subclientInfo)
            val clicks = PublishSubject.create<ItemClickEvent<ProfileItem>>()
            whenever(adapter.clicks).doReturn(clicks)
            attach()
            clicks.onNext(ItemClickEvent(mock(), ProfileAccountItem(accountName, 0, R.id.action_call_phone, false)))
            scheduler.triggerActions()
            verify(view).navigateToPhoneActivity(ApiSampleData.subclientInfo.phone!!)
        }
    }

    private class Environment : FunctionalTestEnvironment() {
        val avatarRemoteRepo = mock<AvatarRemoteRepository> {
            on { fetch(any()) } doReturn avatarWithoutBitmap
        }

        val avatarLocalRepo = SimpleLocalRepository<URL, Avatar?> { _, avatar -> avatar }

        val avatarInteractor = AvatarInteractor(avatarLocalRepo, avatarRemoteRepo, scheduler, scheduler)

        val clientsRemoteRepo = mock<ClientsRemoteRepository>()

        val clientInteractor = CurrentClientInteractor(clientsRemoteRepo, configuration, scheduler, scheduler)

        val pushSettingsInteractor = mock<PushInteractor> {
            on { deleteSubscription() } doReturn Completable.complete()
        }

        val presenter = ProfilePresenter(defaultErrorResolution, scheduler,
                clientInteractor, avatarInteractor, mock(), passportInteractor, pushSettingsInteractor)

        val adapter = mock<ProfileAdapter> {
            on { clicks } doReturn Observable.empty()
        }

        val view = mock<ProfileView>().stubAdapterViewMethods(adapter).stub {
            on { refreshSwipes } doReturn Observable.empty()
            on { showConfirmDialog(anyInt(), any(), any(), anyOrNull()) } doReturn Maybe.empty()
        }

        init {
            resources.stub {
                on { getString(R.string.default_yes) } doReturn "yes"
                on { getString(R.string.default_no) } doReturn "no"
                on { getIdentifier(eq("CURRENCY_YND_FIXED"), any(), any()) } doReturn R.string.CURRENCY_YND_FIXED
                on { getString(R.string.CURRENCY_YND_FIXED) } doReturn "units"
                on { getIdentifier(eq("CURRENCY_RUB"), any(), any()) } doReturn R.string.CURRENCY_RUB
                on { getString(R.string.CURRENCY_RUB) } doReturn "rub"
                on { getString(R.string.dialog_log_out_message) } doReturn logoutMessage
                on { getString(R.string.dialog_log_out_title) } doReturn logoutTitle
            }
        }

        fun attach() {
            presenter.attachView(view, null)
            presenter.onResume()
            scheduler.triggerActions()
        }

        fun setClient(clientInfo: ClientInfo) {
            whenever(configuration.currentClient).doReturn(clientInfo)
            whenever(configuration.isAgency).doReturn(clientInfo.isSubclient)
            whenever(configuration.passportUid).doReturn(PassportUid.Factory.from(1))
        }
    }

    private companion object {
        const val logoutMessage = "logoutMessage"

        const val logoutTitle = "logoutTitle"

        val bitmap = mock<Bitmap>()

        val avatarWithBitmap = Avatar.fromPreparedBitmap(bitmap)

        val avatarWithoutBitmap = Avatar.empty()

        @BeforeClass
        @JvmStatic
        fun runBeforeAllTests() {
            CurrencyInitializer.injectTestDataInStaticFields()
        }
    }
}