package ru.yandex.intranet.d.web.api.services

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestServices.TEST_EMPTY_SERVICE_ID_CLOSING
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.loans.ServiceLoansInDao
import ru.yandex.intranet.d.dao.loans.ServiceLoansOutDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.model.loans.ServiceLoanInModel
import ru.yandex.intranet.d.model.loans.ServiceLoanOutModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.services.CheckServicesRequestDto
import ru.yandex.intranet.d.web.model.services.ClosingServiceDto
import java.time.Instant
import java.util.*

@IntegrationTest
class ApiServicesTest @Autowired constructor(
    private val webClient: WebTestClient,
    private val tableClient: YdbTableClient,
    private val serviceLoansInDao: ServiceLoansInDao,
    private val serviceLoansOutDao: ServiceLoansOutDao,
    @Value("\${abc.tvmSourceId}")
    private var abcTvmSourceId: Long = 0L
) {
    @Test
    fun testServiceWithPendingLoansInNotClosing(): Unit = runBlocking {
        val serviceLoanInModel = ServiceLoanInModel(
            Tenants.DEFAULT_TENANT_ID,
            serviceId = TEST_EMPTY_SERVICE_ID_CLOSING,
            LoanStatus.PENDING,
            dueAt = Instant.now(),
            loanId = UUID.randomUUID().toString()
        )
        dbSessionRetryable(tableClient) {
            serviceLoansInDao.upsertOneRetryable(rwSingleRetryableCommit(), serviceLoanInModel)
        }
        val result = webClient
            .mutateWith(MockUser.tvm(abcTvmSourceId))
            .post()
            .uri("/abc/_checkClosing", 1)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(CheckServicesRequestDto(listOf(TEST_EMPTY_SERVICE_ID_CLOSING)))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ClosingServiceDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, result.closing.size)
        assertTrue(result.closing.any { it.serviceId == TEST_EMPTY_SERVICE_ID_CLOSING })
        assertFalse(result.closing.first { it.serviceId == TEST_EMPTY_SERVICE_ID_CLOSING }.isValue)
    }

    @Test
    fun testServiceWithPendingLoansOutNotClosing(): Unit = runBlocking {
        val serviceLoanOutModel = ServiceLoanOutModel(
            Tenants.DEFAULT_TENANT_ID,
            serviceId = TEST_EMPTY_SERVICE_ID_CLOSING,
            LoanStatus.PENDING,
            dueAt = Instant.now(),
            loanId = UUID.randomUUID().toString()
        )
        dbSessionRetryable(tableClient) {
            serviceLoansOutDao.upsertOneRetryable(rwSingleRetryableCommit(), serviceLoanOutModel)
        }
        val result = webClient
            .mutateWith(MockUser.tvm(abcTvmSourceId))
            .post()
            .uri("/abc/_checkClosing", 1)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(CheckServicesRequestDto(listOf(TEST_EMPTY_SERVICE_ID_CLOSING)))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ClosingServiceDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(1, result.closing.size)
        assertTrue(result.closing.any { it.serviceId == TEST_EMPTY_SERVICE_ID_CLOSING })
        assertFalse(result.closing.first { it.serviceId == TEST_EMPTY_SERVICE_ID_CLOSING }.isValue)
    }
}
