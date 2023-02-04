package ru.yandex.intranet.imscore.grpc.identity;

import java.util.UUID;

import com.google.gson.JsonParser;
import com.google.protobuf.FieldMask;
import com.google.protobuf.StringValue;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.grpc.common.TestData;
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityResponse;
import ru.yandex.intranet.imscore.proto.identity.ExternalIdentity;
import ru.yandex.intranet.imscore.proto.identity.GetIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.GetIdentityResponse;
import ru.yandex.intranet.imscore.proto.identity.Identity;
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId;
import ru.yandex.intranet.imscore.proto.identity.IdentityData;
import ru.yandex.intranet.imscore.proto.identity.IdentityServiceGrpc;
import ru.yandex.intranet.imscore.proto.identity.ModifiableIdentityData;
import ru.yandex.intranet.imscore.proto.identity.UpdateIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.UpdateIdentityResponse;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.GRPC_STRING_DEFAULT;
import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;
import static ru.yandex.intranet.imscore.grpc.identity.IdentityDataHelperTest.assertEquals;

/**
 * Update identity tests.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "checkstyle:methodlength"})
@IntegrationTest
public class UpdateIdentityTest {
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceBlockingStub;

    @Test
    public void updateIdentityWithExternalIdTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                {
                "valid": "json"
                }
                """;
        CreateIdentityRequest createRequest = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug)
                                        .build())
                                .setName(StringValue.newBuilder()
                                        .setValue(name)
                                        .build())
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName)
                                        .build())
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone)
                                        .build())
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email)
                                        .build())
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData)
                                        .build())
                                .build()
                )
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createRequest);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        IdentityData data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(JsonParser.parseString(additionalData).getAsJsonObject(),
                JsonParser.parseString(data.getAdditionalData()).getAsJsonObject());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        String slug2 = "slug2";
        String name2 = "name2";
        String lastName2 = "lastName2";
        String phone2 = "phone2";
        String email2 = "email2";
        String additionalData2 = """
                {
                    "another": "",
                    "valid": "",
                    "json": ""
                }
                """;
        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug2)
                                        .build())
                                .setName(StringValue.newBuilder()
                                        .setValue(name2)
                                        .build())
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName2)
                                        .build())
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone2)
                                        .build())
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email2)
                                        .build())
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData2)
                                        .build())
                                .build()
                )
                .build();

        UpdateIdentityResponse updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        Identity identity2 = updateRes.getIdentity();
        Assertions.assertNotNull(identity2);
        Assertions.assertNotNull(identity2.getId());
        Assertions.assertEquals(externalId, identity2.getExternalId());
        Assertions.assertEquals(typeId, identity2.getType().getId());
        Assertions.assertNotNull(identity2.getCreatedAt());
        Assertions.assertEquals(identity.getCreatedAt(), identity2.getCreatedAt());
        Assertions.assertNotNull(identity2.getModifiedAt());
        Assertions.assertNotEquals(identity.getModifiedAt(), identity2.getModifiedAt());
        Assertions.assertTrue(identity2.hasData());
        data = identity2.getData();
        Assertions.assertEquals(slug2, data.getSlug());
        Assertions.assertEquals(name2, data.getName());
        Assertions.assertEquals(lastName2, data.getLastname());
        Assertions.assertEquals(phone2, data.getPhone());
        Assertions.assertEquals(email2, data.getEmail());
        Assertions.assertEquals(JsonParser.parseString(additionalData2).getAsJsonObject(),
                JsonParser.parseString(data.getAdditionalData()).getAsJsonObject());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity2.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity2, res.getIdentity());
    }

    @Test
    public void updateIdentityWithIdTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                {
                "valid": "json"
                }
                """;
        CreateIdentityRequest createRequest = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug)
                                        .build())
                                .setName(StringValue.newBuilder()
                                        .setValue(name)
                                        .build())
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName)
                                        .build())
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone)
                                        .build())
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email)
                                        .build())
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData)
                                        .build())
                                .build()
                )
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createRequest);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        IdentityData data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(JsonParser.parseString(additionalData).getAsJsonObject(),
                JsonParser.parseString(data.getAdditionalData()).getAsJsonObject());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        String slug2 = "slug2";
        String name2 = "name2";
        String lastName2 = "lastName2";
        String phone2 = "phone2";
        String email2 = "email2";
        String additionalData2 = """
                {
                    "another": "",
                    "valid": "",
                    "json": ""
                }
                """;
        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug2)
                                        .build())
                                .setName(StringValue.newBuilder()
                                        .setValue(name2)
                                        .build())
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName2)
                                        .build())
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone2)
                                        .build())
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email2)
                                        .build())
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData2)
                                        .build())
                                .build()
                )
                .build();

        UpdateIdentityResponse updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        Identity identity2 = updateRes.getIdentity();
        Assertions.assertNotNull(identity2);
        Assertions.assertNotNull(identity2.getId());
        Assertions.assertEquals(externalId, identity2.getExternalId());
        Assertions.assertEquals(typeId, identity2.getType().getId());
        Assertions.assertNotNull(identity2.getCreatedAt());
        Assertions.assertEquals(identity.getCreatedAt(), identity2.getCreatedAt());
        Assertions.assertNotNull(identity2.getModifiedAt());
        Assertions.assertNotEquals(identity.getModifiedAt(), identity2.getModifiedAt());
        Assertions.assertTrue(identity2.hasData());
        data = identity2.getData();
        Assertions.assertEquals(slug2, data.getSlug());
        Assertions.assertEquals(name2, data.getName());
        Assertions.assertEquals(lastName2, data.getLastname());
        Assertions.assertEquals(phone2, data.getPhone());
        Assertions.assertEquals(email2, data.getEmail());
        Assertions.assertEquals(JsonParser.parseString(additionalData2).getAsJsonObject(),
                JsonParser.parseString(data.getAdditionalData()).getAsJsonObject());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity2.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity2, res.getIdentity());
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void updateIdentityWithExternalIdWithPartialDataTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                {
                    "one": "",
                    "more": [
                        2
                    ],
                    "valid": "",
                    "json": ""
                }
                """;
        CreateIdentityRequest createRequest = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createRequest);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());
        IdentityData data = identity.getData();
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug)
                                        .build())
                                .build()
                )
                .build();
        UpdateIdentityResponse updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setName(StringValue.newBuilder()
                                        .setValue(name)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(JsonParser.parseString(additionalData).getAsJsonObject(),
                JsonParser.parseString(data.getAdditionalData()).getAsJsonObject());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void updateIdentityWithIdWithPartialDataTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                {
                    "one": "",
                    "more": [
                        2
                    ],
                    "valid": "",
                    "json": ""
                }
                """;
        CreateIdentityRequest createRequest = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createRequest);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());
        IdentityData data = identity.getData();
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug)
                                        .build())
                                .build()
                )
                .build();

        UpdateIdentityResponse updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setName(StringValue.newBuilder()
                                        .setValue(name)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData)
                                        .build())
                                .build()
                )
                .build();

        updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(JsonParser.parseString(additionalData).getAsJsonObject(),
                JsonParser.parseString(data.getAdditionalData()).getAsJsonObject());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());
    }

    @Test
    public void updateIdentityWithExternalIdWithNullingDataTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                {
                    "one": "",
                    "more": [
                        2
                    ],
                    "valid": "",
                    "json": ""
                }
                """;
        CreateIdentityRequest createRequest = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug)
                                        .build())
                                .setName(StringValue.newBuilder()
                                        .setValue(name)
                                        .build())
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName)
                                        .build())
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone)
                                        .build())
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email)
                                        .build())
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData)
                                        .build())
                                .build()
                )
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createRequest);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        IdentityData data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(JsonParser.parseString(additionalData).getAsJsonObject(),
                JsonParser.parseString(data.getAdditionalData()).getAsJsonObject());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());


        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setName(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setLastname(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setPhone(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setEmail(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .build()
                )
                .build();

        UpdateIdentityResponse updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());
    }

    @Test
    public void updateIdentityWithIdWithNullingDataTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                {
                    "one": "",
                    "more": [
                        2
                    ],
                    "valid": "",
                    "json": ""
                }
                """;
        CreateIdentityRequest createRequest = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug)
                                        .build())
                                .setName(StringValue.newBuilder()
                                        .setValue(name)
                                        .build())
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName)
                                        .build())
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone)
                                        .build())
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email)
                                        .build())
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData)
                                        .build())
                                .build()
                )
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createRequest);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        IdentityData data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(JsonParser.parseString(additionalData).getAsJsonObject(),
                JsonParser.parseString(data.getAdditionalData()).getAsJsonObject());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setName(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setLastname(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setPhone(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setEmail(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .build()
                )
                .build();

        UpdateIdentityResponse updateRes = identityServiceBlockingStub.updateIdentity(updateReq);
        identity = updateRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());
    }

    @Test
    public void updateIdentityFailOnEmptyOneOfTest() {
        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.updateIdentity(updateReq));
        Assertions.assertEquals("INVALID_ARGUMENT: UpdateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "identity.identity_id_oneof", "OneOf must be set");
    }

    @Test
    public void updateIdentityFailOnWrongUUIDOneOfTest() {
        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId("NOT-UUID")
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.updateIdentity(updateReq));
        Assertions.assertEquals("INVALID_ARGUMENT: UpdateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "identity.identity_id_oneof.id", "Value is not a UUID");
    }

    @Test
    public void updateIdentityFailOnEmptyExternalIdAndTypeIdOneOfTest() {
        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId("")
                                .setTypeId("")
                                .build())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.updateIdentity(updateReq));
        Assertions.assertEquals("INVALID_ARGUMENT: UpdateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 2,
                new String[]{"identity.identity_id_oneof.external_identity.external_id", "Value is required"},
                new String[]{"identity.identity_id_oneof.external_identity.type_id", "Value is required"});
    }

    @Test
    public void updateIdentityWithWrongJsonFailsTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();
        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);

        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue("{ wrong json }")
                                        .build())
                                .build()
                )
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.updateIdentity(updateReq));
        Assertions.assertEquals("INVALID_ARGUMENT: UpdateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"data.additional_data", "Value is invalid json"});
    }

    @Test
    public void updateIdentityFailsWithNotJsonObjectTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();
        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);

        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue("""
                [
                        { "id": "5001", "type": "None" },
                            { "id": "5002", "type": "Glazed" },
                            { "id": "5005", "type": "Sugar" },
                            { "id": "5007", "type": "Powdered Sugar" },
                            { "id": "5006", "type": "Chocolate with Sprinkles" },
                            { "id": "5003", "type": "Chocolate" },
                            { "id": "5004", "type": "Maple" }
                        ]
                """)
                                        .build())
                                .build()
                )
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.updateIdentity(updateReq));
        Assertions.assertEquals("INVALID_ARGUMENT: UpdateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"data.additional_data", "Value is invalid json"});
    }

    @Test
    public void updateIdentityFailsOnDuplicateJsonFieldTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();
        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);

        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        UpdateIdentityRequest updateReq = UpdateIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue("""
                {
                    "one": "one",
                    "one": "two",
                }
                """)
                                        .build())
                                .build()
                )
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.updateIdentity(updateReq));
        Assertions.assertEquals("INVALID_ARGUMENT: UpdateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"data.additional_data", "Value is invalid json"});
    }
}
