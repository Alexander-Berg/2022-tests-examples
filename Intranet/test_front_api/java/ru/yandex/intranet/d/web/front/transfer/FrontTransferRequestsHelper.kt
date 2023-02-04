package ru.yandex.intranet.d.web.front.transfer

import com.google.protobuf.util.Timestamps
import com.yandex.ydb.table.transaction.TransactionMode
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.UnitIds
import ru.yandex.intranet.d.UnitsEnsembleIds
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
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse
import ru.yandex.intranet.d.backend.service.provider_proto.UserID
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao
import ru.yandex.intranet.d.dao.accounts.AccountsSpacesDao
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao
import ru.yandex.intranet.d.dao.accounts.ProviderReserveAccountsDao
import ru.yandex.intranet.d.dao.folders.FolderDao
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao
import ru.yandex.intranet.d.dao.loans.LoansDao
import ru.yandex.intranet.d.dao.providers.ProvidersDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.ResourcesDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.dao.services.ServicesDao
import ru.yandex.intranet.d.dao.transfers.PendingTransferRequestsDao
import ru.yandex.intranet.d.dao.transfers.TransferRequestsDao
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao
import ru.yandex.intranet.d.dao.users.UsersDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.kotlin.getOrNull
import ru.yandex.intranet.d.model.accounts.AccountModel
import ru.yandex.intranet.d.model.accounts.AccountReserveType
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountKey
import ru.yandex.intranet.d.model.accounts.ProviderReserveAccountModel
import ru.yandex.intranet.d.model.folders.FolderModel
import ru.yandex.intranet.d.model.folders.FolderType
import ru.yandex.intranet.d.model.loans.LoanActionSubject
import ru.yandex.intranet.d.model.loans.LoanActionSubjects
import ru.yandex.intranet.d.model.loans.LoanAmount
import ru.yandex.intranet.d.model.loans.LoanAmounts
import ru.yandex.intranet.d.model.loans.LoanDueDate
import ru.yandex.intranet.d.model.loans.LoanModel
import ru.yandex.intranet.d.model.loans.LoanStatus
import ru.yandex.intranet.d.model.loans.LoanSubject
import ru.yandex.intranet.d.model.loans.LoanSubjectType
import ru.yandex.intranet.d.model.loans.LoanType
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.model.quotas.QuotaModel
import ru.yandex.intranet.d.model.resources.ResourceModel
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.model.users.AbcServiceMemberModel
import ru.yandex.intranet.d.model.users.AbcServiceMemberState
import ru.yandex.intranet.d.model.users.UserServiceRoles
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub
import ru.yandex.intranet.d.services.tracker.TrackerClientStub
import ru.yandex.intranet.d.services.transfer.TransferRequestIndicesService
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.folders.front.history.FrontFolderOperationLogPageDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateProvisionTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontPutTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferLoanBorrowParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferLoanParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferLoanPayOffParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestVotingDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Component
class FrontTransferRequestsHelper(
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
    lateinit var pendingTransferRequestsDao: PendingTransferRequestsDao
    @Autowired
    lateinit var accountsDao: AccountsDao
    @Autowired
    lateinit var accountQuotaDao: AccountsQuotasDao
    @Autowired
    lateinit var accountsSpaceDao: AccountsSpacesDao
    @Autowired
    lateinit var trackerClientStub: TrackerClientStub
    @Autowired
    lateinit var stubProviderService: StubProviderService
    @Autowired
    lateinit var accountsQuotasOperationDao: AccountsQuotasOperationsDao
    @Autowired
    lateinit var transferRequestDao: TransferRequestsDao
    @Autowired
    lateinit var transferRequestIndicesService: TransferRequestIndicesService
    @Autowired
    lateinit var operationsInProgressDao: OperationsInProgressDao
    @Autowired
    lateinit var servicesDao: ServicesDao
    @Autowired
    lateinit var usersDao: UsersDao
    @Autowired
    lateinit var abcServiceMemberDao: AbcServiceMemberDao
    @Autowired
    lateinit var providerReserveAccountsDao: ProviderReserveAccountsDao
    @Autowired
    lateinit var loansDao: LoansDao

    data class Data(
        val provider: ProviderModel,
        val resourceType: ResourceTypeModel,
        val locationSegmentation: ResourceSegmentationModel,
        val locationSegment: ResourceSegmentModel,
        val resource: ResourceModel,
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

    data class AccountWithQuota(
        val account: AccountModel,
        val accountQuota: AccountsQuotasModel
    )

    data class FolderWithQuota(
        val folder: FolderModel,
        val folderQuota: QuotaModel
    )

    companion object {
        @JvmStatic
        fun accountModel(
            folderId: String,
            providerId: String,
            accountSpaceId: String? = null,
            displayName: String = "Test",
            reserveType: AccountReserveType? = null,
        ): AccountModel {
            val id = UUID.randomUUID().toString()
            return AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(id)
                .setVersion(0L)
                .setProviderId(providerId)
                .setOuterAccountIdInProvider(id)
                .setOuterAccountKeyInProvider(id)
                .setFolderId(folderId)
                .setDisplayName(displayName)
                .setDeleted(false)
                .setLastAccountUpdate(Instant.now())
                .setLastReceivedVersion(null)
                .setLatestSuccessfulAccountOperationId(id)
                .setAccountsSpacesId(accountSpaceId)
                .setFreeTier(false)
                .setReserveType(reserveType)
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
        fun folderModel(
            serviceId: Long,
            displayName: String = "Test"
        ): FolderModel {
            return FolderModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setServiceId(serviceId)
                .setVersion(0L)
                .setDisplayName(displayName)
                .setDescription("Test")
                .setDeleted(false)
                .setFolderType(FolderType.COMMON)
                .setTags(emptySet())
                .setNextOpLogOrder(1L)
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
                .setLastReceivedProvisionVersion(null)
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
            provisionDelta: Long,
            addConfirmation: Boolean = false,
            provideOverCommitReserve: Boolean = false,
            borrowParameters: FrontTransferLoanBorrowParametersDto? = null,
            payOffParametersDto: FrontTransferLoanPayOffParametersDto? = null
        ): FrontCreateTransferRequestDto {
            return FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
                .addConfirmation(addConfirmation)
                .loanParameters(FrontTransferLoanParametersDto(borrowParameters, payOffParametersDto,
                    provideOverCommitReserve))
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(FrontCreateProvisionTransferDto(
                        sourceAccountId = fromAccount.id,
                        sourceFolderId = fromFolder.id,
                        sourceServiceId = fromFolder.serviceId.toString(),
                        destinationAccountId = toAccount.id,
                        destinationFolderId = toFolder.id,
                        destinationServiceId = toFolder.serviceId.toString(),
                        sourceAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(resource.id, (-provisionDelta).toString(),
                                resource.baseUnitId)),
                        destinationAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(resource.id, provisionDelta.toString(),
                                resource.baseUnitId)),
                    ))
                    .build())
                .build()
        }

        @JvmStatic
        fun provisionRequest(
            provisionTransfers: List<FrontCreateProvisionTransferDto>,
            addConfirmation: Boolean = false,
            provideOverCommitReserve: Boolean = false,
            borrowParameters: FrontTransferLoanBorrowParametersDto? = null,
            payOffParametersDto: FrontTransferLoanPayOffParametersDto? = null
        ): FrontCreateTransferRequestDto {
            return FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
                .addConfirmation(addConfirmation)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfers(provisionTransfers)
                    .build())
                .loanParameters(FrontTransferLoanParametersDto(borrowParameters, payOffParametersDto,
                    provideOverCommitReserve))
                .build()
        }

        @JvmStatic
        fun provisionTransfer(
            fromAccount: AccountModel,
            fromFolder: FolderModel,
            toAccount: AccountModel,
            toFolder: FolderModel,
            resource: ResourceModel,
            provisionDelta: Long,
        ) = FrontCreateProvisionTransferDto(
            sourceAccountId = fromAccount.id,
            sourceFolderId = fromFolder.id,
            sourceServiceId = fromFolder.serviceId.toString(),
            destinationAccountId = toAccount.id,
            destinationFolderId = toFolder.id,
            destinationServiceId = toFolder.serviceId.toString(),
            sourceAccountTransfers = listOf(
                FrontCreateQuotaResourceTransferDto(resource.id, (-provisionDelta).toString(),
                    resource.baseUnitId)),
            destinationAccountTransfers = listOf(
                FrontCreateQuotaResourceTransferDto(resource.id, provisionDelta.toString(),
                    resource.baseUnitId))
        )

        @JvmStatic
        fun provisionRequestPut(
            provisionDelta: Long,
            description: String? = null,
            addConfirmation: Boolean? = null,
            fromAccount: AccountModel? = null,
            fromFolder: FolderModel? = null,
            toAccount: AccountModel? = null,
            toFolder: FolderModel? = null,
            resource: ResourceModel? = null,
            provideOverCommitReserve: Boolean? = null,
            borrowParameters: FrontTransferLoanBorrowParametersDto? = null,
            ): FrontPutTransferRequestDto {
            val builder = FrontPutTransferRequestDto.builder()
            return with(builder) {
                description(description)
                if (addConfirmation != null) {
                    addConfirmation(addConfirmation)
                }
                if (fromAccount != null && toAccount != null && fromFolder != null && toFolder != null
                    && resource != null) {
                    val frontCreateProvisionTransferDto = FrontCreateProvisionTransferDto(
                        sourceAccountId = fromAccount.id,
                        sourceFolderId = fromFolder.id,
                        sourceServiceId = fromFolder.serviceId.toString(),
                        destinationAccountId = toAccount.id,
                        destinationFolderId = toFolder.id,
                        destinationServiceId = toFolder.serviceId.toString(),
                        sourceAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(resource.id, (-provisionDelta).toString(),
                                resource.baseUnitId)),
                        destinationAccountTransfers = listOf(
                            FrontCreateQuotaResourceTransferDto(resource.id, provisionDelta.toString(),
                                resource.baseUnitId)),
                    )
                    val params = FrontCreateTransferRequestParametersDto.builder()
                        .addProvisionTransfer(frontCreateProvisionTransferDto)
                        .build()
                    parameters(params)
                }
                loanParameters(FrontTransferLoanParametersDto(borrowParameters, null, provideOverCommitReserve))
                build()
            }
        }

        fun providerModel(
            grpcUri: String? = "in-process:test",
            restUri: String? = null,
            accountsSpacesSupported: Boolean = true,
            serviceId: Long = 69,
            retryCooldownDisabled: Boolean = true,
            moveProvisionSupported: Boolean = true,
            allocatedSupported: Boolean = true
        ): ProviderModel {
            return ProviderModel.builder()
                .id(UUID.randomUUID().toString())
                .grpcApiUri(grpcUri)
                .restApiUri(restUri)
                .destinationTvmId(42L)
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .sourceTvmId(42L)
                .serviceId(serviceId)
                .deleted(false)
                .readOnly(false)
                .multipleAccountsPerFolder(true)
                .accountTransferWithQuota(true)
                .managed(true)
                .key("test")
                .trackerComponentId(1L)
                .accountsSettings(AccountsSettingsModel.builder()
                    .displayNameSupported(true)
                    .keySupported(true)
                    .deleteSupported(true)
                    .softDeleteSupported(true)
                    .moveSupported(true)
                    .renameSupported(true)
                    .perAccountVersionSupported(true)
                    .perProvisionVersionSupported(true)
                    .perAccountLastUpdateSupported(true)
                    .perProvisionLastUpdateSupported(true)
                    .operationIdDeduplicationSupported(true)
                    .syncCoolDownDisabled(false)
                    .retryCoolDownDisabled(retryCooldownDisabled)
                    .accountsSyncPageSize(1000L)
                    .moveProvisionSupported(moveProvisionSupported)
                    .build())
                .importAllowed(true)
                .allocatedSupported(allocatedSupported)
                .accountsSpacesSupported(accountsSpacesSupported)
                .syncEnabled(true)
                .grpcTlsOn(true)
                .build()
        }

        @JvmStatic
        fun resourceModel(
            providerId: String,
            key: String,
            resourceTypeId: String,
            segments: Set<ResourceSegmentSettingsModel>,
            unitsEnsembleId: String,
            allowedUnitIds: Set<String>,
            defaultUnitId: String,
            baseUnitId: String,
            accountsSpaceId: String?,
            allocatedSupported: Boolean = false,
        ): ResourceModel {
            return ResourceModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId(unitsEnsembleId)
                .providerId(providerId)
                .resourceTypeId(resourceTypeId)
                .segments(segments)
                .resourceUnits(ResourceUnitsModel(allowedUnitIds, defaultUnitId, null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId(baseUnitId)
                .accountsSpacesId(accountsSpaceId)
                .allocatedSupported(allocatedSupported)
                .build()
        }

        @JvmStatic
        fun unitsByUnitsEnsembleId(unitsEnsembleId: String): List<String> = when (unitsEnsembleId) {
            UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID -> listOf(UnitIds.BYTES, UnitIds.KILOBYTES, UnitIds.MEGABYTES,
                UnitIds.GIGABYTES)
            UnitsEnsembleIds.CPU_UNITS_ID -> listOf(UnitIds.MILLICORES, UnitIds.CORES)
            else -> listOf()
        }

        @JvmStatic
        fun prepareLoan(
            userId: String,
            type: LoanType,
            sourceAccount: AccountModel,
            sourceFolder: FolderModel,
            destAccount: AccountModel,
            destFolder: FolderModel,
            loanSubjectType: LoanSubjectType,
            resource: ResourceModel,
            delta: Long
        ): LoanModel {
            val borrowedFrom = LoanSubject(
                loanSubjectType, sourceFolder.serviceId, null, sourceAccount.id,
                sourceFolder.id, sourceAccount.providerId, sourceAccount.accountsSpacesId.orElse(null)
            )
            val borrowedTo = LoanSubject(
                LoanSubjectType.SERVICE, destFolder.serviceId, null, destAccount.id,
                destFolder.id, destAccount.providerId, destAccount.accountsSpacesId.orElse(null)
            )
            val loanAmounts = LoanAmounts(listOf(LoanAmount(resource.id, delta.toBigInteger())))
            val dueDate = LocalDate.of(2077, 1, 1)
            return LoanModel(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                id = UUID.randomUUID().toString(),
                status = LoanStatus.PENDING,
                createdAt = Instant.now(),
                type = type,
                dueAt = LoanDueDate(dueDate, ZoneId.systemDefault()),
                settledAt = null,
                updatedAt = null,
                version = 0L,
                requestedBy = LoanActionSubject(userId, null),
                requestApprovedBy = LoanActionSubjects(listOf(LoanActionSubject(userId, null))),
                borrowTransferRequestId = UUID.randomUUID().toString(),
                borrowedFrom = borrowedFrom,
                borrowedTo = borrowedTo,
                payOffFrom = borrowedTo,
                payOffTo = borrowedFrom,
                borrowedAmounts = loanAmounts,
                payOffAmounts = loanAmounts,
                dueAmounts = loanAmounts,
                dueAtTimestamp = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        }
    }

    fun createRequestResponse(
        frontCreateTransferRequestDto: FrontCreateTransferRequestDto,
        mockUser: MockUser
    ) = webClient
        .mutateWith(mockUser)
        .post()
        .uri("/front/transfers")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(frontCreateTransferRequestDto)
        .exchange()

    fun createRequest(
        frontCreateTransferRequestDto: FrontCreateTransferRequestDto,
        mockUser: MockUser
    ): FrontSingleTransferRequestDto {
        return createRequestResponse(frontCreateTransferRequestDto, mockUser)
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun getRequest(
        requestId: String,
        mockUser: MockUser
    ): FrontSingleTransferRequestDto {
        return webClient
            .mutateWith(mockUser)
            .get()
            .uri("/front/transfers/{id}", requestId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun putRequest(
        res: FrontSingleTransferRequestDto,
        frontPutTransferRequestDto: FrontPutTransferRequestDto,
        mockUser: MockUser
    ): FrontSingleTransferRequestDto {
        return putRequestResponse(res, frontPutTransferRequestDto, mockUser)
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun putRequestResponse(
        res: FrontSingleTransferRequestDto,
        frontPutTransferRequestDto: FrontPutTransferRequestDto,
        mockUser: MockUser
    ) = webClient
        .mutateWith(mockUser)
        .put()
        .uri("/front/transfers/{id}?version={version}", res.transfer.id, res.transfer.version)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(frontPutTransferRequestDto)
        .exchange()

    fun voteRequest(
        res: FrontSingleTransferRequestDto,
        voteParams: FrontTransferRequestVotingDto,
        mockUser: MockUser
    ): FrontSingleTransferRequestDto {
        return voteRequestResponse(mockUser, res, voteParams)
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun voteRequestResponse(
        mockUser: MockUser,
        res: FrontSingleTransferRequestDto,
        voteParams: FrontTransferRequestVotingDto
    ) = webClient
        .mutateWith(mockUser)
        .post()
        .uri("/front/transfers/{id}/_vote?version={version}", res.transfer.id, res.transfer.version)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(voteParams)
        .exchange()

    fun cancelRequest(
        res: FrontSingleTransferRequestDto,
        mockUser: MockUser
    ): FrontSingleTransferRequestDto {
        return cancelRequestResponse(res, mockUser)
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun cancelRequestResponse(
        res: FrontSingleTransferRequestDto,
        mockUser: MockUser,
    ) = webClient
        .mutateWith(mockUser)
        .post()
        .uri("/front/transfers/{id}/_cancel?version={version}", res.transfer.id, res.transfer.version)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()

    fun folderHistoryRequest(
        folderId: String
    ): FrontFolderOperationLogPageDto {
        return webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/history/folders/{id}", folderId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderOperationLogPageDto::class.java)
            .returnResult()
            .responseBody!!
    }

    fun prepareData(
        accountsSpacesSupported: Boolean = true,
        allocatedSupported: Boolean = true,
        allocatedSupportedResource: Boolean = true,
        sourceQuota: Long = 500L, sourceBalance: Long = 0L,
        sourceProvided: Long = 200L, sourceAllocated: Long = 50L, sourceFrozen: Long = 50L,
        destQuota: Long = 450L, destBalance: Long = 50L,
        destProvided: Long = 100L, destAllocated: Long = 100L, destFrozen: Long = 0L,
    ): Data {
        val unitsEnsembleId = UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID
        val units = unitsByUnitsEnsembleId(unitsEnsembleId)
        val baseUnitId = units[0]

        val provider = createProvider(providerModel(accountsSpacesSupported = accountsSpacesSupported,
            allocatedSupported = allocatedSupported))
        val resourceType = FrontTransferRequestsControllerTest.resourceTypeModel(provider.id, "test",
            unitsEnsembleId)
        val locationSegmentation = FrontTransferRequestsControllerTest.resourceSegmentationModel(provider.id, "location")
        val vlaSegment = FrontTransferRequestsControllerTest.resourceSegmentModel(locationSegmentation.id, "VLA")
        val segmentIds = if (accountsSpacesSupported) setOf(locationSegmentation.id to vlaSegment.id) else setOf()
        val segments = segmentIds.map { (segmentation, segment) ->
            ResourceSegmentSettingsModel(segmentation, segment)
        }.toSet()
        val accountSpace = if (accountsSpacesSupported) createAccountSpace(provider, segments) else null
        val resource = resourceModel(provider.id, "test", resourceType.id,
            segments, unitsEnsembleId, units.toSet(), baseUnitId, baseUnitId,
            accountSpace?.id, allocatedSupported = allocatedSupportedResource)
        val (folderOne, quotaOne) = createFolderWithQuota(1L, resource, sourceQuota, sourceBalance)
        val (folderTwo, quotaTwo) = createFolderWithQuota(2L, resource, destQuota, destBalance)
        val (accountOne, accountQuotaOne) = createAccountWithQuota(resource, folderOne, sourceProvided, sourceAllocated,
            sourceFrozen, accountSpace)
        val (accountTwo, accountQuotaTwo) = createAccountWithQuota(resource, folderTwo, destProvided, destAllocated,
            destFrozen, accountSpace)
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
        return Data(provider, resourceType, locationSegmentation, vlaSegment, resource, folderOne, folderTwo,
            quotaOne, quotaTwo, accountOne, accountTwo, accountQuotaOne, accountQuotaTwo, accountSpace)
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

    fun setupUpdateProvisionAnswers(
        account: AccountModel,
        expectedProvided: Long,
        expectedAllocated: Long,
        resourceType: ResourceTypeModel,
        unitKey: String,
        segmentKey: String?,
        segmentationKey: String?,
        version: Long = 1L
    ) {
        stubProviderService.setUpdateProvisionResponses(listOf(
            GrpcResponse.success(UpdateProvisionResponse.newBuilder()
                .setAccountVersion(CurrentVersion.newBuilder().setVersion(account.version + 1L).build())
                .setAccountsSpaceKey(toGrpcAccountsSpaceKey(segmentationKey, segmentKey))
                .addProvisions(toGrpcProvision(expectedProvided, expectedAllocated,
                    resourceType, unitKey, version))
                .build()
            )))
    }

    fun addGetAccountAnswers(
        sourceAccount: AccountModel,
        sourceQuotas: List<AccountsQuotasModel>,
        resourceType: ResourceTypeModel,
        unitKey: String,
        segmentKey: String?,
        segmentationKey: String?
    ) {
        stubProviderService.addGetAccountResponse(sourceAccount.id,
            GrpcResponse.success(toGrpcAccount(sourceAccount, sourceQuotas, resourceType, unitKey,
                segmentKey, segmentationKey))
        )
    }

    fun addGetAccountAnswers(
        sourceAccount: AccountModel,
        sourceQuotas: List<AccountsQuotasModel>,
        destAccount: AccountModel,
        destQuotas: List<AccountsQuotasModel>,
        resourceType: ResourceTypeModel,
        unitKey: String,
        segmentKey: String?,
        segmentationKey: String?
    ) {
        stubProviderService.addGetAccountResponse(sourceAccount.id, GrpcResponse.success(toGrpcAccount(sourceAccount,
            sourceQuotas, resourceType, unitKey, segmentKey, segmentationKey)))
        stubProviderService.addGetAccountResponse(destAccount.id, GrpcResponse.success(toGrpcAccount(destAccount,
            destQuotas, resourceType, unitKey, segmentKey, segmentationKey)))
    }

    fun toGrpcAccount(
        account: AccountModel,
        quotas: List<AccountsQuotasModel>,
        resourceType: ResourceTypeModel,
        unitKey: String,
        segmentKey: String?,
        segmentationKey: String?
    ) = toGrpcAccountBuilder(account, quotas, resourceType, unitKey, segmentKey, segmentationKey).build()


    fun toGrpcAccountBuilder(
        account: AccountModel,
        quotas: List<AccountsQuotasModel>,
        resourceType: ResourceTypeModel,
        unitKey: String,
        segmentKey: String?,
        segmentationKey: String?
    ): Account.Builder {
        val provisions = quotas.map { toGrpcProvision(it.providedQuota, it.allocatedQuota, resourceType, unitKey) }
        return Account.newBuilder()
            .setAccountId(account.id)
            .setDeleted(account.isDeleted)
            .setDisplayName(account.displayName.orElse("test"))
            .setFolderId(account.folderId)
            .setKey(account.outerAccountKeyInProvider.orElse("test"))
            .addAllProvisions(provisions)
            .setAccountsSpaceKey(toGrpcAccountsSpaceKey(segmentationKey, segmentKey))
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
    ) = toGrpcProvisionBuilder(provided, allocated, resourceType, unitKey, version).build()

    fun toGrpcProvisionBuilder(
        provided: Long,
        allocated: Long,
        resourceType: ResourceTypeModel,
        unitKey: String,
        version: Long? = null
    ): Provision.Builder {
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
        return builder
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
        accountName: String = "Test",
        reserveType: AccountReserveType? = null
    ): AccountWithQuota {
        val account = accountModel(folder.id, resource.providerId, accountSpaceModel?.id, accountName, reserveType)
        val accountQuota = accountQuotaModel(account.id, folder.id, resource.id, resource.providerId,
            provided, allocated, frozen)
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                accountsDao.upsertOneRetryable(txSession, account)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                accountQuotaDao.upsertOneRetryable(txSession, accountQuota)
            }
        }.block()
        return AccountWithQuota(account, accountQuota)
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
            FrontTransferRequestsControllerTest.quotaModel(resource.providerId, resource.id, it.id, quota, balance)
        }
        rwTx {
            quotasDao.upsertAllRetryable(it, quotas)
        }
        return quotas
    }

    fun createFolderWithQuota(
        serviceId: Long,
        resource: ResourceModel,
        quota: Long,
        balance: Long,
        folderName: String = "Test"
    ): FolderWithQuota {
        val folder = folderModel(serviceId, folderName)
        val quotaModel = FrontTransferRequestsControllerTest.quotaModel(resource.providerId, resource.id, folder.id,
            quota, balance)
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                folderDao.upsertOneRetryable(txSession, folder)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                quotasDao.upsertOneRetryable(txSession, quotaModel)
            }
        }.block()
        return FolderWithQuota(folder, quotaModel)
    }

    fun prepareDataForReserveTransfer(): MinimalData {
        val tuple = SecondMoreFrontTransferRequestsControllerTest.prepareWithRoles(
            tableClient, servicesDao, abcServiceMemberDao, responsibleOfProvider, quotaManagerRoleId, usersDao,
            providersDao, resourceTypesDao, resourceSegmentationsDao, resourceSegmentsDao, resourcesDao, folderDao,
            quotasDao
        )

        return MinimalData(
            provider = tuple.t1, resourceType = tuple.t2, resource = tuple.t4,
            folderOne = tuple.t6, folderTwo = tuple.t5, quotaOne = tuple.t8
        )
    }

    fun createProvider(): ProviderModel {
        val provider = providerModel("in-process:test")
        return createProvider(provider)
    }

    fun createProvider(
        provider: ProviderModel
    ): ProviderModel {
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                providersDao.upsertProviderRetryable(txSession, provider)
            }
        }.block()
        return provider
    }

    fun attachProviderResponsible(
        provider: ProviderModel,
        userStaffId: Long
    ) = runBlocking {
        rwTxSuspend {
            val maxId = abcServiceMemberDao.getMaxId(it).awaitSingle().getOrNull() ?: 0L
            abcServiceMemberDao.upsertManyRetryable(it, listOf(AbcServiceMemberModel.newBuilder()
                .id(maxId + 1)
                .serviceId(provider.serviceId)
                .roleId(responsibleOfProvider)
                .staffId(userStaffId)
                .state(AbcServiceMemberState.ACTIVE)
                .build())).awaitSingleOrNull()
            val user = usersDao.getByStaffId(it, userStaffId, Tenants.DEFAULT_TENANT_ID).awaitSingle().orElseThrow()
            val roles = user.roles.toMutableMap()
            val providerServices = roles[UserServiceRoles.RESPONSIBLE_OF_PROVIDER] ?: setOf()
            roles[UserServiceRoles.RESPONSIBLE_OF_PROVIDER] = providerServices union setOf(provider.serviceId)
            usersDao.upsertUserRetryable(it, user.copyBuilder().roles(roles).build()).awaitSingleOrNull()
        }
    }

    fun createResource(
        provider: ProviderModel,
        key: String,
        accountSpaceModel: AccountSpaceModel? = null,
        allocatedSupported: Boolean = false,
        unitsEnsembleId: String = UnitsEnsembleIds.STORAGE_UNITS_DECIMAL_ID,
    ): Pair<ResourceModel, ResourceTypeModel> {
        val resourceType = FrontTransferRequestsControllerTest.resourceTypeModel(provider.id, key, unitsEnsembleId)
        val units = unitsByUnitsEnsembleId(unitsEnsembleId)
        val baseUnit = units[0]
        val resource = resourceModel(provider.id, key, resourceType.id,
            setOf(),
            unitsEnsembleId, units.toSet(), baseUnit, baseUnit,
            accountSpaceModel?.id, allocatedSupported = allocatedSupported)
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

    fun setReserveAccount(
        provider: ProviderModel,
        account: AccountModel,
        accountSpaceModel: AccountSpaceModel? = null,
    ) = runBlocking {
        rwTxSuspend {
            providerReserveAccountsDao.upsertOneRetryable(it, ProviderReserveAccountModel(ProviderReserveAccountKey(
                Tenants.DEFAULT_TENANT_ID, provider.id, accountSpaceModel?.id, account.id)))
        }
    }

    fun <T> rwTx(f: (tx: YdbTxSession) -> Mono<T>): T? {
        return tableClient.usingSessionMonoRetryable {
            it.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, f)
        }.block()
    }

    suspend inline fun <T> rwTxSuspend(crossinline f: suspend (tx: YdbTxSession) -> T): T? {
        return dbSessionRetryable(tableClient) { rwTxRetryable { f(txSession) } }
    }
}
