package ru.yandex.intranet.imscore.grpc.identity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.JsonParser;
import com.google.protobuf.FieldMask;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
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
import ru.yandex.intranet.imscore.proto.identityGroup.IdentityGroupServiceGrpc;
import ru.yandex.intranet.imscore.proto.identityGroup.ListIdentityGroupsRequest;
import ru.yandex.intranet.imscore.proto.identityGroup.ListIdentityGroupsResponse;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.GRPC_STRING_DEFAULT;
import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;
import static ru.yandex.intranet.imscore.grpc.identity.IdentityDataHelperTest.assertEquals;

/**
 * Create identity test
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class CreateIdentityTest {
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceBlockingStub;
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub identityGroupServiceBlockingStub;

    @Test
    public void createIdentityWithExternalIdTest() {
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
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(identity, res.getIdentity());

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(0, identitiesList.size());
    }

    @Test
    public void createIdentityWithExternalIdWithDataTest() {
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
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
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

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
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

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(0, identitiesList.size());
    }

    @Test
    public void createIdentityWithExternalIdWithDataWithParentIdTest() {
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
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
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
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup1().getId())
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(TestData.Companion.getIdentityGroup1().getId(), identity.getParentId());
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

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(identity.getParentId(), identitiesList.get(0).getId());
    }

    @Test
    public void createIdentityWithExternalIdWithDataWithParentIdWithExternalIdTest() {
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
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
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
                .setParentId(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(TestData.Companion.getIdentityGroup1().getExternalId())
                                .setTypeId(TestData.Companion.getIdentityGroup1().getType().getId())
                                .build())
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(TestData.Companion.getIdentityGroup1().getId(), identity.getParentId());
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

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(identity.getParentId(), identitiesList.get(0).getId());
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void createIdentityWithExternalIdWithPartialDataTest() {
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
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setSlug(StringValue.newBuilder()
                                        .setValue(slug)
                                        .build())
                                .build()
                )
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        IdentityData data = identity.getData();
        Assertions.assertEquals(slug, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT,
                data.getAdditionalData());

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
        Assertions.assertEquals(identity, res.getIdentity());

        externalId = UUID.randomUUID().toString();
        createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setName(StringValue.newBuilder()
                                        .setValue(name)
                                        .build())
                                .build()
                )
                .build();

        createRes = identityServiceBlockingStub.createIdentity(createReq);
        identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getSlug());
        Assertions.assertEquals(name, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT,
                data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(identity, res.getIdentity());

        externalId = UUID.randomUUID().toString();
        createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setLastname(StringValue.newBuilder()
                                        .setValue(lastName)
                                        .build())
                                .build()
                )
                .build();

        createRes = identityServiceBlockingStub.createIdentity(createReq);
        identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(lastName, data.getLastname());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT,
                data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(identity, res.getIdentity());

        externalId = UUID.randomUUID().toString();
        createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setPhone(StringValue.newBuilder()
                                        .setValue(phone)
                                        .build())
                                .build()
                )
                .build();

        createRes = identityServiceBlockingStub.createIdentity(createReq);
        identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertTrue(identity.hasData());
        data = identity.getData();
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getSlug());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getName());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getLastname());
        Assertions.assertEquals(phone, data.getPhone());
        Assertions.assertEquals(GRPC_STRING_DEFAULT, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT,
                data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(identity, res.getIdentity());

        externalId = UUID.randomUUID().toString();
        createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setEmail(StringValue.newBuilder()
                                        .setValue(email)
                                        .build())
                                .build()
                )
                .build();

        createRes = identityServiceBlockingStub.createIdentity(createReq);
        identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
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
        Assertions.assertEquals(email, data.getEmail());
        Assertions.assertEquals(GRPC_STRING_DEFAULT,
                data.getAdditionalData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(identity, res.getIdentity());

        externalId = UUID.randomUUID().toString();
        createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(additionalData)
                                        .build())
                                .build()
                )
                .build();

        createRes = identityServiceBlockingStub.createIdentity(createReq);
        identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
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
    public void createIdentityWithoutExternalIdTest() {
        String typeId = TestData.Companion.getIdentityType().getId();
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setTypeId(typeId)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(identity, res.getIdentity());

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(0, identitiesList.size());
    }

    @Test
    public void createTwoIdentityWithoutExternalIdWithSameTypeIdTest() {
        String typeId = TestData.Companion.getIdentityType().getId();
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setTypeId(typeId)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(identity, res.getIdentity());

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(0, identitiesList.size());

        createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setTypeId(typeId)
                        .build())
                .build();

        createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity2 = createRes.getIdentity();
        Assertions.assertNotNull(identity2);
        Assertions.assertNotNull(identity2.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity2.getParentId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity2.getExternalId());
        Assertions.assertEquals(typeId, identity2.getType().getId());
        Assertions.assertNotNull(identity2.getCreatedAt());
        Assertions.assertNotNull(identity2.getModifiedAt());
        Assertions.assertFalse(identity2.hasData());

        req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity2.getId())
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(identity2, res.getIdentity());

        listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity2.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(0, identitiesList.size());
    }

    @Test
    public void createIdentityWithComplexJsonTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                {
                    "id": "0001",
                    "name": "Cake",
                    "type": "donut",
                    "ppu": 0.55,
                    "batters":
                        {
                            "batter":
                                [
                                    { "id": "1001", "type": "Regular" },
                                    { "id": "1002", "type": "Chocolate" },
                                    { "id": "1003", "type": "Blueberry" },
                                    { "id": "1004", "type": "Devil's Food" }
                                ]
                        },
                    "topping":
                        [
                            { "id": "5001", "type": "None" },
                            { "id": "5002", "type": "Glazed" },
                            { "id": "5005", "type": "Sugar" },
                            { "id": "5007", "type": "Powdered Sugar" },
                            { "id": "5006", "type": "Chocolate with Sprinkles" },
                            { "id": "5003", "type": "Chocolate" },
                            { "id": "5004", "type": "Maple" }
                        ]
                }
                """;
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
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

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(StringValue.getDefaultInstance().toString(), identity.getParentId());
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
        Assertions.assertEquals(JsonParser.parseString(identity.getData().getAdditionalData()).getAsJsonObject(),
                JsonParser.parseString(res.getIdentity().getData().getAdditionalData()).getAsJsonObject());

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(0, identitiesList.size());
    }

    @Test
    public void createIdentityWithParentIdUpdateParentModifiedAtTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest getGroupRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup1().getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse getGroupResponse = identityServiceBlockingStub.getIdentity(getGroupRequest);
        Assertions.assertNotNull(getGroupResponse);
        Identity groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);
        Timestamp oldModifiedAt = groupIdentity.getModifiedAt();

        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup1().getId())
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(TestData.Companion.getIdentityGroup1().getId(), identity.getParentId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(identity.getParentId(), identitiesList.get(0).getId());

        getGroupResponse = identityServiceBlockingStub.getIdentity(getGroupRequest);
        Assertions.assertNotNull(getGroupResponse);
        groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);
        Timestamp newModifiedAt = groupIdentity.getModifiedAt();
        Assertions.assertNotEquals(oldModifiedAt, newModifiedAt);
        Assertions.assertEquals(identity.getModifiedAt(), newModifiedAt);
    }

    @Test
    public void createIdentityWithParentIdUpdateAllGroupsUpModifiedAtTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest getGroup1Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup1().getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse getGroupResponse = identityServiceBlockingStub.getIdentity(getGroup1Request);
        Assertions.assertNotNull(getGroupResponse);
        Identity groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);

        Timestamp oldModifiedAtIdentityGroup1 = groupIdentity.getModifiedAt();

        GetIdentityRequest getGroup2Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup2().getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        getGroupResponse = identityServiceBlockingStub.getIdentity(getGroup2Request);
        Assertions.assertNotNull(getGroupResponse);
        groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);

        Timestamp oldModifiedAtIdentityGroup2 = groupIdentity.getModifiedAt();

        GetIdentityRequest getGroup3Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        getGroupResponse = identityServiceBlockingStub.getIdentity(getGroup3Request);
        Assertions.assertNotNull(getGroupResponse);
        groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);

        Timestamp oldModifiedAtIdentityGroup3 = groupIdentity.getModifiedAt();

        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup1().getId())
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(TestData.Companion.getIdentityGroup1().getId(), identity.getParentId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(false)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());

        Map<String, Identity> groupIdentityById = identitiesList.stream()
                .collect(Collectors.toMap(Identity::getId, Function.identity()));

        Identity group1 = groupIdentityById.get(TestData.Companion.getIdentityGroup1().getId());
        Assertions.assertNotNull(group1);
        Timestamp newModifiedAt = group1.getModifiedAt();
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup1, newModifiedAt);
        Assertions.assertEquals(identity.getModifiedAt(), newModifiedAt);

        Identity group2 = groupIdentityById.get(TestData.Companion.getIdentityGroup2().getId());
        Assertions.assertNotNull(group2);
        newModifiedAt = group2.getModifiedAt();
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup2, newModifiedAt);
        Assertions.assertEquals(identity.getModifiedAt(), newModifiedAt);

        Identity group3 = groupIdentityById.get(TestData.Companion.getIdentityGroup3().getId());
        Assertions.assertNotNull(group3);
        newModifiedAt = group3.getModifiedAt();
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup3, newModifiedAt);
        Assertions.assertEquals(identity.getModifiedAt(), newModifiedAt);
    }

    @Test
    public void createIdentityWithParentIdDontUpdateGroupsDownModifiedAtTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        GetIdentityRequest getGroup1Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup1().getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse getGroupResponse = identityServiceBlockingStub.getIdentity(getGroup1Request);
        Assertions.assertNotNull(getGroupResponse);
        Identity groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);

        Timestamp oldModifiedAtIdentityGroup1 = groupIdentity.getModifiedAt();

        GetIdentityRequest getGroup2Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup2().getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        getGroupResponse = identityServiceBlockingStub.getIdentity(getGroup2Request);
        Assertions.assertNotNull(getGroupResponse);
        groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);

        Timestamp oldModifiedAtIdentityGroup2 = groupIdentity.getModifiedAt();

        GetIdentityRequest getGroup3Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        getGroupResponse = identityServiceBlockingStub.getIdentity(getGroup3Request);
        Assertions.assertNotNull(getGroupResponse);
        groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);

        Timestamp oldModifiedAtIdentityGroup3 = groupIdentity.getModifiedAt();

        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);
        Assertions.assertNotNull(identity.getId());
        Assertions.assertEquals(TestData.Companion.getIdentityGroup3().getId(), identity.getParentId());
        Assertions.assertEquals(externalId, identity.getExternalId());
        Assertions.assertEquals(typeId, identity.getType().getId());
        Assertions.assertNotNull(identity.getCreatedAt());
        Assertions.assertNotNull(identity.getModifiedAt());
        Assertions.assertFalse(identity.hasData());

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setFieldMask(fieldMask)
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        assertEquals(identity, res.getIdentity());

        ListIdentityGroupsRequest listIdentityGroupsRequest = ListIdentityGroupsRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .setOnlyDirectly(true)
                .build();
        ListIdentityGroupsResponse listIdentityGroupsResponse =
                identityGroupServiceBlockingStub.listIdentityGroups(listIdentityGroupsRequest);
        Assertions.assertNotNull(listIdentityGroupsResponse);
        List<Identity> identitiesList = listIdentityGroupsResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Identity group3 = identitiesList.get(0);
        Assertions.assertEquals(identity.getParentId(), group3.getId());
        Assertions.assertNotNull(group3);
        Timestamp  newModifiedAt = group3.getModifiedAt();
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup3, newModifiedAt);
        Assertions.assertEquals(identity.getModifiedAt(), newModifiedAt);

        getGroupResponse = identityServiceBlockingStub.getIdentity(getGroup1Request);
        Assertions.assertNotNull(getGroupResponse);
        groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);
        newModifiedAt = groupIdentity.getModifiedAt();
        Assertions.assertEquals(oldModifiedAtIdentityGroup1, newModifiedAt);
        Assertions.assertNotEquals(identity.getModifiedAt(), newModifiedAt);

        getGroupResponse = identityServiceBlockingStub.getIdentity(getGroup2Request);
        Assertions.assertNotNull(getGroupResponse);
        groupIdentity = getGroupResponse.getIdentity();
        Assertions.assertNotNull(groupIdentity);
        newModifiedAt = groupIdentity.getModifiedAt();
        Assertions.assertEquals(oldModifiedAtIdentityGroup2, newModifiedAt);
        Assertions.assertNotEquals(identity.getModifiedAt(), newModifiedAt);
    }

    @Test
    public void createIdentityFailOnFakeExternalIdTest() {
        String value = UUID.randomUUID().toString();
        String fake = "fake";
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(value)
                        .setTypeId(fake)
                        .build())
                .build();
        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("NOT_FOUND: IdentityType with id " + fake +
                " not found", statusRuntimeException.getMessage());
    }

    @Test
    public void createIdentityFailOnTypeIdOneOfTest() {
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setTypeId("")
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("INVALID_ARGUMENT: CreateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"external_identity.type_id", "Value is required"});
    }

    @Test
    public void createIdentityFailOnNotUUIDParentIdTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String fakeParentId = "NOT-UUID";
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(fakeParentId)
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("INVALID_ARGUMENT: CreateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"parent_id.identity_id_oneof.id", "Value is not a UUID"});
    }

    @Test
    public void createIdentityFailOnNotExistExternalIdParentIdTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String fakeExternalId = "fake";
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(fakeExternalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals(String.format("NOT_FOUND: resource with externalId %s and typeId %s not found",
                fakeExternalId, typeId), statusRuntimeException.getMessage());
    }

    @Test
    public void createIdentityFailOnNotExistExternalTypeIdParentIdTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String fakeExternalTypeId = "fake";
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(TestData.Companion.getIdentityGroup1().getExternalId())
                                .setTypeId(fakeExternalTypeId)
                                .build())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals(String.format("NOT_FOUND: resource with externalId %s and typeId %s not found",
                        TestData.Companion.getIdentityGroup1().getExternalId(), fakeExternalTypeId),
                statusRuntimeException.getMessage());
    }

    @Test
    public void createIdentityFailOnNoExistParentIdTest() {
        String externalId = UUID.randomUUID().toString();
        String parentId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(parentId)
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("NOT_FOUND: resource with id " + parentId + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void createIdentityFailOnExistExternalIdTest() {
        String externalId = TestData.Companion.getIdentity().getExternalId();
        String typeId = TestData.Companion.getIdentityType().getId();
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("ALREADY_EXISTS: Identity with externalId " + externalId + " and typeId "
                        + typeId + " already exists",
                statusRuntimeException.getMessage());
    }

    @Test
    public void createIdentityWithNotGroupParentIdTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        String parentId = TestData.Companion.getIdentity().getId();
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(parentId)
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals(
                String.format("INVALID_ARGUMENT: %s is not a group", TestData.Companion.getIdentity().getId()),
                statusRuntimeException.getMessage()
        );
    }

    @Test
    public void createIdentityWithEmptyJsonFailsTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        String parentId = TestData.Companion.getIdentity().getId();
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(parentId)
                        .build())
                .setData(
                        ModifiableIdentityData.newBuilder()
                                .setAdditionalData(StringValue.newBuilder()
                                        .setValue(GRPC_STRING_DEFAULT)
                                        .build())
                                .build()
                )
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("INVALID_ARGUMENT: CreateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"data.additional_data", "Value is invalid json"});
    }

    @Test
    public void createIdentityWithWrongJsonFailsTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        String parentId = TestData.Companion.getIdentity().getId();
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(parentId)
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
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("INVALID_ARGUMENT: CreateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"data.additional_data", "Value is invalid json"});
    }

    @Test
    public void createIdentityFailsWithNotJsonObjectTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                [
                            { "id": "5001", "type": "None" },
                            { "id": "5002", "type": "Glazed" },
                            { "id": "5005", "type": "Sugar" },
                            { "id": "5007", "type": "Powdered Sugar" },
                            { "id": "5006", "type": "Chocolate with Sprinkles" },
                            { "id": "5003", "type": "Chocolate" },
                            { "id": "5004", "type": "Maple" }
                        ]
                """;
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
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

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("INVALID_ARGUMENT: CreateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"data.additional_data", "Value is invalid json"});
    }

    @Test
    public void createIdentityFailsOnDuplicateJsonFieldTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();
        String slug = "slug";
        String name = "name";
        String lastName = "lastName";
        String phone = "phone";
        String email = "email";
        String additionalData = """
                {
                    "one": "one",
                    "one": "two",
                }
                """;
        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
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

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.createIdentity(createReq));
        Assertions.assertEquals("INVALID_ARGUMENT: CreateIdentityRequest is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"data.additional_data", "Value is invalid json"});
    }
}
