package ru.yandex.intranet.d.grpc.loans

import com.google.protobuf.util.Timestamps
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component
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
import ru.yandex.intranet.d.backend.service.proto.LoansServiceGrpc
import ru.yandex.intranet.d.backend.service.proto.SearchLoansResponse
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.loans.LoansDao
import ru.yandex.intranet.d.dao.loans.LoansHistoryDao
import ru.yandex.intranet.d.dao.loans.ServiceLoansInDao
import ru.yandex.intranet.d.dao.loans.ServiceLoansOutDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.MockGrpcUser
import ru.yandex.intranet.d.grpc.services.GetLoansHistoryRequestProto
import ru.yandex.intranet.d.grpc.services.GetLoansHistoryResponseProto
import ru.yandex.intranet.d.grpc.services.LoanActionSubjectProto
import ru.yandex.intranet.d.grpc.services.LoanActionSubjectsProto
import ru.yandex.intranet.d.grpc.services.LoanAmountProto
import ru.yandex.intranet.d.grpc.services.LoanAmountsProto
import ru.yandex.intranet.d.grpc.services.LoanDirectionProto
import ru.yandex.intranet.d.grpc.services.LoanDueDateProto
import ru.yandex.intranet.d.grpc.services.LoanEventTypeProto
import ru.yandex.intranet.d.grpc.services.LoanProto
import ru.yandex.intranet.d.grpc.services.LoanStatusProto
import ru.yandex.intranet.d.grpc.services.LoanSubjectProto
import ru.yandex.intranet.d.grpc.services.LoanSubjectTypeProto
import ru.yandex.intranet.d.grpc.services.LoanTypeProto
import ru.yandex.intranet.d.grpc.services.LoansHistoryFieldsProto
import ru.yandex.intranet.d.grpc.services.LoansHistoryProto
import ru.yandex.intranet.d.grpc.services.SearchLoansRequestProto
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
import ru.yandex.intranet.d.web.model.loans.LoanActionSubjectDto
import ru.yandex.intranet.d.web.model.loans.LoanActionSubjectsDto
import ru.yandex.intranet.d.web.model.loans.LoanAmountDto
import ru.yandex.intranet.d.web.model.loans.LoanAmountsDto
import ru.yandex.intranet.d.web.model.loans.LoanDirection
import ru.yandex.intranet.d.web.model.loans.LoanDueDateDto
import ru.yandex.intranet.d.web.model.loans.LoanSubjectDto
import ru.yandex.intranet.d.web.model.loans.LoansHistoryFieldsDto
import ru.yandex.intranet.d.web.model.loans.api.ApiLoanDto
import ru.yandex.intranet.d.web.model.loans.api.ApiLoansHistoryDto
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class GrpcLoansTestsHelper(
    private val loansDao: LoansDao,
    private val loansInDao: ServiceLoansInDao,
    private val loansOutDao: ServiceLoansOutDao,
    private val tableClient: YdbTableClient,
    private val loansHistoryDao: LoansHistoryDao
) {
    @GrpcClient("inProcess")
    private lateinit var loansServiceImpl: LoansServiceGrpc.LoansServiceBlockingStub

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

    fun searchLoans(
        serviceId: ServiceId,
        direction: LoanDirection,
        status: LoanStatus? = null,
        from: String? = null,
        limit: Int? = null
    ): SearchLoansResponse {
        val requestBuilder = SearchLoansRequestProto.newBuilder()
        requestBuilder.serviceId = serviceId
        requestBuilder.loanDirection = direction.toProto()
        if (status != null) {
            requestBuilder.loanStatus = status.toProto()
        }
        if (from != null) {
            requestBuilder.from = from
        }
        if (limit != null) {
            requestBuilder.limit = limit
        }

        return loansServiceImpl
            .withCallCredentials(MockGrpcUser.uid(USER_1_UID))
            .searchLoans(requestBuilder.build())
    }

    private fun LoanStatus.toProto(): LoanStatusProto =
        when (this) {
            LoanStatus.PENDING -> LoanStatusProto.PENDING_LOAN
            LoanStatus.SETTLED -> LoanStatusProto.SETTLED_LOAN
        }

    private fun LoanDirection.toProto(): LoanDirectionProto =
        when(this) {
            LoanDirection.IN -> LoanDirectionProto.IN
            LoanDirection.OUT -> LoanDirectionProto.OUT
        }

    fun getLoansHistory(loanId: LoanId, limit: Int? = null, from: String? = null): GetLoansHistoryResponseProto {
        val requestBuilder = GetLoansHistoryRequestProto.newBuilder()
        requestBuilder.loanId = loanId
        if (limit != null) {
            requestBuilder.limit = limit
        }
        if (from != null) {
            requestBuilder.from = from
        }
        return loansServiceImpl
            .withCallCredentials(MockGrpcUser.uid(USER_1_UID))
            .getLoansHistory(requestBuilder.build())
    }

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

private fun LoanStatus.toProto(): LoanStatusProto =
    when (this) {
        LoanStatus.PENDING -> LoanStatusProto.PENDING_LOAN
        LoanStatus.SETTLED -> LoanStatusProto.SETTLED_LOAN
    }

private fun LoanDueDateDto.toProto(): LoanDueDateProto {
    val builder = LoanDueDateProto.newBuilder()
    val localDateBuilder = ru.yandex.intranet.d.backend.service.proto.LocalDate.newBuilder()
    localDateBuilder.day = this.localDate.dayOfMonth
    localDateBuilder.month = this.localDate.month.ordinal
    localDateBuilder.year = this.localDate.year
    builder.localDate = localDateBuilder.build()
    builder.zoneId = this.timeZone.id
    return builder.build()
}

private fun LoanType.toProto(): LoanTypeProto = when (this) {
    LoanType.PROVIDER_RESERVE -> LoanTypeProto.PROVIDER_RESERVE_TYPE
    LoanType.PROVIDER_RESERVE_OVER_COMMIT -> LoanTypeProto.PROVIDER_RESERVE_OVER_COMMIT
    LoanType.SERVICE_RESERVE -> LoanTypeProto.SERVICE_RESERVE
    LoanType.PEER_TO_PEER -> LoanTypeProto.PEER_TO_PEER
}

private fun LoanActionSubjectDto.toProto(): LoanActionSubjectProto {
    val builder = LoanActionSubjectProto.newBuilder()
    if (this.user != null) {
        builder.user = this.user
    }
    if (this.provider != null) {
        builder.provider = this.provider
    }
    return builder.build()
}

private fun LoanActionSubjectsDto.toProto(): LoanActionSubjectsProto {
    val builder = LoanActionSubjectsProto.newBuilder()
    this.subjects.forEach { builder.addSubjects(it.toProto()) }
    return builder.build()
}

private fun LoanSubjectType.toProto(): LoanSubjectTypeProto = when (this) {
    LoanSubjectType.PROVIDER_RESERVE_ACCOUNT -> LoanSubjectTypeProto.PROVIDER_RESERVE_ACCOUNT
    LoanSubjectType.PROVIDER_RESERVE_OVER_COMMIT -> LoanSubjectTypeProto.PROVIDER_RESERVE_OVER_COMMIT_TYPE
    LoanSubjectType.SERVICE_RESERVE_ACCOUNT -> LoanSubjectTypeProto.SERVICE_RESERVE_ACCOUNT
    LoanSubjectType.ACCOUNT -> LoanSubjectTypeProto.ACCOUNT
    LoanSubjectType.SERVICE -> LoanSubjectTypeProto.SERVICE
}

private fun LoanSubjectDto.toProto(): LoanSubjectProto {
    val builder = LoanSubjectProto.newBuilder()
    builder.type = this.type.toProto()
    builder.service = this.service
    if (this.reserveService != null) {
        builder.reserveService = this.reserveService!!
    }
    if (this.account != null) {
        builder.account = this.account
    }
    if (this.folder != null) {
        builder.folder = this.folder
    }
    if (this.provider != null) {
        builder.provider = this.provider
    }
    return builder.build()
}

private fun LoanAmountsDto.toProto(): LoanAmountsProto {
    val builder = LoanAmountsProto.newBuilder()
    this.amounts.forEach { builder.addAmounts(it.toProto()) }
    return builder.build()
}

private fun LoanAmountDto.toProto(): LoanAmountProto {
    val builder = LoanAmountProto.newBuilder()
    builder.resource = this.resource
    builder.amount = this.amount.toString()
    return builder.build()
}

fun ApiLoanDto.toProto(): LoanProto {
    val builder = LoanProto.newBuilder()
    builder.id = this.id
    builder.status = this.status.toProto()
    builder.type = this.type.toProto()
    builder.createdAt = Timestamps.fromMillis(this.createdAt.toEpochMilli())
    builder.dueAt = this.dueAt.toProto()
    if (this.settledAt != null) {
        builder.settledAt = Timestamps.fromMillis(this.settledAt!!.toEpochMilli())
    }
    if (this.updatedAt != null) {
        builder.updatedAt = Timestamps.fromMillis(this.updatedAt!!.toEpochMilli())
    }
    builder.version = this.version
    builder.requestedBy = this.requestedBy.toProto()
    builder.requestApprovedBy = this.requestApprovedBy.toProto()
    builder.borrowTransferRequestId = this.borrowTransferRequestId
    builder.borrowedFrom = this.borrowedFrom.toProto()
    builder.borrowedTo = this.borrowedTo.toProto()
    builder.payOffFrom = this.payOffFrom.toProto()
    builder.borrowedTo = this.borrowedTo.toProto()
    builder.borrowedAmounts = this.borrowedAmounts.toProto()
    builder.payOffAmounts = this.payOffAmounts.toProto()
    builder.dueAmounts = this.dueAmounts.toProto()
    return builder.build()
}

fun ApiLoansHistoryDto.toProto(): LoansHistoryProto {
    val builder = LoansHistoryProto.newBuilder()
    builder.historyId = historyId
    builder.loanId = loanId
    builder.eventTimestamp = Timestamps.fromMillis(eventTimestamp.toEpochMilli())
    builder.eventAuthor = eventAuthor.toProto()
    if (eventApprovedBy != null) {
        builder.eventApprovedBy = eventApprovedBy!!.toProto()
    }
    builder.eventType = eventType.toProto()
    builder.transferRequestId = transferRequestId
    if (oldFields != null) {
        builder.oldFields = oldFields!!.toProto()
    }
    if (newFields != null) {
        builder.newFields = newFields!!.toProto()
    }
    return builder.build()
}

private fun LoanEventType.toProto(): LoanEventTypeProto = when (this) {
    LoanEventType.LOAN_CREATED -> LoanEventTypeProto.LOAN_CREATED
    LoanEventType.LOAN_UPDATED -> LoanEventTypeProto.LOAN_UPDATED
    LoanEventType.LOAN_PAY_OFF -> LoanEventTypeProto.LOAN_PAY_OFF
    LoanEventType.LOAN_SETTLED -> LoanEventTypeProto.LOAN_SETTLED
}

private fun LoansHistoryFieldsDto.toProto(): LoansHistoryFieldsProto {
    val builder = LoansHistoryFieldsProto.newBuilder()
    builder.version = version
    if (status != null) {
        builder.status = status!!.toProto()
    }
    if (dueAt != null) {
        builder.dueAt = dueAt!!.toProto()
    }
    if (payOffFrom != null) {
        builder.payOffFrom = payOffFrom!!.toProto()
    }
    if (payOffTo != null) {
        builder.payOffTo = payOffTo!!.toProto()
    }
    if (borrowedAmounts != null) {
        builder.borrowedAmounts = borrowedAmounts!!.toProto()
    }
    if (payOffAmounts != null) {
        builder.payOffAmounts = payOffAmounts!!.toProto()
    }
    if (dueAmounts != null) {
        builder.dueAmounts = dueAmounts!!.toProto()
    }
    return builder.build()
}
