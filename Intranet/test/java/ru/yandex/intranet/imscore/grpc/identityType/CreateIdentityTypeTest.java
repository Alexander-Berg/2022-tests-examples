package ru.yandex.intranet.imscore.grpc.identityType;

import java.util.UUID;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.proto.identityType.CreateIdentityTypeRequest;
import ru.yandex.intranet.imscore.proto.identityType.CreateIdentityTypeResponse;
import ru.yandex.intranet.imscore.proto.identityType.IdentityType;
import ru.yandex.intranet.imscore.proto.identityType.IdentityTypeServiceGrpc;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;
import static ru.yandex.intranet.imscore.grpc.common.IdentityTypeSourceTestData.STAFF_IDENTITY_TYPE_SOURCE;

/**
 * Create identity type test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class CreateIdentityTypeTest {

    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityTypeServiceGrpc.IdentityTypeServiceBlockingStub identityTypeServiceBlockingStub;

    @Test
    public void createIdentityTest() {
        IdentityType example = IdentityType.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setIsGroup(false)
                .setSourceId(STAFF_IDENTITY_TYPE_SOURCE.getId())
                .build();

        CreateIdentityTypeRequest request = CreateIdentityTypeRequest.newBuilder()
                .setId(example.getId())
                .setIsGroup(example.getIsGroup())
                .setSourceId(example.getSourceId())
                .build();

        CreateIdentityTypeResponse identityType = identityTypeServiceBlockingStub.createIdentityType(request);
        Assertions.assertEquals(example, identityType.getIdentityType());
    }

    @Test
    public void createIdentityFailedOnDuplicateTest() {
        IdentityType example = IdentityType.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setIsGroup(false)
                .setSourceId(STAFF_IDENTITY_TYPE_SOURCE.getId())
                .build();

        CreateIdentityTypeRequest request = CreateIdentityTypeRequest.newBuilder()
                .setId(example.getId())
                .setIsGroup(example.getIsGroup())
                .setSourceId(example.getSourceId())
                .build();

        CreateIdentityTypeResponse identityType = identityTypeServiceBlockingStub.createIdentityType(request);
        Assertions.assertEquals(example, identityType.getIdentityType());

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeServiceBlockingStub.createIdentityType(request));
        Assertions.assertEquals(
                String.format("ALREADY_EXISTS: Identity type with id %s already exists", example.getId()),
                statusRuntimeException.getMessage()
        );
    }

    @Test
    public void createIdentityFailOnEmptyIdTest() {
        CreateIdentityTypeRequest request = CreateIdentityTypeRequest.newBuilder()
                .setIsGroup(false)
                .setSourceId(STAFF_IDENTITY_TYPE_SOURCE.getId())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeServiceBlockingStub.createIdentityType(request));
        Assertions.assertEquals(
                "INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage()
        );
        assertMetadata(statusRuntimeException, 1,
                new String[]{"id", "Value is required"});
    }

    @Test
    public void createIdentityFailedEmptySourceIdTest() {
        CreateIdentityTypeRequest request = CreateIdentityTypeRequest.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setIsGroup(false)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeServiceBlockingStub.createIdentityType(request));
        Assertions.assertEquals(
                "INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage()
        );
        assertMetadata(statusRuntimeException, 1,
                new String[]{"source_id", "Value is required"});
    }

    @Test
    public void createIdentityFailedFakeSourceIdTest() {
        String value = "fake-id";
        CreateIdentityTypeRequest request = CreateIdentityTypeRequest.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setIsGroup(false)
                .setSourceId(value)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeServiceBlockingStub.createIdentityType(request));
        Assertions.assertEquals(
                String.format("NOT_FOUND: resource with id %s not found", value),
                statusRuntimeException.getMessage()
        );
    }

}
