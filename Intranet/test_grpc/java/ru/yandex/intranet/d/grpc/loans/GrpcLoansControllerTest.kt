package ru.yandex.intranet.d.grpc.loans

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.web.model.loans.LoanDirection
import ru.yandex.intranet.d.web.model.loans.api.ApiLoanDto
import ru.yandex.intranet.d.web.model.loans.api.ApiLoansHistoryDto
import java.util.*

@IntegrationTest
class GrpcLoansControllerTest @Autowired constructor(
    private val helper: GrpcLoansTestsHelper
) {
    @Test
    fun testSearchLoansIn() = runBlocking {
        val data = helper.prepareLoansIn()
        val result = helper.searchLoans(data[0].second.serviceId, LoanDirection.IN, LoanStatus.PENDING)
        val expectedOrderedLoans =
            data.filter { it.first.status == LoanStatus.PENDING }.map { ApiLoanDto(it.first).toProto() }
        assertEquals(expectedOrderedLoans, result.loansList)
    }

    @Test
    fun testSearchLoansInPagination() = runBlocking {
        val data = helper.prepareLoansIn()
        val firstPage = helper.searchLoans(data[0].second.serviceId, LoanDirection.IN, LoanStatus.PENDING, limit = 1)
        val expectedFirstPageLoans = listOf(ApiLoanDto(data[0].first).toProto())
        assertEquals(expectedFirstPageLoans, firstPage.loansList)
        val secondPage = helper.searchLoans(
            data[0].second.serviceId,
            LoanDirection.IN,
            LoanStatus.PENDING,
            limit = 1,
            from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(ApiLoanDto(data[1].first).toProto())
        assertEquals(expectedSecondPageLoans, secondPage.loansList)
    }

    @Test
    fun testSearchLoansOut() = runBlocking {
        val data = helper.prepareLoansOut()
        val result = helper.searchLoans(data[0].second.serviceId, LoanDirection.OUT, LoanStatus.PENDING)
        val expectedOrderedLoans =
            data.filter { it.first.status == LoanStatus.PENDING }.map { ApiLoanDto(it.first).toProto() }
        assertEquals(expectedOrderedLoans, result.loansList)
    }

    @Test
    fun testSearchLoansOutPagination() = runBlocking {
        val data = helper.prepareLoansOut()
        val firstPage = helper.searchLoans(data[0].second.serviceId, LoanDirection.OUT, LoanStatus.PENDING, limit = 1)
        val expectedFirstPageLoans = listOf(ApiLoanDto(data[0].first).toProto())
        assertEquals(expectedFirstPageLoans, firstPage.loansList)
        val secondPage = helper.searchLoans(
            data[0].second.serviceId,
            LoanDirection.OUT,
            LoanStatus.PENDING,
            limit = 1,
            from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(ApiLoanDto(data[1].first).toProto())
        assertEquals(expectedSecondPageLoans, secondPage.loansList)
    }

    @Test
    fun testSearchLoansEmptyResult() = runBlocking {
        val resultIn = helper.searchLoans(serviceId = 42L, LoanDirection.IN)
        assertTrue(resultIn.loansList.isEmpty())
        val resultOut = helper.searchLoans(serviceId = 42L, LoanDirection.OUT)
        assertTrue(resultOut.loansList.isEmpty())
    }

    @Test
    fun testGetLoansHistory() = runBlocking {
        val models = helper.prepareLoansHistory()
        val expectedHistoryDtos = models.map { ApiLoansHistoryDto(it).toProto() }
        val result = helper.getLoansHistory(models.first().key.loanId)
        assertEquals(expectedHistoryDtos, result.eventsList)
    }

    @Test
    fun testGetLoansHistoryPaging() = runBlocking {
        val models = helper.prepareLoansHistory()
        val expectedHistoryDtos = models.map { ApiLoansHistoryDto(it).toProto() }
        val firstPage = helper.getLoansHistory(models.first().key.loanId, limit = 1)
        assertEquals(listOf(expectedHistoryDtos[0]), firstPage.eventsList)

        val secondPage = helper.getLoansHistory(
            models.first().key.loanId,
            from = firstPage.continuationToken!!,
            limit = 1
        )
        assertEquals(listOf(expectedHistoryDtos[1]), secondPage.eventsList)
    }

    @Test
    fun testGetAllLoansInPagination() = runBlocking {
        val data = helper.prepareLoansIn()
        val firstPage = helper.searchLoans(data[0].second.serviceId, LoanDirection.IN, limit = 1)
        val expectedFirstPageLoans = listOf(ApiLoanDto(data[0].first).toProto())
        assertEquals(expectedFirstPageLoans, firstPage.loansList)
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, LoanDirection.IN, limit = 2, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(ApiLoanDto(data[1].first).toProto(), ApiLoanDto(data[2].first).toProto())
        assertEquals(expectedSecondPageLoans, secondPage.loansList)
    }

    @Test
    fun testGetAllLoansOutPagination() = runBlocking {
        val data = helper.prepareLoansOut()
        val firstPage = helper.searchLoans(data[0].second.serviceId, LoanDirection.OUT, limit = 1)
        val expectedFirstPageLoans = listOf(ApiLoanDto(data[0].first).toProto())
        assertEquals(expectedFirstPageLoans, firstPage.loansList)
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, LoanDirection.OUT, limit = 2, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(ApiLoanDto(data[1].first).toProto(), ApiLoanDto(data[2].first).toProto())
        assertEquals(expectedSecondPageLoans, secondPage.loansList)
    }

    @Test
    fun testGetLoansHistoryEmptyResult() = runBlocking {
        val result = helper.getLoansHistory(loanId = UUID.randomUUID().toString())
        assertTrue(result.eventsList.isEmpty())
    }
}
