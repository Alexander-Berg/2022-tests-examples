package ru.yandex.intranet.d.web.api.loans

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
class ApiLoansControllerTest @Autowired constructor(
    private val helper: ApiLoansTestsHelper
) {
    @Test
    fun testSearchLoansIn() = runBlocking {
        val data = helper.prepareLoansIn()
        val result = helper.searchLoans(data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.IN)
        val expectedOrderedLoans = data.filter { it.first.status == LoanStatus.PENDING }.map { ApiLoanDto(it.first) }
        assertEquals(expectedOrderedLoans, result.loans)
    }

    @Test
    fun testSearchLoansInPagination() = runBlocking {
        val data = helper.prepareLoansIn()
        val firstPage = helper.searchLoans(data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.IN, limit = 1)
        val expectedFirstPageLoans = listOf(ApiLoanDto(data[0].first))
        assertEquals(expectedFirstPageLoans, firstPage.loans)
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.IN, limit = 1, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(ApiLoanDto(data[1].first))
        assertEquals(expectedSecondPageLoans, secondPage.loans)
    }

    @Test
    fun testSearchLoansOut() = runBlocking {
        val data = helper.prepareLoansOut()
        val result = helper.searchLoans(data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.OUT)
        val expectedOrderedLoans = data.filter { it.first.status == LoanStatus.PENDING }.map { ApiLoanDto(it.first) }
        assertEquals(expectedOrderedLoans, result.loans)
    }

    @Test
    fun testSearchLoansOutPagination() = runBlocking {
        val data = helper.prepareLoansOut()
        val firstPage = helper.searchLoans(data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.OUT, limit = 1)
        val expectedFirstPageLoans = listOf(ApiLoanDto(data[0].first))
        assertEquals(expectedFirstPageLoans, firstPage.loans)
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, status = LoanStatus.PENDING, LoanDirection.OUT, limit = 1, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(ApiLoanDto(data[1].first))
        assertEquals(expectedSecondPageLoans, secondPage.loans)
    }

    @Test
    fun testSearchLoansEmptyResult() = runBlocking {
        val resultIn = helper.searchLoans(serviceId = 42L, status = null, LoanDirection.IN)
        assertTrue(resultIn.loans.isEmpty())
        val resultOut = helper.searchLoans(serviceId = 42L, status = null, LoanDirection.OUT)
        assertTrue(resultOut.loans.isEmpty())
    }

    @Test
    fun testGetLoansHistory() = runBlocking {
        val models = helper.prepareLoansHistory()
        val expectedHistoryDtos = models.map { ApiLoansHistoryDto(it) }
        val result = helper.getLoansHistory(models.first().key.loanId)
        assertEquals(expectedHistoryDtos, result.events)
    }

    @Test
    fun testGetLoansHistoryPaging() = runBlocking {
        val models = helper.prepareLoansHistory()
        val expectedHistoryDtos = models.map { ApiLoansHistoryDto(it) }
        val firstPage = helper.getLoansHistory(models.first().key.loanId, limit = 1)
        assertEquals(listOf(expectedHistoryDtos[0]), firstPage.events)

        val secondPage = helper.getLoansHistoryNextPage(
            models.first().key.loanId,
            pageToken = firstPage.continuationToken!!,
            limit = 1
        )
        assertEquals(listOf(expectedHistoryDtos[1]), secondPage.events)
    }

    @Test
    fun testSearchAllLoansInPagination() = runBlocking {
        val data = helper.prepareLoansIn()
        val firstPage = helper.searchLoans(data[0].second.serviceId, status = null, LoanDirection.IN, limit = 1)
        val expectedFirstPageLoans = listOf(ApiLoanDto(data[0].first))
        assertEquals(expectedFirstPageLoans, firstPage.loans)
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, status = null, LoanDirection.IN, limit = 2, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(ApiLoanDto(data[1].first), ApiLoanDto(data[2].first))
        assertEquals(expectedSecondPageLoans, secondPage.loans)
    }

    @Test
    fun testSearchAllLoansOutPagination() = runBlocking {
        val data = helper.prepareLoansOut()
        val firstPage = helper.searchLoans(data[0].second.serviceId, status = null, LoanDirection.OUT, limit = 1)
        val expectedFirstPageLoans = listOf(ApiLoanDto(data[0].first))
        assertEquals(expectedFirstPageLoans, firstPage.loans)
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, status = null, LoanDirection.OUT, limit = 2, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(ApiLoanDto(data[1].first), ApiLoanDto(data[2].first))
        assertEquals(expectedSecondPageLoans, secondPage.loans)
    }

    @Test
    fun testGetLoansHistoryEmptyResult() = runBlocking {
        val result = helper.getLoansHistory(loanId = UUID.randomUUID().toString())
        assertTrue(result.events.isEmpty())
    }
}
