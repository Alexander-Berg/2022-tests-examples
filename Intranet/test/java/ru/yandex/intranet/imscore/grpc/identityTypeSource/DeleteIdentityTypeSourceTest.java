package ru.yandex.intranet.imscore.grpc.identityTypeSource;

import java.util.HashSet;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.DeleteIdentityTypeSourceRequest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.GetIdentityTypeSourceRequest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.GetIdentityTypeSourceResponse;
import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSource;
import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSourceServiceGrpc;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;
import static ru.yandex.intranet.imscore.grpc.common.IdentityTypeSourceTestData.ABC_IDENTITY_TYPE_SOURCE;
import static ru.yandex.intranet.imscore.grpc.common.IdentityTypeSourceTestData.STAFF_IDENTITY_TYPE_SOURCE;

/**
 * Delete identity type source test
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class DeleteIdentityTypeSourceTest {

    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityTypeSourceServiceGrpc.IdentityTypeSourceServiceBlockingStub identityTypeSourceServiceBlockingStub;

    @Test
    public void deleteIdentityTypeSourceTest() {
        GetIdentityTypeSourceRequest request = GetIdentityTypeSourceRequest.newBuilder()
                .setId(ABC_IDENTITY_TYPE_SOURCE.getId())
                .build();

        GetIdentityTypeSourceResponse response =
                identityTypeSourceServiceBlockingStub.getIdentityTypeSource(request);
        IdentityTypeSource identityTypeSource = response.getIdentityTypeSource();
        Assertions.assertNotNull(identityTypeSource);
        Assertions.assertEquals(ABC_IDENTITY_TYPE_SOURCE, identityTypeSource);

        DeleteIdentityTypeSourceRequest deleteRequest = DeleteIdentityTypeSourceRequest.newBuilder()
                .setId(ABC_IDENTITY_TYPE_SOURCE.getId())
                .build();

        identityTypeSourceServiceBlockingStub.deleteIdentityTypeSource(deleteRequest);

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.getIdentityTypeSource(request));
        Assertions.assertEquals(
                String.format("NOT_FOUND: resource with id %s not found", ABC_IDENTITY_TYPE_SOURCE.getId()),
                statusRuntimeException.getMessage()
        );
    }

    @Test
    public void deleteIdentityTypeSourceFailOnIdentityTypeSourceWithIdentityTypeTest() {
        GetIdentityTypeSourceRequest request = GetIdentityTypeSourceRequest.newBuilder()
                .setId(STAFF_IDENTITY_TYPE_SOURCE.getId())
                .build();

        GetIdentityTypeSourceResponse response =
                identityTypeSourceServiceBlockingStub.getIdentityTypeSource(request);
        IdentityTypeSource identityTypeSource = response.getIdentityTypeSource();
        Assertions.assertNotNull(identityTypeSource);
        Assertions.assertEquals(STAFF_IDENTITY_TYPE_SOURCE.getId(), identityTypeSource.getId());
        Assertions.assertEquals(new HashSet<>(STAFF_IDENTITY_TYPE_SOURCE.getAllowedTvmIdsList()),
                new HashSet<>(identityTypeSource.getAllowedTvmIdsList()));

        DeleteIdentityTypeSourceRequest deleteRequest = DeleteIdentityTypeSourceRequest.newBuilder()
                .setId(STAFF_IDENTITY_TYPE_SOURCE.getId())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.deleteIdentityTypeSource(deleteRequest));
        Assertions.assertEquals(
                String.format("INVALID_ARGUMENT: Entity with id %s still referenced",
                        STAFF_IDENTITY_TYPE_SOURCE.getId()), statusRuntimeException.getMessage()
        );
    }

    @Test
    public void deleteIdentityTypeSourceFailOnEmptyIdTest() {
        DeleteIdentityTypeSourceRequest deleteRequest = DeleteIdentityTypeSourceRequest.newBuilder()
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.deleteIdentityTypeSource(deleteRequest));
        Assertions.assertEquals(
                "INVALID_ARGUMENT: INVALID_ARGUMENT", statusRuntimeException.getMessage()
        );
        assertMetadata(statusRuntimeException, 1,
                new String[]{"id", "Value is required"});
    }

    @Test
    public void deleteIdentityTypeSourceFailOnNotExistingIdTest() {
        String test = "test";
        DeleteIdentityTypeSourceRequest deleteRequest = DeleteIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.deleteIdentityTypeSource(deleteRequest));
        Assertions.assertEquals(
                String.format("NOT_FOUND: resource with id %s not found", test), statusRuntimeException.getMessage()
        );
    }

}
