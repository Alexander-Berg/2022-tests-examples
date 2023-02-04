package ru.yandex.intranet.d.web.api.loans

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.TestAccounts
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_6_ID
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_8_ID
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2_ID
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_7_ID
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestResources.YP_HDD_MAN
import ru.yandex.intranet.d.TestResources.YP_SSD_MAN
import ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER
import ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_ZERO_QUOTAS
import ru.yandex.intranet.d.TestUsers.USER_1_ID
import ru.yandex.intranet.d.TestUsers.USER_1_UID
import ru.yandex.intranet.d.TestUsers.USER_2_ID
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.loans.LoansDao
import ru.yandex.intranet.d.dao.loans.LoansHistoryDao
import ru.yandex.intranet.d.dao.loans.ServiceLoansInDao
import ru.yandex.intranet.d.dao.loans.ServiceLoansOutDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.kotlin.LoanId
import ru.yandex.intranet.d.kotlin.ServiceId
import ru.yandex.intranet.d.model.loans.LoanActionSubject
import ru.yandex.intranet.d.model.loans.LoanActionSubjects
import ru.yandex.intranet.d.model.loans.LoanAmount
import ru.yandex.intranet.d.model.loans.LoanAmounts
import ru.yandex.intranet.d.model.loans.LoanDueDate
import ru.yandex.intranet.d.model.loans.LoanEventType
import ru.yandex.intranet.d.model.loans.LoanModel
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.model.loans.LoanSubject
import ru.yandex.intranet.d.model.loans.LoanSubjectType
import ru.yandex.intranet.d.model.loans.LoanType
import ru.yandex.intranet.d.model.loans.LoansHistoryFields
import ru.yandex.intranet.d.model.loans.LoansHistoryKey
import ru.yandex.intranet.d.model.loans.LoansHistoryModel
import ru.yandex.intranet.d.model.loans.ServiceLoanInModel
import ru.yandex.intranet.d.model.loans.ServiceLoanOutModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.loans.api.ApiGetLoansHistoryResponseDto
import ru.yandex.intranet.d.web.model.loans.api.ApiSearchLoansRequestDto
import ru.yandex.intranet.d.web.model.loans.api.ApiSearchLoansResponseDto
import ru.yandex.intranet.d.web.model.loans.LoanDirection
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class ApiLoansTestsHelper(
    private val loansDao: LoansDao,
    private val loansInDao: ServiceLoansInDao,
    private val loansOutDao: ServiceLoansOutDao,
    private val tableClient: YdbTableClient,
    private val webClient: WebTestClient,
    private val loansHistoryDao: LoansHistoryDao
) {
    fun prepareLoansIn(): List<Pair<LoanModel, ServiceLoanInModel>> = runBlocking {
        val loanModelOne = getLoanModel()
        val loanModelTwo = getLoanModel(plusMillis = 1000L)
        val settledModel = getLoanModel(status = LoanStatus.SETTLED, plusMillis = 2000L)
        dbSessionRetryable(tableClient) {
            loansDao.upsertManyRetryable(rwSingleRetryableCommit(), listOf(loanModelOne, loanModelTwo, settledModel))
        }

        val serviceLoanInModelOne = getLoanInModel(loanModelOne)
        val serviceLoanInModelTwo = getLoanInModel(loanModelTwo)
        val serviceLoanInSettledModel = getLoanInModel(settledModel)
        dbSessionRetryable(tableClient) {
            loansInDao.upsertManyRetryable(
                rwSingleRetryableCommit(),
                listOf(serviceLoanInModelOne, serviceLoanInModelTwo, serviceLoanInSettledModel)
            )
        }

        listOf(
            loanModelOne to serviceLoanInModelOne,
            loanModelTwo to serviceLoanInModelTwo,
            settledModel to serviceLoanInSettledModel
        )
    }

    fun prepareLoansOut(): List<Pair<LoanModel, ServiceLoanOutModel>> = runBlocking {
        val loanModelOne = getLoanModel()
        val loanModelTwo = getLoanModel(plusMillis = 1000L)
        val settledModel = getLoanModel(status = LoanStatus.SETTLED, plusMillis = 2000L)
        dbSessionRetryable(tableClient) {
            loansDao.upsertManyRetryable(rwSingleRetryableCommit(), listOf(loanModelOne, loanModelTwo, settledModel))
        }

        val serviceLoanOutModelOne = getLoanOutModel(loanModelOne)
        val serviceLoanOutModelTwo = getLoanOutModel(loanModelTwo)
        val serviceLoanOutSettledModel = getLoanOutModel(settledModel)
        dbSessionRetryable(tableClient) {
            loansOutDao.upsertManyRetryable(
                rwSingleRetryableCommit(),
                listOf(serviceLoanOutModelOne, serviceLoanOutModelTwo, serviceLoanOutSettledModel)
            )
        }

        listOf(
            loanModelOne to serviceLoanOutModelOne,
            loanModelTwo to serviceLoanOutModelTwo,
            settledModel to serviceLoanOutSettledModel
        )
    }

    fun prepareLoansHistory(): List<LoansHistoryModel> = runBlocking {
        val loanId = UUID.randomUUID().toString()
        val loanHistoryOne = getLoansHistoryModel(loanId, plusMillis = 1000L)
        val loanHistoryTwo = getLoansHistoryModel(loanId)
        dbSessionRetryable(tableClient) {
            loansHistoryDao.upsertManyRetryable(rwSingleRetryableCommit(), listOf(loanHistoryOne, loanHistoryTwo))
        }
        listOf(loanHistoryOne, loanHistoryTwo)
    }

    suspend fun searchLoans(
        serviceId: ServiceId,
        status: LoanStatus? = LoanStatus.PENDING,
        direction: LoanDirection,
        from: String? = null,
        limit: Int? = null
    ): ApiSearchLoansResponseDto = webClient
        .mutateWith(MockUser.uid(USER_1_UID))
        .post()
        .uri("/api/v1/loans/_search")
        .bodyValue(ApiSearchLoansRequestDto(serviceId, status, direction, from, limit))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApiSearchLoansResponseDto::class.java)
        .responseBody
        .awaitSingle()!!

    suspend fun getLoansHistory(loanId: LoanId, limit: Int = 100): ApiGetLoansHistoryResponseDto = webClient
        .mutateWith(MockUser.uid(USER_1_UID))
        .get()
        .uri("/api/v1/loans/$loanId/_history?&limit=$limit")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApiGetLoansHistoryResponseDto::class.java)
        .responseBody
        .awaitSingle()!!

    suspend fun getLoansHistoryNextPage(
        loanId: LoanId,
        pageToken: String,
        limit: Int = 100
    ): ApiGetLoansHistoryResponseDto = webClient
        .mutateWith(MockUser.uid(USER_1_UID))
        .get()
        .uri("/api/v1/loans/$loanId/_history?&limit=$limit&pageToken=$pageToken")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApiGetLoansHistoryResponseDto::class.java)
        .responseBody
        .awaitSingle()!!

    private fun getLoanModel(
        status: LoanStatus = LoanStatus.PENDING,
        plusMillis: Long = 0L
    ): LoanModel = LoanModel(
        tenantId = Tenants.DEFAULT_TENANT_ID,
        id = UUID.randomUUID().toString(),
        status = status,
        type = LoanType.PROVIDER_RESERVE,
        createdAt = Instant.now().plusMillis(plusMillis).truncatedTo(ChronoUnit.MILLIS),
        dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
        settledAt = null,
        updatedAt = null,
        version = 0L,
        requestedBy = LoanActionSubject(USER_1_ID, null),
        requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(USER_2_ID, null))),
        borrowTransferRequestId = UUID.randomUUID().toString(),
        borrowedFrom = LoanSubject(
            type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
            service = TEST_SERVICE_ID_DISPENSER,
            reserveService = null,
            account = TEST_ACCOUNT_6_ID,
            folder = TEST_FOLDER_2_ID,
            provider = YP_ID,
            accountsSpace = TestAccounts.TEST_ACCOUNT_SPACE_3_ID
        ),
        borrowedTo = LoanSubject(
            type = LoanSubjectType.ACCOUNT,
            service = TEST_SERVICE_ID_ZERO_QUOTAS,
            reserveService = null,
            account = TEST_ACCOUNT_8_ID,
            folder = TEST_FOLDER_7_ID,
            provider = null,
            accountsSpace = TestAccounts.TEST_ACCOUNT_SPACE_3_ID
        ),
        payOffFrom = LoanSubject(
            type = LoanSubjectType.ACCOUNT,
            service = TEST_SERVICE_ID_ZERO_QUOTAS,
            reserveService = null,
            account = TEST_ACCOUNT_8_ID,
            folder = TEST_FOLDER_7_ID,
            provider = null,
            accountsSpace = TestAccounts.TEST_ACCOUNT_SPACE_3_ID
        ),
        payOffTo = LoanSubject(
            type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
            service = TEST_SERVICE_ID_DISPENSER,
            reserveService = null,
            account = TEST_ACCOUNT_6_ID,
            folder = TEST_FOLDER_2_ID,
            provider = YP_ID,
            accountsSpace = TestAccounts.TEST_ACCOUNT_SPACE_3_ID
        ),
        borrowedAmounts = LoanAmounts(listOf(
            LoanAmount(
                resource = YP_HDD_MAN,
                amount = BigInteger.valueOf(64L)
            )
        )),
        payOffAmounts = LoanAmounts(listOf(
            LoanAmount(
                resource = YP_HDD_MAN,
                amount = BigInteger.valueOf(64L)
            )
        )),
        dueAmounts = LoanAmounts(listOf(
            LoanAmount(
                resource = YP_HDD_MAN,
                amount = BigInteger.valueOf(64L)
            )
        )),
        dueAtTimestamp = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
    )

    private fun getLoanInModel(
        loanModel: LoanModel
    ): ServiceLoanInModel = ServiceLoanInModel(
        tenantId = Tenants.DEFAULT_TENANT_ID,
        serviceId = TEST_SERVICE_ID_DISPENSER,
        status = loanModel.status,
        dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
        loanId = loanModel.id
    )

    private fun getLoanOutModel(
        loanModel: LoanModel
    ): ServiceLoanOutModel = ServiceLoanOutModel(
        tenantId = Tenants.DEFAULT_TENANT_ID,
        serviceId = TEST_SERVICE_ID_DISPENSER,
        status = loanModel.status,
        dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
        loanId = loanModel.id
    )

    private fun getLoansHistoryModel(loanId: LoanId, plusMillis: Long = 0L) = LoansHistoryModel(
        key = LoansHistoryKey(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            loanId = loanId,
            eventTimestamp = Instant.now().plusMillis(plusMillis).truncatedTo(ChronoUnit.MICROS)
        ),
        eventAuthor = LoanActionSubject(USER_1_ID, null),
        eventApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(USER_2_ID, null))),
        eventType = LoanEventType.LOAN_CREATED,
        transferRequestId = UUID.randomUUID().toString(),
        oldFields = LoansHistoryFields(
            version = 0L,
            status = LoanStatus.PENDING,
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = TEST_SERVICE_ID_DISPENSER,
                reserveService = null,
                account = TEST_ACCOUNT_6_ID,
                folder = TEST_FOLDER_2_ID,
                provider = YP_ID,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = TEST_SERVICE_ID_ZERO_QUOTAS,
                reserveService = null,
                account = TEST_ACCOUNT_8_ID,
                folder = TEST_FOLDER_7_ID,
                provider = YP_ID,
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = YP_HDD_MAN,
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = YP_HDD_MAN,
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = YP_HDD_MAN,
                    amount = BigInteger.valueOf(64L)
                )
            ))
        ),
        newFields = LoansHistoryFields(
            version = 0L,
            status = LoanStatus.PENDING,
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = TEST_SERVICE_ID_DISPENSER,
                reserveService = null,
                account = TEST_ACCOUNT_6_ID,
                folder = TEST_FOLDER_2_ID,
                provider = YP_ID,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = TEST_SERVICE_ID_ZERO_QUOTAS,
                reserveService = null,
                account = TEST_ACCOUNT_8_ID,
                folder = TEST_FOLDER_7_ID,
                provider = YP_ID,
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = YP_SSD_MAN,
                    amount = BigInteger.valueOf(65L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = YP_SSD_MAN,
                    amount = BigInteger.valueOf(65L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = YP_SSD_MAN,
                    amount = BigInteger.valueOf(65L)
                )
            ))
        )
    )
}
