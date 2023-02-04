package ru.yandex.intranet.d.web.api.transfers

import com.google.protobuf.util.Timestamps
import com.yandex.ydb.table.transaction.TransactionMode
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import reactor.util.function.Tuples
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.backend.service.provider_proto.Account
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.Amount
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CurrentVersion
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate
import ru.yandex.intranet.d.backend.service.provider_proto.MoveProvisionResponse
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID
import ru.yandex.intranet.d.backend.service.provider_proto.Provision
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin
import ru.yandex.intranet.d.backend.service.provider_proto.UserID
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.services.ServicesDao
import ru.yandex.intranet.d.dao.transfers.PendingTransferRequestsDao
import ru.yandex.intranet.d.dao.transfers.TransferRequestsDao
import ru.yandex.intranet.d.dao.units.UnitsEnsemblesDao
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao
import ru.yandex.intranet.d.dao.users.UsersDao
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.units.UnitsEnsembleModel
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub
import ru.yandex.intranet.d.services.tracker.TrackerClientStub
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.folderModel
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.providerModel
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.quotaModel
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceModel
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceSegmentModel
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceSegmentationModel
import ru.yandex.intranet.d.web.api.transfers.PublicApiTransferRequestsControllerTest.resourceTypeModel
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateProvisionTransferDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateQuotaResourceTransferDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestParametersDto
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestVotingDto
import java.time.Instant
import java.util.*

@Component
class PublicApiTransferRequestsHelper(
    @Value("\${abc.roles.quotaManager}")
    val quotaManagerRoleId: Long,
    @Value("\${abc.roles.responsibleOfProvider}")
    val responsibleOfProvider: Long
) {

    @Autowired
    lateinit var webClient: WebTestClient
    @Autowired
    lateinit var providersDao: ProvidersDao
    @Autowired
    lateinit var resourceTypesDao: ResourceTypesDao
    @Autowired
    lateinit var resourceSegmentationsDao: ResourceSegmentationsDao
    @Autowired
    lateinit var resourceSegmentsDao: ResourceSegmentsDao
    @Autowired
    lateinit var resourcesDao: ResourcesDao
    @Autowired
    lateinit var quotasDao: QuotasDao
    @Autowired
    lateinit var folderOperationLogDao: FolderOperationLogDao
    @Autowired
    lateinit var folderDao: FolderDao
    @Autowired
    lateinit var tableClient: YdbTableClient
    @Autowired
    lateinit var mailSender: NotificationMailSenderStub
    @Autowired
    lateinit var accountsDao: AccountsDao
    @Autowired
    lateinit var accountQuotaDao: AccountsQuotasDao
    @Autowired
    lateinit var unitsEnsemblesDao: UnitsEnsemblesDao
    @Autowired
    lateinit var accountsSpaceDao: AccountsSpacesDao
    @Autowired
    lateinit var stubProviderService: StubProviderService
    @Autowired
    lateinit var pendingTransferRequestsDao: PendingTransferRequestsDao
    @Autowired
    lateinit var transferRequestDao: TransferRequestsDao
    @Autowired
    lateinit var accountsQuotasOperationDao: AccountsQuotasOperationsDao
    @Autowired
    lateinit var trackerClientStub: TrackerClientStub
    @Autowired
    lateinit var servicesDao: ServicesDao
    @Autowired
    lateinit var usersDao: UsersDao
    @Autowired
    lateinit var abcServiceMemberDao: AbcServiceMemberDao

    data class AccountWithQuota(
        val account: AccountModel,
        val accountQuota: AccountsQuotasModel
    )

    data class FolderWithQuota(
        val folder: FolderModel,
        val folderQuota: QuotaModel
    )

    data class Data(
        val provider: ProviderModel,
        val resourceType: ResourceTypeModel,
        val locationSegmentation: ResourceSegmentationModel,
        val locationSegment: ResourceSegmentModel,
        val resource: ResourceModel,
        val unitsEnsemble: UnitsEnsembleModel,
        val folderOne: FolderModel,
        val folderTwo: FolderModel,
        val quotaOne: QuotaModel,
        val quotaTwo: QuotaModel,
        val accountOne: AccountModel,
        val accountTwo: AccountModel,
        val accountQuotaOne: AccountsQuotasModel,
        val accountQuotaTwo: AccountsQuotasModel,
        val accountSpaceModel: AccountSpaceModel?
    )

    data class MinimalData(
        val provider: ProviderModel,
        val resourceType: ResourceTypeModel,
        val resource: ResourceModel,
        val folderOne: FolderModel,
        val folderTwo: FolderModel,
        val quotaOne: QuotaModel
    )

    companion object {
        @JvmStatic
        fun accountModel(folderId: String, providerId: String, accountSpaceId: String? = null): AccountModel {
            val id = UUID.randomUUID().toString()
            return AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(id)
                .setVersion(0L)
                .setProviderId(providerId)
                .setOuterAccountIdInProvider(id)
                .setOuterAccountKeyInProvider(id)
                .setFolderId(folderId)
                .setDisplayName("Test")
                .setDeleted(false)
                .setLastAccountUpdate(Instant.now())
                .setLastReceivedVersion(0L)
                .setLatestSuccessfulAccountOperationId(id)
                .setAccountsSpacesId(accountSpaceId)
                .setFreeTier(false)
                .build()
        }

        @JvmStatic
        fun accountSpaceModel(
            providerId: String,
            segments: Set<ResourceSegmentSettingsModel> = setOf()
        ): AccountSpaceModel {
            val id = UUID.randomUUID().toString()
            return AccountSpaceModel.newBuilder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(id)
                .setVersion(0L)
                .setProviderId(providerId)
                .setNameEn("Test")
                .setNameRu("Test")
                .setDescriptionEn("Test")
                .setDescriptionRu("Test")
                .setDeleted(false)
                .setReadOnly(false)
                .setSegments(segments)
                .setOuterKeyInProvider("Test_$id")
                .build()
        }

        @JvmStatic
        fun accountQuotaModel(accountId: String, folderId: String, resourceId: String, providerId: String,
                              provided: Long, allocated: Long, frozen: Long): AccountsQuotasModel {
            return AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setAccountId(accountId)
                .setResourceId(resourceId)
                .setProvidedQuota(provided)
                .setAllocatedQuota(allocated)
                .setFolderId(folderId)
                .setProviderId(providerId)
                .setLastProvisionUpdate(Instant.now())
                .setLastReceivedProvisionVersion(0L)
                .setLatestSuccessfulProvisionOperationId(UUID.randomUUID().toString())
                .setFrozenProvidedQuota(frozen)
                .build()
        }


        @JvmStatic
        fun provisionRequest(
            fromAccount: AccountModel,
            fromFolder: FolderModel,
            toAccount: AccountModel,
            toFolder: FolderModel,
            resource: ResourceModel,
            unitsEnsemble: UnitsEnsembleModel,
            provisionDelta: Long,
            addConfirmation: Boolean = false
        ): CreateTransferRequestDto {
            return CreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
                .addConfirmation(addConfirmation)
                .parameters(CreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(provisionTransfer(fromAccount, fromFolder, toAccount, toFolder, resource,
                        unitsEnsemble, provisionDelta))
                    .build())
                .build()
        }

        @JvmStatic
        fun provisionRequest(
            provisionTransfers: List<CreateProvisionTransferDto>,
            addConfirmation: Boolean = false
        ): CreateTransferRequestDto {
            return CreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
                .addConfirmation(addConfirmation)
                .parameters(CreateTransferRequestParametersDto.builder()
                    .addProvisionTransfers(provisionTransfers)
                    .build())
                .build()
        }

        @JvmStatic
        fun provisionTransfer(
            fromAccount: AccountModel,
            fromFolder: FolderModel,
            toAccount: AccountModel,
            toFolder: FolderModel,
            resource: ResourceModel,
            unitsEnsemble: UnitsEnsembleModel,
            provisionDelta: Long
        ): CreateProvisionTransferDto {
            val unitById = unitsEnsemble.unitById(resource.baseUnitId)
            Assertions.assertTrue(unitById.isPresent)
            val unitKey = unitById.get().key
            return CreateProvisionTransferDto(
                sourceAccountId = fromAccount.id,
                sourceFolderId = fromFolder.id,
                destinationAccountId = toAccount.id,
                destinationFolderId = toFolder.id,
                sourceAccountTransfers = listOf(
                    CreateQuotaResourceTransferDto(resource.providerId, resource.id, -provisionDelta,
                        unitKey)),
                destinationAccountTransfers = listOf(
                    CreateQuotaResourceTransferDto(resource.providerId, resource.id, provisionDelta,
                        unitKey)),
            )
        }
    }


    fun createRequestResponse(
        mockUser: MockUser,
        createTransferRequestDto: CreateTransferRequestDto
    ) = webClient
        .mutateWith(mockUser)
        .post()
        .uri("/api/v1/transfers")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(createTransferRequestDto)
        .exchange()

    fun createRequest(
        createTransferRequestDto: CreateTransferRequestDto,
        mockUser: MockUser
    ): TransferRequestDto {
        return createRequestResponse(mockUser, createTransferRequestDto)
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun getRequest(
        requestId: String,
        mockUser: MockUser
    ): TransferRequestDto {
        return webClient
            .mutateWith(mockUser)
            .get()
            .uri("/api/v1/transfers/{id}", requestId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun voteRequest(
        res: TransferRequestDto,
        transferRequestVotingDto: TransferRequestVotingDto,
        mockUser: MockUser
    ): TransferRequestDto {
        return voteRequestResponse(mockUser, res, transferRequestVotingDto)
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun voteRequestResponse(
        mockUser: MockUser,
        res: TransferRequestDto,
        voteParams: TransferRequestVotingDto
    ) = webClient
        .mutateWith(mockUser)
        .post()
        .uri("/api/v1/transfers/{id}/_vote?version={version}", res.id, res.version)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(voteParams)
        .exchange()

    fun prepareData(
        sourceQuota: Long = 500,
        sourceBalance: Long = 0,
        sourceProvided: Long = 200,
        destinationQuota: Long = 450,
        destinationBalance: Long = 50,
        destinationProvided: Long = 100,
        upsertSourceQuota: Boolean = true,
        upsertDestinationQuota: Boolean = true
    ): Data {
        val provider = createProvider()
        val resourceType = resourceTypeModel(provider.id, "test",
            "b02344bf-96af-4cc5-937c-66a479989ce8")
        val unitsEnsemblesId = "b02344bf-96af-4cc5-937c-66a479989ce8"
        val locationSegmentation = resourceSegmentationModel(provider.id, "location")
        val vlaSegment = resourceSegmentModel(locationSegmentation.id, "VLA")
        val segmentIds = setOf(Tuples.of(locationSegmentation.id, vlaSegment.id))
        val segments = segmentIds.map { ResourceSegmentSettingsModel(it.t1, it.t2) }.toSet()
        val accountSpace = createAccountSpace(provider, segments)
        val resource = resourceModel(provider.id, "test", resourceType.id,
            segmentIds,
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
            "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            accountSpace.id)
        val (folderOne, quotaOne) = createFolderWithQuota(1L, resource, sourceQuota, sourceBalance, upsertSourceQuota)
        val (folderTwo, quotaTwo) = createFolderWithQuota(
            2L,
            resource,
            destinationQuota,
            destinationBalance,
            upsertDestinationQuota
        )
        val (accountOne, accountQuotaOne) = createAccountWithQuota(
            resource, folderOne, sourceProvided,
            allocated = if (upsertSourceQuota) 50 else 0,
            frozen = if (upsertSourceQuota) 50 else 0,
            accountSpace, upsertSourceQuota
        )
        val (accountTwo, accountQuotaTwo) = createAccountWithQuota(
            resource, folderTwo, destinationProvided,
            allocated = if (upsertDestinationQuota) 100 else 0, 0, accountSpace, upsertDestinationQuota
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, locationSegmentation)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, vlaSegment)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        val unitsEnsembleModel = tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                unitsEnsemblesDao.getById(txSession, unitsEnsemblesId, Tenants.DEFAULT_TENANT_ID)
            }
        }.block()!!.get()
        return Data(provider, resourceType, locationSegmentation, vlaSegment, resource, unitsEnsembleModel,
            folderOne, folderTwo, quotaOne, quotaTwo, accountOne, accountTwo, accountQuotaOne, accountQuotaTwo,
            accountSpace)
    }

    fun prepareDataForReserveTransfer(): MinimalData {
        val tuple = SecondMorePublicApiTransferRequestsControllerTest.prepareWithRoles(
            tableClient, servicesDao, abcServiceMemberDao, responsibleOfProvider, quotaManagerRoleId, usersDao,
            providersDao, resourceTypesDao, resourceSegmentationsDao, resourceSegmentsDao, resourcesDao, folderDao,
            quotasDao
        )

        return MinimalData(
            provider = tuple.t1, resourceType = tuple.t2, resource = tuple.t4,
            folderOne = tuple.t6, folderTwo = tuple.t5, quotaOne = tuple.t8
        )
    }

    fun setupMoveProvisionAnswers(
        sourceAccount: AccountModel,
        destAccount: AccountModel,
        expectedSourceProvided: Long,
        expectedSourceAllocated: Long,
        expectedDestProvided: Long,
        expectedDestAllocated: Long,
        resourceType: ResourceTypeModel,
        unitKey: String,
        segmentKey: String?,
        segmentationKey: String?,
        version: Long = 1L
    ) {
        stubProviderService.setMoveProvisionResponses(listOf(
            GrpcResponse.success(MoveProvisionResponse.newBuilder()
                .setSourceAccountVersion(CurrentVersion.newBuilder().setVersion(sourceAccount.version + 1L).build())
                .setDestinationAccountVersion(CurrentVersion.newBuilder().setVersion(destAccount.version + 1L).build())
                .setAccountsSpaceKey(toGrpcAccountsSpaceKey(segmentationKey, segmentKey))
                .addSourceProvisions(toGrpcProvision(expectedSourceProvided, expectedSourceAllocated,
                    resourceType, unitKey, version))
                .addDestinationProvisions(toGrpcProvision(expectedDestProvided, expectedDestAllocated,
                    resourceType, unitKey, version))
                .build()
            )))
    }

    fun setupGetAccountAnswers(
        sourceAccount: AccountModel,
        sourceQuotas: List<AccountsQuotasModel>,
        destAccount: AccountModel,
        destQuotas: List<AccountsQuotasModel>,
        resourceType: ResourceTypeModel,
        unitKey: String,
        segmentKey: String?,
        segmentationKey: String?
    ) {

        stubProviderService.setGetAccountResponses(listOf(
            GrpcResponse.success(toGrpcAccount(sourceAccount, sourceQuotas, resourceType, unitKey,
                segmentKey, segmentationKey)),
            GrpcResponse.success(toGrpcAccount(destAccount, destQuotas, resourceType, unitKey,
                segmentKey, segmentationKey))
        ))
    }

    fun toGrpcAccount(
        account: AccountModel,
        quotas: List<AccountsQuotasModel>,
        resourceType: ResourceTypeModel,
        unitKey: String,
        segmentKey: String?,
        segmentationKey: String?
    ): Account {
        val provisions = quotas.map { toGrpcProvision(it.providedQuota, it.allocatedQuota, resourceType, unitKey) }
        return Account.newBuilder()
            .setAccountId(account.id)
            .setDeleted(account.isDeleted)
            .setDisplayName(account.displayName.orElse("test"))
            .setFolderId(account.folderId)
            .setKey(account.outerAccountKeyInProvider.orElse("test"))
            .addAllProvisions(provisions)
            .setAccountsSpaceKey(toGrpcAccountsSpaceKey(segmentationKey, segmentKey))
            .build()
    }

    private fun toGrpcAccountsSpaceKey(segmentationKey: String?, segmentKey: String?): AccountsSpaceKey {
        val compoundSpaceKey = CompoundAccountsSpaceKey.newBuilder()
        if (segmentKey != null && segmentationKey != null) {
            compoundSpaceKey.addAllResourceSegmentKeys(listOf(
                ResourceSegmentKey.newBuilder()
                    .setResourceSegmentationKey(segmentationKey)
                    .setResourceSegmentKey(segmentKey)
                    .build()
            ))
        }
        return AccountsSpaceKey.newBuilder()
            .setCompoundKey(compoundSpaceKey.build())
            .build()
    }

    fun toGrpcProvision(
        provided: Long,
        allocated: Long,
        resourceType: ResourceTypeModel,
        unitKey: String,
        version: Long? = null
    ): Provision {
        val builder = Provision.newBuilder()
            .setResourceKey(ResourceKey.newBuilder()
                .setCompoundKey(CompoundResourceKey.newBuilder()
                    .setResourceTypeKey(resourceType.key)
                    .build())
                .build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID)
                        .build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN)
                        .build())
                    .build())
                .setOperationId(UUID.randomUUID().toString())
                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                .build())
            .setProvided(Amount.newBuilder()
                .setValue(provided)
                .setUnitKey(unitKey)
                .build())
            .setAllocated(Amount.newBuilder()
                .setValue(allocated)
                .setUnitKey(unitKey)
                .build())
        if (version != null) {
            builder.setVersion(CurrentVersion.newBuilder().setVersion(version).build())
        }
        return builder.build()
    }

    fun createAccountSpace(
        provider: ProviderModel,
        segments: Set<ResourceSegmentSettingsModel> = setOf()
    ): AccountSpaceModel {
        val accountSpaceModel = accountSpaceModel(provider.id, segments)
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                accountsSpaceDao.upsertOneRetryable(txSession, accountSpaceModel)
            }
        }.block()
        return accountSpaceModel
    }

    fun createAccountWithQuota(
        resource: ResourceModel,
        folder: FolderModel,
        provided: Long,
        allocated: Long,
        frozen: Long,
        accountSpaceModel: AccountSpaceModel? = null,
        upsertQuota: Boolean = true
    ): AccountWithQuota {
        val account = accountModel(folder.id, resource.providerId, accountSpaceModel?.id)
        val accountQuota = accountQuotaModel(account.id, folder.id, resource.id, resource.providerId,
            provided, allocated, frozen)
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        if (upsertQuota) {
            tableClient.usingSessionMonoRetryable { session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                    accountQuotaDao.upsertOneRetryable(txSession, accountQuota)
                }
            }.block()
        }
        return AccountWithQuota(account, accountQuota)
    }

    fun createFolderWithQuota(
        serviceId: Long,
        resource: ResourceModel,
        quota: Long,
        balance: Long,
        upsertQuota: Boolean = true
    ): FolderWithQuota {
        val folder = folderModel(serviceId)
        val quotaModel = quotaModel(resource.providerId, resource.id, folder.id,
            quota, balance)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        if (upsertQuota) {
            tableClient.usingSessionMonoRetryable { session: YdbSession ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                    quotasDao.upsertOneRetryable(txSession, quotaModel)
                }
            }.block()
        }
        return FolderWithQuota(folder, quotaModel)
    }

    fun createProvider(): ProviderModel {
        val provider = providerModel("in-process:test", null, true)
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        return provider
    }

    fun createResource(
        provider: ProviderModel,
        key: String,
        accountSpaceModel: AccountSpaceModel? = null
    ): Pair<ResourceModel, ResourceTypeModel> {
        val resourceType = resourceTypeModel(provider.id, key,
            "b02344bf-96af-4cc5-937c-66a479989ce8")
        val resource = resourceModel(provider.id, key, resourceType.id,
            setOf(),
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            setOf("b15101c2-da50-4d6f-9a8e-b90160871b0a", "cdcc3651-c116-440a-a65d-33b74675fe60",
                "24468eb0-6238-4240-beb6-0e8dc56fc62c", "74fe1983-144a-4156-8839-aa791cc2deb6"),
            "74fe1983-144a-4156-8839-aa791cc2deb6", "b15101c2-da50-4d6f-9a8e-b90160871b0a",
            accountSpaceModel?.id)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourcesDao.upsertResourceRetryable(txSession, resource)
            }
        }.block()
        return Pair(resource, resourceType)
    }

    fun createAccountsQuotas(
        accounts: List<AccountModel>,
        resource: ResourceModel,
        provided: Long,
        allocated: Long,
        frozen: Long,
    ): List<AccountsQuotasModel> {
        val quotas = accounts.map {
            accountQuotaModel(it.id, it.folderId, resource.id, resource.providerId, provided, allocated, frozen)
        }
        rwTx {
            accountQuotaDao.upsertAllRetryable(it, quotas)
        }
        return quotas
    }

    fun createFolderQuotas(
        folders: List<FolderModel>,
        resource: ResourceModel,
        quota: Long,
        balance: Long,
    ): List<QuotaModel> {
        val quotas = folders.map {
            quotaModel(resource.providerId, resource.id, it.id, quota, balance)
        }
        rwTx {
            quotasDao.upsertAllRetryable(it, quotas)
        }
        return quotas
    }

    fun <T> rwTx(f: (tx: YdbTxSession) -> Mono<T>): T? {
        return tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, f)
        }.block()
    }
}
