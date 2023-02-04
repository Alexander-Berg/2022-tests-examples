package ru.yandex.intranet.imscore.grpc.identity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.protobuf.FieldMask;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.grpc.common.TestData;
import ru.yandex.intranet.imscore.infrastructure.presentation.grpc.validators.GrpcValidatorUtils;
import ru.yandex.intranet.imscore.proto.identity.Identity;
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId;
import ru.yandex.intranet.imscore.proto.identity.IdentityServiceGrpc;
import ru.yandex.intranet.imscore.proto.identity.ListIdentitiesRequest;
import ru.yandex.intranet.imscore.proto.identity.ListIdentitiesResponse;
import ru.yandex.intranet.imscore.proto.identityGroup.AddToGroupRequest;
import ru.yandex.intranet.imscore.proto.identityGroup.IdentityGroupServiceGrpc;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertEqualsWithoutUpdateDate;
import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;

@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class ListIdentityTest {
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceBlockingStub;
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub identityGroupServiceBlockingStub;

    @BeforeEach
    public void beforeEach() {
        AddToGroupRequest add = AddToGroupRequest.newBuilder()
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup1().getId())
                        .build())
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build())
                .build();
        identityGroupServiceBlockingStub.addToGroup(add);
    }

    @Test
    public void getListTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(100)
                .setFieldMask(fieldMask)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(6, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        assertEqualsWithoutUpdateDate(Set.of(TestData.Companion.getIdentity(),
                TestData.Companion.getIdentityGroup1(),
                TestData.Companion.getIdentityGroup2(),
                TestData.Companion.getIdentityGroup3(),
                TestData.Companion.getIdentityGroup4(),
                TestData.Companion.getIdentityGroup4SubGroup1()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListWithDataTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(100)
                .setFieldMask(fieldMask)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(6, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        assertEqualsWithoutUpdateDate(Set.of(TestData.Companion.getIdentityWithData(),
                TestData.Companion.getIdentityGroup1WithData(),
                TestData.Companion.getIdentityGroup2(),
                TestData.Companion.getIdentityGroup3(),
                TestData.Companion.getIdentityGroup4(),
                TestData.Companion.getIdentityGroup4SubGroup1()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListByGroupTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(100)
                .setFieldMask(fieldMask)
                .setGroupId(TestData.Companion.getIdentityGroup3().getId())
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(TestData.Companion.getIdentity(),
                TestData.Companion.getIdentityGroup1(),
                TestData.Companion.getIdentityGroup2()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListByGroupWithDataTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at")
                .addPaths("data");


        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(100)
                .setFieldMask(fieldMask)
                .setGroupId(TestData.Companion.getIdentityGroup3().getId())
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(TestData.Companion.getIdentityWithData(),
                TestData.Companion.getIdentityGroup1WithData(),
                TestData.Companion.getIdentityGroup2()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListWithPartialFieldMaskTest() {
        ListIdentitiesRequest.Builder listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(1);

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id");
        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setId(TestData.Companion.getIdentity().getId())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("parent_id");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setParentId(TestData.Companion.getIdentity().getParentId())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("external_id");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setExternalId(TestData.Companion.getIdentity().getExternalId())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("type");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setType(TestData.Companion.getIdentity().getType())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("created_at");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setCreatedAt(TestData.Companion.getIdentity().getCreatedAt())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("modified_at");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setModifiedAt(TestData.Companion.getIdentity().getModifiedAt())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("data");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setData(TestData.Companion.getIdentityData())
                .build()), new HashSet<>(identitiesList));

        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.clearFieldMask().build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(TestData.Companion.getIdentity()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListByGroupWithPartialFieldMaskTest() {
        ListIdentitiesRequest.Builder listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(2)
                .setGroupId(TestData.Companion.getIdentityGroup1().getId());

        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id");
        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setId(TestData.Companion.getIdentity().getId())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("parent_id");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setParentId(TestData.Companion.getIdentity().getParentId())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("external_id");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setExternalId(TestData.Companion.getIdentity().getExternalId())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("type");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setType(TestData.Companion.getIdentity().getType())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("created_at");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setCreatedAt(TestData.Companion.getIdentity().getCreatedAt())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("modified_at");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setModifiedAt(TestData.Companion.getIdentity().getModifiedAt())
                .build()), new HashSet<>(identitiesList));

        fieldMask = FieldMask.newBuilder()
                .addPaths("data");
        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.setFieldMask(fieldMask).build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(Identity.newBuilder()
                .setData(TestData.Companion.getIdentityData())
                .build()), new HashSet<>(identitiesList));

        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest.clearFieldMask().build());
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(TestData.Companion.getIdentity()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListByGroupIdWithOnlyDirectlyTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(100)
                .setFieldMask(fieldMask)
                .setGroupId(TestData.Companion.getIdentityGroup3().getId())
                .setOnlyDirectly(true)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(2, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(
                TestData.Companion.getIdentityGroup1(),
                TestData.Companion.getIdentityGroup2()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListWithPagingTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(2)
                .setFieldMask(fieldMask)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(2, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(TestData.Companion.getIdentity(),
                TestData.Companion.getIdentityGroup1()), new HashSet<>(identitiesList));

        listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageToken(listIdentitiesResponse.getNextPageToken())
                .setPageSize(2)
                .setFieldMask(fieldMask)
                .build();

        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(2, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        assertEqualsWithoutUpdateDate(Set.of(TestData.Companion.getIdentityGroup2(),
                TestData.Companion.getIdentityGroup3()), new HashSet<>(identitiesList));

        listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageToken(listIdentitiesResponse.getNextPageToken())
                .setPageSize(3)
                .setFieldMask(fieldMask)
                .build();

        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(2, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(TestData.Companion.getIdentityGroup4(),
                TestData.Companion.getIdentityGroup4SubGroup1()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListByGroupIdWithOnlyDirectlyWithPagingTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(1)
                .setFieldMask(fieldMask)
                .setGroupId(TestData.Companion.getIdentityGroup3().getId())
                .setOnlyDirectly(true)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(TestData.Companion.getIdentityGroup1()), new HashSet<>(identitiesList));

        listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(1)
                .setFieldMask(fieldMask)
                .setGroupId(TestData.Companion.getIdentityGroup3().getId())
                .setOnlyDirectly(true)
                .setPageToken(listIdentitiesResponse.getNextPageToken())
                .build();

        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(1, identitiesList.size());
        Assertions.assertNotEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        Assertions.assertEquals(Set.of(TestData.Companion.getIdentityGroup2()), new HashSet<>(identitiesList));

        listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(2)
                .setFieldMask(fieldMask)
                .setGroupId(TestData.Companion.getIdentityGroup3().getId())
                .setOnlyDirectly(true)
                .setPageToken(listIdentitiesResponse.getNextPageToken())
                .build();

        listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(0, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
    }

    @Test
    public void getListWithOnlyDirectlySetTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(100)
                .setFieldMask(fieldMask)
                .setOnlyDirectly(true)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        Assertions.assertNotNull(listIdentitiesResponse);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(6, identitiesList.size());
        Assertions.assertEquals(GrpcValidatorUtils.Companion.getDefaultValue(),
                listIdentitiesResponse.getNextPageToken());
        assertEqualsWithoutUpdateDate(Set.of(TestData.Companion.getIdentity(),
                TestData.Companion.getIdentityGroup1(),
                TestData.Companion.getIdentityGroup2(),
                TestData.Companion.getIdentityGroup3(),
                TestData.Companion.getIdentityGroup4(),
                TestData.Companion.getIdentityGroup4SubGroup1()), new HashSet<>(identitiesList));
    }

    @Test
    public void getListFailOnNotExistingGroupIdTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        String value = UUID.randomUUID().toString();
        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(1)
                .setFieldMask(fieldMask)
                .setGroupId(value)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.listIdentities(listIdentitiesRequest));
        Assertions.assertEquals("NOT_FOUND: resource with id " + value + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void getListFailOnNotGroupGroupIdTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(1)
                .setFieldMask(fieldMask)
                .setGroupId(TestData.Companion.getIdentity().getId())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.listIdentities(listIdentitiesRequest));
        Assertions.assertEquals("INVALID_ARGUMENT: " + TestData.Companion.getIdentity().getId()
                        + " is not a group",
                statusRuntimeException.getMessage());
    }

    @Test
    public void getListFailOnBadPageTokenTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(1)
                .setFieldMask(fieldMask)
                .setPageToken("FAKE-PAGE-TOKEN")
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.listIdentities(listIdentitiesRequest));
        Assertions.assertEquals("INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "page_token", "Value is not a page token");
    }

    @Test
    public void getListFailOnBadPageSizeTest() {
        FieldMask.Builder fieldMask = FieldMask.newBuilder()
                .addPaths("id")
                .addPaths("parent_id")
                .addPaths("external_id")
                .addPaths("type")
                .addPaths("created_at")
                .addPaths("modified_at");

        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setPageSize(-1)
                .setFieldMask(fieldMask)
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.listIdentities(listIdentitiesRequest));
        Assertions.assertEquals("INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "page_size", "Value should be greater than or equal to 0");

        ListIdentitiesRequest listIdentitiesRequest2 = ListIdentitiesRequest.newBuilder()
                .setPageSize(1001)
                .setFieldMask(fieldMask)
                .build();

        statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.listIdentities(listIdentitiesRequest2));
        Assertions.assertEquals("INVALID_ARGUMENT: INVALID_ARGUMENT",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "page_size", "Value should be less than or equal to 1000");
    }
}
