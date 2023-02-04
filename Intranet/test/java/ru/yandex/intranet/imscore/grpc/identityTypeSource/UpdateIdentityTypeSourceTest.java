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
import ru.yandex.intranet.imscore.proto.identityTypeSource.GetIdentityTypeSourceRequest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.GetIdentityTypeSourceResponse;
import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSource;
import ru.yandex.intranet.imscore.proto.identityTypeSource.IdentityTypeSourceServiceGrpc;
import ru.yandex.intranet.imscore.proto.identityTypeSource.UpdateIdentityTypeSourceRequest;
import ru.yandex.intranet.imscore.proto.identityTypeSource.UpdateIdentityTypeSourceResponse;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;
import static ru.yandex.intranet.imscore.grpc.common.IdentityTypeSourceTestData.ABC_IDENTITY_TYPE_SOURCE;

/**
 * Update identity type source test
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class UpdateIdentityTypeSourceTest {

    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityTypeSourceServiceGrpc.IdentityTypeSourceServiceBlockingStub identityTypeSourceServiceBlockingStub;

    @Test
    public void updateIdentityTypeSourceTest() {
        Set<Integer> values = Set.of(1, 2, 3);
        UpdateIdentityTypeSourceRequest request = UpdateIdentityTypeSourceRequest.newBuilder()
                .setId(ABC_IDENTITY_TYPE_SOURCE.getId())
                .addAllAllowedTvmIds(values)
                .build();

        UpdateIdentityTypeSourceResponse response =
                identityTypeSourceServiceBlockingStub.updateIdentityTypeSource(request);

        IdentityTypeSource identityTypeSource = response.getIdentityTypeSource();
        Assertions.assertNotNull(identityTypeSource);
        Assertions.assertEquals(ABC_IDENTITY_TYPE_SOURCE.getId(), identityTypeSource.getId());
        Assertions.assertEquals(values, new HashSet<>(identityTypeSource.getAllowedTvmIdsList()));

        GetIdentityTypeSourceRequest getRequest = GetIdentityTypeSourceRequest.newBuilder()
                .setId(ABC_IDENTITY_TYPE_SOURCE.getId())
                .build();

        GetIdentityTypeSourceResponse getResponse =
                identityTypeSourceServiceBlockingStub.getIdentityTypeSource(getRequest);
        Assertions.assertNotNull(getResponse.getIdentityTypeSource());
        Assertions.assertEquals(ABC_IDENTITY_TYPE_SOURCE.getId(), getResponse.getIdentityTypeSource().getId());
        Assertions.assertEquals(values, new HashSet<>(getResponse.getIdentityTypeSource().getAllowedTvmIdsList()));
    }

    @Test
    public void updateIdentityTypeToEmptyAllowedTvmIdsListSourceTest() {
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

        GetIdentityTypeSourceRequest getRequest = GetIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .build();

        GetIdentityTypeSourceResponse getResponse =
                identityTypeSourceServiceBlockingStub.getIdentityTypeSource(getRequest);

        Assertions.assertNotNull(getResponse.getIdentityTypeSource());
        Assertions.assertEquals(test, getResponse.getIdentityTypeSource().getId());
        Assertions.assertEquals(values, new HashSet<>(getResponse.getIdentityTypeSource().getAllowedTvmIdsList()));

        UpdateIdentityTypeSourceRequest request = UpdateIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .build();

        UpdateIdentityTypeSourceResponse updateResponse =
                identityTypeSourceServiceBlockingStub.updateIdentityTypeSource(request);

        IdentityTypeSource updatedIdentityTypeSource = updateResponse.getIdentityTypeSource();
        Assertions.assertNotNull(updatedIdentityTypeSource);
        Assertions.assertEquals(test, updatedIdentityTypeSource.getId());
        Assertions.assertEquals(List.of(), updatedIdentityTypeSource.getAllowedTvmIdsList());

        getRequest = GetIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .build();

        getResponse = identityTypeSourceServiceBlockingStub.getIdentityTypeSource(getRequest);
        Assertions.assertNotNull(getResponse.getIdentityTypeSource());
        Assertions.assertEquals(test, getResponse.getIdentityTypeSource().getId());
        Assertions.assertEquals(List.of(), getResponse.getIdentityTypeSource().getAllowedTvmIdsList());
    }

    @Test
    public void updateIdentityTypeSourceFailOnNotExistingIdTest() {
        String test = "test";
        Set<Integer> values = Set.of(1, 2, 3);

        UpdateIdentityTypeSourceRequest request = UpdateIdentityTypeSourceRequest.newBuilder()
                .setId(test)
                .addAllAllowedTvmIds(values)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.updateIdentityTypeSource(request));
        Assertions.assertEquals(
                String.format("NOT_FOUND: resource with id %s not found", test),
                statusRuntimeException.getMessage()
        );
    }

    @Test
    public void updateIdentityTypeSourceFailOnEmptyIdTest() {
        Set<Integer> values = Set.of(1, 2, 3);

        UpdateIdentityTypeSourceRequest request = UpdateIdentityTypeSourceRequest.newBuilder()
                .addAllAllowedTvmIds(values)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityTypeSourceServiceBlockingStub.updateIdentityTypeSource(request));
        Assertions.assertEquals(
                "INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage()
        );
        assertMetadata(statusRuntimeException, 1,
                new String[]{"id", "Value is required"});
    }

}
