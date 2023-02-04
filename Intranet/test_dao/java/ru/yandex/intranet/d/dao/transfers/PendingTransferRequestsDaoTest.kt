package ru.yandex.intranet.d.dao.transfers

import com.yandex.ydb.table.transaction.TransactionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.model.WithTenant
import ru.yandex.intranet.d.model.transfers.PendingTransferRequestsModel
import java.util.*

/**
 * PendingTransferRequestsDaoTest.
 *
 * @author Petr Surkov <petrsurkov></petrsurkov>@yandex-team.ru>
 */
@IntegrationTest
class PendingTransferRequestsDaoTest {
    @Autowired
    private lateinit var dao: PendingTransferRequestsDao

    @Autowired
    private lateinit var ydbTableClient: YdbTableClient

    @Test
    fun upsertAndGetById() {
        val model = PendingTransferRequestsModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .requestId(UUID.randomUUID().toString())
            .build()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.upsertOneRetryable(txSession, model) }
        }.block()
        val result = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.getById(txSession, model.requestId, model.tenantId) }
        }.block()
        Assertions.assertEquals(Optional.of(model), result)
    }

    @Test
    fun upsertManyAndGetByIds() {
        val modelOne = PendingTransferRequestsModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .requestId(UUID.randomUUID().toString())
            .build()
        val modelTwo = PendingTransferRequestsModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .requestId(UUID.randomUUID().toString())
            .build()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.upsertAllRetryable(txSession, listOf(modelOne, modelTwo)) }
        }.block()
        val result = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession ->
                dao.getByIds(
                    txSession, listOf(modelOne.requestId, modelTwo.requestId),
                    Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(result)
        Assertions.assertEquals(setOf(modelOne, modelTwo), result!!.toSet())
    }

    @Test
    fun deleteOne() {
        val model = PendingTransferRequestsModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .requestId(UUID.randomUUID().toString())
            .build()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.upsertOneRetryable(txSession, model) }
        }.block()
        val upsertResult = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.getById(txSession, model.requestId, model.tenantId) }
        }.block()
        Assertions.assertEquals(Optional.of(model), upsertResult)
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession ->
                dao.deleteOneRetryable(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID,
                        model.requestId
                    )
                )
            }
        }.block()
        val deleteResult = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.getById(txSession, model.requestId, model.tenantId) }
        }.block()
        Assertions.assertEquals(Optional.empty<Any>(), deleteResult)
    }

    @Test
    fun deleteMany() {
        val modelOne = PendingTransferRequestsModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .requestId(UUID.randomUUID().toString())
            .build()
        val modelTwo = PendingTransferRequestsModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .requestId(UUID.randomUUID().toString())
            .build()
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.upsertAllRetryable(txSession, listOf(modelOne, modelTwo)) }
        }.block()
        val upsertResult = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession ->
                dao.getByIds(
                    txSession, listOf(modelOne.requestId, modelTwo.requestId),
                    Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(upsertResult)
        Assertions.assertEquals(setOf(modelOne, modelTwo), upsertResult!!.toSet())
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession ->
                dao.deleteManyRetryable(
                    txSession, listOf(
                        WithTenant(
                            Tenants.DEFAULT_TENANT_ID,
                            modelOne.requestId
                        ), WithTenant(
                            Tenants.DEFAULT_TENANT_ID,
                            modelTwo.requestId
                        )
                    )
                )
            }
        }.block()
        val deleteResult = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession ->
                dao.getByIds(
                    txSession, listOf(modelOne.requestId, modelTwo.requestId),
                    Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(deleteResult)
        Assertions.assertTrue(deleteResult!!.isEmpty())
    }

    @Test
    fun upsertManyAndGetAllByTenantId() {
        val models = List(2500) {
            PendingTransferRequestsModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .requestId(UUID.randomUUID().toString())
                .build()
        }
        ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.upsertAllRetryable(txSession, models) }
        }.block()
        val result = ydbTableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession -> dao.getAllByTenant(txSession, Tenants.DEFAULT_TENANT_ID) }
        }.block()
        Assertions.assertNotNull(result)
        Assertions.assertEquals(models.toSet(), result!!.toSet())
    }
}
