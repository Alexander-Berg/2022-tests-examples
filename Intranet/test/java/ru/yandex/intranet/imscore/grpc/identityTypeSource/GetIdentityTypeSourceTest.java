package ru.yandex.intranet.imscore.grpc.identityTypeSource;

import java.util.HashSet;
import java.util.Set;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.CreateIdentityTypeSourceRequest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.CreateIdentityTypeSourceResponse;
import ru.yandex.intranet.imscore.proto.identityTypeSource.GetIdentityTypeSourceRequest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.GetIdentityTypeSourceResponse;
import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSource;
import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSourceServiceGrpc;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;
import static ru.yandex.intranet.imscore.grpc.common.IdentityTypeSourceTestData.ABC_IDENTITY_TYPE_SOURCE;

/**
 * Get identity type source test
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class GetIdentityTypeSourceTest {

    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityTypeSourceServiceGrpc.IdentityTypeSourceServiceBlockingStub identityTypeSourceServiceBlockingStub;

    @Test
    public void getIdentityTypeSourceTest() {
        GetIdentityTypeSourceRequest request = GetIdentityTypeSourceRequest.newBuilder()
                .setId(ABC_IDENTITY_TYPE_SOURCE.getId())
                .build();

        GetIdentityTypeSourceResponse response =
                identityTypeSourceServiceBlockingStub.getIdentityTypeSource(request);
        IdentityTypeSource identityTypeSource = response.getIdentityTypeSource();
        Assertions.assertNotNull(identityTypeSource);
        Assertions.assertEquals(ABC_IDENTITY_TYPE_SOURCE, identityTypeSource);
    }

    @Test
    public void getIdentityTypeSourceWithAllowedTvmIdsTest() {
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

        GetIdentityTypeSourceRequest request = GetIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .build();

        GetIdentityTypeSourceResponse getResponse =
                identityTypeSourceServiceBlockingStub.getIdentityTypeSource(request);
        IdentityTypeSource identityTypeSource2 = getResponse.getIdentityTypeSource();
        Assertions.assertNotNull(identityTypeSource);
        Assertions.assertEquals(identityTypeSource, identityTypeSource2);
    }

    @Test
    public void getIdentityTypeSourceFailOnEmptyIdTest() {
        GetIdentityTypeSourceRequest request = GetIdentityTypeSourceRequest.newBuilder()
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.getIdentityTypeSource(request));
        Assertions.assertEquals(
                "INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage()
        );
        assertMetadata(statusRuntimeException, 1,
                new String[]{"id", "Value is required"});
    }

    @Test
    public void getIdentityTypeSourceFailOnFakeIdTest() {
        String test = "test";
        GetIdentityTypeSourceRequest request = GetIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.getIdentityTypeSource(request));
        Assertions.assertEquals(
                String.format("NOT_FOUND: resource with id %s not found", test),
                statusRuntimeException.getMessage()
        );
    }

}
