package ru.yandex.market.clean.domain.usecase.cms

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.domain.user.model.UserProfile
import ru.yandex.market.domain.user.model.userProfileTestInstance
import ru.yandex.market.clean.domain.model.cms.cmsPlusHomeNavigationItemTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.PlusHomeGarson
import ru.yandex.market.clean.domain.model.cms.garson.plusHomeGarsonTestInstance
import ru.yandex.market.clean.domain.usecase.plushome.GetPlusHomeWidgetWasInteractedUseCase
import ru.yandex.market.clean.domain.usecase.plushome.PlusHomeEnabledUseCase
import ru.yandex.market.domain.user.usecase.ObserveCurrentUserProfileUseCase
import ru.yandex.market.optional.Optional

class CmsPlusHomeUseCaseTest {

    private val enabledSubject: SingleSubject<Boolean> = SingleSubject.create()
    private val interactedSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    private val currentUserSubject: BehaviorSubject<Optional<UserProfile>> = BehaviorSubject.create()
    private val plusHomeEnabledUseCase = mock<PlusHomeEnabledUseCase> {
        on { execute() } doReturn enabledSubject
    }
    private val getPlusHomeWidgetWasInteractedUseCase = mock<GetPlusHomeWidgetWasInteractedUseCase> {
        on { execute(any()) } doReturn interactedSubject
    }
    private val observeCurrentUserProfileUseCase = mock<ObserveCurrentUserProfileUseCase> {
        on { execute() } doReturn currentUserSubject
    }

    private val useCase = CmsPlusHomeUseCase(
        plusHomeEnabledUseCase, getPlusHomeWidgetWasInteractedUseCase, observeCurrentUserProfileUseCase
    )

    @Test
    fun `return empty if garson has incorrect type`() {
        useCase.execute(mock()).test().assertValue(emptyList())
    }

    @Test
    fun `return empty if plus home disabled`() {
        enabledSubject.onSuccess(false)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.of(userProfileTestInstance()))
        useCase.execute(
            plusHomeGarsonTestInstance(
                showTo = listOf(
                    PlusHomeGarson.UserType.REGULAR,
                    PlusHomeGarson.UserType.PLUS,
                    PlusHomeGarson.UserType.NOT_LOGGED_IN
                )
            )
        )
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `return empty if widget was interacted`() {
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(true)
        currentUserSubject.onNext(Optional.of(userProfileTestInstance()))
        useCase.execute(
            plusHomeGarsonTestInstance(
                showTo = listOf(
                    PlusHomeGarson.UserType.REGULAR,
                    PlusHomeGarson.UserType.PLUS,
                    PlusHomeGarson.UserType.NOT_LOGGED_IN
                )
            )
        )
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `return empty if showTo = PLUS and current user is not plus user`() {
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.of(userProfileTestInstance(hasYandexPlus = false)))
        useCase.execute(plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.PLUS)))
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `return empty if showTo = REGULAR and current user is not logged id`() {
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.empty())
        useCase.execute(plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.REGULAR)))
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `return empty if showTo = NOT_LOGGE_IND and current user logged in`() {
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.of(userProfileTestInstance()))
        useCase.execute(plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.NOT_LOGGED_IN)))
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `return value for plus user if showTo contains PLUS`() {
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.of(userProfileTestInstance(hasYandexPlus = true)))
        val expected = cmsPlusHomeNavigationItemTestInstance()
        useCase.execute(plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.PLUS)))
            .test()
            .assertValue(listOf(expected))
    }

    @Test
    fun `return value for regular user is showTo contains REGULAR`() {
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.of(userProfileTestInstance(hasYandexPlus = false)))
        val expected = cmsPlusHomeNavigationItemTestInstance()
        useCase.execute(plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.REGULAR)))
            .test()
            .assertValue(listOf(expected))
    }

    @Test
    fun `return value for not logged in user is showTo contains NOT_LOGGED_IN`() {
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.empty())
        val expected = cmsPlusHomeNavigationItemTestInstance()
        useCase.execute(plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.NOT_LOGGED_IN)))
            .test()
            .assertValue(listOf(expected))
    }

    @Test
    fun `change value on widget interaction status change`() {
        val testObserver = useCase.execute(
            plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.PLUS))
        ).test()
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.of(userProfileTestInstance(hasYandexPlus = true)))
        interactedSubject.onNext(true)
        testObserver
            .assertValueCount(2)
            .assertValueAt(0, listOf(cmsPlusHomeNavigationItemTestInstance()))
            .assertValueAt(1, emptyList())
    }

    @Test
    fun `change value on user status change`() {
        val testObserver = useCase.execute(
            plusHomeGarsonTestInstance(showTo = listOf(PlusHomeGarson.UserType.PLUS))
        ).test()
        enabledSubject.onSuccess(true)
        interactedSubject.onNext(false)
        currentUserSubject.onNext(Optional.of(userProfileTestInstance(hasYandexPlus = true)))
        currentUserSubject.onNext(Optional.of(userProfileTestInstance(hasYandexPlus = false)))
        testObserver
            .assertValueCount(2)
            .assertValueAt(0, listOf(cmsPlusHomeNavigationItemTestInstance()))
            .assertValueAt(1, emptyList())
    }

}