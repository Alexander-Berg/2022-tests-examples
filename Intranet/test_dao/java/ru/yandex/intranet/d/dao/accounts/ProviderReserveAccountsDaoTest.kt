package ru.yandex.intranet.d.dao.accounts

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountKey
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountModel
import java.util.*

/**
 * Provider reserve accounts DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ProviderReserveAccountsDaoTest(
    @Autowired private val dao: ProviderReserveAccountsDao,
    @Autowired private val tableClient: YdbTableClient
) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString()
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
        val modelOne = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString()
            )
        )
        val modelTwo = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = null,
                accountId = UUID.randomUUID().toString()
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
    fun testDelete(): Unit = runBlocking {
        val model = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString()
            )
        )
        val resultExists = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model.key)
            }
        }
        Assertions.assertEquals(model, resultExists)
        val resultNotExists = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.deleteByIdRetryable(txSession, model.key)
            }
            rwTxRetryable {
                dao.getById(txSession, model.key)
            }
        }
        Assertions.assertNull(resultNotExists)
    }

    @Test
    fun testDeleteMany(): Unit = runBlocking {
        val modelOne = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString()
            )
        )
        val modelTwo = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = null,
                accountId = UUID.randomUUID().toString()
            )
        )
        val resultExists = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(modelOne, modelTwo))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne.key, modelTwo.key))
            }
        }
        Assertions.assertEquals(setOf(modelOne, modelTwo), resultExists!!.toSet())
        val resultNotExists = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.deleteByIdsRetryable(txSession, listOf(modelOne.key, modelTwo.key))
            }
            rwTxRetryable {
                dao.getByIds(txSession, listOf(modelOne.key, modelTwo.key))
            }
        }
        Assertions.assertTrue(resultNotExists!!.isEmpty())
    }

    @Test
    fun testExistsByProvider(): Unit = runBlocking {
        val model = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString()
            )
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.existsByProvider(txSession, model.key.tenantId, model.key.providerId)
            }
        }!!
        Assertions.assertTrue(result)
        val negativeResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.existsByProvider(txSession, model.key.tenantId, UUID.randomUUID().toString())
            }
        }!!
        Assertions.assertFalse(negativeResult)
    }

    @Test
    fun testExistsByProviderAccountSpace(): Unit = runBlocking {
        val model = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString()
            )
        )
        val modelNoAccountSpace = ProviderReserveAccountModel(
            key = ProviderReserveAccountKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                accountsSpaceId = null,
                accountId = UUID.randomUUID().toString()
            )
        )
        val resultOne = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, listOf(model, modelNoAccountSpace))
            }
            rwTxRetryable {
                dao.existsByProviderAccountSpace(txSession, model.key.tenantId, model.key.providerId,
                    model.key.accountsSpaceId)
            }
        }!!
        Assertions.assertTrue(resultOne)
        val resultTwo = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.existsByProviderAccountSpace(txSession, modelNoAccountSpace.key.tenantId,
                    modelNoAccountSpace.key.providerId, modelNoAccountSpace.key.accountsSpaceId)
            }
        }!!
        Assertions.assertTrue(resultTwo)
        val negativeResult = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.existsByProviderAccountSpace(txSession, model.key.tenantId, UUID.randomUUID().toString(),
                    UUID.randomUUID().toString())
            }
        }!!
        Assertions.assertFalse(negativeResult)
    }

    @Test
    fun testGetAllByTenant(): Unit = runBlocking {
        val models = mutableListOf<ProviderReserveAccountModel>()
        repeat(1050) {
            models.add(ProviderReserveAccountModel(
                key = ProviderReserveAccountKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    providerId = UUID.randomUUID().toString(),
                    accountsSpaceId = UUID.randomUUID().toString(),
                    accountId = UUID.randomUUID().toString()
                )
            ))
            models.add(ProviderReserveAccountModel(
                key = ProviderReserveAccountKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    providerId = UUID.randomUUID().toString(),
                    accountsSpaceId = null,
                    accountId = UUID.randomUUID().toString()
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID)
            }
        }
        models.sortWith(Comparator
            .comparing<ProviderReserveAccountModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.accountsSpaceId ?: "" }
            .thenComparing { v -> v.key.accountId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun testGetAllByProvider(): Unit = runBlocking {
        val models = mutableListOf<ProviderReserveAccountModel>()
        val providerId = UUID.randomUUID().toString()
        repeat(1050) {
            models.add(ProviderReserveAccountModel(
                key = ProviderReserveAccountKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    providerId = providerId,
                    accountsSpaceId = UUID.randomUUID().toString(),
                    accountId = UUID.randomUUID().toString()
                )
            ))
            models.add(ProviderReserveAccountModel(
                key = ProviderReserveAccountKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    providerId = providerId,
                    accountsSpaceId = null,
                    accountId = UUID.randomUUID().toString()
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getAllByProvider(txSession, Tenants.DEFAULT_TENANT_ID, providerId)
            }
        }
        models.sortWith(Comparator
            .comparing<ProviderReserveAccountModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.accountsSpaceId ?: "" }
            .thenComparing { v -> v.key.accountId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun testGetAllByProviderAccountsSpace(): Unit = runBlocking {
        val models = mutableListOf<ProviderReserveAccountModel>()
        val providerId = UUID.randomUUID().toString()
        val accountsSpaceId = UUID.randomUUID().toString()
        repeat(2100) {
            models.add(ProviderReserveAccountModel(
                key = ProviderReserveAccountKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    providerId = providerId,
                    accountsSpaceId = accountsSpaceId,
                    accountId = UUID.randomUUID().toString()
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getAllByProviderAccountsSpace(txSession, Tenants.DEFAULT_TENANT_ID, providerId, accountsSpaceId)
            }
        }
        models.sortWith(Comparator
            .comparing<ProviderReserveAccountModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.accountsSpaceId ?: "" }
            .thenComparing { v -> v.key.accountId })
        Assertions.assertEquals(models, result)
    }

    @Test
    fun testGetAllByProviderAccountsSpaceEmpty(): Unit = runBlocking {
        val models = mutableListOf<ProviderReserveAccountModel>()
        val providerId = UUID.randomUUID().toString()
        val accountsSpaceId = null
        repeat(2100) {
            models.add(ProviderReserveAccountModel(
                key = ProviderReserveAccountKey(
                    tenantId = Tenants.DEFAULT_TENANT_ID,
                    providerId = providerId,
                    accountsSpaceId = accountsSpaceId,
                    accountId = UUID.randomUUID().toString()
                )
            ))
        }
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertManyRetryable(txSession, models)
            }
            rwTxRetryable {
                dao.getAllByProviderAccountsSpace(txSession, Tenants.DEFAULT_TENANT_ID, providerId, accountsSpaceId)
            }
        }
        models.sortWith(Comparator
            .comparing<ProviderReserveAccountModel, String> { it.key.tenantId.id }
            .thenComparing { v -> v.key.providerId }
            .thenComparing { v -> v.key.accountsSpaceId ?: "" }
            .thenComparing { v -> v.key.accountId })
        Assertions.assertEquals(models, result)
    }

}
