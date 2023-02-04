package ru.yandex.intranet.d.dao.loans

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.loans.ServiceLoanBalanceKey
import ru.yandex.intranet.d.model.loans.ServiceLoanBalanceModel
import java.math.BigInteger
import java.util.*

/**
 * Service loans balance DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ServiceLoansBalanceTest(@Autowired private val dao: ServiceLoansBalanceDao,
                              @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = ServiceLoanBalanceModel(
            key = ServiceLoanBalanceKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42L,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            amountIn = BigInteger.valueOf(25L),
            amountOut = BigInteger.valueOf(75L)
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
        val modelOne = ServiceLoanBalanceModel(
            key = ServiceLoanBalanceKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42L,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            amountIn = BigInteger.valueOf(25L),
            amountOut = BigInteger.valueOf(75L)
        )
        val modelTwo = ServiceLoanBalanceModel(
            key = ServiceLoanBalanceKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 69L,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            amountIn = BigInteger.valueOf(25L),
            amountOut = BigInteger.valueOf(75L)
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
    fun getByServiceFirstPage(): Unit = runBlocking {
        val modelOne = ServiceLoanBalanceModel(
            key = ServiceLoanBalanceKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42L,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            amountIn = BigInteger.valueOf(25L),
            amountOut = BigInteger.valueOf(75L)
        )
        val modelTwo = ServiceLoanBalanceModel(
            key = ServiceLoanBalanceKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42L,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            amountIn = BigInteger.valueOf(25L),
            amountOut = BigInteger.valueOf(75L)
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByServiceFirstPage(txSession, Tenants.DEFAULT_TENANT_ID, 42L, 2)
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun getByServiceNextPage(): Unit = runBlocking {
        val modelOne = ServiceLoanBalanceModel(
            key = ServiceLoanBalanceKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42L,
                providerId = "6e747953-f664-4e3b-b41f-53c7628c446c",
                resourceId = "d2410c13-13f0-4018-835b-c6a9ab0aedc4"
            ),
            amountIn = BigInteger.valueOf(25L),
            amountOut = BigInteger.valueOf(75L)
        )
        val modelTwo = ServiceLoanBalanceModel(
            key = ServiceLoanBalanceKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                serviceId = 42L,
                providerId = "6e747953-f664-4e3b-b41f-53c7628c446d",
                resourceId = "d2410c13-13f0-4018-835b-c6a9ab0aedc5"
            ),
            amountIn = BigInteger.valueOf(25L),
            amountOut = BigInteger.valueOf(75L)
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByServiceNextPage(txSession, Tenants.DEFAULT_TENANT_ID, 42L, modelOne.key.providerId,
                    modelOne.key.resourceId, 1)
            }
        }
        Assertions.assertEquals(setOf(modelTwo), result!!.toSet())
    }

    @Test
    fun getAllByService(): Unit = runBlocking {
        val models = mutableListOf<ServiceLoanBalanceModel>()
        repeat(2000) {
            val model = ServiceLoanBalanceModel(
                key = ServiceLoanBalanceKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    serviceId = 42L,
                    providerId = UUID.randomUUID().toString(),
                    resourceId = UUID.randomUUID().toString()
                ),
                amountIn = BigInteger.valueOf(25L),
                amountOut = BigInteger.valueOf(75L)
            )
            models.add(model)
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getAllByService(txSession, Tenants.DEFAULT_TENANT_ID, 42L)
            }
        }
        Assertions.assertEquals(models.toSet(), result!!.toSet())
    }

}
