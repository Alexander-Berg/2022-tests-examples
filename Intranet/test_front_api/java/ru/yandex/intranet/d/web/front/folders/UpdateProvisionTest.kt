package ru.yandex.intranet.d.web.front.folders

import com.yandex.ydb.table.transaction.TransactionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts.*
import ru.yandex.intranet.d.TestFolders.*
import ru.yandex.intranet.d.TestProviders.DEFAULT_QUOTAS_PROVIDER_ID
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestResourceTypes.YP_HDD
import ru.yandex.intranet.d.TestResources.*
import ru.yandex.intranet.d.TestServices
import ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_CLOSING
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds.*
import ru.yandex.intranet.d.UnitsEnsembleIds
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.units.UnitsEnsemblesDao
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.front.transfer.FrontTransferRequestsControllerTest
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.quotas.*
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto.*
import java.lang.Long.parseLong
import java.util.Locale.ENGLISH

/**
 * UpdateProvisionTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 13-07-2021
 */
@IntegrationTest
class UpdateProvisionTest {
    @Autowired
    private lateinit var helperFactory: UpdateProvisionTestHelperFactory

    @Autowired
    private lateinit var tableClient: YdbTableClient

    @Autowired
    private lateinit var quotasDao: QuotasDao
    @Autowired
    private lateinit var resourceTypesDao: ResourceTypesDao
    @Autowired
    private lateinit var resourcesDao: ResourcesDao
    @Autowired
    private lateinit var unitsEnsemblesDao: UnitsEnsemblesDao
    @Autowired
    private lateinit var webClient: WebTestClient
    @Autowired
    @Qualifier("messageSource")
    private lateinit var messages: MessageSource

    @Test
    fun missingQuotasShouldBeConsideredAsZero() {
        helperFactory
            .newHelper(TEST_FOLDER_1_SERVICE_ID, TEST_FOLDER_1_ID, TEST_ACCOUNT_1.providerId, TEST_ACCOUNT_1.id)
            .withAccountsSpaceKey("location" to "man", "segment" to "default")
            .build().withProviderAnswer(
//            "hdd" provide 0 unit "gigabytes", // -- missing
                "cpu" provide 1 unit "millicores",
            ) { executor ->
                val updateProvisionsResponse = executor.updateProvisions(
                    YP_HDD_MAN change 200 unit GIGABYTES to "0" unit GIGABYTES,
                    YP_CPU_MAN change 0 unit MILLICORES to "1" unit MILLICORES,
                )

                val expandedAccount = updateProvisionsResponse.expandedProvider
                    ?.accounts?.first { it.account.id.equals(TEST_ACCOUNT_1.id) }
                assertEquals("0",
                    expandedAccount
                        ?.resources?.first { it.resourceId.equals(YP_HDD_MAN) }
                        ?.provided?.rawAmount
                )
                assertEquals("1",
                    expandedAccount
                        ?.resources?.first { it.resourceId.equals(YP_CPU_MAN) }
                        ?.provided?.rawAmount
                )
                assertEquals("1000000000000",
                    updateProvisionsResponse.expandedProvider
                        ?.resourceTypes?.first { it.resourceTypeId.equals(YP_HDD) }
                        ?.resources?.first { it.resourceId.equals(YP_HDD_MAN) }
                        ?.balance?.rawAmount
                )
            }
    }

    @Test
    fun orderOfSegmentsDoesNotMatter() {
        helperFactory
            .newHelper(TEST_FOLDER_6_SERVICE_ID, TEST_FOLDER_6_ID, TEST_ACCOUNT_7.providerId, TEST_ACCOUNT_7.id)
            .build().withProviderAnswer(
                "ram" segment "location" to "sas" and "segment" to "default" provide 1 unit "mebibytes"
            ) { executor ->
                val response = executor.updateProvisions(
                    YDB_RAM_SAS change 0 unit MEBIBYTES to "1" unit MEBIBYTES
                )
                executor.commonAssert(response, YDB_RAM_SAS, "1048576")
            }

        helperFactory
            .newHelper(TEST_FOLDER_6_SERVICE_ID, TEST_FOLDER_6_ID, TEST_ACCOUNT_7.providerId, TEST_ACCOUNT_7.id)
            .build().withProviderAnswer { executor ->
                executor.updateProvisions(
                    YDB_RAM_SAS change 1 unit MEBIBYTES to "0" unit MEBIBYTES
                )
            }

        helperFactory
            .newHelper(TEST_FOLDER_6_SERVICE_ID, TEST_FOLDER_6_ID, TEST_ACCOUNT_7.providerId, TEST_ACCOUNT_7.id)
            .build().withProviderAnswer(
                "ram" segment "segment" to "default" and "location" to "sas" provide 1 unit "mebibytes"
            ) { executor ->
                val response = executor.updateProvisions(
                    YDB_RAM_SAS change 0 unit MEBIBYTES to "1" unit MEBIBYTES
                )
                executor.commonAssert(response, YDB_RAM_SAS, "1048576")
            }
    }

    @Test
    fun checkNonChangedAllocatedResourcesInFolder() {
        helperFactory
            .newHelper(TEST_SERVICE_ID_CLOSING, TEST_FOLDER_IN_CLOSING_SERVICE, YP_ID, TEST_ACCOUNT_IN_CLOSING_SERVICE)
            .build().withProviderAnswer { executor ->
                val updateProvisionsResponse = executor.updateProvisions(
                    YP_SSD_MAN change 50000 unit GIGABYTES to "800" unit GIGABYTES
                )

                val resourceTypeYpHdd = updateProvisionsResponse.expandedProvider
                    ?.resourceTypes?.first { it.resourceTypeId.equals(YP_HDD) }
                val resourceYpHddMan = resourceTypeYpHdd?.resources?.first { it.resourceId.equals(YP_HDD_MAN) }

                assertEquals("10000000000", resourceTypeYpHdd?.sums?.allocated?.rawAmount)
                assertEquals("10000000000", resourceYpHddMan?.allocated?.rawAmount)
            }
    }

    @Test
    fun defaultQuotaAddedToProvisionInNewAccount() {
        helperFactory
            .newHelper(TEST_FOLDER_17_SERVICE_ID, TEST_FOLDER_17_ID, DEFAULT_QUOTAS_PROVIDER_ID, "")
            .build().withProviderAnswer(
                "default_resource" provide 1048576 unit "bytes"
            ) { executor ->
                val oldFolderQuotas = executor.getFolderQuotas()
                val oldProvider = oldFolderQuotas.provider(DEFAULT_QUOTAS_PROVIDER_ID)
                val oldResource = oldProvider?.resourceType(DEFAULT_RESOURCE_TYPE_ID)?.resource(DEFAULT_RESOURCE_ID)
                val oldQuota: Long = parseLong(oldResource?.quota?.rawAmount)
                val oldBalance = parseLong(oldResource?.balance?.rawAmount)

                val createAccountAnswer = executor.createAccount()

                // Handle response
                val expandedProvider = createAccountAnswer.expandedProvider
                val account = expandedProvider.accounts.first()
                val accountId = account.account.id
                assertEquals("1048576", account.resource(DEFAULT_RESOURCE_ID)?.provided?.rawAmount)
                val resource = expandedProvider.resourceType(DEFAULT_RESOURCE_TYPE_ID)?.resource(DEFAULT_RESOURCE_ID)
                assertEquals(oldBalance.toString(), resource?.balance?.rawAmount)
                assertEquals((oldQuota + 1048576L).toString(), resource?.quota?.rawAmount)

                // Folder quota and balance
                val folderQuotas = executor.getFolderQuotas()
                val provider = folderQuotas.provider(DEFAULT_QUOTAS_PROVIDER_ID)
                val accountDefaultResource = provider?.account(accountId)?.resource(DEFAULT_RESOURCE_ID)
                assertEquals("1048576", accountDefaultResource?.provided?.rawAmount)
                val defaultResource = provider?.resourceType(DEFAULT_RESOURCE_TYPE_ID)?.resource(DEFAULT_RESOURCE_ID)
                assertEquals(oldBalance.toString(), defaultResource?.balance?.rawAmount)
                assertEquals((oldQuota + 1048576L).toString(), defaultResource?.quota?.rawAmount)

                // Folder history
                val folderHistory = executor.getFolderHistory().first()
                val historyProvision = folderHistory.newProvisions
                    .provisionByByAccountId[accountId]
                    ?.provisionByResourceId?.get(DEFAULT_RESOURCE_ID)
                assertEquals("1048576", historyProvision?.provision?.rawAmount)

                // Test provision over default
                helperFactory
                    .newHelper(TEST_FOLDER_17_SERVICE_ID, TEST_FOLDER_17_ID, DEFAULT_QUOTAS_PROVIDER_ID, accountId)
                    .build()
                    .withProviderAnswer(
                        "default_resource" provide 2 unit "mebibytes"
                    ) { provisionExecutor ->
                        val updateProvisionsResponse = provisionExecutor.updateProvisions(
                            DEFAULT_RESOURCE_ID change 1 unit MEBIBYTES to "2" unit MEBIBYTES
                        )
                        provisionExecutor.commonAssert(updateProvisionsResponse, DEFAULT_RESOURCE_ID, "2097152")
                        val folderQuotaAfterUpdateProvisions =
                            updateProvisionsResponse.expandedProvider
                                .resourceType(DEFAULT_RESOURCE_TYPE_ID)?.resource(DEFAULT_RESOURCE_ID)
                        assertEquals(
                            (oldBalance - 1048576).toString(),
                            folderQuotaAfterUpdateProvisions?.balance?.rawAmount
                        )
                    }
            }
    }

    @Test
    fun defaultQuotaAddedToProvisionInNewAccountWithRetry() {
        helperFactory
            .newHelper(TEST_FOLDER_17_SERVICE_ID, TEST_FOLDER_17_ID, DEFAULT_QUOTAS_PROVIDER_ID, "")
            .withRetryOperation()
            .build().withProviderAnswer(
                "default_resource" provide 1048576 unit "bytes"
            ) { executor ->
                val oldFolderQuotas = executor.getFolderQuotas()
                val oldProvider = oldFolderQuotas.provider(DEFAULT_QUOTAS_PROVIDER_ID)
                val oldResource = oldProvider?.resourceType(DEFAULT_RESOURCE_TYPE_ID)?.resource(DEFAULT_RESOURCE_ID)
                val oldQuota: Long = parseLong(oldResource?.quota?.rawAmount)
                val oldBalance = parseLong(oldResource?.balance?.rawAmount)

                val createAccountAnswer = executor.createAccount()

                // Handle response
                val expandedProvider = createAccountAnswer.expandedProvider
                val account = expandedProvider.accounts.first()
                val accountId = account.account.id
                assertEquals("1048576", account.resource(DEFAULT_RESOURCE_ID)?.provided?.rawAmount)
                val resource = expandedProvider.resourceType(DEFAULT_RESOURCE_TYPE_ID)?.resource(DEFAULT_RESOURCE_ID)
                assertEquals(oldBalance.toString(), resource?.balance?.rawAmount)
                assertEquals((oldQuota + 1048576L).toString(), resource?.quota?.rawAmount)

                assertEquals("Provider with default quotas", createAccountAnswer.provider.name)
                assertEquals("Default resource", createAccountAnswer.resourceTypes[0].name)
                assertEquals("Default resource", createAccountAnswer.resources[0].displayName)

                // Folder quota and balance
                val folderQuotas = executor.getFolderQuotas()
                val provider = folderQuotas.provider(DEFAULT_QUOTAS_PROVIDER_ID)
                val accountDefaultResource = provider?.account(accountId)?.resource(DEFAULT_RESOURCE_ID)
                assertEquals("1048576", accountDefaultResource?.provided?.rawAmount)
                val defaultResource = provider?.resourceType(DEFAULT_RESOURCE_TYPE_ID)?.resource(DEFAULT_RESOURCE_ID)
                assertEquals(oldBalance.toString(), defaultResource?.balance?.rawAmount)
                assertEquals((oldQuota + 1048576L).toString(), defaultResource?.quota?.rawAmount)

                // Folder history
                val folderHistory = executor.getFolderHistory().first()
                val historyProvision = folderHistory.newProvisions
                    .provisionByByAccountId[accountId]
                    ?.provisionByResourceId?.get(DEFAULT_RESOURCE_ID)
                assertEquals("1048576", historyProvision?.provision?.rawAmount)

                // Test provision over default
                helperFactory
                    .newHelper(TEST_FOLDER_17_SERVICE_ID, TEST_FOLDER_17_ID, DEFAULT_QUOTAS_PROVIDER_ID, accountId)
                    .withRetryOperation()
                    .build()
                    .withProviderAnswer(
                        "default_resource" provide 2 unit "mebibytes"
                    ) { provisionExecutor ->
                        val updateProvisionsResponse = provisionExecutor.updateProvisions(
                            DEFAULT_RESOURCE_ID change 1 unit MEBIBYTES to "2" unit MEBIBYTES
                        )
                        provisionExecutor.commonAssert(updateProvisionsResponse, DEFAULT_RESOURCE_ID, "2097152")
                        val folderQuotaAfterUpdateProvisions =
                            updateProvisionsResponse.expandedProvider
                                .resourceType(DEFAULT_RESOURCE_TYPE_ID)?.resource(DEFAULT_RESOURCE_ID)
                        assertEquals(
                            (oldBalance - 1048576).toString(),
                            folderQuotaAfterUpdateProvisions?.balance?.rawAmount
                        )
                    }

            }
    }

    @Test
    fun testFrozenQuotaSum() {
        val quotaModel = QuotaModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(TEST_ACCOUNT_1.providerId)
            .resourceId(YP_HDD_VLA)
            .folderId(TEST_FOLDER_1_ID)
            .quota(10L)
            .balance(5L)
            .frozenQuota(3L)
            .build()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quotaModel)
            }
        }
            .block()
        helperFactory
            .newHelper(TEST_FOLDER_1_SERVICE_ID, TEST_FOLDER_1_ID, TEST_ACCOUNT_1.providerId, TEST_ACCOUNT_1.id)
            .build()
            .withProviderAnswer(
                "hdd" provide 1 unit "gigabytes"
            ) { provisionExecutor ->
                val updateProvisionsResponse = provisionExecutor.updateProvisions(
                    YP_HDD_MAN change 200 unit GIGABYTES to "1" unit GIGABYTES,
                )
                val resourceTypes = updateProvisionsResponse.expandedProvider.resourceTypes
                val type = resourceTypes.find { it.resourceTypeId == YP_HDD }
                val frozenQuota = type?.sums?.frozenQuota
                assertEquals("3", frozenQuota?.readableAmount)
                assertEquals("3", frozenQuota?.rawAmount)
                assertEquals("3", frozenQuota?.forEditAmount)
                assertEquals("3", frozenQuota?.amountInMinAllowedUnit)
            }
    }

    @Test
    fun provideVeryLargeBinaryAmount() {
        val resourceKey = "test_ram_42"
        val amount = 1152921504606846976L //1 exbibyte
        val resourceTypeModel = FrontTransferRequestsControllerTest.resourceTypeModel(
            TEST_ACCOUNT_1.providerId, resourceKey, UnitsEnsembleIds.STORAGE_UNITS_BINARY_ID)
        val resourceModel = FrontTransferRequestsControllerTest.resourceModel(
            TEST_ACCOUNT_1.providerId, resourceKey, resourceTypeModel.id,
            setOf(), UnitsEnsembleIds.STORAGE_UNITS_BINARY_ID, setOf(GIBIBYTES, TEBIBYTES, PEBIBYTES, EXBIBYTES),
            GIBIBYTES, BINARY_BYTES, TEST_ACCOUNT_1.accountsSpacesId.orElse(null))
        val quotaModel = QuotaModel.builder()
            .tenantId(Tenants.DEFAULT_TENANT_ID)
            .providerId(TEST_ACCOUNT_1.providerId)
            .resourceId(resourceModel.id)
            .folderId(TEST_FOLDER_1_ID)
            .quota(amount)
            .balance(amount)
            .frozenQuota(0L)
            .build()
        val block = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            unitsEnsemblesDao.getAll(session).collectList()
        }.block()


        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                quotasDao.upsertOneRetryable(txSession, quotaModel)
            }
        }.block()

        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceTypeModel)
            }
        }.block()

        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE) { txSession: YdbTxSession? ->
                resourcesDao.upsertResourceRetryable(txSession, resourceModel)
            }
        }.block()

        val amountInGiB = amount ushr 30
        helperFactory
            .newHelper(TEST_FOLDER_1_SERVICE_ID, TEST_FOLDER_1_ID, TEST_ACCOUNT_1.providerId, TEST_ACCOUNT_1.id)
            .build()
            .withProviderAnswer(
                resourceKey provide amount unit "bytes"
            ) { provisionExecutor ->
                val updateProvisionsResponse = provisionExecutor.updateProvisions(
                    resourceModel.id change 0 unit GIBIBYTES to amountInGiB.toString() unit GIBIBYTES,
                )
                val resourceTypes = updateProvisionsResponse.expandedProvider.resourceTypes
                val type = resourceTypes.find { it.resourceTypeId == resourceTypeModel.id }!!
                assertEquals("0", type.sums.frozenQuota.rawAmount)
                assertEquals("0", type.sums.balance.rawAmount)
                assertEquals("1", type.sums.quota.readableAmount)
                assertEquals("EiB", type.sums.quota.readableUnit)

                val resource = updateProvisionsResponse.expandedProvider.accounts.first().resources.first()
                assertEquals("1", resource.provided.readableAmount)
                assertEquals("EiB", resource.provided.readableUnit)
                assertEquals(amount.toString(), resource.provided.rawAmount)
                assertEquals("B", resource.provided.rawUnit)
            }
    }

    /**
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController.updateProvisionDryRun
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController.updateProvision
     */
    @Test
    fun updateProvisionsErrorOnWrongUpdatedProvisionsInconvertibleToApiUnitTest() {
        tableClient.usingSessionMonoRetryable { s -> s.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) {
                ts ->
            resourcesDao.getById(ts, YP_HDD_MAN, Tenants.DEFAULT_TENANT_ID).flatMap { r -> r.map { resource ->
            resourcesDao.updateResourceRetryable(ts, ResourceModel.builder(resource)
                .resourceUnits(ResourceUnitsModel.builder(resource.resourceUnits)
                    .addAllowedUnitId(BYTES)
                    .providerApiUnitId(KILOBYTES)
                    .build())
                .build())
            }.orElseThrow() } }
        }.block()

        val dryRunResponse = webClient
            .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
            .post()
            .uri("/front/quotas/_updateProvisionDryRun")
            .bodyValue(
                UpdateProvisionDryRunRequestDto(
                    YP_HDD_MAN,  // resourceId
                    UpdateProvisionDryRunAmounts(
                        "200000000000",  // quota
                        "0",  // balance
                        "200000000000",  // provided
                        "0",  // allocated
                        BYTES // forEditUnitId
                    ),  // oldAmounts
                    OldEditFormFields(
                        "200000000000",  // providedAbsolute
                        "0",  // providedDelta
                        BYTES,  // forEditUnitId
                        "200000000000",  // providedAbsoluteInMinAllowedUnit
                        BYTES // minAllowedUnitId
                    ),  // oldFormFields,
                    ChangedEditFormField(
                        null,  // providedAbsolute
                        "-1",  // providedDelta
                        null // forEditUnitId
                    ), EditedField.DELTA,  // editedField
                    null
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UpdateProvisionDryRunAnswerDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(
            messages.getMessage("errors.value.can.not.be.converted.to.providers.api.unit", null, ENGLISH),
            dryRunResponse.validationMessages.fieldMessages["newProvided"]!!.first().message
        )


        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
            .post()
            .uri("/front/quotas/_updateProvisions")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(
                UpdateProvisionsRequestDto.builder()
                    .setAccountId(TEST_ACCOUNT_1_ID)
                    .setFolderId(TEST_FOLDER_1_ID) // folder in Dispenser
                    .setServiceId(TestServices.TEST_SERVICE_ID_DISPENSER) // Dispenser
                    .setUpdatedProvisions(listOf(
                        ProvisionLiteDto(
                            YP_HDD_MAN,  // resourceId
                            "999",  // provided amount
                            BYTES,  // provided amount unit key
                            "200",  // old provided amount
                            GIGABYTES // old provided amount unit id
                        )
                    )).build()
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertFalse(result.fieldErrors.isEmpty())
        assertEquals(
            messages.getMessage("errors.value.can.not.be.converted.to.providers.api.unit", null, ENGLISH),
            result.fieldErrors["updatedProvisions.0.providedAmount"]!!.first()
        )
    }

    // todo Добавить тестов на не стандартные ответы провайдера:
    //  1. дополнительные ресурсы
    //  2. не существующие ресурсы
    //  3. величины в ответе не равны переданным
}
