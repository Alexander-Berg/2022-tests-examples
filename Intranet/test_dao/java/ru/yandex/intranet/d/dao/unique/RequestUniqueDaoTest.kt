package ru.yandex.intranet.d.dao.unique

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.uniques.RequestUniqueEndpoint
import ru.yandex.intranet.d.model.uniques.RequestUniqueMetadata
import ru.yandex.intranet.d.model.uniques.RequestUniqueModel
import ru.yandex.intranet.d.model.uniques.RequestUniqueSubject
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Request unique DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class RequestUniqueDaoTest(@Autowired private val dao: RequestUniqueDao,
                           @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val model = RequestUniqueModel(tenantId = Tenants.DEFAULT_TENANT_ID,
            unique = UUID.randomUUID().toString(),
            subject = RequestUniqueSubject.user(UUID.randomUUID().toString()),
            endpoint = RequestUniqueEndpoint.UPDATE_PROVISIONS,
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
            metadata = RequestUniqueMetadata(UUID.randomUUID().toString(), null)
        )
        val result = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertRetryable(txSession, model)
            }
            rwTxRetryable {
                dao.getById(txSession, model.identity())
            }
        }
        assertEquals(model, result)
    }

}
