package ru.yandex.market.clean.domain.usecase.plustrial

import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.plustrial.PlusTrialRepository
import ru.yandex.market.clean.domain.model.plustrial.PlusTrialInfo
import ru.yandex.market.clean.domain.model.plustrial.plusTrialInfoTestInstance
import ru.yandex.market.safe.Safe

class GetPlusTrialInfoUseCaseTest {
    private val plusTrialInfoSubject: SingleSubject<Safe<PlusTrialInfo>> = SingleSubject.create()

    private val plusTrialRepository = mock<PlusTrialRepository> {
        on { getPlusTrialInfo() } doReturn plusTrialInfoSubject
    }

    private val getPlusTrialInfoUseCase = GetPlusTrialInfoUseCase(plusTrialRepository)

    @Test
    fun `return value if plus trial info received`() {
        val expectedValue = plusTrialInfoTestInstance()
        plusTrialInfoSubject.onSuccess(Safe.value(expectedValue))

        getPlusTrialInfoUseCase.execute()
            .test()
            .assertNoErrors()
            .assertValue(expectedValue)
    }

    @Test
    fun `return error if plus trial info result is error`() {
        val expectedError = IllegalArgumentException()
        plusTrialInfoSubject.onSuccess(Safe.error(expectedError))

        getPlusTrialInfoUseCase.execute()
            .test()
            .assertError(expectedError)
    }
}