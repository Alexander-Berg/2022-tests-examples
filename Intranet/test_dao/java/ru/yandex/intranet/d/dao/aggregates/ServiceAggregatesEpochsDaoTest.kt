package ru.yandex.intranet.d.dao.aggregates

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.aggregates.AggregateEpochKey
import ru.yandex.intranet.d.model.aggregates.AggregateEpochModel
import java.util.*

/**
 * Service aggregates epochs DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ServiceAggregatesEpochsDaoTest(@Autowired private val dao: ServiceAggregatesEpochsDao,
                                     @Autowired private val tableClient: YdbTableClient
) {
    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = AggregateEpochModel(
            key = AggregateEpochKey(
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
        val modelOne = AggregateEpochModel(
            key = AggregateEpochKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                resourceId = UUID.randomUUID().toString()
            ),
            epoch = 1
        )
        val modelTwo = AggregateEpochModel(
            key = AggregateEpochKey(
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
