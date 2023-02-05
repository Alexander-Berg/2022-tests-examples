package ru.yandex.market.clean.presentation.feature.notifications.fragment

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.NotificationsSettingsAnalytics
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.SystemSettingsNotificationsTargetScreen
import ru.yandex.market.presentationSchedulersMock

class NotificationsSettingsPresenterTest {

    private val routerMock = mock<Router>()
    private val analyticsMock = mock<NotificationsSettingsAnalytics>()
    private val useCasesMock = mock<NotificationsSettingsUseCases> {
        on { updateUserNotificationsStatus(any()) } doReturn Single.just(emptyList())
        on { checkInternetConnection() } doReturn true
    }

    private val presenter = NotificationsSettingsPresenter(
        errorFormatter = mock(),
        router = routerMock,
        notificationSectionFormatter = mock(),
        notificationToggleFormatter = mock(),
        notificationsSettingsAnalytics = analyticsMock,
        schedulers = presentationSchedulersMock(),
        useCases = useCasesMock,
        healthService = mock(),
        healthErrorFormatter = mock(),
    )

    @Test
    fun `Send analytics on notification enable`() {
        presenter.onNotificationToggleClick(ID, true)

        verify(analyticsMock).sendNotificationSettingTurnOnEvent(ID)
    }

    @Test
    fun `Send analytics on notification disable`() {
        presenter.onNotificationToggleClick(ID, false)

        verify(analyticsMock).sendNotificationSettingTurnOffEvent(ID)
    }

    @Test
    fun `Send analytics error on notification status update`() {
        whenever(useCasesMock.updateUserNotificationsStatus(any())) doReturn Single.error(Throwable())

        presenter.onNotificationToggleClick(ID, true)

        verify(analyticsMock).sendNotificationsSettingsScreenErrorEvent()
    }

    @Test
    fun onGoToSystemPushNotificationsSettingsButtonClick() {
        presenter.onGoToSystemPushNotificationsSettingsButtonClick()

        verify(analyticsMock).sendNotificationsSettingsGoToSystemSettingsEvent()
        verify(routerMock).navigateTo(SystemSettingsNotificationsTargetScreen())
    }

    private companion object {
        const val ID = 1L
    }
}