package ru.yandex.intranet.d.web.front.folders

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import com.yandex.ydb.table.transaction.TransactionMode.ONLINE_READ_ONLY
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.backend.service.provider_proto.Account
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.Amount
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CurrentVersion
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID
import ru.yandex.intranet.d.backend.service.provider_proto.Provision
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse
import ru.yandex.intranet.d.backend.service.provider_proto.UserID
import ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.CreateAccountExpandedAnswerDto
import ru.yandex.intranet.d.web.model.folders.FrontAccountInputDto
import ru.yandex.intranet.d.web.model.folders.FrontFolderWithQuotesDto
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccount
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccountResource
import ru.yandex.intranet.d.web.model.folders.front.ExpandedFolder
import ru.yandex.intranet.d.web.model.folders.front.ExpandedProvider
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResource
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResourceType
import ru.yandex.intranet.d.web.model.folders.front.history.FrontFolderOperationLogDto
import ru.yandex.intranet.d.web.model.folders.front.history.FrontFolderOperationLogPageDto
import ru.yandex.intranet.d.web.model.quotas.ProvisionLiteDto
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionsAnswerDto
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionsRequestDto
import java.time.Instant
import java.util.*
import java.util.function.Consumer

typealias ResourceTypeKey = String
typealias ResourceId = String
typealias SegmentationKey = String
typealias SegmentationId = String
typealias SegmentKey = String
typealias SegmentId = String
typealias UnitKey = String
typealias AccountId = String

data class ResponseQuotaAmount(
    val value: Long,
    val unitKey: String
)

data class RequestQuotaAmount(
    val value: String,
    val unitId: String
)

data class RequestQuotaChange(
    val resourceId: ResourceId,
    val oldQuota: RequestQuotaAmount,
    val newQuota: RequestQuotaAmount
)

infix fun String.change(value: Int): RequestQuotaAmountBuilder.StageOldValue {
    return RequestQuotaAmountBuilder(this).StageOldValue(value.toLong())
}

class RequestQuotaAmountBuilder(val resourceId: ResourceId) {
    inner class StageOldValue(val oldValue: Long) {
        infix fun unit(unitId: String): StageOldUnit {
            return StageOldUnit(unitId)
        }

        inner class StageOldUnit(val oldUnitId: String) {
            infix fun to(value: String): StageNewValue {
                return StageNewValue(value)
            }

            inner class StageNewValue(val newValue: String) {
                infix fun unit(newUnitId: String): RequestQuotaChange {
                    return RequestQuotaChange(
                        resourceId,
                        RequestQuotaAmount(oldValue.toString(), oldUnitId),
                        RequestQuotaAmount(newValue, newUnitId)
                    )
                }
            }
        }
    }
}

infix fun String.segment(segmentationId: SegmentationId): ProviderResponseBuilder.StageSegments.StageSegmentation {
    return ProviderResponseBuilder(this).StageSegments(arrayOf()).StageSegmentation(segmentationId)
}

infix fun String.provide(value: Long): ProviderResponseBuilder.StageSegments.ValueStage {
    return ProviderResponseBuilder(this).StageSegments(arrayOf()).ValueStage(value)
}

class ProviderResponseBuilder(val resourceTypeKey: ResourceTypeKey) {
    interface Result {
        fun getResourceTypeKey(): ResourceTypeKey
        fun getSegments(): Array<Pair<SegmentationKey, SegmentKey>>
        fun getValue(): Long
        fun getUnitKey(): UnitKey
    }

    inner class StageSegments(val segments: Array<Pair<SegmentationKey, SegmentKey>>) {
        infix fun and(segmentationKey: SegmentationKey): StageSegmentation {
            return StageSegmentation(segmentationKey)
        }

        inner class StageSegmentation(val segmentationKey: SegmentationKey) {
            infix fun to(segmentKey: SegmentKey): StageSegments {
                return StageSegments(segments.plusElement(segmentationKey to segmentKey))
            }
        }

        infix fun provide(value: Long): ValueStage {
            return ValueStage(value)
        }

        inner class ValueStage(val value: Long) {
            infix fun unit(unitKey: UnitKey): Result {
                return object : Result {
                    override fun getResourceTypeKey(): ResourceTypeKey {
                        return resourceTypeKey
                    }

                    override fun getSegments(): Array<Pair<SegmentationKey, SegmentKey>> {
                        return segments
                    }

                    override fun getValue(): Long {
                        return value
                    }

                    override fun getUnitKey(): UnitKey {
                        return unitKey
                    }

                }
            }
        }
    }
}

infix fun Int.unit(unitKey: String): ResponseQuotaAmount {
    return ResponseQuotaAmount(this.toLong(), unitKey)
}

infix fun Long.unit(unitKey: String): ResponseQuotaAmount {
    return ResponseQuotaAmount(this, unitKey)
}

interface RequestExecutor {
    fun updateProvisions(vararg quotaChanges: RequestQuotaChange): UpdateProvisionsAnswerDto
    fun createAccount(): CreateAccountExpandedAnswerDto
    fun getFolderQuotas(): ExpandedFolder
    fun getFolderHistory(): MutableList<FrontFolderOperationLogDto>
    fun getProvision(accountId: AccountId, resourceId: ResourceId): AccountsQuotasModel?
    fun commonAssert(
        updateProvisionsResponse: UpdateProvisionsAnswerDto, resourceId: String, expectedProvidedRawAmount: String
    )
}

@Component
class UpdateProvisionTestHelperFactory {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var stubProviderService: StubProviderService

    @Autowired
    private lateinit var ydbTableClient: YdbTableClient

    @Autowired
    private lateinit var accountsQuotasDao: AccountsQuotasDao

    fun newHelper(
        serviceId: Long,
        testFolderId: String,
        testProviderId: String,
        testAccountId: String
    ): Builder {
        return Builder(serviceId, testFolderId, testProviderId, testAccountId)
    }

    inner class Builder(
        val serviceId: Long,
        val testFolderId: String,
        val testProviderId: String,
        val testAccountId: String,
        var retryOperation: Boolean = false,
    ) {
        fun withAccountsSpaceKey(vararg accountsSpaceKey: Pair<SegmentationKey, SegmentKey>): WithAccountsSpaceKey {
            return WithAccountsSpaceKey(accountsSpaceKey.toMap())
        }

        fun withRetryOperation(): Builder{
            retryOperation = true
            return this
        }

        fun build(): UpdateProvisionTestHelper {
            return withAccountsSpaceKey().build()
        }

        inner class WithAccountsSpaceKey(val accountsSpaceKey: Map<SegmentationKey, SegmentKey>) {
            fun build(): UpdateProvisionTestHelper {
                return UpdateProvisionTestHelper(
                    serviceId,
                    testFolderId,
                    testProviderId,
                    testAccountId,
                    accountsSpaceKey,
                    webClient,
                    stubProviderService,
                    ydbTableClient,
                    accountsQuotasDao,
                    retryOperation,
                )
            }
        }
    }
}

class UpdateProvisionTestHelper(
    val testServiceId: Long,
    val testFolderId: String,
    val testProviderId: String,
    val testAccountId: String,
    val accountsSpaceKey: Map<SegmentationKey, SegmentKey>,
    val webClient: WebTestClient,
    val stubProviderService: StubProviderService,
    val ydbTableClient: YdbTableClient,
    val accountsQuotasDao: AccountsQuotasDao,
    val retryOperation: Boolean,
) {
    fun withProviderAnswer(
        vararg provisions: ProviderResponseBuilder.Result,
        body: Consumer<RequestExecutor>
    ) {
        stubProviderService.reset()
        val builder = UpdateProvisionResponse.newBuilder();
        provisions.forEach {
            val resourceTypeKey = CompoundResourceKey.newBuilder().setResourceTypeKey(it.getResourceTypeKey())
            it.getSegments().forEach { segmentationToSegment ->
                resourceTypeKey.addResourceSegmentKeys(
                    ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey(segmentationToSegment.first)
                        .setResourceSegmentKey(segmentationToSegment.second)
                )
            }
            builder.addProvisions(
                Provision.newBuilder()
                    .setResourceKey(ResourceKey.newBuilder().setCompoundKey(resourceTypeKey))
                    .setProvided(
                        Amount.newBuilder()
                            .setValue(it.getValue())
                            .setUnitKey(it.getUnitKey())
                    )
                    .setLastUpdate(
                        LastUpdate.newBuilder()
                            .setAuthor(
                                UserID.newBuilder()
                                    .setPassportUid(PassportUID.newBuilder().setPassportUid("1"))
                                    .setStaffLogin(StaffLogin.newBuilder().setStaffLogin("test"))
                            )
                            .setOperationId(UUID.randomUUID().toString())
                            .setTimestamp(Timestamps.fromSeconds(Instant.now().epochSecond))
                    )
            )
        }
        val compoundAccountsSpaceKey = CompoundAccountsSpaceKey.newBuilder()
        accountsSpaceKey.forEach {
            compoundAccountsSpaceKey.addResourceSegmentKeys(
                ResourceSegmentKey.newBuilder()
                    .setResourceSegmentationKey(it.key)
                    .setResourceSegmentKey(it.value)
            )
        }
        builder.setAccountsSpaceKey(AccountsSpaceKey.newBuilder().setCompoundKey(compoundAccountsSpaceKey))
        val updateProvisionResponses = mutableListOf<GrpcResponse<UpdateProvisionResponse>>()
        if (retryOperation) {
            updateProvisionResponses.add(GrpcResponse.failure(StatusRuntimeException(Status.DEADLINE_EXCEEDED)))
        }
        updateProvisionResponses.add(GrpcResponse.success(builder.build()))
        stubProviderService.setUpdateProvisionResponses(updateProvisionResponses)

        val accountProvisions = provisions.map {
            val resourceTypeKey = CompoundResourceKey.newBuilder().setResourceTypeKey(it.getResourceTypeKey())
            it.getSegments().forEach { segmentationToSegment ->
                resourceTypeKey.addResourceSegmentKeys(
                    ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey(segmentationToSegment.first)
                        .setResourceSegmentKey(segmentationToSegment.second)
                )
            }
            Provision.newBuilder()
                .setResourceKey(ResourceKey.newBuilder().setCompoundKey(resourceTypeKey))
                .setProvided(
                    Amount.newBuilder()
                        .setValue(it.getValue())
                        .setUnitKey(it.getUnitKey())
                )
                .setLastUpdate(
                    LastUpdate.newBuilder()
                        .setAuthor(
                            UserID.newBuilder()
                                .setPassportUid(PassportUID.newBuilder().setPassportUid("1"))
                                .setStaffLogin(StaffLogin.newBuilder().setStaffLogin("test"))
                        )
                        .setOperationId(UUID.randomUUID().toString())
                        .setTimestamp(Timestamps.fromSeconds(Instant.now().epochSecond))
                )
                .build()
        }
        val createAccountResponses: MutableList<GrpcResponse<Account>> = mutableListOf()
        if (retryOperation) {
            createAccountResponses.add(GrpcResponse.failure(StatusRuntimeException(Status.DEADLINE_EXCEEDED)))
        }
        createAccountResponses.add(
            GrpcResponse.success(
                Account.newBuilder()
                    .setAccountId(UUID.randomUUID().toString())
                    .setDeleted(false)
                    .setFolderId(testFolderId)
                    .setDisplayName("outerDisplayName")
                    .setKey("outerKey")
                    .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
                    .setLastUpdate(
                        LastUpdate.newBuilder()
                            .setAuthor(
                                UserID.newBuilder()
                                    .setPassportUid(PassportUID.newBuilder().setPassportUid("uuid").build())
                                    .setStaffLogin(StaffLogin.newBuilder().setStaffLogin("login").build())
                                    .build()
                            )
                            .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                            .setOperationId(UUID.randomUUID().toString())
                            .build()
                    )
                    .addAllProvisions(accountProvisions)
                    .build()
            )
        )
        stubProviderService.setCreateAccountResponses(createAccountResponses)

        body.accept(object : RequestExecutor {
            override fun updateProvisions(vararg quotaChanges: RequestQuotaChange): UpdateProvisionsAnswerDto {
                val updatedProvisions = quotaChanges.map {
                    ProvisionLiteDto(
                        it.resourceId, it.newQuota.value, it.newQuota.unitId, it.oldQuota.value, it.oldQuota.unitId
                    )
                }
                return webClient
                    .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                    .post()
                    .uri("/front/quotas/_updateProvisions")
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(
                        UpdateProvisionsRequestDto.builder()
                            .setAccountId(testAccountId)
                            .setFolderId(testFolderId)
                            .setServiceId(testServiceId)
                            .setUpdatedProvisions(updatedProvisions).build()
                    )
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody<UpdateProvisionsAnswerDto>()
                    .returnResult()
                    .responseBody!!
            }

            override fun createAccount(): CreateAccountExpandedAnswerDto {
                val result = webClient
                    .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                    .post()
                    .uri("/front/accounts/_expanded")
                    .bodyValue(
                        FrontAccountInputDto(
                            testFolderId,
                            testProviderId,
                            null,
                            null,
                            null,
                            null,
                            null
                        )
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(CreateAccountExpandedAnswerDto::class.java)
                    .returnResult()
                    .responseBody

                return result!!
            }

            /**
             * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController.getFolders
             */
            override fun getFolderQuotas(): ExpandedFolder {
                val responseBody = webClient
                    .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                    .get()
                    .uri("/front/quotas/_folders?folderIds={id}", testFolderId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(FrontFolderWithQuotesDto::class.java)
                    .returnResult()
                    .responseBody

                return responseBody.folders[0]!!
            }

            override fun getFolderHistory(): MutableList<FrontFolderOperationLogDto> {
                val responseBody = webClient
                    .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                    .get()
                    .uri("/front/history/folders/{id}", testFolderId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(FrontFolderOperationLogPageDto::class.java)
                    .returnResult()
                    .responseBody
                return responseBody.page.items!!
            }

            override fun getProvision(accountId: AccountId, resourceId: ResourceId): AccountsQuotasModel? {
                return ydbTableClient.usingSessionMonoRetryable { session ->
                        accountsQuotasDao.getByIdStartTx(session.asTxCommitRetryable(ONLINE_READ_ONLY),
                            AccountsQuotasModel.Identity(accountId, resourceId), DEFAULT_TENANT_ID
                        )
                    }.block()?.get()?.orElse(null)
            }

            override fun commonAssert(
                updateProvisionsResponse: UpdateProvisionsAnswerDto,
                resourceId: String,
                expectedProvidedRawAmount: String
            ) {
                val expandedAccount = updateProvisionsResponse.expandedProvider
                    ?.accounts?.first { it.account.id.equals(testAccountId) }
                Assertions.assertEquals(expectedProvidedRawAmount,
                    expandedAccount
                        ?.resources?.first { it.resourceId.equals(resourceId) }
                        ?.provided?.rawAmount
                )
            }
        })

        stubProviderService.reset()
    }
}

fun ExpandedFolder.provider(providerId: String): ExpandedProvider? {
    return this.providers.firstOrNull { it.providerId == providerId }
}

fun ExpandedProvider.account(accountId: String): ExpandedAccount? {
    return this.accounts?.firstOrNull { it.account.id == accountId }
}

fun ExpandedAccount.resource(resourceId: String): ExpandedAccountResource? {
    return this.resources?.firstOrNull { it.resourceId == resourceId }
}

fun ExpandedProvider.resourceType(resourceTypeId: String): ExpandedResourceType? {
    return this.resourceTypes?.firstOrNull { it.resourceTypeId == resourceTypeId }
}

fun ExpandedResourceType.resource(resourceId: String): ExpandedResource? {
    return this.resources?.firstOrNull { it.resourceId == resourceId }
}
