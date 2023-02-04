package ru.yandex.intranet.d.web.front.loans

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_6_ID
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_8_ID
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3_ID
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2_ID
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_7_ID
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestResourceTypes.YP_HDD
import ru.yandex.intranet.d.TestResources.YP_HDD_MAN
import ru.yandex.intranet.d.TestResources.YP_SSD_MAN
import ru.yandex.intranet.d.TestSegmentations.YP_LOCATION
import ru.yandex.intranet.d.TestSegmentations.YP_LOCATION_MAN
import ru.yandex.intranet.d.TestSegmentations.YP_SEGMENT
import ru.yandex.intranet.d.TestSegmentations.YP_SEGMENT_DEFAULT
import ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER
import ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_ZERO_QUOTAS
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.web.model.loans.LoanDirection
import ru.yandex.intranet.d.web.model.loans.front.FrontLoanDto
import ru.yandex.intranet.d.web.model.loans.front.FrontLoansHistoryDto
import java.util.*

@IntegrationTest
class FrontLoansControllerTest @Autowired constructor(
    private val helper: FrontLoansTestsHelper
) {
    @Test
    fun testSearchLoansIn() = runBlocking {
        val data = helper.prepareLoansIn()
        val result = helper.searchLoans(data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.IN)
        val expectedOrderedLoans = data.filter { it.first.status == LoanStatus.PENDING }.map { FrontLoanDto(it.first) }
        assertEquals(expectedOrderedLoans, result.loans)
        assertTrue(result.resources.containsKey(YP_HDD_MAN))
        assertTrue(result.resourceTypes.containsKey(YP_HDD))
        assertTrue(result.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(result.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(result.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(result.segmentations.containsKey(YP_LOCATION))
        assertTrue(result.segmentations.containsKey(YP_SEGMENT))
        assertTrue(result.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(result.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(result.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(result.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(result.providers.containsKey(YP_ID))
        assertTrue(result.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(result.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(result.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(result.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
    }

    @Test
    fun testSearchLoansInPagination() = runBlocking {
        val data = helper.prepareLoansIn()
        val firstPage = helper.searchLoans(data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.IN, limit = 1)
        val expectedFirstPageLoans = listOf(FrontLoanDto(data[0].first))
        assertEquals(expectedFirstPageLoans, firstPage.loans)
        assertTrue(firstPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(firstPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(firstPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(firstPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(firstPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(firstPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(firstPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(firstPage.providers.containsKey(YP_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.IN, limit = 1, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(FrontLoanDto(data[1].first))
        assertEquals(expectedSecondPageLoans, secondPage.loans)
        assertTrue(secondPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(secondPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(secondPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(secondPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(secondPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(secondPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(secondPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(secondPage.providers.containsKey(YP_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
    }

    @Test
    fun testSearchLoansOut() = runBlocking {
        val data = helper.prepareLoansOut()
        val result = helper.searchLoans(data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.OUT)
        val expectedOrderedLoans = data.filter { it.first.status == LoanStatus.PENDING }.map { FrontLoanDto(it.first) }
        assertEquals(expectedOrderedLoans, result.loans)
        assertTrue(result.resources.containsKey(YP_HDD_MAN))
        assertTrue(result.resourceTypes.containsKey(YP_HDD))
        assertTrue(result.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(result.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(result.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(result.segmentations.containsKey(YP_LOCATION))
        assertTrue(result.segmentations.containsKey(YP_SEGMENT))
        assertTrue(result.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(result.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(result.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(result.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(result.providers.containsKey(YP_ID))
        assertTrue(result.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(result.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(result.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(result.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
    }

    @Test
    fun testSearchLoansOutPagination() = runBlocking {
        val data = helper.prepareLoansOut()
        val firstPage = helper.searchLoans(data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.OUT, limit = 1)
        val expectedFirstPageLoans = listOf(FrontLoanDto(data[0].first))
        assertEquals(expectedFirstPageLoans, firstPage.loans)
        assertTrue(firstPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(firstPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(firstPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(firstPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(firstPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(firstPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(firstPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(firstPage.providers.containsKey(YP_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, LoanStatus.PENDING, LoanDirection.OUT, limit = 1, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(FrontLoanDto(data[1].first))
        assertEquals(expectedSecondPageLoans, secondPage.loans)
        assertTrue(secondPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(secondPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(secondPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(secondPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(secondPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(secondPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(secondPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(secondPage.providers.containsKey(YP_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
    }

    @Test
    fun testSearchAllLoansInPagination() = runBlocking {
        val data = helper.prepareLoansIn()
        val firstPage = helper.searchLoans(data[0].second.serviceId, status = null, LoanDirection.IN, limit = 1)
        val expectedFirstPageLoans = listOf(FrontLoanDto(data[0].first))
        assertEquals(expectedFirstPageLoans, firstPage.loans)
        assertTrue(firstPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(firstPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(firstPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(firstPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(firstPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(firstPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(firstPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(firstPage.providers.containsKey(YP_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, status = null, LoanDirection.IN, limit = 2, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(FrontLoanDto(data[1].first), FrontLoanDto(data[2].first))
        assertEquals(expectedSecondPageLoans, secondPage.loans)
        assertTrue(secondPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(secondPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(secondPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(secondPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(secondPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(secondPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(secondPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(secondPage.providers.containsKey(YP_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
    }

    @Test
    fun testSearchAllLoansOutPagination() = runBlocking {
        val data = helper.prepareLoansOut()
        val firstPage = helper.searchLoans(data[0].second.serviceId, status = null, LoanDirection.OUT, limit = 1)
        val expectedFirstPageLoans = listOf(FrontLoanDto(data[0].first))
        assertEquals(expectedFirstPageLoans, firstPage.loans)
        assertTrue(firstPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(firstPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(firstPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(firstPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(firstPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(firstPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(firstPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(firstPage.providers.containsKey(YP_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
        val secondPage = helper.searchLoans(
            data[0].second.serviceId, status = null, LoanDirection.OUT, limit = 2, from = firstPage.continuationToken
        )
        val expectedSecondPageLoans = listOf(FrontLoanDto(data[1].first), FrontLoanDto(data[2].first))
        assertEquals(expectedSecondPageLoans, secondPage.loans)
        assertTrue(secondPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(secondPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(secondPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(secondPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(secondPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(secondPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(secondPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(secondPage.providers.containsKey(YP_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
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
        val expectedHistoryDtos = models.map { FrontLoansHistoryDto(it) }
        val result = helper.getLoansHistory(models.first().key.loanId)
        assertEquals(expectedHistoryDtos, result.events)
        assertTrue(result.resources.containsKey(YP_HDD_MAN))
        assertTrue(result.resources.containsKey(YP_SSD_MAN))
        assertTrue(result.resourceTypes.containsKey(YP_HDD))
        assertTrue(result.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(result.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(result.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(result.segmentations.containsKey(YP_LOCATION))
        assertTrue(result.segmentations.containsKey(YP_SEGMENT))
        assertTrue(result.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(result.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(result.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(result.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(result.providers.containsKey(YP_ID))
        assertTrue(result.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(result.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(result.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(result.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
    }

    @Test
    fun testGetLoansHistoryPaging() = runBlocking {
        val models = helper.prepareLoansHistory()
        val expectedHistoryDtos = models.map { FrontLoansHistoryDto(it) }
        val firstPage = helper.getLoansHistory(models.first().key.loanId, limit = 1)
        assertEquals(listOf(expectedHistoryDtos[0]), firstPage.events)
        assertTrue(firstPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(firstPage.resources.containsKey(YP_SSD_MAN))
        assertTrue(firstPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(firstPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(firstPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(firstPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(firstPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(firstPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(firstPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(firstPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(firstPage.providers.containsKey(YP_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(firstPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(firstPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))

        val secondPage = helper.getLoansHistoryNextPage(
            models.first().key.loanId,
            pageToken = firstPage.continuationToken!!,
            limit = 1
        )
        assertEquals(listOf(expectedHistoryDtos[1]), secondPage.events)
        assertTrue(secondPage.resources.containsKey(YP_HDD_MAN))
        assertTrue(secondPage.resources.containsKey(YP_SSD_MAN))
        assertTrue(secondPage.resourceTypes.containsKey(YP_HDD))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_8_ID))
        assertTrue(secondPage.accounts.containsKey(TEST_ACCOUNT_6_ID))
        assertTrue(secondPage.accountsSpaces.containsKey(TEST_ACCOUNT_SPACE_3_ID))
        assertTrue(secondPage.segmentations.containsKey(YP_LOCATION))
        assertTrue(secondPage.segmentations.containsKey(YP_SEGMENT))
        assertTrue(secondPage.segments.containsKey(YP_LOCATION_MAN))
        assertTrue(secondPage.segments.containsKey(YP_SEGMENT_DEFAULT))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_1_ID))
        assertTrue(secondPage.users.containsKey(TestUsers.USER_2_ID))
        assertTrue(secondPage.providers.containsKey(YP_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_2_ID))
        assertTrue(secondPage.folders.containsKey(TEST_FOLDER_7_ID))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_DISPENSER))
        assertTrue(secondPage.services.containsKey(TEST_SERVICE_ID_ZERO_QUOTAS))
    }

    @Test
    fun testGetLoansHistoryEmptyResult() = runBlocking {
        val result = helper.getLoansHistory(loanId = UUID.randomUUID().toString())
        assertTrue(result.events.isEmpty())
    }
}
