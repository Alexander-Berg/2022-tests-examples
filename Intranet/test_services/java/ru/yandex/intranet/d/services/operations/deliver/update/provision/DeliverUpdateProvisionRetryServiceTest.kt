package ru.yandex.intranet.d.services.operations.deliver.update.provision

import com.yandex.ydb.table.transaction.TransactionMode
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.backend.service.provider_proto.*
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.*
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.i18n.Locales
import ru.yandex.intranet.d.model.WithTenant
import ru.yandex.intranet.d.model.accounts.*
import ru.yandex.intranet.d.model.accounts.AccountModel.ExternalId
import ru.yandex.intranet.d.model.folders.*
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.services.operations.OperationsRetryService
import ru.yandex.intranet.d.utils.DummyModels.accountSpaceModel
import ru.yandex.intranet.d.web.model.SortOrderDto
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Deliver update provision operations retry service test.
 *
 * @author Ruslan Kadriev <aqru></aqru>@yandex-team.ru>
 */
@IntegrationTest
class DeliverUpdateProvisionRetryServiceTest(
    @Autowired private val providersDao: ProvidersDao,
    @Autowired private val accountsDao: AccountsDao,
    @Autowired private val folderOperationLogDao: FolderOperationLogDao,
    @Autowired private val folderDao: FolderDao,
    @Autowired private val accountsQuotasOperationsDao: AccountsQuotasOperationsDao,
    @Autowired private val operationsInProgressDao: OperationsInProgressDao,
    @Autowired private val resourceTypesDao: ResourceTypesDao,
    @Autowired private val resourceSegmentationsDao: ResourceSegmentationsDao,
    @Autowired private val resourceSegmentsDao: ResourceSegmentsDao,
    @Autowired private val resourcesDao: ResourcesDao,
    @Autowired private val quotasDao: QuotasDao,
    @Autowired private val accountsQuotasDao: AccountsQuotasDao,
    @Autowired private val tableClient: YdbTableClient,
    @Autowired private val stubProviderService: StubProviderService,
    @Autowired private val operationsRetryService: OperationsRetryService,
    @Autowired private val accountsSpacesDao: AccountsSpacesDao
) {

    companion object {
        private const val GRPC_URI = "in-process:test"
    }

    @Test
    fun testSuccessAfterRefreshNoAccountsSpacePush() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(100L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(70L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id))
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(100L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(70L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRefreshNoAccountsSpacePull() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 100L, 0L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 200L, 150L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 120L), mapOf(resource.id to 0L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(120L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(80L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(180L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id))
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(120L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(80L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 100L)), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 180L)), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(200L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(120L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(120L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRetryNoAccountsSpacePush() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(60L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(30L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                )
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.success(
                    UpdateProvisionResponse.newBuilder().addProvisions(
                        Provision.newBuilder().setResourceKey(
                            ResourceKey.newBuilder().setCompoundKey(
                                CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("VLA").build()
                                ).build()
                            ).build()
                        ).setProvided(
                            Amount.newBuilder().setValue(100L).setUnitKey("bytes").build()
                        ).setAllocated(
                            Amount.newBuilder().setValue(70L).setUnitKey("bytes").build()
                        ).build()
                    ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id))
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(100L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(70L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRetryNoAccountsSpacePull() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 100L, 0L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 200L, 150L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 120L), mapOf(resource.id to 0L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(200L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(150L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                )
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.success(
                    UpdateProvisionResponse.newBuilder().addProvisions(
                        Provision.newBuilder().setResourceKey(
                            ResourceKey.newBuilder().setCompoundKey(
                                CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("VLA").build()
                                ).build()
                            ).build()
                        ).setProvided(
                            Amount.newBuilder().setValue(120L).setUnitKey("bytes").build()
                        ).setAllocated(
                            Amount.newBuilder().setValue(80L).setUnitKey("bytes").build()
                        ).build()
                    ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(180L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id))
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(120L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(80L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 100L)), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 180L)), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(200L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(120L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(120L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterConflictNoAccountsSpacePush() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(60L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(30L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                ), GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(100L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(70L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                )
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata()))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id))
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(100L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(70L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterConflictNoAccountsSpacePull() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 100L, 0L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 200L, 150L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 120L), mapOf(resource.id to 0L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(200L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(150L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                ), GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(120L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(80L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                )
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata()))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(180L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id))
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(120L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(80L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 100L)), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 180L)), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(200L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(120L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(120L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testCompleteFailureNoAccountsSpacePush() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT))
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.ERROR, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertTrue(updatedOperation.get().orders.closeOrder.isEmpty)
        Assertions.assertTrue(updatedOperation.get().errorMessage.isPresent)
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(240L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id))
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(60L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(30L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertFalse(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertTrue(newOpLog!!.isEmpty())
        stubProviderService.reset()
    }

    @Test
    fun testCompleteFailureNoAccountsSpacePull() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 100L, 0L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 200L, 150L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 120L), mapOf(resource.id to 0L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT))
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT)),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.ERROR, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertTrue(updatedOperation.get().errorMessage.isPresent)
        Assertions.assertTrue(updatedOperation.get().orders.closeOrder.isEmpty)
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(100L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id))
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(200L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(150L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isEmpty)
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertTrue(newOpLog!!.isEmpty())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRefreshNoAccountsSpacePushOperationIdMatch() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(105L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(75L).setUnitKey("bytes").build()
                            ).setLastUpdate(
                                LastUpdate.newBuilder().setOperationId(operation.operationId).build()
                            ).build()
                        ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(195L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(105L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(75L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 200L)), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 195L)), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(105L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testTwoFailuresNoAccountsSpacePush() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(60L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(30L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                ),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT, Metadata()))
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.INVALID_ARGUMENT, Metadata()))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.ERROR, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertTrue(updatedOperation.get().orders.closeOrder.isEmpty)
        Assertions.assertTrue(updatedOperation.get().errorMessage.isPresent)
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(240L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(60L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(30L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertFalse(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertTrue(newOpLog!!.isEmpty())
        stubProviderService.reset()
    }

    @Test
    fun testNextIterationAfterRefreshNoAccountsSpacePush() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE, Metadata()))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.WAITING, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertTrue(updatedOperation.get().orders.closeOrder.isEmpty)
        Assertions.assertFalse(updatedOperation.get().errorMessage.isPresent)
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertEquals(1, inProgressAfter!!.size)
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(40L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(60L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(30L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertFalse(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertTrue(newOpLog!!.isEmpty())
        stubProviderService.reset()
    }

    @Test
    fun testNextIterationAfterRetryNoAccountsSpacePush() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(60L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(30L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                )
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE, Metadata()))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.WAITING, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertTrue(updatedOperation.get().orders.closeOrder.isEmpty)
        Assertions.assertFalse(updatedOperation.get().errorMessage.isPresent)
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertEquals(1, inProgressAfter!!.size)
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(40L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(60L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(30L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertFalse(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertTrue(newOpLog!!.isEmpty())
        stubProviderService.reset()
    }

    @Test
    fun testNextIterationAfterPostRetryRefreshNoAccountsSpacePush() {
        val provider = providerModel(
            GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true
        )
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(60L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(30L).setUnitKey("bytes").build()
                            ).build()
                        ).build()
                ),
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE)),
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE)),
                GrpcResponse.failure(StatusRuntimeException(Status.UNAVAILABLE))
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata()))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.WAITING, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertTrue(updatedOperation.get().orders.closeOrder.isEmpty)
        Assertions.assertFalse(updatedOperation.get().errorMessage.isPresent)
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertEquals(1, inProgressAfter!!.size)
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(40L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(60L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(30L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertFalse(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertTrue(newOpLog!!.isEmpty())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRetryNoAccountsSpacePushRefreshNotSupported() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.UNIMPLEMENTED)),
                GrpcResponse.failure(StatusRuntimeException(Status.UNIMPLEMENTED)),
                GrpcResponse.failure(StatusRuntimeException(Status.UNIMPLEMENTED))
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.success(
                    UpdateProvisionResponse.newBuilder().addProvisions(
                        Provision.newBuilder().setResourceKey(
                            ResourceKey.newBuilder().setCompoundKey(
                                CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("VLA").build()
                                ).build()
                            ).build()
                        ).setProvided(
                            Amount.newBuilder().setValue(100L).setUnitKey("bytes").build()
                        ).setAllocated(
                            Amount.newBuilder().setValue(70L).setUnitKey("bytes").build()
                        ).build()
                    ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(100L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(70L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRefreshNoAccountsSpacePushOperationIdAndVersionMatch() {
        val provider = providerModel(
            GRPC_URI,
            accountKeySupported = true,
            accountDisplayNameSupported = true,
            perProvisionVersionSupported = true
        )
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, 1L, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(105L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(75L).setUnitKey("bytes").build()
                            ).setLastUpdate(
                                LastUpdate.newBuilder().setOperationId(operation.operationId).build()
                            ).setVersion(
                                CurrentVersion.newBuilder().setVersion(2L).build()
                            ).build()
                        ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(195L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(105L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(75L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isPresent)
        Assertions.assertEquals(2L, updatedProvisions[0].lastReceivedProvisionVersion.get())
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 200L)), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 195L)), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, 1L)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, 1L)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(105L, 2L)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRefreshNoAccountsSpacePushOperationIdAndAccountVersionMatch() {
        val provider = providerModel(
            GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true, perAccountVersionSupported = true
        )
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", 1L, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test update")
                        .setFolderId(folder.id).setDeleted(false).setVersion(
                            CurrentVersion.newBuilder().setVersion(2L).build()
                        ).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").addResourceSegmentKeys(
                                        ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                            .setResourceSegmentKey("VLA").build()
                                    ).build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(105L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(75L).setUnitKey("bytes").build()
                            ).setLastUpdate(
                                LastUpdate.newBuilder().setOperationId(operation.operationId).build()
                            ).build()
                        ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test update", updatedAccount.get().displayName.get())
        Assertions.assertEquals(1L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertTrue(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertEquals(2L, updatedAccount.get().lastReceivedVersion.get())
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(195L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(105L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(75L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 200L)), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf(resource.id to 195L)), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(105L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isPresent)
        Assertions.assertEquals(1L, newOpLog[0].oldAccounts.get().accounts[account.id]!!.lastReceivedVersion.get())
        Assertions.assertEquals("Test", newOpLog[0].oldAccounts.get().accounts[account.id]!!.displayName.get())
        Assertions.assertTrue(newOpLog[0].newAccounts.isPresent)
        Assertions.assertEquals(2L, newOpLog[0].newAccounts.get().accounts[account.id]!!.lastReceivedVersion.get())
        Assertions.assertEquals("Test update", newOpLog[0].newAccounts.get().accounts[account.id]!!.displayName.get())
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRefreshPush() {
        val provider = providerModel(
            GRPC_URI, accountsSpacesSupported = true, accountKeySupported = true, accountDisplayNameSupported = true
        )
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val accountsSpace: AccountSpaceModel = accountSpaceModel(
            provider.id, "test", setOf(Tuples.of(locationSegmentation.id, vlaSegment.id))
        )
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            accountsSpace.id
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, accountsSpace.id, "test-id", "test", folder.id, "Test", null, null, false
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, accountsSpace.id, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsSpacesDao.upsertOneRetryable(txSession, accountsSpace)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(100L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(70L).setUnitKey("bytes").build()
                            ).build()
                        ).setAccountsSpaceKey(
                            AccountsSpaceKey.newBuilder().setCompoundKey(
                                CompoundAccountsSpaceKey.newBuilder().addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("VLA").build()
                                ).build()
                            ).build()
                        ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", accountsSpace.id)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        Assertions.assertEquals(accountsSpace.id, updatedAccount.get().accountsSpacesId.get())
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(100L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(70L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterRetryPush() {
        val provider = providerModel(
            GRPC_URI, accountsSpacesSupported = true, accountKeySupported = true, accountDisplayNameSupported = true
        )
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val accountsSpace: AccountSpaceModel = accountSpaceModel(
            provider.id, "test", setOf(Tuples.of(locationSegmentation.id, vlaSegment.id))
        )
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            accountsSpace.id
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, accountsSpace.id, "test-id", "test", folder.id, "Test", null, null, false
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, accountsSpace.id, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsSpacesDao.upsertOneRetryable(txSession, accountsSpace)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(60L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(30L).setUnitKey("bytes").build()
                            ).build()
                        ).setAccountsSpaceKey(
                            AccountsSpaceKey.newBuilder().setCompoundKey(
                                CompoundAccountsSpaceKey.newBuilder().addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("VLA").build()
                                ).build()
                            ).build()
                        ).build()
                )
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.success(
                    UpdateProvisionResponse.newBuilder().addProvisions(
                        Provision.newBuilder().setResourceKey(
                            ResourceKey.newBuilder().setCompoundKey(
                                CompoundResourceKey.newBuilder().setResourceTypeKey("test").build()
                            ).build()
                        ).setProvided(
                            Amount.newBuilder().setValue(100L).setUnitKey("bytes").build()
                        ).setAllocated(
                            Amount.newBuilder().setValue(70L).setUnitKey("bytes").build()
                        ).build()
                    ).setAccountsSpaceKey(
                        AccountsSpaceKey.newBuilder().setCompoundKey(
                            CompoundAccountsSpaceKey.newBuilder().addResourceSegmentKeys(
                                ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                    .setResourceSegmentKey("VLA").build()
                            ).build()
                        ).build()
                    ).build()
                )
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", accountsSpace.id)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        Assertions.assertEquals(accountsSpace.id, updatedAccount.get().accountsSpacesId.get())
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(100L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(70L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testSuccessAfterConflictPush() {
        val provider = providerModel(
            GRPC_URI, accountsSpacesSupported = true, accountKeySupported = true, accountDisplayNameSupported = true
        )
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val accountsSpace: AccountSpaceModel = accountSpaceModel(
            provider.id, "test", setOf(Tuples.of(locationSegmentation.id, vlaSegment.id))
        )
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            accountsSpace.id
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, accountsSpace.id, "test-id", "test", folder.id, "Test", null, null, false
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, accountsSpace.id, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsSpacesDao.upsertOneRetryable(txSession, accountsSpace)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(60L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(30L).setUnitKey("bytes").build()
                            ).build()
                        ).setAccountsSpaceKey(
                            AccountsSpaceKey.newBuilder().setCompoundKey(
                                CompoundAccountsSpaceKey.newBuilder().addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("VLA").build()
                                ).build()
                            ).build()
                        ).build()
                ), GrpcResponse.success(
                    Account.newBuilder().setAccountId("test-id").setKey("test").setDisplayName("Test")
                        .setFolderId(folder.id).setDeleted(false).addProvisions(
                            Provision.newBuilder().setResourceKey(
                                ResourceKey.newBuilder().setCompoundKey(
                                    CompoundResourceKey.newBuilder().setResourceTypeKey("test").build()
                                ).build()
                            ).setProvided(
                                Amount.newBuilder().setValue(100L).setUnitKey("bytes").build()
                            ).setAllocated(
                                Amount.newBuilder().setValue(70L).setUnitKey("bytes").build()
                            ).build()
                        ).setAccountsSpaceKey(
                            AccountsSpaceKey.newBuilder().setCompoundKey(
                                CompoundAccountsSpaceKey.newBuilder().addResourceSegmentKeys(
                                    ResourceSegmentKey.newBuilder().setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("VLA").build()
                                ).build()
                            ).build()
                        ).build()
                )
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.FAILED_PRECONDITION, Metadata()))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", accountsSpace.id)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        Assertions.assertEquals(accountsSpace.id, updatedAccount.get().accountsSpacesId.get())
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.OK, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertEquals(2L, updatedOperation.get().orders.closeOrder.get())
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(200L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(100L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(70L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        Assertions.assertEquals(
            operation.operationId, updatedProvisions[0].latestSuccessfulProvisionOperationId.get()
        )
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertEquals(1, newOpLog!!.size)
        Assertions.assertEquals(folder.id, newOpLog[0].folderId)
        Assertions.assertEquals(
            FolderOperationType.DELIVER_PROVIDE_QUOTAS_TO_ACCOUNT, newOpLog[0].operationType
        )
        Assertions.assertEquals("0b204534-d0ec-452d-99fe-a3d1da5a49a9", newOpLog[0].authorUserId.get())
        Assertions.assertEquals("1120000000000001", newOpLog[0].authorUserUid.get())
        Assertions.assertTrue(newOpLog[0].authorProviderId.isEmpty)
        Assertions.assertTrue(newOpLog[0].sourceFolderOperationsLogId.isEmpty)
        Assertions.assertTrue(newOpLog[0].oldFolderFields.isEmpty)
        Assertions.assertTrue(newOpLog[0].newFolderFields.isEmpty)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newQuotas)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].oldBalance)
        Assertions.assertEquals(QuotasByResource(mapOf()), newOpLog[0].newBalance)
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(60L, null)))
                )
            ), newOpLog[0].oldProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].newProvisions
        )
        Assertions.assertEquals(
            QuotasByAccount(
                mapOf(
                    account.id to ProvisionsByResource(mapOf(resource.id to ProvisionHistoryModel(100L, null)))
                )
            ), newOpLog[0].actuallyAppliedProvisions.get()
        )
        Assertions.assertTrue(newOpLog[0].oldAccounts.isEmpty)
        Assertions.assertTrue(newOpLog[0].newAccounts.isEmpty)
        Assertions.assertEquals(2L, newOpLog[0].order)
        Assertions.assertEquals(operation.operationId, newOpLog[0].accountsQuotasOperationsId.get())
        stubProviderService.reset()
    }

    @Test
    fun testFailuresNotFoundDeletedAccountPush() {
        val provider = providerModel(
            GRPC_URI, accountsSpacesSupported = true, accountKeySupported = true, accountDisplayNameSupported = true
        )
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 200L, 40L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null, true
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60L, 30L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100L), mapOf(resource.id to 40L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.NOT_FOUND, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.NOT_FOUND, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.NOT_FOUND, Metadata()))
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.NOT_FOUND, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.NOT_FOUND, Metadata())),
                GrpcResponse.failure(StatusRuntimeException(Status.NOT_FOUND, Metadata()))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.ERROR, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertTrue(updatedOperation.get().orders.closeOrder.isEmpty)
        Assertions.assertTrue(updatedOperation.get().errorMessage.isPresent)
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(240L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(60L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(30L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertFalse(updatedProvisions[0].latestSuccessfulProvisionOperationId.isPresent)
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertTrue(newOpLog!!.isEmpty())
        stubProviderService.reset()
    }

    @Test
    fun testCompleteFailureNoAccountsSpacePullStatusUnknown() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300L, 100L, 0L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null, false
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 200L, 150L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 120L), mapOf(resource.id to 0L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        stubProviderService.setGetAccountResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.UNKNOWN))
            )
        )
        stubProviderService.setUpdateProvisionResponses(
            listOf(
                GrpcResponse.failure(StatusRuntimeException(Status.UNKNOWN))
            )
        )
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        val updatedAccount = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.getAllByExternalId(
                    txSession, WithTenant(
                        Tenants.DEFAULT_TENANT_ID, ExternalId(provider.id, "test-id", null)
                    )
                )
            }
        }.block()
        Assertions.assertNotNull(updatedAccount)
        Assertions.assertTrue(updatedAccount!!.isPresent)
        Assertions.assertEquals(provider.id, updatedAccount.get().providerId)
        Assertions.assertEquals("test-id", updatedAccount.get().outerAccountIdInProvider)
        Assertions.assertEquals("test", updatedAccount.get().outerAccountKeyInProvider.get())
        Assertions.assertEquals(folder.id, updatedAccount.get().folderId)
        Assertions.assertEquals("Test", updatedAccount.get().displayName.get())
        Assertions.assertEquals(0L, updatedAccount.get().version)
        Assertions.assertFalse(updatedAccount.get().isDeleted)
        Assertions.assertFalse(updatedAccount.get().lastReceivedVersion.isPresent)
        Assertions.assertTrue(updatedAccount.get().latestSuccessfulAccountOperationId.isEmpty)
        val updatedOperation = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.getById(
                    txSession, operation.operationId, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedOperation)
        Assertions.assertTrue(updatedOperation!!.isPresent)
        Assertions.assertEquals(
            AccountsQuotasOperationsModel.RequestStatus.ERROR, updatedOperation.get().requestStatus.get()
        )
        Assertions.assertTrue(updatedOperation.get().errorMessage.isPresent)
        Assertions.assertTrue(updatedOperation.get().orders.closeOrder.isEmpty)
        val inProgressAfter = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.getAllByTenant(
                    txSession, Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(inProgressAfter)
        Assertions.assertTrue(inProgressAfter!!.isEmpty())
        val updatedQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.getByFolders(
                    txSession, listOf(folder.id), Tenants.DEFAULT_TENANT_ID
                )
            }
        }.block()
        Assertions.assertNotNull(updatedQuotas)
        Assertions.assertEquals(1, updatedQuotas!!.size)
        Assertions.assertEquals(folder.id, updatedQuotas[0].folderId)
        Assertions.assertEquals(provider.id, updatedQuotas[0].providerId)
        Assertions.assertEquals(resource.id, updatedQuotas[0].resourceId)
        Assertions.assertEquals(300L, updatedQuotas[0].quota)
        Assertions.assertEquals(100L, updatedQuotas[0].balance)
        Assertions.assertEquals(0L, updatedQuotas[0].frozenQuota)
        val updatedProvisions = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.getAllByAccountIds(
                    txSession, Tenants.DEFAULT_TENANT_ID, setOf(account.id)
                )
            }
        }.block()
        Assertions.assertNotNull(updatedProvisions)
        Assertions.assertEquals(1, updatedProvisions!!.size)
        Assertions.assertEquals(account.id, updatedProvisions[0].accountId)
        Assertions.assertEquals(resource.id, updatedProvisions[0].resourceId)
        Assertions.assertEquals(folder.id, updatedProvisions[0].folderId)
        Assertions.assertEquals(provider.id, updatedProvisions[0].providerId)
        Assertions.assertEquals(200L, updatedProvisions[0].providedQuota)
        Assertions.assertEquals(150L, updatedProvisions[0].allocatedQuota)
        Assertions.assertTrue(updatedProvisions[0].lastReceivedProvisionVersion.isEmpty)
        Assertions.assertTrue(updatedProvisions[0].latestSuccessfulProvisionOperationId.isEmpty)
        val newOpLog = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderOperationLogDao.getFirstPageByFolder(
                    txSession, Tenants.DEFAULT_TENANT_ID, folder.id, SortOrderDto.ASC, 100
                )
            }
        }.block()
        Assertions.assertNotNull(newOpLog)
        Assertions.assertTrue(newOpLog!!.isEmpty())
        stubProviderService.reset()
    }

    @Test
    fun testUpdateProvisionRetryProviderApiUnitIsSet() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null,
            "bb6b1e08-49a7-4cf8-b1b2-e8e71871d6d3"
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300000L, 200000L, 40000L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60000L, 30000L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100000L), mapOf(resource.id to 40000L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(
                    txSession, provider
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        Assertions.assertNotNull(stubProviderService.updateProvisionRequests)
        Assertions.assertEquals(1, stubProviderService.updateProvisionRequests.size)
        Assertions.assertEquals(
            1, stubProviderService.updateProvisionRequests.first.t1.updatedProvisionsList.size
        )
        Assertions.assertEquals(
            1, stubProviderService.updateProvisionRequests.first.t1.knownProvisionsList.size
        )
        Assertions.assertEquals(
            1, stubProviderService.updateProvisionRequests.first.t1.getKnownProvisions(0).knownProvisionsList.size
        )
        val updatedProvisionsRequest = stubProviderService.updateProvisionRequests.first.t1.getUpdatedProvisions(0)
        val knownProvisionsRequest =
            stubProviderService.updateProvisionRequests.first.t1.getKnownProvisions(0).getKnownProvisions(0)
        Assertions.assertNotNull(updatedProvisionsRequest)
        Assertions.assertNotNull(knownProvisionsRequest)
        Assertions.assertEquals(100L, updatedProvisionsRequest.provided.value)
        Assertions.assertEquals("kilobytes", updatedProvisionsRequest.provided.unitKey)
        Assertions.assertEquals(60L, knownProvisionsRequest.provided.value)
        Assertions.assertEquals("kilobytes", knownProvisionsRequest.provided.unitKey)
        stubProviderService.reset()
    }

    @Test
    fun testUpdateProvisionRetryProviderApiUnitIsNotSet() {
        val provider = providerModel(GRPC_URI, accountKeySupported = true, accountDisplayNameSupported = true)
        val folder = folderModel(1L)
        val resourceType = resourceTypeModel(
            provider.id, "test", "b02344bf-96af-4cc5-937c-66a479989ce8"
        )
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val resource = resourceModel(
            provider.id,
            "test",
            resourceType.id,
            setOf(Tuples.of(locationSegmentation.id, vlaSegment.id)),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf(
                "b15101c2-da50-4d6f-9a8e-b90160871b0a",
                "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c",
                "74fe1983-144a-4156-8839-aa791cc2deb6"
            ),
            "74fe1983-144a-4156-8839-aa791cc2deb6",
            "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            null,
            null
        )
        val quota = quotaModel(provider.id, resource.id, folder.id, 300000L, 200000L, 40000L)
        val account = accountModel(
            provider.id, null, "test-id", "test", folder.id, "Test", null, null
        )
        val provision = accountQuotaModel(
            provider.id, resource.id, folder.id, account.id, 60000L, 30000L, null, null
        )
        val operation = operationModel(
            provider.id, null, account.id, mapOf(resource.id to 100000L), mapOf(resource.id to 40000L)
        )
        val inProgress = inProgressModel(operation.operationId, folder.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                providersDao.upsertProviderRetryable(
                    txSession, provider
                )
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quota)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasDao.upsertOneRetryable(txSession, provision)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                accountsQuotasOperationsDao.upsertOneRetryable(txSession, operation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE
            ) { txSession: YdbTxSession? ->
                operationsInProgressDao.upsertOneRetryable(txSession, inProgress)
            }
        }.block()
        operationsRetryService.retryOperations(Clock.systemUTC(), Locales.ENGLISH).block()
        Assertions.assertNotNull(stubProviderService.updateProvisionRequests)
        Assertions.assertEquals(1, stubProviderService.updateProvisionRequests.size)
        Assertions.assertEquals(
            1, stubProviderService.updateProvisionRequests.first.t1.updatedProvisionsList.size
        )
        Assertions.assertEquals(
            1, stubProviderService.updateProvisionRequests.first.t1.knownProvisionsList.size
        )
        Assertions.assertEquals(
            1, stubProviderService.updateProvisionRequests.first.t1.getKnownProvisions(0).knownProvisionsList.size
        )
        val updatedProvisionsRequest = stubProviderService.updateProvisionRequests.first.t1.getUpdatedProvisions(0)
        val knownProvisionsRequest =
            stubProviderService.updateProvisionRequests.first.t1.getKnownProvisions(0).getKnownProvisions(0)
        Assertions.assertNotNull(updatedProvisionsRequest)
        Assertions.assertNotNull(knownProvisionsRequest)
        Assertions.assertEquals(100000L, updatedProvisionsRequest.provided.value)
        Assertions.assertEquals("bytes", updatedProvisionsRequest.provided.unitKey)
        Assertions.assertEquals(60000L, knownProvisionsRequest.provided.value)
        Assertions.assertEquals("bytes", knownProvisionsRequest.provided.unitKey)
        stubProviderService.reset()
    }

    fun providerModel(
        grpcUri: String? = null,
        restUri: String? = null,
        accountsSpacesSupported: Boolean = false,
        softDeleteSupported: Boolean = false,
        accountKeySupported: Boolean = false,
        accountDisplayNameSupported: Boolean = false,
        perAccountVersionSupported: Boolean = false,
        perProvisionVersionSupported: Boolean = false,
        perAccountLastUpdateSupported: Boolean = false,
        operationIdDeduplicationSupported: Boolean = false,
        perProvisionLastUpdateSupported: Boolean = true
    ): ProviderModel {
        return ProviderModel.builder().id(UUID.randomUUID().toString()).grpcApiUri(grpcUri).restApiUri(restUri)
            .destinationTvmId(42L).tenantId(Tenants.DEFAULT_TENANT_ID).version(0L).nameEn("Test").nameRu("Test")
            .descriptionEn("Test").descriptionRu("Test").sourceTvmId(42L).serviceId(69L).deleted(false).readOnly(false)
            .multipleAccountsPerFolder(true).accountTransferWithQuota(true).managed(true).key("test")
            .trackerComponentId(1L).accountsSettings(
                AccountsSettingsModel.builder().displayNameSupported(accountDisplayNameSupported)
                    .keySupported(accountKeySupported).deleteSupported(true).softDeleteSupported(softDeleteSupported)
                    .moveSupported(true).renameSupported(true).perAccountVersionSupported(perAccountVersionSupported)
                    .perProvisionVersionSupported(perProvisionVersionSupported)
                    .perAccountLastUpdateSupported(perAccountLastUpdateSupported)
                    .perProvisionLastUpdateSupported(perProvisionLastUpdateSupported)
                    .operationIdDeduplicationSupported(operationIdDeduplicationSupported).syncCoolDownDisabled(false)
                    .retryCoolDownDisabled(false).accountsSyncPageSize(1000L).build()
            ).importAllowed(true).accountsSpacesSupported(accountsSpacesSupported).syncEnabled(true).grpcTlsOn(true)
            .build()
    }

    fun folderModel(serviceId: Long): FolderModel {
        return FolderModel.newBuilder().setId(UUID.randomUUID().toString()).setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setServiceId(serviceId).setVersion(0L).setDisplayName("Test").setDescription("Test").setDeleted(false)
            .setFolderType(FolderType.COMMON).setTags(emptySet()).setNextOpLogOrder(2L).build()
    }

    fun operationModel(
        providerId: String,
        accountsSpaceId: String?,
        accountId: String,
        updatedProvisionsByResourceId: Map<String, Long>,
        frozenProvisionsByResourceId: Map<String, Long>
    ): AccountsQuotasOperationsModel {
        return AccountsQuotasOperationsModel.builder().setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(UUID.randomUUID().toString()).setLastRequestId(null)
            .setCreateDateTime(Instant.now().minus(Duration.ofMinutes(2))).setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.DELIVER_AND_UPDATE_PROVISION)
            .setAuthorUserId("0b204534-d0ec-452d-99fe-a3d1da5a49a9").setAuthorUserUid("1120000000000001")
            .setProviderId(providerId).setAccountsSpaceId(accountsSpaceId).setUpdateDateTime(null)
            .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING).setErrorMessage(null)
            .setRequestedChanges(OperationChangesModel.builder().accountId(accountId)
                .updatedProvisions(updatedProvisionsByResourceId.map {
                    OperationChangesModel.Provision(
                        it.key, it.value
                    )
                }).frozenProvisions(frozenProvisionsByResourceId.map {
                    OperationChangesModel.Provision(
                        it.key, it.value
                    )
                }).build()
            ).setOrders(
                OperationOrdersModel.builder().submitOrder(1L).build()
            ).build()
    }

    fun inProgressModel(operationId: String, folderId: String): OperationInProgressModel {
        return OperationInProgressModel.builder().tenantId(Tenants.DEFAULT_TENANT_ID).operationId(operationId)
            .folderId(folderId).accountId(null).build()
    }

    fun resourceSegmentationModel(providerId: String, key: String): ResourceSegmentationModel {
        return ResourceSegmentationModel.builder().id(UUID.randomUUID().toString()).tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(providerId).version(0L).key(key).nameEn("Test").nameRu("Test").descriptionEn("Test")
            .descriptionRu("Test").deleted(false).build()
    }

    fun resourceSegmentModel(segmentationId: String, key: String): ResourceSegmentModel {
        return ResourceSegmentModel.builder().id(UUID.randomUUID().toString()).tenantId(Tenants.DEFAULT_TENANT_ID)
            .segmentationId(segmentationId).version(0L).key(key).nameEn("Test").nameRu("Test").descriptionEn("Test")
            .descriptionRu("Test").deleted(false).build()
    }

    fun resourceTypeModel(providerId: String, key: String, unitsEnsembleId: String): ResourceTypeModel {
        return ResourceTypeModel.builder().id(UUID.randomUUID().toString()).tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(providerId).version(0L).key(key).nameEn("Test").nameRu("Test").descriptionEn("Test")
            .descriptionRu("Test").deleted(false).unitsEnsembleId(unitsEnsembleId).build()
    }

    fun resourceModel(
        providerId: String,
        key: String,
        resourceTypeId: String,
        segments: Set<Tuple2<String, String>>,
        unitsEnsembleId: String,
        allowedUnitIds: Set<String>,
        defaultUnitId: String,
        baseUnitId: String,
        accountsSpaceId: String?,
        providerApiUnitId: String? = null
    ): ResourceModel {
        return ResourceModel.builder().id(UUID.randomUUID().toString()).tenantId(Tenants.DEFAULT_TENANT_ID).version(0L)
            .key(key).nameEn("Test").nameRu("Test").descriptionEn("Test").descriptionRu("Test").deleted(false)
            .unitsEnsembleId(unitsEnsembleId).providerId(providerId).resourceTypeId(resourceTypeId)
            .segments(segments.mapTo(HashSet()) { t: Tuple2<String, String> ->
                ResourceSegmentSettingsModel(t.t1, t.t2)
            }).resourceUnits(ResourceUnitsModel(allowedUnitIds, defaultUnitId, providerApiUnitId)).managed(true)
            .orderable(true).readOnly(false).baseUnitId(baseUnitId).accountsSpacesId(accountsSpaceId).build()
    }

    fun quotaModel(
        providerId: String, resourceId: String, folderId: String, quota: Long, balance: Long, frozen: Long
    ): QuotaModel {
        return QuotaModel.builder().tenantId(Tenants.DEFAULT_TENANT_ID).providerId(providerId).resourceId(resourceId)
            .folderId(folderId).quota(quota).balance(balance).frozenQuota(frozen).build()
    }

    fun accountModel(
        providerId: String,
        accountsSpaceId: String?,
        externalId: String,
        externalKey: String,
        folderId: String,
        displayName: String,
        lastReceivedVersion: Long?,
        lastOpId: String?,
        deleted: Boolean = false
    ): AccountModel {
        return AccountModel.Builder().setTenantId(Tenants.DEFAULT_TENANT_ID).setId(UUID.randomUUID().toString())
            .setVersion(0L).setProviderId(providerId).setAccountsSpacesId(accountsSpaceId)
            .setOuterAccountIdInProvider(externalId).setOuterAccountKeyInProvider(externalKey).setFolderId(folderId)
            .setDisplayName(displayName).setDeleted(deleted).setLastAccountUpdate(Instant.now())
            .setLastReceivedVersion(lastReceivedVersion).setLatestSuccessfulAccountOperationId(lastOpId).build()
    }

    fun accountQuotaModel(
        providerId: String,
        resourceId: String,
        folderId: String,
        accountId: String,
        provided: Long,
        allocated: Long,
        lastReceivedVersion: Long?,
        lastOpId: String?
    ): AccountsQuotasModel {
        return AccountsQuotasModel.Builder().setTenantId(Tenants.DEFAULT_TENANT_ID).setProviderId(providerId)
            .setResourceId(resourceId).setFolderId(folderId).setAccountId(accountId).setProvidedQuota(provided)
            .setAllocatedQuota(allocated).setLastProvisionUpdate(Instant.now())
            .setLastReceivedProvisionVersion(lastReceivedVersion).setLatestSuccessfulProvisionOperationId(lastOpId)
            .build()
    }
}
