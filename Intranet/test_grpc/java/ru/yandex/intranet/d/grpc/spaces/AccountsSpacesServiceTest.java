package ru.yandex.intranet.d.grpc.spaces;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.backend.service.proto.AccountsSpace;
import ru.yandex.intranet.d.backend.service.proto.AccountsSpacesLimit;
import ru.yandex.intranet.d.backend.service.proto.AccountsSpacesPageToken;
import ru.yandex.intranet.d.backend.service.proto.AccountsSpacesServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetAccountsSpaceRequest;
import ru.yandex.intranet.d.backend.service.proto.ListAccountsSpacesByProviderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListAccountsSpacesByProviderResponse;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.utils.ErrorsHelper;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_4_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_5_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_6_ID;

/**
 * AccountsSpacesTest.
 *
 * @see ru.yandex.intranet.d.grpc.services.GrpcAccountsSpacesServiceImpl
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 22.01.2021
 */
@IntegrationTest
public class AccountsSpacesServiceTest {
    @GrpcClient("inProcess")
    private AccountsSpacesServiceGrpc.AccountsSpacesServiceBlockingStub accountsSpacesService;

    /**
     * Get accounts space test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcAccountsSpacesServiceImpl#getAccountsSpace
     */
    @Test
    public void getAccountsSpaceTest() {
        GetAccountsSpaceRequest accountsSpaceRequest = GetAccountsSpaceRequest.newBuilder()
                .setProviderId(TEST_ACCOUNT_SPACE_2.getProviderId())
                .setAccountsSpaceId(TEST_ACCOUNT_SPACE_2.getId())
                .build();
        AccountsSpace accountsSpace = accountsSpacesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getAccountsSpace(accountsSpaceRequest);
        Assertions.assertNotNull(accountsSpace);
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getNameEn(), accountsSpace.getName());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getDescriptionEn(), accountsSpace.getDescription());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getProviderId(), accountsSpace.getProviderId());
        Assertions.assertEquals("", accountsSpace.getKey());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getVersion(), accountsSpace.getVersion());
        Set<ResourceSegmentSettingsModel> segments =
                accountsSpace.getSegmentsList().stream().map(segment -> new ResourceSegmentSettingsModel(
                        segment.getSegmentation().getResourceSegmentationId(),
                        segment.getSegment().getResourceSegmentId()
                )).collect(Collectors.toSet());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getSegments(), segments);
    }

    /**
     * Accounts space not found test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcAccountsSpacesServiceImpl#getAccountsSpace
     */
    @Test
    public void getAccountsSpaceNotFoundTest() {
        GetAccountsSpaceRequest accountsSpaceRequest = GetAccountsSpaceRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .setAccountsSpaceId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            //noinspection ResultOfMethodCallIgnored
            accountsSpacesService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getAccountsSpace(accountsSpaceRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    /**
     * Get accounts spaces page test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcAccountsSpacesServiceImpl#listAccountsSpacesByProvider
     */
    @Test
    public void getAccountsSpacesPageTest() {
        ListAccountsSpacesByProviderRequest accountsSpacesRequest = ListAccountsSpacesByProviderRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .build();
        ListAccountsSpacesByProviderResponse page = accountsSpacesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsSpacesByProvider(accountsSpacesRequest);
        Assertions.assertNotNull(page);
        Assertions.assertEquals(6, page.getAccountsSpacesCount());
        Optional<AccountsSpace> optionalAccountsSpace =
                page.getAccountsSpacesList().stream().filter(accountsSpace -> accountsSpace.getAccountsSpaceId()
                        .equals(TEST_ACCOUNT_SPACE_2.getId())).findFirst();
        Assertions.assertTrue(optionalAccountsSpace.isPresent());
        AccountsSpace accountsSpace = optionalAccountsSpace.get();
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getNameEn(), accountsSpace.getName());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getDescriptionEn(), accountsSpace.getDescription());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getProviderId(), accountsSpace.getProviderId());
        Assertions.assertEquals("", accountsSpace.getKey());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getVersion(), accountsSpace.getVersion());
        Set<ResourceSegmentSettingsModel> segments =
                accountsSpace.getSegmentsList().stream().map(segment -> new ResourceSegmentSettingsModel(
                        segment.getSegmentation().getResourceSegmentationId(),
                        segment.getSegment().getResourceSegmentId()
                )).collect(Collectors.toSet());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getSegments(), segments);
        List<String> accountsSpaceIds = page.getAccountsSpacesList().stream().map(AccountsSpace::getAccountsSpaceId)
                .collect(Collectors.toList());
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_1_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_2_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_3_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_4_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_5_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_6_ID));
    }

    /**
     * Get two accounts spaces pages test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcAccountsSpacesServiceImpl#listAccountsSpacesByProvider
     */
    @Test
    public void getAccountsSpacesTwoPagesTest() {
        ListAccountsSpacesByProviderRequest firstRequest = ListAccountsSpacesByProviderRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .setLimit(AccountsSpacesLimit.newBuilder().setLimit(1L).build())
                .build();
        ListAccountsSpacesByProviderResponse firstPage = accountsSpacesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsSpacesByProvider(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getAccountsSpacesCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());

        ListAccountsSpacesByProviderRequest secondRequest = ListAccountsSpacesByProviderRequest.newBuilder()
                .setProviderId(TestProviders.YP_ID)
                .setLimit(AccountsSpacesLimit.newBuilder().setLimit(1L).build())
                .setPageToken(AccountsSpacesPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListAccountsSpacesByProviderResponse secondPage = accountsSpacesService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listAccountsSpacesByProvider(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertEquals(1, secondPage.getAccountsSpacesCount());

        List<String> accountsSpaceIds = List.of(
                firstPage.getAccountsSpaces(0).getAccountsSpaceId(),
                secondPage.getAccountsSpaces(0).getAccountsSpaceId()
        );
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_5_ID));
        Assertions.assertTrue(accountsSpaceIds.contains(TEST_ACCOUNT_SPACE_2_ID));
    }

    /**
     * Accounts spaces page not found test.
     *
     * @see ru.yandex.intranet.d.grpc.services.GrpcAccountsSpacesServiceImpl#listAccountsSpacesByProvider
     */
    @Test
    public void getAccountsSpacesPageNotFoundTest() {
        ListAccountsSpacesByProviderRequest accountsSpacesRequest = ListAccountsSpacesByProviderRequest.newBuilder()
                .setProviderId("dummy")
                .build();
        try {
            //noinspection ResultOfMethodCallIgnored
            accountsSpacesService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listAccountsSpacesByProvider(accountsSpacesRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }
}
