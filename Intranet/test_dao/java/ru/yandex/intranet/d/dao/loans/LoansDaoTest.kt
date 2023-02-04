package ru.yandex.intranet.d.dao.loans

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.loans.LoanActionSubject
import ru.yandex.intranet.d.model.loans.LoanActionSubjects
import ru.yandex.intranet.d.model.loans.LoanAmount
import ru.yandex.intranet.d.model.loans.LoanAmounts
import ru.yandex.intranet.d.model.loans.LoanDueDate
import ru.yandex.intranet.d.model.loans.LoanModel
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.model.loans.LoanSubject
import ru.yandex.intranet.d.model.loans.LoanSubjectType
import ru.yandex.intranet.d.model.loans.LoanType
import ru.yandex.intranet.d.model.loans.LoansPageEntry
import ru.yandex.intranet.d.model.loans.ServiceLoanInModel
import ru.yandex.intranet.d.model.loans.ServiceLoanOutModel
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Loans DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class LoansDaoTest(@Autowired private val dao: LoansDao,
                   @Autowired private val inDao: ServiceLoansInDao,
                   @Autowired private val outDao: ServiceLoansOutDao,
                   @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model.id, model.tenantId)
            }
        }
        Assertions.assertEquals(model, result)
    }

    @Test
    fun testUpsertMany(): Unit = runBlocking {
        val modelOne = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val modelTwo = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne.id, modelTwo.id), Tenants.DEFAULT_TENANT_ID)
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun getByServiceStatusInFirstPage(): Unit = runBlocking {
        val modelOne = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val modelTwo = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val indexModelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = modelOne.dueAtTimestamp,
            loanId = modelOne.id
        )
        val indexModelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = modelTwo.dueAtTimestamp,
            loanId = modelTwo.id
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                inDao.upsertManyRetryable(txSession, listOf(indexModelOne, indexModelTwo))
            }
            rwTxRetryable {
                dao.getByServiceStatusInFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, 42L, LoanStatus.PENDING, 2)
            }
        }
        Assertions.assertEquals(setOf(LoansPageEntry(modelOne, indexModelOne.serviceId, indexModelOne.dueAt),
            LoansPageEntry(modelTwo, indexModelTwo.serviceId, indexModelTwo.dueAt)), result!!.toSet())
    }

    @Test
    fun getByServiceStatusOutFirstPage(): Unit = runBlocking {
        val modelOne = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val modelTwo = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val indexModelOne = ServiceLoanOutModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = modelOne.dueAtTimestamp,
            loanId = modelOne.id
        )
        val indexModelTwo = ServiceLoanOutModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = modelTwo.dueAtTimestamp,
            loanId = modelTwo.id
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                outDao.upsertManyRetryable(txSession, listOf(indexModelOne, indexModelTwo))
            }
            rwTxRetryable {
                dao.getByServiceStatusOutFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, 42L, LoanStatus.PENDING, 2)
            }
        }
        Assertions.assertEquals(setOf(LoansPageEntry(modelOne, indexModelOne.serviceId, indexModelOne.dueAt),
            LoansPageEntry(modelTwo, indexModelTwo.serviceId, indexModelTwo.dueAt)), result!!.toSet())
    }

    @Test
    fun getByServiceStatusInNextPage(): Unit = runBlocking {
        val modelOne = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val modelTwo = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val indexModelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = modelOne.dueAtTimestamp,
            loanId = modelOne.id
        )
        val indexModelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = modelTwo.dueAtTimestamp,
            loanId = modelTwo.id
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                inDao.upsertManyRetryable(txSession, listOf(indexModelOne, indexModelTwo))
            }
            rwTxRetryable {
                dao.getByServiceStatusInNextPage(txSession, Tenants.DEFAULT_TENANT_ID, 42L, LoanStatus.PENDING,
                    indexModelOne.dueAt, indexModelOne.loanId, 1)
            }
        }
        Assertions.assertEquals(setOf(LoansPageEntry(modelTwo, indexModelTwo.serviceId, indexModelTwo.dueAt)),
            result!!.toSet())
    }

    @Test
    fun getByServiceStatusOutNextPage(): Unit = runBlocking {
        val modelOne = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val modelTwo = LoanModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            id = UUID.randomUUID().toString(),
            status = LoanStatus.PENDING,
            type = LoanType.PROVIDER_RESERVE,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            settledAt = null,
            updatedAt = null,
            version = 0L,
            requestedBy = LoanActionSubject(UUID.randomUUID().toString(), null),
            requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            borrowTransferRequestId = UUID.randomUUID().toString(),
            borrowedFrom = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedTo = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffFrom = LoanSubject(
                type = LoanSubjectType.ACCOUNT,
                service = 69L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = null,
                accountsSpace = null
            ),
            payOffTo = LoanSubject(
                type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                service = 42L,
                reserveService = null,
                account = UUID.randomUUID().toString(),
                folder = UUID.randomUUID().toString(),
                provider = UUID.randomUUID().toString(),
                accountsSpace = null
            ),
            borrowedAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            payOffAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAmounts = LoanAmounts(listOf(
                LoanAmount(
                    resource = UUID.randomUUID().toString(),
                    amount = BigInteger.valueOf(64L)
                )
            )),
            dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
        )
        val indexModelOne = ServiceLoanOutModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = modelOne.dueAtTimestamp,
            loanId = modelOne.id
        )
        val indexModelTwo = ServiceLoanOutModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = modelTwo.dueAtTimestamp,
            loanId = modelTwo.id
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                outDao.upsertManyRetryable(txSession, listOf(indexModelOne, indexModelTwo))
            }
            rwTxRetryable {
                dao.getByServiceStatusOutNextPage(txSession, Tenants.DEFAULT_TENANT_ID, 42L, LoanStatus.PENDING,
                    indexModelOne.dueAt, indexModelOne.loanId, 1)
            }
        }
        Assertions.assertEquals(setOf(LoansPageEntry(modelTwo, indexModelTwo.serviceId, indexModelTwo.dueAt)),
            result!!.toSet())
    }

}
