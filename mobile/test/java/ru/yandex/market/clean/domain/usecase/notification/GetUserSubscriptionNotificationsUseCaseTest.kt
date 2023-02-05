package ru.yandex.market.clean.domain.usecase.notification

import io.reactivex.Single
import junit.framework.TestCase
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.notifications.NotificationsRepository
import ru.yandex.market.clean.domain.model.notifications.section.subscriptionNotificationSectionTestInstance
import ru.yandex.market.domain.auth.model.uuidTestInstance
import ru.yandex.market.domain.auth.usecase.GetUuidUseCase

class GetUserSubscriptionNotificationsUseCaseTest : TestCase() {

    private val getUuidUseCaseMock = mock<GetUuidUseCase>() {
        on {
            getUuid()
        } doReturn Single.just(uuidTestInstance(UUID))
    }
    private val repositoryMock = mock<NotificationsRepository>() {
        on {
            getMultiNotifications(UUID)
        } doReturn Single.just(response)
    }

    private val useCase = GetUserSubscriptionNotificationsUseCase(repositoryMock, getUuidUseCaseMock)

    fun testExecute() {
        useCase.execute()
            .test()
            .assertValue(response)
    }

    private companion object {
        const val UUID = "some UUID"
        val response = listOf(subscriptionNotificationSectionTestInstance())
    }
}