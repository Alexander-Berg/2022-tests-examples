package ru.auto.feature.carfax.interactor

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.model.carfax.CarfaxServerGenerateModel
import ru.auto.data.network.exception.ApiException
import ru.auto.data.network.scala.ScalaApiConst
import ru.auto.data.repository.report.ICarfaxRepository
import rx.Single
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class CarfaxSsrInteractorTest {

    private val repository: ICarfaxRepository = mock()
    private val interactor = CarfaxSsrInteractor(repository)

    @Test
    fun `get in progress error should re request search`() {
        val vinOrLicense = "0123qwerty"
        val decrementQuota = false
        val requestCount = 4
        var counter = 0

        whenever(repository.getXmlForSearch(vinOrLicense, decrementQuota))
            .thenAnswer {
                Single.error<CarfaxServerGenerateModel>(ApiException(errorCode = ScalaApiConst.IN_PROGRESS))
                    .doOnEach { counter++ }
            }
        interactor.getXmlForSearch(vinOrLicense, decrementQuota)
            .onErrorReturn { null }
            .toBlocking()
            .value()

        assertEquals(requestCount, counter)
    }
}
