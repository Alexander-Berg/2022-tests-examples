package ru.yandex.intranet.d.grpc.provisions;

import java.util.Optional;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetResourceProvisionByFolderAccountRequest;
import ru.yandex.intranet.d.backend.service.proto.ListProvisionsByFolderAccountRequest;
import ru.yandex.intranet.d.backend.service.proto.ListProvisionsByFolderAccountResponse;
import ru.yandex.intranet.d.backend.service.proto.ProviderProvision;
import ru.yandex.intranet.d.backend.service.proto.ProvisionsLimit;
import ru.yandex.intranet.d.backend.service.proto.ProvisionsPageToken;
import ru.yandex.intranet.d.backend.service.proto.ProvisionsServiceGrpc;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Provisions GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ProvisionsServiceTest {

    @GrpcClient("inProcess")
    private ProvisionsServiceGrpc.ProvisionsServiceBlockingStub provisionsService;

    @Test
    public void getProvisionTest() {
        GetResourceProvisionByFolderAccountRequest provisionRequest = GetResourceProvisionByFolderAccountRequest
                .newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setAccountId("56a41608-84df-41c4-9653-89106462e0ce")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceId("ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
                .build();
        ProviderProvision provision = provisionsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResourceProvisionByFolderAccount(provisionRequest);
        Assertions.assertNotNull(provision);
        Assertions.assertEquals("f714c483-c347-41cc-91d0-c6722f5daac7", provision.getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", provision.getProviderId());
        Assertions.assertEquals("56a41608-84df-41c4-9653-89106462e0ce", provision.getAccountId());
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", provision.getResourceId());
        Assertions.assertEquals(200000000L, provision.getProvidedAmount().getValue());
        Assertions.assertEquals("kilobytes", provision.getProvidedAmount().getUnitKey());
        Assertions.assertEquals(100000000L, provision.getAllocatedAmount().getValue());
        Assertions.assertEquals("kilobytes", provision.getAllocatedAmount().getUnitKey());
    }

    @Test
    public void getProvisionNotFoundTest() {
        GetResourceProvisionByFolderAccountRequest provisionRequest = GetResourceProvisionByFolderAccountRequest
                .newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setAccountId("56a41608-84df-41c4-9653-89106462e0ce")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                .build();
        ProviderProvision provision = provisionsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getResourceProvisionByFolderAccount(provisionRequest);
        Assertions.assertNotNull(provision);
        Assertions.assertEquals("f714c483-c347-41cc-91d0-c6722f5daac7", provision.getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", provision.getProviderId());
        Assertions.assertEquals("56a41608-84df-41c4-9653-89106462e0ce", provision.getAccountId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", provision.getResourceId());
        Assertions.assertEquals(0L, provision.getProvidedAmount().getValue());
        Assertions.assertEquals("gigabytes", provision.getProvidedAmount().getUnitKey());
        Assertions.assertEquals(0L, provision.getAllocatedAmount().getValue());
        Assertions.assertEquals("gigabytes", provision.getAllocatedAmount().getUnitKey());
    }

    @Test
    public void getProvisionFolderNotFoundTest() {
        GetResourceProvisionByFolderAccountRequest provisionRequest = GetResourceProvisionByFolderAccountRequest
                .newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .setAccountId("56a41608-84df-41c4-9653-89106462e0ce")
                .setProviderId("96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .setResourceId("ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
                .build();
        try {
            provisionsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getResourceProvisionByFolderAccount(provisionRequest);
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
    public void getProvisionsPageTest() {
        ListProvisionsByFolderAccountRequest provisionsRequest = ListProvisionsByFolderAccountRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setAccountId("9567ae7c-9b76-44bc-87c7-e18d998778b3")
                .build();
        ListProvisionsByFolderAccountResponse page = provisionsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProvisionsByFolderAccount(provisionsRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getProvisionsCount() > 0);
    }

    @Test
    public void getProvisionsTwoPagesTest() {
        ListProvisionsByFolderAccountRequest firstRequest = ListProvisionsByFolderAccountRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setAccountId("9567ae7c-9b76-44bc-87c7-e18d998778b3")
                .setLimit(ProvisionsLimit.newBuilder().setLimit(1L).build())
                .build();
        ListProvisionsByFolderAccountResponse firstPage = provisionsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProvisionsByFolderAccount(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getProvisionsCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListProvisionsByFolderAccountRequest secondRequest = ListProvisionsByFolderAccountRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setAccountId("9567ae7c-9b76-44bc-87c7-e18d998778b3")
                .setPageToken(ProvisionsPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListProvisionsByFolderAccountResponse secondPage = provisionsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProvisionsByFolderAccount(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getProvisionsCount() > 0);
    }

    @Test
    public void getProvisionsNotFoundPageTest() {
        ListProvisionsByFolderAccountRequest provisionsRequest = ListProvisionsByFolderAccountRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .setAccountId("9567ae7c-9b76-44bc-87c7-e18d998778b3")
                .build();
        try {
            provisionsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listProvisionsByFolderAccount(provisionsRequest);
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
    public void getProvisionsTwoPagesNotFoundTest() {
        ListProvisionsByFolderAccountRequest firstRequest = ListProvisionsByFolderAccountRequest.newBuilder()
                .setFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                .setAccountId("9567ae7c-9b76-44bc-87c7-e18d998778b3")
                .setLimit(ProvisionsLimit.newBuilder().setLimit(1L).build())
                .build();
        ListProvisionsByFolderAccountResponse firstPage = provisionsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listProvisionsByFolderAccount(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getProvisionsCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListProvisionsByFolderAccountRequest secondRequest = ListProvisionsByFolderAccountRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .setAccountId("9567ae7c-9b76-44bc-87c7-e18d998778b3")
                .setPageToken(ProvisionsPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        try {
            provisionsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .listProvisionsByFolderAccount(secondRequest);
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
