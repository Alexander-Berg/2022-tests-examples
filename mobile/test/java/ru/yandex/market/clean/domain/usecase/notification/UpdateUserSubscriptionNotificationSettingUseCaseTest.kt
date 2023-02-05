package ru.yandex.market.clean.domain.usecase.notification

import io.reactivex.Single
import junit.framework.TestCase
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.notifications.NotificationsRepository
import ru.yandex.market.clean.domain.model.notifications.notification.subscriptionNotificationTestInstance
import ru.yandex.market.clean.domain.model.notifications.section.subscriptionNotificationSectionTestInstance
import ru.yandex.market.domain.auth.model.uuidTestInstance
import ru.yandex.market.domain.auth.usecase.GetUuidUseCase

class UpdateUserSubscriptionNotificationSettingUseCaseTest : TestCase() {

    private val getUuidUseCaseMock = mock<GetUuidUseCase>() {
        on { getUuid() } doReturn Single.just(uuidTestInstance(UUID))
    }
    private val subscriptionRepositoryMock = mock<NotificationsRepository>() {
        on { updateMultiNotificationSetting(UUID, inputData) } doReturn Single.just(response)
    }
    private val useCase = UpdateUserSubscriptionNotificationSettingUseCase(
        subscriptionRepositoryMock,
        getUuidUseCaseMock
    )

    fun testExecute() {
        useCase.execute(inputData)
            .test()
            .assertValue(response)
    }

    private companion object {
        const val UUID = "some UUID"
        val inputData = listOf(subscriptionNotificationTestInstance())
        val response = listOf(subscriptionNotificationSectionTestInstance())
    }
}