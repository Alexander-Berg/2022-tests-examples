package ru.yandex.intranet.d.dao.usage

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.usage.UsageEpochKey
import ru.yandex.intranet.d.model.usage.UsageEpochModel
import java.util.*

/**
 * Usage epochs DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class UsageEpochsDaoTest(@Autowired private val dao: UsageEpochsDao,
                         @Autowired private val tableClient: YdbTableClient) {
    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = UsageEpochModel(
            key = UsageEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            epoch = 1
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
        val modelOne = UsageEpochModel(
            key = UsageEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            epoch = 1
        )
        val modelTwo = UsageEpochModel(
            key = UsageEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            epoch = 2
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
}
