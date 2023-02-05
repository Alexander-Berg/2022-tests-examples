package ru.yandex.market.clean.data.repository.plustrial

import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.CompletableSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.base.network.common.Response
import ru.yandex.market.base.network.common.exception.CommunicationException
import ru.yandex.market.clean.data.fapi.source.plustrial.PlusTrialFapiClient
import ru.yandex.market.clean.domain.model.plustrial.plusTrialInfoTestInstance
import ru.yandex.market.common.schedulers.NetworkingScheduler

class PlusTrialRepositoryTest {
    private val plusTrialIno = plusTrialInfoTestInstance()
    private val plusTrialSubject: CompletableSubject = CompletableSubject.create()

    private val networkingScheduler = mock<NetworkingScheduler> {
        on { scheduler } doReturn Schedulers.trampoline()
    }

    private val plusTrialFapiClient = mock<PlusTrialFapiClient> {
        on { getPlusTrial() } doReturn plusTrialSubject
    }

    private val repository = PlusTrialRepository(networkingScheduler, plusTrialFapiClient)

    // Вернуть при получении данных с бэка
//    @Test
//    fun `Return result from fapi client for plus trial info`() {
//        repository.getPlusTrialInfo()
//            .test()
//            .assertNoErrors()
//            .assertValue(Safe.value(plusTrialIno))
//    }

    @Test
    fun `Do not handle nay error from fapi client when getting plus trial info`() {
        val error = CommunicationException(Response.BAD_REQUEST)
        repository.getPlusTrialInfo()
            .test()
            .assertError(error)
    }

    @Test
    fun `Return result from fapi client for getting plus trial`() {
        plusTrialSubject.onComplete()
        repository.getPlusTrial()
            .test()
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `Do not handle nay error from fapi client when getting plus trial`() {
        val error = CommunicationException(Response.SERVICE_ERROR)
        plusTrialSubject.onError(error)
        repository.getPlusTrial()
            .test()
            .assertError(error)
    }

    @Test
    fun `Subscribe get plus trial on network scheduler`() {
        repository.getPlusTrial()
        verify(networkingScheduler).scheduler
    }

    // Вернуть при получении данных с бэка
//    @Test
//    fun `Subscribe get plus trial info on network scheduler`() {
//        repository.getPlusTrialInfo()
//        verify(networkingScheduler).scheduler
//    }
}