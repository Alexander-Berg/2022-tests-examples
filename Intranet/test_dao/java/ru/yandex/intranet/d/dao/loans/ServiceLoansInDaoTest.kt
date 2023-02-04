package ru.yandex.intranet.d.dao.loans

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.model.loans.ServiceLoanInModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Service loans incoming DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ServiceLoansInDaoTest(@Autowired private val dao: ServiceLoansInDao,
                            @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model)
            }
        }
        Assertions.assertEquals(model, result)
    }

    @Test
    fun testUpsertMany(): Unit = runBlocking {
        val modelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val modelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 69L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne, modelTwo))
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun testGetByServiceFirstPage(): Unit = runBlocking {
        val modelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val modelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByServiceStatusOrderByDueFirstPage(
                    txSession, Tenants.DEFAULT_TENANT_ID, serviceId = 42L, LoanStatus.PENDING, limit = 10
                )
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun testGetByServiceNextPage(): Unit = runBlocking {
        val modelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val modelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val resultFirstPage = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByServiceStatusOrderByDueFirstPage(
                    txSession, Tenants.DEFAULT_TENANT_ID, serviceId = 42L, LoanStatus.PENDING, limit = 1
                )
            }
        }
        Assertions.assertEquals(setOf(modelOne), resultFirstPage!!.toSet())
        val resultNextPage = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getByServiceStatusOrderByDueNextPage(
                    txSession, Tenants.DEFAULT_TENANT_ID, serviceId = 42L, LoanStatus.PENDING,
                    fromDueAt = modelOne.dueAt, fromLoanId = modelOne.loanId, limit = 1
                )
            }
        }
        Assertions.assertEquals(setOf(modelTwo), resultNextPage!!.toSet())
    }

    @Test
    fun testGetByServiceStatusFirstPage(): Unit = runBlocking {
        val modelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val modelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.SETTLED,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByServiceOrderByDueFirstPage(
                    txSession, Tenants.DEFAULT_TENANT_ID, serviceId = 42L, limit = 10
                )
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun testGetByServiceStatusNextPage(): Unit = runBlocking {
        val modelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val modelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.SETTLED,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val resultFirstPage = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByServiceStatusOrderByDueFirstPage(
                    txSession, Tenants.DEFAULT_TENANT_ID, serviceId = 42L, LoanStatus.PENDING, limit = 1
                )
            }
        }
        Assertions.assertEquals(setOf(modelOne), resultFirstPage!!.toSet())
        val resultNextPage = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.getByServiceOrderByDueNextPage(
                    txSession, Tenants.DEFAULT_TENANT_ID, serviceId = 42L, fromStatus = modelOne.status,
                    fromDueAt = modelOne.dueAt, fromLoanId = modelOne.loanId, limit = 1
                )
            }
        }
        Assertions.assertEquals(setOf(modelTwo), resultNextPage!!.toSet())
    }

    @Test
    fun testDeleteById(): Unit = runBlocking {
        val model = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model)
            }
        }
        Assertions.assertEquals(model, result)
        val deleteResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.deleteById(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model)
            }
        }
        Assertions.assertNull(deleteResult)
    }

    @Test
    fun testDeleteByIds(): Unit = runBlocking {
        val modelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val modelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 69L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne, modelTwo))
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
        val deleteResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.deleteByIds(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne, modelTwo))
            }
        }!!
        Assertions.assertTrue(deleteResult.isEmpty())
    }

    @Test
    fun testFilterServiceIdsByStatus(): Unit = runBlocking {
        val modelOne = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 42L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val modelTwo = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 69L,
            status = LoanStatus.PENDING,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val modelThree = ServiceLoanInModel(
            tenantId = Tenants.DEFAULT_TENANT_ID,
            serviceId = 123L,
            status = LoanStatus.SETTLED,
            dueAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            loanId = UUID.randomUUID().toString()
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo, modelThree))
            }
            val serviceIds = listOf(modelOne, modelTwo, modelThree).map { it.serviceId }
            rwTxRetryable {
                dao.filterServiceIdsByLoanStatus(txSession, Tenants.DEFAULT_TENANT_ID, serviceIds, LoanStatus.PENDING)
            }
        }
        val expectedResult = listOf(modelOne, modelTwo, modelThree)
            .filter { it.status == LoanStatus.PENDING }
            .map { it.serviceId }
            .toSet()
        Assertions.assertEquals(expectedResult, result)
    }
}
