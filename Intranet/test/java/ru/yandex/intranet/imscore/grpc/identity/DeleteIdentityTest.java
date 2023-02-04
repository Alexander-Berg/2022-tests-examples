package ru.yandex.intranet.imscore.grpc.identity;

import java.util.UUID;

import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.grpc.common.TestData;
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityResponse;
import ru.yandex.intranet.imscore.proto.identity.DeleteIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.ExternalIdentity;
import ru.yandex.intranet.imscore.proto.identity.GetIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.GetIdentityResponse;
import ru.yandex.intranet.imscore.proto.identity.Identity;
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId;
import ru.yandex.intranet.imscore.proto.identity.IdentityServiceGrpc;
import ru.yandex.intranet.imscore.proto.identityGroup.AddToGroupRequest;
import ru.yandex.intranet.imscore.proto.identityGroup.IdentityGroupServiceGrpc;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;

/**
 * Delete identity tests.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class DeleteIdentityTest {
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceBlockingStub;

    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub identityGroupServiceBlockingStub;

    @Test
    public void deleteIdentityWithIdTest() {
        String id = TestData.Companion.getIdentity().getId();
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();
        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(req));
        Assertions.assertEquals("NOT_FOUND: resource with id " + id + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void deleteIdentityWithExternalIdTest() {
        String id = TestData.Companion.getIdentity().getId();
        String externalId = TestData.Companion.getIdentity().getExternalId();
        String typeId = TestData.Companion.getIdentity().getType().getId();
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(externalId)
                                .setTypeId(typeId)
                                .build())
                        .build())
                .build();
        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(req));
        Assertions.assertEquals("NOT_FOUND: resource with id " + id + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void deleteIdentityWithoutParentUpdateGroupsModifiedAtTest() {
        String id = TestData.Companion.getIdentity().getId();
        String groupId = TestData.Companion.getIdentityGroup1().getId();

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(groupId)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Identity groupIdentity = res.getIdentity();
        Assertions.assertNotNull(groupIdentity);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();

        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Identity updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity.getModifiedAt(), updatedGroupIdentity.getModifiedAt());
    }

    @Test
    public void deleteIdentityWithoutParentUpdateAllGroupsUpModifiedAtTest() {
        String id = TestData.Companion.getIdentity().getId();
        String group1Id = TestData.Companion.getIdentityGroup1().getId();
        String group2Id = TestData.Companion.getIdentityGroup2().getId();
        String group3Id = TestData.Companion.getIdentityGroup3().getId();

        GetIdentityRequest req1 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group1Id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity groupIdentity1 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity1);

        GetIdentityRequest req2 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group2Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        Identity groupIdentity2 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity2);

        GetIdentityRequest req3 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group3Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        Identity groupIdentity3 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity3);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();

        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity1.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity2.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity3.getModifiedAt(), updatedGroupIdentity.getModifiedAt());
    }

    @Test
    public void deleteIdentityWithoutParentDontUpdateGroupsDownModifiedAtTest() {
        String group1Id = TestData.Companion.getIdentityGroup1().getId();
        String group2Id = TestData.Companion.getIdentityGroup2().getId();
        String group3Id = TestData.Companion.getIdentityGroup3().getId();

        GetIdentityRequest req1 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group1Id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity groupIdentity1 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity1);

        GetIdentityRequest req2 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group2Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        Identity groupIdentity2 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity2);

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

        AddToGroupRequest addToGroupRequest = AddToGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(group3Id)
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        Empty empty = identityGroupServiceBlockingStub.addToGroup(addToGroupRequest);
        Assertions.assertNotNull(empty);

        GetIdentityRequest req3 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group3Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        Identity groupIdentity3 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity3);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertEquals(groupIdentity1.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertEquals(groupIdentity2.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity3.getModifiedAt(), updatedGroupIdentity.getModifiedAt());
    }

    @Test
    public void deleteIdentityWithParentUpdateGroupsModifiedAtTest() {
        String id = TestData.Companion.getIdentityGroup4SubGroup1().getId();
        String groupId = TestData.Companion.getIdentityGroup4().getId();

        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(groupId)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Identity groupIdentity = res.getIdentity();
        Assertions.assertNotNull(groupIdentity);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();

        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);
        Identity updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity.getModifiedAt(), updatedGroupIdentity.getModifiedAt());
    }

    @Test
    public void deleteIdentityWithParentUpdateAllGroupsUpModifiedAtTest() {
        String group1Id = TestData.Companion.getIdentityGroup1().getId();
        String group2Id = TestData.Companion.getIdentityGroup2().getId();
        String group3Id = TestData.Companion.getIdentityGroup3().getId();

        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(group1Id)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);

        GetIdentityRequest req1 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group1Id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity groupIdentity1 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity1);

        GetIdentityRequest req2 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group2Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        Identity groupIdentity2 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity2);

        GetIdentityRequest req3 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group3Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        Identity groupIdentity3 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity3);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity1.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity2.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity3.getModifiedAt(), updatedGroupIdentity.getModifiedAt());
    }

    @Test
    public void deleteIdentityWithParentDontUpdateGroupsDownModifiedAtTest() {
        String group1Id = TestData.Companion.getIdentityGroup1().getId();
        String group2Id = TestData.Companion.getIdentityGroup2().getId();
        String group3Id = TestData.Companion.getIdentityGroup3().getId();

        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(group3Id)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);

        GetIdentityRequest req1 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group1Id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity groupIdentity1 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity1);

        GetIdentityRequest req2 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group2Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        Identity groupIdentity2 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity2);

        GetIdentityRequest req3 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group3Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        Identity groupIdentity3 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity3);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertEquals(groupIdentity1.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertEquals(groupIdentity2.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity3.getModifiedAt(), updatedGroupIdentity.getModifiedAt());
    }

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void deleteIdentityUpdateChildrenGroupsModifiedAtTest() {
        String group1Id = TestData.Companion.getIdentityGroup1().getId();
        String group2Id = TestData.Companion.getIdentityGroup2().getId();
        String group3Id = TestData.Companion.getIdentityGroup3().getId();

        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityGroupType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(group3Id)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);

        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);

        CreateIdentityRequest create1ChildReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(UUID.randomUUID().toString())
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        createRes = identityServiceBlockingStub.createIdentity(create1ChildReq);

        Identity identity1Child = createRes.getIdentity();
        Assertions.assertNotNull(identity1Child);

        CreateIdentityRequest create2ChildReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(UUID.randomUUID().toString())
                        .setTypeId(typeId)
                        .build())
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(identity1Child.getId())
                        .build())
                .build();

        createRes = identityServiceBlockingStub.createIdentity(create2ChildReq);

        Identity identity2Child = createRes.getIdentity();
        Assertions.assertNotNull(identity2Child);

        CreateIdentityRequest create2Child1GroupReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(UUID.randomUUID().toString())
                        .setTypeId(typeId)
                        .build())
                .build();

        createRes = identityServiceBlockingStub.createIdentity(create2Child1GroupReq);

        Identity create2Child1Group = createRes.getIdentity();
        Assertions.assertNotNull(create2Child1Group);

        CreateIdentityRequest create2Child2GroupReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(UUID.randomUUID().toString())
                        .setTypeId(typeId)
                        .build())
                .build();

        createRes = identityServiceBlockingStub.createIdentity(create2Child2GroupReq);

        Identity create2Child2Group = createRes.getIdentity();
        Assertions.assertNotNull(create2Child2Group);

        CreateIdentityRequest create1Child1GroupReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(UUID.randomUUID().toString())
                        .setTypeId(typeId)
                        .build())
                .build();

        createRes = identityServiceBlockingStub.createIdentity(create1Child1GroupReq);

        Identity create1Child1Group = createRes.getIdentity();
        Assertions.assertNotNull(create1Child1Group);

        CreateIdentityRequest create1Child2GroupReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(UUID.randomUUID().toString())
                        .setTypeId(typeId)
                        .build())
                .build();

        createRes = identityServiceBlockingStub.createIdentity(create1Child2GroupReq);

        Identity create1Child2Group = createRes.getIdentity();
        Assertions.assertNotNull(create1Child2Group);

        AddToGroupRequest addToGroupRequest = AddToGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(create1Child1Group.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(identity1Child.getId())
                        .build())
                .build();

        Empty empty = identityGroupServiceBlockingStub.addToGroup(addToGroupRequest);
        Assertions.assertNotNull(empty);

        addToGroupRequest = AddToGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(create1Child2Group.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(identity1Child.getId())
                        .build())
                .build();

        empty = identityGroupServiceBlockingStub.addToGroup(addToGroupRequest);
        Assertions.assertNotNull(empty);

        addToGroupRequest = AddToGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(create2Child1Group.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(identity2Child.getId())
                        .build())
                .build();

        empty = identityGroupServiceBlockingStub.addToGroup(addToGroupRequest);
        Assertions.assertNotNull(empty);

        addToGroupRequest = AddToGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(create2Child2Group.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(identity2Child.getId())
                        .build())
                .build();

        empty = identityGroupServiceBlockingStub.addToGroup(addToGroupRequest);
        Assertions.assertNotNull(empty);

        GetIdentityRequest req1 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group1Id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity groupIdentity1 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity1);

        GetIdentityRequest req2 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group2Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        Identity groupIdentity2 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity2);

        GetIdentityRequest req3 = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(group3Id)
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        Identity groupIdentity3 = res.getIdentity();
        Assertions.assertNotNull(groupIdentity3);

        GetIdentityRequest req1Child1Group = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(create1Child1Group.getId())
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req1Child1Group);
        Assertions.assertNotNull(res);
        Identity group1Child1 = res.getIdentity();
        Assertions.assertNotNull(group1Child1);

        GetIdentityRequest req1Child2Group = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(create1Child2Group.getId())
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req1Child2Group);
        Assertions.assertNotNull(res);
        Identity group2Child1 = res.getIdentity();
        Assertions.assertNotNull(group2Child1);

        GetIdentityRequest req2Child1Group = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(create2Child1Group.getId())
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req2Child1Group);
        Assertions.assertNotNull(res);
        Identity group1Child2 = res.getIdentity();
        Assertions.assertNotNull(group1Child2);

        GetIdentityRequest req2Child2Group = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(create2Child2Group.getId())
                        .build())
                .build();

        res = identityServiceBlockingStub.getIdentity(req2Child2Group);
        Assertions.assertNotNull(res);
        Identity group2Child2 = res.getIdentity();
        Assertions.assertNotNull(group2Child2);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        res = identityServiceBlockingStub.getIdentity(req1);
        Assertions.assertNotNull(res);
        Identity updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertEquals(groupIdentity1.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req2);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertEquals(groupIdentity2.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req3);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(groupIdentity3.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req1Child1Group);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(group1Child1.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req1Child2Group);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(group1Child2.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req2Child1Group);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(group2Child1.getModifiedAt(), updatedGroupIdentity.getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(req2Child2Group);
        Assertions.assertNotNull(res);
        updatedGroupIdentity = res.getIdentity();
        Assertions.assertNotNull(updatedGroupIdentity);

        Assertions.assertNotEquals(group2Child2.getModifiedAt(), updatedGroupIdentity.getModifiedAt());
    }

    @Test
    public void deleteIdentityFailOnFakeIdTest() {
        String value = UUID.randomUUID().toString();
        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(value)
                        .build())
                .build();
        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest));
        Assertions.assertEquals("NOT_FOUND: resource with id " + value + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void deleteIdentityFailOnFakeExternalIdTest() {
        String value = UUID.randomUUID().toString();
        String fake = "fake";
        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(value)
                                .setTypeId(fake)
                                .build())
                        .build())
                .build();
        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest));
        Assertions.assertEquals("NOT_FOUND: resource with externalId " + value + " and typeId " + fake +
                " not found", statusRuntimeException.getMessage());
    }

    @Test
    public void deleteIdentityWithIdTwiceFailTest() {
        String id = TestData.Companion.getIdentity().getId();
        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();
        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest));
        Assertions.assertEquals("NOT_FOUND: resource with id " + id + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void deleteIdentityFailOnEmptyOneOfTest() {
        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest));
        Assertions.assertEquals("INVALID_ARGUMENT: id_oneof is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "identity.identity_id_oneof", "OneOf must be set");
    }

    @Test
    public void deleteIdentityFailOnWrongUUIDOneOfTest() {
        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId("NOT-UUID")
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest));
        Assertions.assertEquals("INVALID_ARGUMENT: id_oneof is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, "identity.identity_id_oneof.id", "Value is not a UUID");
    }

    @Test
    public void deleteIdentityFailOnEmptyExternalIdAndTypeIdOneOfTest() {
        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId("")
                                .setTypeId("")
                                .build())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest));
        Assertions.assertEquals("INVALID_ARGUMENT: id_oneof is invalid",
                statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 2,
                new String[]{"identity.identity_id_oneof.external_identity.external_id", "Value is required"},
                new String[]{"identity.identity_id_oneof.external_identity.type_id", "Value is required"});
    }

    @Test
    public void deleteIdentityWithIdOnlySingleRowTest() {
        String anotherIdentityId = TestData.Companion.getIdentityGroup1().getId();
        GetIdentityRequest getAnotherIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(anotherIdentityId)
                        .build())
                .build();
        GetIdentityResponse getAnotherIdentityRes = identityServiceBlockingStub.getIdentity(getAnotherIdentityRequest);
        Assertions.assertNotNull(getAnotherIdentityRes);

        String id = TestData.Companion.getIdentity().getId();
        GetIdentityRequest req = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(req);
        Assertions.assertNotNull(res);

        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .build();
        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(req));
        Assertions.assertEquals("NOT_FOUND: resource with id " + id + " not found",
                statusRuntimeException.getMessage());

        getAnotherIdentityRes = identityServiceBlockingStub.getIdentity(getAnotherIdentityRequest);
        Assertions.assertNotNull(getAnotherIdentityRes);
    }

    @Test
    public void deleteParentIdentityTest() {
        String parentId = TestData.Companion.getIdentityGroup4().getId();
        GetIdentityRequest getParentReq = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(parentId)
                        .build())
                .build();
        GetIdentityResponse getParentRes = identityServiceBlockingStub.getIdentity(getParentReq);
        Assertions.assertNotNull(getParentRes);

        String childId = TestData.Companion.getIdentityGroup4SubGroup1().getId();
        GetIdentityRequest getChildReq = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(childId)
                        .build())
                .build();
        GetIdentityResponse getChildRes = identityServiceBlockingStub.getIdentity(getChildReq);
        Assertions.assertNotNull(getChildRes);


        DeleteIdentityRequest deleteIdentityRequest = DeleteIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(parentId)
                        .build())
                .build();
        Empty empty = identityServiceBlockingStub.deleteIdentity(deleteIdentityRequest);
        Assertions.assertNotNull(empty);

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(getParentReq));
        Assertions.assertEquals("NOT_FOUND: resource with id " + parentId + " not found",
                statusRuntimeException.getMessage());

        statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(getChildReq));
        Assertions.assertEquals("NOT_FOUND: resource with id " + childId + " not found",
                statusRuntimeException.getMessage());
    }
}
