package ru.yandex.intranet.d.grpc.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.Folder;
import ru.yandex.intranet.d.backend.service.proto.FolderServiceGrpc;
import ru.yandex.intranet.d.backend.service.proto.FoldersLimit;
import ru.yandex.intranet.d.backend.service.proto.FoldersPageToken;
import ru.yandex.intranet.d.backend.service.proto.GetFolderRequest;
import ru.yandex.intranet.d.backend.service.proto.ListFoldersByIdsRequest;
import ru.yandex.intranet.d.backend.service.proto.ListFoldersByIdsResponse;
import ru.yandex.intranet.d.backend.service.proto.ListFoldersByServiceRequest;
import ru.yandex.intranet.d.backend.service.proto.ListFoldersByServiceResponse;
import ru.yandex.intranet.d.backend.service.proto.ListFoldersRequest;
import ru.yandex.intranet.d.backend.service.proto.ListFoldersResponse;
import ru.yandex.intranet.d.dao.services.ServicesDao;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.model.services.ServiceMinimalModel;
import ru.yandex.intranet.d.utils.ErrorsHelper;

import static ru.yandex.intranet.d.TestServices.ABSENTED_SERVICE_ID;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;

/**
 * Folders GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class FolderServiceTest {

    @GrpcClient("inProcess")
    private FolderServiceGrpc.FolderServiceBlockingStub folderService;
    @Autowired
    private ServicesDao servicesDao;
    @Autowired
    private YdbTableClient tableClient;

    @Test
    public void getFolderTest() {
        GetFolderRequest folderRequest = GetFolderRequest.newBuilder()
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .build();
        Folder folder = folderService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getFolder(folderRequest);
        Assertions.assertNotNull(folder);
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_ID, folder.getFolderId());
    }

    @Test
    public void getFolderNotFoundTest() {
        GetFolderRequest folderRequest = GetFolderRequest.newBuilder()
                .setFolderId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            folderService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getFolder(folderRequest);
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
    public void listFoldersByServiceTest() {
        List<Folder> folders = readAllFolders(TestFolders.TEST_FOLDER_1_SERVICE_ID, false, 1L);

        List<String> folderIds = folders.stream().map(Folder::getFolderId).collect(Collectors.toList());
        Assertions.assertEquals(3, folderIds.size());
        Assertions.assertTrue(folderIds.contains(TestFolders.TEST_FOLDER_1_ID));

        List<Folder> foldersIncludeDeleted = readAllFolders(TestFolders.TEST_FOLDER_1_SERVICE_ID, true, 1L);

        List<Boolean> folderDeletedFlags =
                foldersIncludeDeleted.stream().map(Folder::getDeleted).collect(Collectors.toList());
        Assertions.assertEquals(4, folderDeletedFlags.size());
        Assertions.assertTrue(folderDeletedFlags.contains(true));
    }

    @Test
    public void listFoldersByServiceNoLimitTest() {
        List<Folder> folders = readAllFolders(TestFolders.TEST_FOLDER_1_SERVICE_ID, false, null);

        List<String> folderIds = folders.stream().map(Folder::getFolderId).collect(Collectors.toList());
        Assertions.assertEquals(3, folderIds.size());
        Assertions.assertTrue(folderIds.contains(TestFolders.TEST_FOLDER_1_ID));

        List<Folder> foldersIncludeDeleted = readAllFolders(TestFolders.TEST_FOLDER_1_SERVICE_ID, true, null);

        List<Boolean> folderDeletedFlags =
                foldersIncludeDeleted.stream().map(Folder::getDeleted).collect(Collectors.toList());
        Assertions.assertEquals(4, folderDeletedFlags.size());
        Assertions.assertTrue(folderDeletedFlags.contains(true));
    }

    @Test
    public void listFoldersByServiceNotFoundAnswerCodeTest() {
        try {
            readAllFolders(ABSENTED_SERVICE_ID, false, 1L);
            Assertions.fail();
        } catch (io.grpc.StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertEquals(1, details.get().getErrorsCount());
            Assertions.assertEquals("Service not found.", details.get().getErrors(0));
        }

        WithTxId<Optional<ServiceMinimalModel>> serviceB = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                        servicesDao.getByIdMinimal(ts, TEST_SERVICE_ID_DISPENSER)
                ))
                .block();

        Assertions.assertNotNull(serviceB);
        Assertions.assertTrue(serviceB.get().isPresent());
    }

    @Test
    public void listFoldersTest() {
        ListFoldersRequest foldersRequest = ListFoldersRequest.newBuilder()
                .build();
        ListFoldersResponse folders = folderService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listFolders(foldersRequest);
        Assertions.assertNotNull(folders);
        Assertions.assertTrue(folders.getFoldersList().size() > 0);
    }

    @Test
    public void listFoldersTwoPagesTest() {
        ListFoldersRequest foldersRequestFirst = ListFoldersRequest.newBuilder()
                .setLimit(FoldersLimit.newBuilder().setLimit(1).build())
                .build();
        ListFoldersResponse foldersFirst = folderService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listFolders(foldersRequestFirst);
        Assertions.assertNotNull(foldersFirst);
        Assertions.assertTrue(foldersFirst.getFoldersList().size() > 0);
        ListFoldersRequest foldersRequestSecond = ListFoldersRequest.newBuilder()
                .setPageToken(FoldersPageToken.newBuilder()
                        .setToken(foldersFirst.getNextPageToken().getToken()).build())
                .build();
        ListFoldersResponse foldersSecond = folderService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listFolders(foldersRequestSecond);
        Assertions.assertNotNull(foldersSecond);
        Assertions.assertTrue(foldersSecond.getFoldersList().size() > 0);
    }

    @Test
    public void listFoldersByServiceNotFoundTest() {
        MockGrpcUser tvm = MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID);
        try {
            folderService.withCallCredentials(tvm)
                    .listFoldersByService(ListFoldersByServiceRequest.newBuilder()
                            .setServiceId(66)
                            .setLimit(FoldersLimit.newBuilder().setLimit(10).build())
                            .setIncludeDeleted(false)
                            .build());
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            Assertions.assertEquals("Service not found.", details.get().getErrors(0));
            return;
        }
        Assertions.fail();
    }

    @Test
    public void listFoldersByIdsTest() {
        ListFoldersByIdsRequest foldersRequest = ListFoldersByIdsRequest.newBuilder()
                .addFolderId(TestFolders.TEST_FOLDER_1_ID)
                .addFolderId(TestFolders.TEST_FOLDER_3_ID)
                .build();
        ListFoldersByIdsResponse folders = folderService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listFoldersByIds(foldersRequest);
        Assertions.assertNotNull(folders);
        Assertions.assertEquals(2, folders.getFoldersList().size());
        Set<String> ids = folders.getFoldersList().stream().map(Folder::getFolderId).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(TestFolders.TEST_FOLDER_1_ID, TestFolders.TEST_FOLDER_3_ID), ids);
    }

    @SuppressWarnings("SameParameterValue")
    private List<Folder> readAllFolders(long serviceId, boolean includeDeleted, Long limit) {
        MockGrpcUser tvm = MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID);
        ListFoldersByServiceRequest.Builder requestBuilder = ListFoldersByServiceRequest.newBuilder()
                .setServiceId(serviceId)
                .setIncludeDeleted(includeDeleted);
        if (limit != null) {
            requestBuilder.setLimit(FoldersLimit.newBuilder().setLimit(limit).build());
        }
        ListFoldersByServiceRequest request = requestBuilder.build();
        ListFoldersByServiceResponse foldersResp = folderService.withCallCredentials(tvm)
                .listFoldersByService(request);
        List<Folder> folders = new ArrayList<>(foldersResp.getFoldersList());
        while (foldersResp.hasNextPageToken()) {
            ListFoldersByServiceRequest.Builder nextRequestBuilder = ListFoldersByServiceRequest.newBuilder()
                    .setServiceId(serviceId)
                    .setIncludeDeleted(includeDeleted)
                    .setPageToken(foldersResp.getNextPageToken());
            if (limit != null) {
                nextRequestBuilder.setLimit(FoldersLimit.newBuilder().setLimit(limit).build());
            }
            ListFoldersByServiceRequest nextRequest = nextRequestBuilder.build();
            foldersResp = folderService.withCallCredentials(tvm)
                    .listFoldersByService(nextRequest);
            folders.addAll(foldersResp.getFoldersList());
        }
        return folders;
    }
}
