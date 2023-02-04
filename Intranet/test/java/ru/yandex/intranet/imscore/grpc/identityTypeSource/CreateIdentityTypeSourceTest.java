package ru.yandex.intranet.imscore.grpc.identityTypeSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.CreateIdentityTypeSourceRequest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.CreateIdentityTypeSourceResponse;
import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSource;
import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSourceServiceGrpc;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;

/**
 * Create identity type source test
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class CreateIdentityTypeSourceTest {

    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityTypeSourceServiceGrpc.IdentityTypeSourceServiceBlockingStub identityTypeSourceServiceBlockingStub;

    @Test
    public void createIdentityTypeSourceTest() {
        String test = "test";
        CreateIdentityTypeSourceRequest build = CreateIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .build();

        CreateIdentityTypeSourceResponse response =
                identityTypeSourceServiceBlockingStub.createIdentityTypeSource(build);
        Assertions.assertNotNull(response);
        IdentityTypeSource identityTypeSource = response.getIdentityTypeSource();
        Assertions.assertEquals(test, identityTypeSource.getId());
        Assertions.assertEquals(List.of(), identityTypeSource.getAllowedTvmIdsList());
    }

    @Test
    public void createIdentityTypeSourceWithAllowedTvmIdsTest() {
        String test = "test";
        Set<Integer> values = Set.of(1, 2, 3);
        CreateIdentityTypeSourceRequest build = CreateIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .addAllAllowedTvmIds(values)
                .build();

        CreateIdentityTypeSourceResponse response =
                identityTypeSourceServiceBlockingStub.createIdentityTypeSource(build);
        Assertions.assertNotNull(response);
        IdentityTypeSource identityTypeSource = response.getIdentityTypeSource();
        Assertions.assertEquals(test, identityTypeSource.getId());
        Assertions.assertEquals(values, new HashSet<>(identityTypeSource.getAllowedTvmIdsList()));
    }

    @Test
    public void createIdentityTypeSourceFailOnDuplicateIdTest() {
        String test = "test";
        CreateIdentityTypeSourceRequest build = CreateIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .build();

        CreateIdentityTypeSourceResponse response =
                identityTypeSourceServiceBlockingStub.createIdentityTypeSource(build);
        Assertions.assertNotNull(response);
        IdentityTypeSource identityTypeSource = response.getIdentityTypeSource();
        Assertions.assertEquals(test, identityTypeSource.getId());
        Assertions.assertEquals(List.of(), identityTypeSource.getAllowedTvmIdsList());

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.createIdentityTypeSource(build));
        Assertions.assertEquals(
                String.format("ALREADY_EXISTS: Identity type source with id %s already exists", test),
                statusRuntimeException.getMessage()
        );
    }

    @Test
    public void createIdentityTypeSourceFailOnEmptyIdTest() {
        CreateIdentityTypeSourceRequest build = CreateIdentityTypeSourceRequest.newBuilder()
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.createIdentityTypeSource(build));
        Assertions.assertEquals(
                "INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage()
        );
        assertMetadata(statusRuntimeException, 1,
                new String[]{"id", "Value is required"});
    }

}
