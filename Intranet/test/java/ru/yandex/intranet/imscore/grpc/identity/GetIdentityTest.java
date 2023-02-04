package ru.yandex.intranet.imscore.grpc.identity;

import java.util.UUID;

import com.google.protobuf.FieldMask;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.grpc.common.TestData;
import ru.yandex.intranet.imscore.proto.identity.ExternalIdentity;
import ru.yandex.intranet.imscore.proto.identity.GetIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.GetIdentityResponse;
import ru.yandex.intranet.imscore.proto.identity.Identity;
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId;
import ru.yandex.intranet.imscore.proto.identity.IdentityServiceGrpc;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;

/**
 * Get identity tests.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class GetIdentityTest {
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceBlockingStub;

    @Test
    public void getIdentityByIdTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        String id = TestData.Companion.getIdentity().getId();
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(TestData.Companion.getIdentity(), res.getIdentity());
    }

    @Test
    public void getIdentityByIdWithDataTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        String id = TestData.Companion.getIdentity().getId();
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertTrue(res.getIdentity().hasData());
        Assertions.assertEquals(TestData.Companion.getIdentityWithData(), res.getIdentity());
    }

    @Test
    public void getIdentityByExternalIdTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        String externalId = TestData.Companion.getIdentity().getExternalId();
        String typeId = TestData.Companion.getIdentity().getType().getId();

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(TestData.Companion.getIdentity(), res.getIdentity());
    }

    @Test
    public void getIdentityByExternalIdWithDataTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        String externalId = TestData.Companion.getIdentity().getExternalId();
        String typeId = TestData.Companion.getIdentity().getType().getId();

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(TestData.Companion.getIdentityWithData(), res.getIdentity());
    }

    @Test
    public void getIdentityByIdWithoutFieldMaskTest() {
        String id = TestData.Companion.getIdentity().getId();
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(TestData.Companion.getIdentity(), res.getIdentity());
    }

    @Test
    public void getIdentityByIdWithEmptyFieldMaskTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder();
        String id = TestData.Companion.getIdentity().getId();
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(TestData.Companion.getIdentity(), res.getIdentity());
    }

    @Test
    public void getIdentityByExternalIdWithoutFieldMaskTest() {
        String externalId = TestData.Companion.getIdentity().getExternalId();
        String typeId = TestData.Companion.getIdentity().getType().getId();

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(TestData.Companion.getIdentity(), res.getIdentity());
    }

    @Test
    public void getIdentityByExternalIdWithEmptyFieldMaskTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder();
        String externalId = TestData.Companion.getIdentity().getExternalId();
        String typeId = TestData.Companion.getIdentity().getType().getId();

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(TestData.Companion.getIdentity(), res.getIdentity());
    }

    @Test
    public void getIdentityByIdWithPartialFieldMaskTest() {
        String id = TestData.Companion.getIdentity().getId();

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id");
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(Identity.newBuilder()
                .setId(TestData.Companion.getIdentity().getId())
                .build(), res.getIdentity());

        fieldMask = FieldMask.newBuilder()
                .addPaths("parent_id");
        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(Identity.newBuilder()
                .setExternalId(TestData.Companion.getIdentity().getParentId())
                .build(), res.getIdentity());

        fieldMask = FieldMask.newBuilder()
                .addPaths("external_id");
        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(Identity.newBuilder()
                .setExternalId(TestData.Companion.getIdentity().getExternalId())
                .build(), res.getIdentity());

        fieldMask = FieldMask.newBuilder()
                .addPaths("type");
        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(Identity.newBuilder()
                .setType(TestData.Companion.getIdentity().getType())
                .build(), res.getIdentity());

        fieldMask = FieldMask.newBuilder()
                .addPaths("created_at");
        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(Identity.newBuilder()
                .setCreatedAt(TestData.Companion.getIdentity().getCreatedAt())
                .build(), res.getIdentity());

        fieldMask = FieldMask.newBuilder()
                .addPaths("modified_at");
        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(Identity.newBuilder()
                .setModifiedAt(TestData.Companion.getIdentity().getModifiedAt())
                .build(), res.getIdentity());
    }

    @Test
    public void getIdentityFailOnFakeIdTest() {
        String value = UUID.randomUUID().toString();
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(value)
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(req));
        Assertions.assertEquals("NOT_FOUND: resource with id " + value + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void getIdentityFailOnFakeExternalIdTest() {
        String value = UUID.randomUUID().toString();
        String fake = "fake";
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(value)
                        .setTypeId(fake)
                        .build())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(req));
        Assertions.assertEquals("NOT_FOUND: resource with externalId " + value + " and typeId " + fake +
                " not found", statusRuntimeException.getMessage());
    }

    @Test
    public void getIdentityFailOnEmptyOneOfTest() {
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(req));
        Assertions.assertEquals("INVALID_ARGUMENT: identity.identity_id_oneof is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "identity.identity_id_oneof", "OneOf must be set");
    }

    @Test
    public void getIdentityFailOnWrongUUIDOneOfTest() {
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId("NOT-UUID")
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(req));
        Assertions.assertEquals("INVALID_ARGUMENT: identity.identity_id_oneof is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "identity.identity_id_oneof.id", "Value is not a UUID");
    }

    @Test
    public void getIdentityFailOnEmptyExternalIdAndTypeIdOneOfTest() {
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId("")
                                .setTypeId("")
                                .build())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(req));
        Assertions.assertEquals("INVALID_ARGUMENT: identity.identity_id_oneof is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 2,
                new String[]{"identity.identity_id_oneof.external_identity.external_id", "Value is required"},
                new String[]{"identity.identity_id_oneof.external_identity.type_id", "Value is required"});
    }
}
