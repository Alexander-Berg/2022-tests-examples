package ru.yandex.intranet.imscore.grpc.identityType;

import java.util.List;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.grpc.common.TestData;
import ru.yandex.intranet.imscore.proto.identityType.IdentityType;
import ru.yandex.intranet.imscore.proto.identityType.IdentityTypeServiceGrpc;
import ru.yandex.intranet.imscore.proto.identityType.ListIdentityTypesRequest;
import ru.yandex.intranet.imscore.proto.identityType.ListIdentityTypesResponse;

import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.GRPC_STRING_DEFAULT;
import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;

/**
 * Get list identity type test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class GetListIdentityTypeTest {

    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityTypeServiceGrpc.IdentityTypeServiceBlockingStub identityTypeServiceBlockingStub;

    @Test
    public void getListTest() {
        ListIdentityTypesRequest request = ListIdentityTypesRequest.newBuilder()
                .setPageSize(1)
                .build();

        ListIdentityTypesResponse response =
                identityTypeServiceBlockingStub.listIdentityTypes(request);

        Assertions.assertNotNull(response);
        List<IdentityType> identityTypesList = response.getIdentityTypesList();
        Assertions.assertFalse(isEmpty(identityTypesList));
        Assertions.assertEquals(1, identityTypesList.size());
        Assertions.assertEquals(TestData.Companion.getIdentityType().getId(), identityTypesList.get(0).getId());
        Assertions.assertNotEquals(GRPC_STRING_DEFAULT, response.getNextPageToken());
    }

    @Test
    public void getListWithPaginationTest() {
        ListIdentityTypesRequest request = ListIdentityTypesRequest.newBuilder()
                .setPageSize(1)
                .build();

        ListIdentityTypesResponse response =
                identityTypeServiceBlockingStub.listIdentityTypes(request);

        Assertions.assertNotNull(response);
        List<IdentityType> identityTypesList = response.getIdentityTypesList();
        Assertions.assertFalse(isEmpty(identityTypesList));
        Assertions.assertEquals(1, identityTypesList.size());
        Assertions.assertEquals(TestData.Companion.getIdentityType().getId(), identityTypesList.get(0).getId());
        Assertions.assertNotEquals(GRPC_STRING_DEFAULT, response.getNextPageToken());

        request = ListIdentityTypesRequest.newBuilder()
                .setPageSize(1)
                .setPageToken(response.getNextPageToken())
                .build();
        response =
                identityTypeServiceBlockingStub.listIdentityTypes(request);

        Assertions.assertNotNull(response);
        identityTypesList = response.getIdentityTypesList();
        Assertions.assertFalse(isEmpty(identityTypesList));
        Assertions.assertEquals(1, identityTypesList.size());
        Assertions.assertEquals(TestData.Companion.getIdentityGroupType().getId(), identityTypesList.get(0).getId());
        Assertions.assertNotEquals(GRPC_STRING_DEFAULT, response.getNextPageToken());

        request = ListIdentityTypesRequest.newBuilder()
                .setPageSize(1)
                .setPageToken(response.getNextPageToken())
                .build();
        response =
                identityTypeServiceBlockingStub.listIdentityTypes(request);

        Assertions.assertNotNull(response);
        identityTypesList = response.getIdentityTypesList();
        Assertions.assertTrue(isEmpty(identityTypesList));
        Assertions.assertEquals(GRPC_STRING_DEFAULT, response.getNextPageToken());
    }

    @Test
    public void getListFailsOnTooBigPageSizeTest() {
        ListIdentityTypesRequest request = ListIdentityTypesRequest.newBuilder()
                .setPageSize(1001)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeServiceBlockingStub.listIdentityTypes(request));
        Assertions.assertEquals(
                "INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage()
        );
        assertMetadata(statusRuntimeException, 1,
                new String[]{"page_size", "Value should be less than or equal to 1000"});
    }

    @Test
    public void getListFailsOnFakePageTokenTest() {
        ListIdentityTypesRequest request = ListIdentityTypesRequest.newBuilder()
                .setPageToken("fake-token")
                .setPageSize(1)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeServiceBlockingStub.listIdentityTypes(request));
        Assertions.assertEquals(
                "INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage()
        );
        assertMetadata(statusRuntimeException, 1,
                new String[]{"page_token", "Value is not a page token"});
    }

}
