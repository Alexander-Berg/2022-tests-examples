package ru.yandex.intranet.d.services.sync

import com.google.protobuf.util.Timestamps
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import ru.yandex.intranet.d.TestFolders
import ru.yandex.intranet.d.TestProviders
import ru.yandex.intranet.d.backend.service.provider_proto.*
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService
import ru.yandex.intranet.d.loaders.providers.ProvidersLoader
import ru.yandex.intranet.d.model.providers.ProviderModel
import ru.yandex.intranet.d.web.security.model.YaUserDetails
import java.time.Instant
import java.util.List
import java.util.Set
import java.util.function.BiConsumer

/**
 * SyncTestHelper.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 25-06-2021
 */
private const val GRPC_URI = "in-process:test"
private const val ACCOUNT_OPERATION_ID = "35ed7bac-ab43-4c87-809b-c80159c04c7a"
private const val PROVISION_OPERATION_ID = "5fc8d1b7-4a68-4a97-8bbc-8781b40aa4fd"

@Component
class SyncTestHelper {
    @Autowired
    private lateinit var accountsSyncService: AccountsSyncService

    @Autowired
    private lateinit var providersLoader: ProvidersLoader

    @Autowired
    private lateinit var stubProviderService: StubProviderService

    fun initProviderStub(body: BiConsumer<ProviderModel, YaUserDetails>) {
        val nowEpochMilli = Instant.now().toEpochMilli()
        stubProviderService.setListAccountsResponses(listOf(GrpcResponse
            .success(ListAccountsResponse.newBuilder()
                .addAccounts(Account.newBuilder()
                    .setAccountId("sync-test-1")
                    .setDeleted(false)
                    .setDisplayName("sync-test-1")
                    .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                    .setKey("sync-test-1")
                    .setVersion(CurrentVersion.newBuilder().setVersion(0L).build())
                    .setLastUpdate(LastUpdate.newBuilder()
                        .setAuthor(UserID.newBuilder()
                            .setPassportUid(PassportUID.newBuilder()
                                .setPassportUid("1120000000000001")
                                .build())
                            .setStaffLogin(StaffLogin.newBuilder()
                                .setStaffLogin("login-1")
                                .build())
                            .build())
                        .setOperationId(ACCOUNT_OPERATION_ID)
                        .setTimestamp(Timestamps.fromMillis(nowEpochMilli))
                        .build())
                    .addProvisions(Provision.newBuilder()
                        .setResourceKey(ResourceKey.newBuilder()
                            .setCompoundKey(CompoundResourceKey.newBuilder()
                                .setResourceTypeKey("ram")
                                .addAllResourceSegmentKeys(listOf(
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey("location")
                                        .setResourceSegmentKey("sas")
                                        .build(),
                                    ResourceSegmentKey.newBuilder()
                                        .setResourceSegmentationKey("segment")
                                        .setResourceSegmentKey("default")
                                        .build(),
                                ))
                                .build())
                            .build())
                        .setLastUpdate(LastUpdate.newBuilder()
                            .setAuthor(UserID.newBuilder()
                                .setPassportUid(PassportUID.newBuilder()
                                    .setPassportUid("1120000000000001")
                                    .build())
                                .setStaffLogin(StaffLogin.newBuilder()
                                    .setStaffLogin("login-1")
                                    .build())
                                .build())
                            .setOperationId(PROVISION_OPERATION_ID)
                            .setTimestamp(Timestamps.fromMillis(nowEpochMilli))
                            .build())
                        .setProvided(Amount.newBuilder()
                            .setValue(2)
                            .setUnitKey("mebibytes")
                            .build())
                        .setAllocated(Amount.newBuilder()
                            .setValue(1)
                            .setUnitKey("mebibytes")
                            .build())
                        .setVersion(CurrentVersion.newBuilder().setVersion(0L).build())
                        .build())
                    .build())
                .setNextPageToken(AccountsPageToken.newBuilder().setToken("nextPageToken"))
                .build()),
            GrpcResponse.success(ListAccountsResponse.newBuilder()
                .setNextPageToken(AccountsPageToken.newBuilder().setToken("nextPageToken"))
                .build())
        ))

        val provider = ProviderModel.builder(
            providersLoader.getProviderByIdImmediate(TestProviders.YDB_ID, Tenants.DEFAULT_TENANT_ID).block()!!.get()
        )
            .grpcApiUri(GRPC_URI)
            .syncEnabled(true)
            .build()

        body.accept(provider, yaUserDetails(provider))

        stubProviderService.reset()
    }

    fun yaUserDetails(provider: ProviderModel) = YaUserDetails(
        "",// uid,
        provider.sourceTvmId, // tvmServiceId,
        "",// oauthClientId,
        "", // oauthClientName,
        emptySet(), // scopes,
        null, // user,
        List.of(provider), // providers,
        Set.of(SimpleGrantedAuthority(YaUserDetails.USER_ROLE)) // authorities
    )
}
