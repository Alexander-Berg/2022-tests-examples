package ru.yandex.intranet.d.dao.loans

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.loans.LoanDueDate
import ru.yandex.intranet.d.model.loans.LoanNotifications
import ru.yandex.intranet.d.model.loans.PendingLoanKey
import ru.yandex.intranet.d.model.loans.PendingLoanModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Pending loans DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class PendingLoansDaoTest(@Autowired private val dao: PendingLoansDao,
                          @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = PendingLoanModel(
            key = PendingLoanKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS),
                loanId = UUID.randomUUID().toString()
            ),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            notifications = LoanNotifications(
                lastSent = Instant.now().truncatedTo(ChronoUnit.MICROS),
                sentTo = listOf(UUID.randomUUID().toString())
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
        val modelOne = PendingLoanModel(
            key = PendingLoanKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS),
                loanId = UUID.randomUUID().toString()
            ),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            notifications = LoanNotifications(
                lastSent = Instant.now().truncatedTo(ChronoUnit.MICROS),
                sentTo = listOf(UUID.randomUUID().toString())
            )
        )
        val modelTwo = PendingLoanModel(
            key = PendingLoanKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS),
                loanId = UUID.randomUUID().toString()
            ),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            notifications = LoanNotifications(
                lastSent = Instant.now().truncatedTo(ChronoUnit.MICROS),
                sentTo = listOf(UUID.randomUUID().toString())
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
    fun getOverdueFirstPage(): Unit = runBlocking {
        val modelOne = PendingLoanModel(
            key = PendingLoanKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS),
                loanId = UUID.randomUUID().toString()
            ),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            notifications = LoanNotifications(
                lastSent = Instant.now().truncatedTo(ChronoUnit.MICROS),
                sentTo = listOf(UUID.randomUUID().toString())
            )
        )
        val modelTwo = PendingLoanModel(
            key = PendingLoanKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS),
                loanId = UUID.randomUUID().toString()
            ),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            notifications = LoanNotifications(
                lastSent = Instant.now().truncatedTo(ChronoUnit.MICROS),
                sentTo = listOf(UUID.randomUUID().toString())
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getOverdueFirstPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    Instant.now().truncatedTo(ChronoUnit.MICROS).plus(1, ChronoUnit.DAYS), 2)
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun getOverdueNextPage(): Unit = runBlocking {
        val modelOne = PendingLoanModel(
            key = PendingLoanKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS),
                loanId = UUID.randomUUID().toString()
            ),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            notifications = LoanNotifications(
                lastSent = Instant.now().truncatedTo(ChronoUnit.MICROS),
                sentTo = listOf(UUID.randomUUID().toString())
            )
        )
        val modelTwo = PendingLoanModel(
            key = PendingLoanKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                dueAtTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS),
                loanId = UUID.randomUUID().toString()
            ),
            dueAt = LoanDueDate(LocalDate.now(), ZoneOffset.UTC),
            notifications = LoanNotifications(
                lastSent = Instant.now().truncatedTo(ChronoUnit.MICROS),
                sentTo = listOf(UUID.randomUUID().toString())
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getOverdueNextPage(txSession, Tenants.DEFAULT_TENANT_ID,
                    Instant.now().truncatedTo(ChronoUnit.MICROS).plus(1, ChronoUnit.DAYS),
                    modelTwo.key.loanId, 2)
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

}
