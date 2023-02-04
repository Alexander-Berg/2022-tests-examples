package ru.yandex.intranet.d.grpc.quotas;

import java.util.Optional;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.FolderQuota;
import ru.yandex.intranet.d.backend.service.proto.FolderResourceQuota;
import ru.yandex.intranet.d.backend.service.proto.GetQuotaByFolderResourceRequest;
import ru.yandex.intranet.d.backend.service.proto.ListQuotasByFolderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListQuotasByFolderResponse;
import ru.yandex.intranet.d.backend.service.proto.QuotasLimit;
import ru.yandex.intranet.d.backend.service.proto.QuotasPageToken;
import ru.yandex.intranet.d.backend.service.proto.QuotasServiceGrpc;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Quotas GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class QuotasServiceTest {

    @GrpcClient("inProcess")
    private QuotasServiceGrpc.QuotasServiceBlockingStub quotasService;

    @Test
    public void getQuotasPageTest() {
        ListQuotasByFolderRequest quotasRequest = ListQuotasByFolderRequest.newBuilder()
                .setFolderId("aa6a5d64-5b94-4057-8d43-e65812475e73")
                .build();
        ListQuotasByFolderResponse page = quotasService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listQuotasByFolder(quotasRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getQuotasList().size() > 0);
    }

    @Test
    public void getQuotasPageNotFoundTest() {
        ListQuotasByFolderRequest quotasRequest = ListQuotasByFolderRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            quotasService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listQuotasByFolder(quotasRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getQuotasPageTwoPagesTest() {
        ListQuotasByFolderRequest quotasRequestFirst = ListQuotasByFolderRequest.newBuilder()
                .setFolderId("aa6a5d64-5b94-4057-8d43-e65812475e73")
                .setLimit(QuotasLimit.newBuilder().setLimit(1L).build())
                .build();
        ListQuotasByFolderResponse pageFirst = quotasService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listQuotasByFolder(quotasRequestFirst);
        Assertions.assertNotNull(pageFirst);
        Assertions.assertEquals(1, pageFirst.getQuotasList().size());
        ListQuotasByFolderRequest quotasRequestSecond = ListQuotasByFolderRequest.newBuilder()
                .setFolderId("aa6a5d64-5b94-4057-8d43-e65812475e73")
                .setPageToken(QuotasPageToken.newBuilder().setToken(pageFirst.getNextPageToken().getToken()).build())
                .build();
        ListQuotasByFolderResponse pageSecond = quotasService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listQuotasByFolder(quotasRequestSecond);
        Assertions.assertNotNull(pageSecond);
        Assertions.assertTrue(pageSecond.getQuotasList().size() > 0);
    }

    @Test
    public void getResourceQuotaTest() {
        GetQuotaByFolderResourceRequest quotaRequest = GetQuotaByFolderResourceRequest.newBuilder()
                .setFolderId("aa6a5d64-5b94-4057-8d43-e65812475e73")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                .build();
        FolderResourceQuota quota = quotasService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getQuotaByFolderResource(quotaRequest);
        Assertions.assertNotNull(quota);
    }

    @Test
    public void getResourceQuotaNotFoundTest() {
        GetQuotaByFolderResourceRequest quotaRequest = GetQuotaByFolderResourceRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                .build();
        try {
            quotasService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getQuotaByFolderResource(quotaRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getResourceQuotaMissingTest() {
        GetQuotaByFolderResourceRequest quotaRequest = GetQuotaByFolderResourceRequest.newBuilder()
                .setFolderId("b2872163-18fb-44ef-9365-b66f7756d636")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                .build();
        FolderResourceQuota quota = quotasService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getQuotaByFolderResource(quotaRequest);
        Assertions.assertNotNull(quota);
        Assertions.assertEquals(0L, quota.getQuota().getValue());
        Assertions.assertEquals(0L, quota.getBalance().getValue());
    }

    @Test
    public void getUnmanagedResourceQuotaTest() {
        GetQuotaByFolderResourceRequest quotaRequest = GetQuotaByFolderResourceRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_5_ID)
                .setProviderId(TestProviders.YP_ID)
                .setResourceId(TestResources.YP_HDD_UNMANAGED)
                .build();
        FolderResourceQuota quota = quotasService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getQuotaByFolderResource(quotaRequest);
        Assertions.assertNotNull(quota);
        Assertions.assertEquals(0L, quota.getQuota().getValue());
        Assertions.assertEquals(0L, quota.getBalance().getValue());
    }

    @Test
    public void getUnmanagedProviderResourceQuotaTest() {
        GetQuotaByFolderResourceRequest quotaRequest = GetQuotaByFolderResourceRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_WITH_UNMANAGED_PROVIDER_ID)
                .setProviderId(TestProviders.UNMANAGED_PROVIDER_ID)
                .setResourceId(TestResources.UNMANAGED_PROVIDER_RESOURCE_ID)
                .build();
        FolderResourceQuota quota = quotasService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .getQuotaByFolderResource(quotaRequest);
        Assertions.assertNotNull(quota);
        Assertions.assertEquals(0L, quota.getQuota().getValue());
        Assertions.assertEquals(0L, quota.getBalance().getValue());
    }

    @Test
    public void getUnmanagedResourceQuotasPageTest() {
        ListQuotasByFolderRequest quotasRequest = ListQuotasByFolderRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_5_ID)
                .build();
        ListQuotasByFolderResponse page = quotasService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .listQuotasByFolder(quotasRequest);
        Assertions.assertNotNull(page);
        FolderQuota quota = page.getQuotasList().stream()
                .filter(q -> q.getResourceId().equals(TestResources.YP_HDD_UNMANAGED))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(0L, quota.getQuota().getValue());
        Assertions.assertEquals(0L, quota.getBalance().getValue());
    }

    @Test
    public void getUnmanagedProviderResourceQuotasPageTest() {
        ListQuotasByFolderRequest quotasRequest = ListQuotasByFolderRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_WITH_UNMANAGED_PROVIDER_ID)
                .build();
        ListQuotasByFolderResponse page = quotasService
                .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
                .listQuotasByFolder(quotasRequest);
        Assertions.assertNotNull(page);
        FolderQuota quota = page.getQuotasList().stream()
                .filter(q -> q.getResourceId().equals(TestResources.UNMANAGED_PROVIDER_RESOURCE_ID))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(0L, quota.getQuota().getValue());
        Assertions.assertEquals(0L, quota.getBalance().getValue());
    }

}
