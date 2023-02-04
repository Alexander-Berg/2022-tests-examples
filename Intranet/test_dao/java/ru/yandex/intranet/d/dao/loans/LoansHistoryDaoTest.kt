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
import ru.yandex.intranet.d.model.loans.LoanEventType
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.model.loans.LoanSubject
import ru.yandex.intranet.d.model.loans.LoanSubjectType
import ru.yandex.intranet.d.model.loans.LoansHistoryFields
import ru.yandex.intranet.d.model.loans.LoansHistoryKey
import ru.yandex.intranet.d.model.loans.LoansHistoryModel
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
class LoansHistoryDaoTest(@Autowired private val dao: LoansHistoryDao,
                          @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = LoansHistoryModel(
            key = LoansHistoryKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                id = UUID.randomUUID().toString(),
                loanId = UUID.randomUUID().toString(),
                eventTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
            ),
            eventAuthor = LoanActionSubject(UUID.randomUUID().toString(), null),
            eventApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            eventType = LoanEventType.LOAN_CREATED,
            transferRequestId = UUID.randomUUID().toString(),
            oldFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            ),
            newFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model.key)
            }
        }
        Assertions.assertEquals(model, result)
    }

    @Test
    fun testUpsertMany(): Unit = runBlocking {
        val modelOne = LoansHistoryModel(
            key = LoansHistoryKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                id = UUID.randomUUID().toString(),
                loanId = UUID.randomUUID().toString(),
                eventTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
            ),
            eventAuthor = LoanActionSubject(UUID.randomUUID().toString(), null),
            eventApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            eventType = LoanEventType.LOAN_CREATED,
            transferRequestId = UUID.randomUUID().toString(),
            oldFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            ),
            newFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            )
        )
        val modelTwo = LoansHistoryModel(
            key = LoansHistoryKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                id = UUID.randomUUID().toString(),
                loanId = UUID.randomUUID().toString(),
                eventTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
            ),
            eventAuthor = LoanActionSubject(UUID.randomUUID().toString(), null),
            eventApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            eventType = LoanEventType.LOAN_CREATED,
            transferRequestId = UUID.randomUUID().toString(),
            oldFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            ),
            newFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne.key, modelTwo.key))
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun getByLoanFirstPage(): Unit = runBlocking {
        val loanId = UUID.randomUUID().toString()
        val modelOne = LoansHistoryModel(
            key = LoansHistoryKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                id = UUID.randomUUID().toString(),
                loanId = loanId,
                eventTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
            ),
            eventAuthor = LoanActionSubject(UUID.randomUUID().toString(), null),
            eventApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            eventType = LoanEventType.LOAN_CREATED,
            transferRequestId = UUID.randomUUID().toString(),
            oldFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            ),
            newFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            )
        )
        val modelTwo = LoansHistoryModel(
            key = LoansHistoryKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                id = UUID.randomUUID().toString(),
                loanId = loanId,
                eventTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
            ),
            eventAuthor = LoanActionSubject(UUID.randomUUID().toString(), null),
            eventApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            eventType = LoanEventType.LOAN_CREATED,
            transferRequestId = UUID.randomUUID().toString(),
            oldFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            ),
            newFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByLoanFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, loanId, 2)
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun getByLoanNextPage(): Unit = runBlocking {
        val loanId = UUID.randomUUID().toString()
        val modelOne = LoansHistoryModel(
            key = LoansHistoryKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                id = UUID.randomUUID().toString(),
                loanId = loanId,
                eventTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
            ),
            eventAuthor = LoanActionSubject(UUID.randomUUID().toString(), null),
            eventApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            eventType = LoanEventType.LOAN_CREATED,
            transferRequestId = UUID.randomUUID().toString(),
            oldFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            ),
            newFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            )
        )
        val modelTwo = LoansHistoryModel(
            key = LoansHistoryKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                id = UUID.randomUUID().toString(),
                loanId = loanId,
                eventTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
            ),
            eventAuthor = LoanActionSubject(UUID.randomUUID().toString(), null),
            eventApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(UUID.randomUUID().toString(), null))),
            eventType = LoanEventType.LOAN_CREATED,
            transferRequestId = UUID.randomUUID().toString(),
            oldFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            ),
            newFields = LoansHistoryFields(
                version = 0L,
                status = LoanStatus.PENDING,
                dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
                payOffFrom = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 42L,
                    reserveService = null,
                    account = UUID.randomUUID().toString(),
                    folder = UUID.randomUUID().toString(),
                    provider = UUID.randomUUID().toString(),
                    accountsSpace = null
                ),
                payOffTo = LoanSubject(
                    type = LoanSubjectType.PROVIDER_RESERVE_ACCOUNT,
                    service = 69L,
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
                ))
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByLoanNextPage(txSession, Tenants.DEFAULT_TENANT_ID, loanId, modelTwo.key.eventTimestamp, modelTwo.key.id, 1)
            }
        }
        Assertions.assertEquals(setOf(modelOne), result!!.toSet())
    }

}
