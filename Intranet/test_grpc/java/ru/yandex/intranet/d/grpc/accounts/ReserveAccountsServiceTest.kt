package ru.yandex.intranet.d.grpc.accounts

import com.google.protobuf.Timestamp
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts
import ru.yandex.intranet.d.TestFolders
import ru.yandex.intranet.d.TestProviders
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.backend.service.proto.AccountKey
import ru.yandex.intranet.d.backend.service.proto.AccountName
import ru.yandex.intranet.d.backend.service.proto.AccountOperationStatus
import ru.yandex.intranet.d.backend.service.proto.AccountReserveType
import ru.yandex.intranet.d.backend.service.proto.AccountsServiceGrpc
import ru.yandex.intranet.d.backend.service.proto.CreateAccountRequest
import ru.yandex.intranet.d.backend.service.proto.GetOperationStateRequest
import ru.yandex.intranet.d.backend.service.proto.GetProviderReserveAccountsRequest
import ru.yandex.intranet.d.backend.service.proto.OperationStatus
import ru.yandex.intranet.d.backend.service.proto.OperationsServiceGrpc
import ru.yandex.intranet.d.backend.service.proto.ProviderAccountsSpace
import ru.yandex.intranet.d.backend.service.proto.ProvidersServiceGrpc
import ru.yandex.intranet.d.backend.service.proto.PutAccountRequest
import ru.yandex.intranet.d.backend.service.provider_proto.Account
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey
import ru.yandex.intranet.d.backend.service.provider_proto.CurrentVersion
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin
import ru.yandex.intranet.d.backend.service.provider_proto.UserID
import ru.yandex.intranet.d.grpc.MockGrpcUser
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import java.time.Instant
import java.util.*

/**
 * Reserve accounts GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class ReserveAccountsServiceTest(
    @Autowired private val stubProviderService: StubProviderService
) {

    @GrpcClient("inProcess")
    private lateinit var accountsService: AccountsServiceGrpc.AccountsServiceFutureStub
    @GrpcClient("inProcess")
    private lateinit var operationsService: OperationsServiceGrpc.OperationsServiceFutureStub
    @GrpcClient("inProcess")
    private lateinit var providersService: ProvidersServiceGrpc.ProvidersServiceFutureStub

    @Test
    fun testCreateAccountSuccess() = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("location")
                        .setResourceSegmentKey("vla")
                        .build())
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("segment")
                        .setResourceSegmentKey("default")
                        .build())
                    .build())
                .build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = CreateAccountRequest.newBuilder()
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setProviderId(TestProviders.YP_ID)
            .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
            .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
            .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TestAccounts.TEST_ACCOUNT_SPACE_1.id).build())
            .setFreeTier(false)
            .setReserveType(AccountReserveType.RESERVE_TYPE_PROVIDER)
            .build()
        val response = accountsService
            .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
            .createAccount(request)
            .await()
        Assertions.assertTrue(response.hasAccount())
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, response.account.folderId)
        Assertions.assertEquals(TestProviders.YP_ID, response.account.providerId)
        Assertions.assertEquals("outerKey", response.account.externalKey.value)
        Assertions.assertEquals("outerDisplayName", response.account.displayName.value)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, response.account.accountsSpace.id)
        Assertions.assertNotEquals("", response.account.id)
        Assertions.assertNotEquals("", response.account.externalId)
        Assertions.assertNotEquals("", response.operationId)
        Assertions.assertFalse(response.account.freeTier)
        Assertions.assertEquals(AccountReserveType.RESERVE_TYPE_PROVIDER, response.account.reserveType)
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_SUCCESS, response.operationStatus)
        val operationStateRequest = GetOperationStateRequest.newBuilder()
            .setOperationId(response.operationId)
            .build()
        val operationState = operationsService
            .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
            .getOperationState(operationStateRequest)
            .await()
        Assertions.assertNotNull(operationState)
        Assertions.assertEquals(response.operationId, operationState.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationState.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationState.accountsSpaceId.id)
        Assertions.assertEquals(OperationStatus.SUCCESS, operationState.status)
        Assertions.assertFalse(operationState.hasFailure())
        val reserveAccounts = providersService
            .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
            .getProviderReserveAccounts(GetProviderReserveAccountsRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .build())
            .await()
        Assertions.assertEquals(1, reserveAccounts.reserveAccountsList.size)
    }

    @Test
    fun testPutAccountSuccess() = runBlocking {
        stubProviderService.setCreateAccountResponses(listOf(GrpcResponse.success(Account.newBuilder()
            .setAccountId(UUID.randomUUID().toString())
            .setDeleted(false)
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setDisplayName("outerDisplayName")
            .setKey("outerKey")
            .setVersion(CurrentVersion.newBuilder().setVersion(1000L).build())
            .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("location")
                        .setResourceSegmentKey("vla")
                        .build())
                    .addResourceSegmentKeys(ResourceSegmentKey.newBuilder()
                        .setResourceSegmentationKey("segment")
                        .setResourceSegmentKey("default")
                        .build())
                    .build())
                .build())
            .setLastUpdate(LastUpdate.newBuilder()
                .setAuthor(UserID.newBuilder()
                    .setPassportUid(PassportUID.newBuilder()
                        .setPassportUid(TestUsers.USER_1_UID).build())
                    .setStaffLogin(StaffLogin.newBuilder()
                        .setStaffLogin(TestUsers.USER_1_LOGIN).build())
                    .build())
                .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build())
                .build()).build())))
        val request = CreateAccountRequest.newBuilder()
            .setFolderId(TestFolders.TEST_FOLDER_1_ID)
            .setProviderId(TestProviders.YP_ID)
            .setExternalKey(AccountKey.newBuilder().setValue("outerKey").build())
            .setDisplayName(AccountName.newBuilder().setValue("outerDisplayName").build())
            .setAccountsSpace(ProviderAccountsSpace.newBuilder().setId(TestAccounts.TEST_ACCOUNT_SPACE_1.id).build())
            .setFreeTier(false)
            .build()
        val response = accountsService
            .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
            .createAccount(request)
            .await()
        Assertions.assertTrue(response.hasAccount())
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, response.account.folderId)
        Assertions.assertEquals(TestProviders.YP_ID, response.account.providerId)
        Assertions.assertEquals("outerKey", response.account.externalKey.value)
        Assertions.assertEquals("outerDisplayName", response.account.displayName.value)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, response.account.accountsSpace.id)
        Assertions.assertNotEquals("", response.account.id)
        Assertions.assertNotEquals("", response.account.externalId)
        Assertions.assertNotEquals("", response.operationId)
        Assertions.assertFalse(response.account.freeTier)
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_SUCCESS, response.operationStatus)
        val operationStateRequest = GetOperationStateRequest.newBuilder()
            .setOperationId(response.operationId)
            .build()
        val operationState = operationsService
            .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
            .getOperationState(operationStateRequest)
            .await()
        Assertions.assertNotNull(operationState)
        Assertions.assertEquals(response.operationId, operationState.id)
        Assertions.assertEquals(TestProviders.YP_ID, operationState.providerId)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, operationState.accountsSpaceId.id)
        Assertions.assertEquals(OperationStatus.SUCCESS, operationState.status)
        Assertions.assertFalse(operationState.hasFailure())
        val putResponse = accountsService
            .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
            .putAccount(PutAccountRequest.newBuilder()
                .setAccountId(response.account.id)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setVersion(response.account.version)
                .setReserveType(AccountReserveType.RESERVE_TYPE_PROVIDER)
                .build())
            .await()
        Assertions.assertTrue(putResponse.hasAccount())
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, putResponse.account.folderId)
        Assertions.assertEquals(TestProviders.YP_ID, putResponse.account.providerId)
        Assertions.assertEquals("outerKey", putResponse.account.externalKey.value)
        Assertions.assertEquals("outerDisplayName", putResponse.account.displayName.value)
        Assertions.assertEquals(TestAccounts.TEST_ACCOUNT_SPACE_1.id, putResponse.account.accountsSpace.id)
        Assertions.assertNotEquals("", putResponse.account.id)
        Assertions.assertNotEquals("", putResponse.account.externalId)
        Assertions.assertNotEquals("", putResponse.operationId)
        Assertions.assertFalse(putResponse.account.freeTier)
        Assertions.assertEquals(AccountReserveType.RESERVE_TYPE_PROVIDER, putResponse.account.reserveType)
        Assertions.assertEquals(AccountOperationStatus.ACCOUNT_OPERATION_SUCCESS, putResponse.operationStatus)
        val reserveAccounts = providersService
            .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
            .getProviderReserveAccounts(GetProviderReserveAccountsRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .build())
            .await()
        Assertions.assertEquals(1, reserveAccounts.reserveAccountsList.size)
    }

}
