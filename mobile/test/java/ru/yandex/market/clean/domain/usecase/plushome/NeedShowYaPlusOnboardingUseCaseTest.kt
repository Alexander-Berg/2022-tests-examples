package ru.yandex.market.clean.domain.usecase.plushome

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.data.repository.plushome.YaPlusShownOnboardingRepository
import ru.yandex.market.clean.domain.model.plushome.YaPlusOnboardingType

@RunWith(Parameterized::class)
class NeedShowYaPlusOnboardingUseCaseTest(
    private val requiredOnboarding: YaPlusOnboardingType,
    private val alreadyShownOnboarding: YaPlusOnboardingType,
    private val needShowOnboarding: Boolean
) {

    private val shownOnboardingsSubject: SingleSubject<List<YaPlusOnboardingType>> = SingleSubject.create()
    private val repository = mock<YaPlusShownOnboardingRepository> {
        on { getShownOnboardings() } doReturn shownOnboardingsSubject
    }
    private val useCase = NeedShowYaPlusOnboardingUseCase(repository)

    @Test
    fun `Check need show onboarding`() {
        shownOnboardingsSubject.onSuccess(listOf(alreadyShownOnboarding))

        useCase.execute(requiredOnboarding)
            .test()
            .assertNoErrors()
            .assertValue(needShowOnboarding)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: expected {2} if requested {0} and already shown {1}")
        @JvmStatic
        fun data() = listOf(
            //1
            arrayOf(
                YaPlusOnboardingType.PLUS_USER_POSITIVE_BALANCE,
                YaPlusOnboardingType.PLUS_USER_POSITIVE_BALANCE,
                false
            ),
            //2
            arrayOf(
                YaPlusOnboardingType.PLUS_USER_POSITIVE_BALANCE,
                YaPlusOnboardingType.NOT_PLUS_USER_POSITIVE_BALANCE,
                false
            ),
            //3
            arrayOf(
                YaPlusOnboardingType.PLUS_USER_POSITIVE_BALANCE,
                YaPlusOnboardingType.UNKNOWN,
                true
            ),
            //4
            arrayOf(
                YaPlusOnboardingType.PLUS_USER_ZERO_BALANCE,
                YaPlusOnboardingType.PLUS_USER_ZERO_BALANCE,
                false
            ),
            //5
            arrayOf(
                YaPlusOnboardingType.PLUS_USER_ZERO_BALANCE,
                YaPlusOnboardingType.NOT_PLUS_USER_ZERO_BALANCE,
                false
            ),
            //6
            arrayOf(
                YaPlusOnboardingType.PLUS_USER_ZERO_BALANCE,
                YaPlusOnboardingType.FREE_DELIVERY_BY_PLUS,
                true
            ),
            //7
            arrayOf(
                YaPlusOnboardingType.NOT_PLUS_USER_POSITIVE_BALANCE,
                YaPlusOnboardingType.PLUS_USER_POSITIVE_BALANCE,
                false
            ),
            //8
            arrayOf(
                YaPlusOnboardingType.NOT_PLUS_USER_POSITIVE_BALANCE,
                YaPlusOnboardingType.PLUS_USER_POSITIVE_BALANCE,
                false
            ),
            //9
            arrayOf(
                YaPlusOnboardingType.NOT_PLUS_USER_POSITIVE_BALANCE,
                YaPlusOnboardingType.UNKNOWN,
                true
            ),
            //10
            arrayOf(
                YaPlusOnboardingType.NOT_PLUS_USER_ZERO_BALANCE,
                YaPlusOnboardingType.PLUS_USER_ZERO_BALANCE,
                false
            ),
            //11
            arrayOf(
                YaPlusOnboardingType.NOT_PLUS_USER_ZERO_BALANCE,
                YaPlusOnboardingType.NOT_PLUS_USER_ZERO_BALANCE,
                false
            ),
            //12
            arrayOf(
                YaPlusOnboardingType.NOT_PLUS_USER_ZERO_BALANCE,
                YaPlusOnboardingType.FREE_DELIVERY_BY_PLUS,
                true
            )
        )
    }
}