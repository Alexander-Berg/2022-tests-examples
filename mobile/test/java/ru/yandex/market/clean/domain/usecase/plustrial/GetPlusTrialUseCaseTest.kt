package ru.yandex.market.clean.domain.usecase.plustrial

import io.reactivex.Completable
import io.reactivex.subjects.CompletableSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.data.repository.plustrial.PlusTrialRepository
import ru.yandex.market.fragment.main.profile.SetUserInformationAffectedUseCase

class GetPlusTrialUseCaseTest {
    private val plusTrialSubject: CompletableSubject = CompletableSubject.create()

    private val plusTrialRepository = mock<PlusTrialRepository> {
        on { getPlusTrial() } doReturn plusTrialSubject
    }
    private val setUserInformationAffectedUseCase = mock<SetUserInformationAffectedUseCase> {
        on { execute() } doReturn Completable.complete()
    }

    private val getPlusTrialUseCase = GetPlusTrialUseCase(plusTrialRepository, setUserInformationAffectedUseCase)

    @Test
    fun `return complete if plus trial received`() {
        plusTrialSubject.onComplete()

        getPlusTrialUseCase.execute()
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `not handle error if was error on plus trial receive`() {
        val error = Error()
        plusTrialSubject.onError(error)

        getPlusTrialUseCase.execute()
            .test()
            .assertError(error)
    }

    @Test
    fun `set user information affected on plus trial received`() {
        plusTrialSubject.onComplete()

        getPlusTrialUseCase.execute()
            .test()

        verify(setUserInformationAffectedUseCase).execute()
    }
}