package ru.yandex.intranet.imscore.grpc.identityGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.grpc.common.TestData;
import ru.yandex.intranet.imscore.grpc.identity.IdentityDataHelperTest;
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityResponse;
import ru.yandex.intranet.imscore.proto.identity.ExternalIdentity;
import ru.yandex.intranet.imscore.proto.identity.GetIdentityRequest;
import ru.yandex.intranet.imscore.proto.identity.GetIdentityResponse;
import ru.yandex.intranet.imscore.proto.identity.Identity;
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId;
import ru.yandex.intranet.imscore.proto.identity.IdentityServiceGrpc;
import ru.yandex.intranet.imscore.proto.identity.ListIdentitiesRequest;
import ru.yandex.intranet.imscore.proto.identity.ListIdentitiesResponse;
import ru.yandex.intranet.imscore.proto.identityGroup.AddToGroupRequest;
import ru.yandex.intranet.imscore.proto.identityGroup.IdentityGroupServiceGrpc;
import ru.yandex.intranet.imscore.proto.identityGroup.ReplaceGroupRequest;

import static ru.yandex.intranet.imscore.grpc.GrpcHelperTest.assertMetadata;

/**
 * Replace group test
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@IntegrationTest
public class ReplaceGroupTest {
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityServiceGrpc.IdentityServiceBlockingStub identityServiceBlockingStub;
    @SuppressWarnings("unused")
    @GrpcClient("inProcess")
    private IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub identityGroupServiceBlockingStub;

    private Identity groupIdentity;
    private Identity firstGroupMemberOfGroup;
    private Identity secondIdentityMemberOfGroup;
    private Identity thirdGroupChildOfGroup;
    private Identity firstNotInGroup;
    private Identity secondNotInGroup;
    private Identity childOfThirdGroupChildOfGroup;
    private Identity childOfFirstGroupMemberOfGroup;

    @BeforeEach
    public void beforeEach() {
        String typeId = TestData.Companion.getIdentityType().getId();
        String groupTypeId = TestData.Companion.getIdentityGroupType().getId();

        CreateIdentityRequest.Builder createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setTypeId(groupTypeId)
                        .build());

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq.build());
        groupIdentity = createRes.getIdentity();

        createRes = identityServiceBlockingStub.createIdentity(createReq.build());
        firstGroupMemberOfGroup = createRes.getIdentity();

        createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setTypeId(typeId)
                        .build());

        createRes = identityServiceBlockingStub.createIdentity(CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId("ext")
                        .setTypeId(groupTypeId)
                        .build()).build());
        secondIdentityMemberOfGroup = createRes.getIdentity();

        createRes = identityServiceBlockingStub.createIdentity(createReq.build());
        firstNotInGroup = createRes.getIdentity();

        createRes = identityServiceBlockingStub.createIdentity(createReq.build());
        secondNotInGroup = createRes.getIdentity();

        createRes = identityServiceBlockingStub.createIdentity(createReq
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setTypeId(groupTypeId)
                        .build())
                .build());
        thirdGroupChildOfGroup = createRes.getIdentity();

        createRes = identityServiceBlockingStub.createIdentity(createReq
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(thirdGroupChildOfGroup.getId())
                        .build())
                .build());
        childOfThirdGroupChildOfGroup = createRes.getIdentity();

        createRes = identityServiceBlockingStub.createIdentity(createReq
                .setParentId(IdentityCompositeId.newBuilder()
                        .setId(firstGroupMemberOfGroup.getId())
                        .build())
                .build());
        childOfFirstGroupMemberOfGroup = createRes.getIdentity();

        AddToGroupRequest.Builder add = AddToGroupRequest.newBuilder()
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstGroupMemberOfGroup.getId())
                        .build())
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build());
        identityGroupServiceBlockingStub.addToGroup(add.build());
        identityGroupServiceBlockingStub.addToGroup(add
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(secondIdentityMemberOfGroup.getId())
                        .build())
                .build());
    }

    @Test
    public void fullReplaceGroupTest() {
        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setGroupId(groupIdentity.getId())
                .setOnlyDirectly(true)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        IdentityDataHelperTest.assertEquals(
                Set.of(firstGroupMemberOfGroup, secondIdentityMemberOfGroup, thirdGroupChildOfGroup),
                new HashSet<>(identitiesList), false);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstNotInGroup.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(secondNotInGroup.getId())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        listIdentitiesResponse = identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(2, identitiesList.size());
        IdentityDataHelperTest.assertEquals(Set.of(firstNotInGroup, secondNotInGroup),
                new HashSet<>(identitiesList));

        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(childOfFirstGroupMemberOfGroup.getId())
                        .build());

        GetIdentityResponse identity = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertEquals(childOfFirstGroupMemberOfGroup, identity.getIdentity());

        getIdentityRequest.setIdentity(IdentityCompositeId.newBuilder()
                .setId(childOfThirdGroupChildOfGroup.getId())
                .build());

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(getIdentityRequest.build()));
        Assertions.assertEquals("NOT_FOUND: resource with id " + childOfThirdGroupChildOfGroup.getId() + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void partialReplaceGroupTest() {
        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setGroupId(groupIdentity.getId())
                .setOnlyDirectly(true)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        IdentityDataHelperTest.assertEquals(
                Set.of(firstGroupMemberOfGroup, secondIdentityMemberOfGroup, thirdGroupChildOfGroup),
                new HashSet<>(identitiesList), false);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstNotInGroup.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstGroupMemberOfGroup.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(secondIdentityMemberOfGroup.getId())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        listIdentitiesResponse = identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        IdentityDataHelperTest.assertEquals(
                Set.of(firstNotInGroup, firstGroupMemberOfGroup, secondIdentityMemberOfGroup),
                new HashSet<>(identitiesList), false);

        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(childOfFirstGroupMemberOfGroup.getId())
                        .build());

        GetIdentityResponse identity = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertEquals(childOfFirstGroupMemberOfGroup, identity.getIdentity());

        getIdentityRequest.setIdentity(IdentityCompositeId.newBuilder()
                .setId(childOfThirdGroupChildOfGroup.getId())
                .build());

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(getIdentityRequest.build()));
        Assertions.assertEquals("NOT_FOUND: resource with id " + childOfThirdGroupChildOfGroup.getId() + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void emptyReplaceGroupTest() {
        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setGroupId(groupIdentity.getId())
                .setOnlyDirectly(true)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        IdentityDataHelperTest.assertEquals(
                Set.of(firstGroupMemberOfGroup, secondIdentityMemberOfGroup, thirdGroupChildOfGroup),
                new HashSet<>(identitiesList), false);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        listIdentitiesResponse = identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(0, identitiesList.size());

        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(childOfFirstGroupMemberOfGroup.getId())
                        .build());

        GetIdentityResponse identity = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertEquals(childOfFirstGroupMemberOfGroup, identity.getIdentity());

        getIdentityRequest.setIdentity(IdentityCompositeId.newBuilder()
                .setId(childOfThirdGroupChildOfGroup.getId())
                .build());

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(getIdentityRequest.build()));
        Assertions.assertEquals("NOT_FOUND: resource with id " + childOfThirdGroupChildOfGroup.getId() + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void sameReplaceGroupTest() {
        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setGroupId(groupIdentity.getId())
                .setOnlyDirectly(true)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        IdentityDataHelperTest.assertEquals(
                Set.of(firstGroupMemberOfGroup, secondIdentityMemberOfGroup, thirdGroupChildOfGroup),
                new HashSet<>(identitiesList), false);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstGroupMemberOfGroup.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(secondIdentityMemberOfGroup.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(thirdGroupChildOfGroup.getId())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        listIdentitiesResponse = identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        IdentityDataHelperTest.assertEquals(
                Set.of(firstGroupMemberOfGroup, secondIdentityMemberOfGroup, thirdGroupChildOfGroup),
                new HashSet<>(identitiesList), false);

        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(childOfFirstGroupMemberOfGroup.getId())
                        .build());

        GetIdentityResponse identity = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertEquals(childOfFirstGroupMemberOfGroup, identity.getIdentity());

        getIdentityRequest.setIdentity(IdentityCompositeId.newBuilder()
                .setId(childOfThirdGroupChildOfGroup.getId())
                .build());

        identity = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertEquals(childOfThirdGroupChildOfGroup, identity.getIdentity());
    }

    @Test
    public void partialReplaceGroupWithMixIdsTest() {
        ListIdentitiesRequest listIdentitiesRequest = ListIdentitiesRequest.newBuilder()
                .setGroupId(groupIdentity.getId())
                .setOnlyDirectly(true)
                .build();

        ListIdentitiesResponse listIdentitiesResponse =
                identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        List<Identity> identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        IdentityDataHelperTest.assertEquals(
                Set.of(firstGroupMemberOfGroup, secondIdentityMemberOfGroup, thirdGroupChildOfGroup),
                new HashSet<>(identitiesList), false);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstNotInGroup.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstGroupMemberOfGroup.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(ExternalIdentity.newBuilder()
                                .setExternalId(secondIdentityMemberOfGroup.getExternalId())
                                .setTypeId(secondIdentityMemberOfGroup.getType().getId())
                                .build())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        listIdentitiesResponse = identityServiceBlockingStub.listIdentities(listIdentitiesRequest);
        identitiesList = listIdentitiesResponse.getIdentitiesList();
        Assertions.assertEquals(3, identitiesList.size());
        IdentityDataHelperTest.assertEquals(
                Set.of(firstNotInGroup, firstGroupMemberOfGroup, secondIdentityMemberOfGroup),
                new HashSet<>(identitiesList), false);

        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(childOfFirstGroupMemberOfGroup.getId())
                        .build());

        GetIdentityResponse identity = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertEquals(childOfFirstGroupMemberOfGroup, identity.getIdentity());

        getIdentityRequest.setIdentity(IdentityCompositeId.newBuilder()
                .setId(childOfThirdGroupChildOfGroup.getId())
                .build());

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityServiceBlockingStub.getIdentity(getIdentityRequest.build()));
        Assertions.assertEquals("NOT_FOUND: resource with id " + childOfThirdGroupChildOfGroup.getId() + " not found",
                statusRuntimeException.getMessage());
    }

    @Test
    public void replaceGroupUpdateGroupModifiedAtTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Assertions.assertNotNull(createRes);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);

        String id = TestData.Companion.getIdentityGroup1().getId();
        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build());

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertNotNull(res);
        Identity group1 = res.getIdentity();
        Assertions.assertNotNull(group1);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        res = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertNotEquals(group1.getModifiedAt(), res.getIdentity().getModifiedAt());
    }

    @Test
    public void replaceGroupUpdateAllGroupsUpModifiedAtTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Assertions.assertNotNull(createRes);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);

        String id = TestData.Companion.getIdentityGroup1().getId();
        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build());

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertNotNull(res);
        Identity group1 = res.getIdentity();
        Assertions.assertNotNull(group1);

        GetIdentityRequest.Builder getIdentity2Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup2().getId())
                        .build());

        res = identityServiceBlockingStub.getIdentity(getIdentity2Request.build());
        Assertions.assertNotNull(res);
        Identity group2 = res.getIdentity();
        Assertions.assertNotNull(group2);

        GetIdentityRequest.Builder getIdentity3Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build());

        res = identityServiceBlockingStub.getIdentity(getIdentity3Request.build());
        Assertions.assertNotNull(res);
        Identity group3 = res.getIdentity();
        Assertions.assertNotNull(group3);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        res = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertNotEquals(group1.getModifiedAt(), res.getIdentity().getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(getIdentity2Request.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertNotEquals(group2.getModifiedAt(), res.getIdentity().getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(getIdentity3Request.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertNotEquals(group3.getModifiedAt(), res.getIdentity().getModifiedAt());
    }

    @Test
    public void replaceGroupDontUpdateGroupsDownModifiedAtTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Assertions.assertNotNull(createRes);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);

        String id = TestData.Companion.getIdentityGroup1().getId();
        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build());

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertNotNull(res);
        Identity group1 = res.getIdentity();
        Assertions.assertNotNull(group1);

        GetIdentityRequest.Builder getIdentity2Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup2().getId())
                        .build());

        res = identityServiceBlockingStub.getIdentity(getIdentity2Request.build());
        Assertions.assertNotNull(res);
        Identity group2 = res.getIdentity();
        Assertions.assertNotNull(group2);

        GetIdentityRequest.Builder getIdentity3Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build());

        res = identityServiceBlockingStub.getIdentity(getIdentity3Request.build());
        Assertions.assertNotNull(res);
        Identity group3 = res.getIdentity();
        Assertions.assertNotNull(group3);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(identity.getId())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        res = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(group1.getModifiedAt(), res.getIdentity().getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(getIdentity2Request.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(group2.getModifiedAt(), res.getIdentity().getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(getIdentity3Request.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertNotEquals(group3.getModifiedAt(), res.getIdentity().getModifiedAt());
    }

    @Test
    public void replaceGroupDontUpdateGroupsOnSameGroupRequestModifiedAtTest() {
        String externalId = UUID.randomUUID().toString();
        String typeId = TestData.Companion.getIdentityType().getId();

        CreateIdentityRequest createReq = CreateIdentityRequest.newBuilder()
                .setExternalIdentity(ExternalIdentity.newBuilder()
                        .setExternalId(externalId)
                        .setTypeId(typeId)
                        .build())
                .build();

        CreateIdentityResponse createRes = identityServiceBlockingStub.createIdentity(createReq);
        Assertions.assertNotNull(createRes);
        Identity identity = createRes.getIdentity();
        Assertions.assertNotNull(identity);

        String id = TestData.Companion.getIdentityGroup1().getId();
        GetIdentityRequest.Builder getIdentityRequest = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(id)
                        .build());

        GetIdentityResponse res = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertNotNull(res);
        Identity group1 = res.getIdentity();
        Assertions.assertNotNull(group1);

        GetIdentityRequest.Builder getIdentity2Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup2().getId())
                        .build());

        res = identityServiceBlockingStub.getIdentity(getIdentity2Request.build());
        Assertions.assertNotNull(res);
        Identity group2 = res.getIdentity();
        Assertions.assertNotNull(group2);

        GetIdentityRequest.Builder getIdentity3Request = GetIdentityRequest.newBuilder()
                .setIdentity(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build());

        res = identityServiceBlockingStub.getIdentity(getIdentity3Request.build());
        Assertions.assertNotNull(res);
        Identity group3 = res.getIdentity();
        Assertions.assertNotNull(group3);

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup3().getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(TestData.Companion.getIdentityGroup2().getId())
                        .build())
                .build();

        identityGroupServiceBlockingStub.replaceGroup(req);

        res = identityServiceBlockingStub.getIdentity(getIdentityRequest.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(group1.getModifiedAt(), res.getIdentity().getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(getIdentity2Request.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(group2.getModifiedAt(), res.getIdentity().getModifiedAt());

        res = identityServiceBlockingStub.getIdentity(getIdentity3Request.build());
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getIdentity());
        Assertions.assertEquals(group3.getModifiedAt(), res.getIdentity().getModifiedAt());
    }

    @Test
    public void  replaceGroupFailOnFakeGroupIdTest() {
        String fakeId = UUID.randomUUID().toString();

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(fakeId)
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstGroupMemberOfGroup.getId())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityGroupServiceBlockingStub.replaceGroup(req));
        Assertions.assertEquals(String.format("NOT_FOUND: resource with id %s not found", fakeId),
                statusRuntimeException.getMessage());
    }

    @Test
    public void replaceGroupFailOnFakeGroupTypeIdTest() {
        String fakeTypeId = "fake_type_id";

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder().setExternalIdentity(
                        ExternalIdentity.newBuilder()
                                .setExternalId(groupIdentity.getExternalId())
                                .setTypeId(fakeTypeId)
                                .build()
                ).build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstGroupMemberOfGroup.getId())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityGroupServiceBlockingStub.replaceGroup(req));
        Assertions.assertEquals(String.format("NOT_FOUND: resource with externalId %s and typeId %s not found",
                        groupIdentity.getExternalId(), fakeTypeId),
                statusRuntimeException.getMessage());
    }

    @Test
    public void replaceGroupFailOnFakeGroupExternalIdTest() {
        String fakeExternalId = "fake_external_id";

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder().setExternalIdentity(
                        ExternalIdentity.newBuilder()
                                .setExternalId(fakeExternalId)
                                .setTypeId(groupIdentity.getType().getId())
                                .build()
                ).build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(firstGroupMemberOfGroup.getId())
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityGroupServiceBlockingStub.replaceGroup(req));
        Assertions.assertEquals(String.format("NOT_FOUND: resource with externalId %s and typeId %s not found",
                        fakeExternalId, groupIdentity.getType().getId()),
                statusRuntimeException.getMessage());
    }

    @Test
    public void replaceGroupFailOnFakeIdentityIdTest() {
        String fakeId = UUID.randomUUID().toString();

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setId(fakeId)
                        .build())
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityGroupServiceBlockingStub.replaceGroup(req));
        Assertions.assertEquals("NOT_FOUND: Resources not found", statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"", String.format("resource with id %s not found", fakeId)});
    }

    @Test
    public void replaceFailOnFakeIdentityTypeIdTest() {
        String fakeTypeId = "fake_type_id";

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(
                                ExternalIdentity.newBuilder()
                                        .setExternalId(firstGroupMemberOfGroup.getExternalId())
                                        .setTypeId(fakeTypeId)
                                        .build())
                )
                .build();


        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityGroupServiceBlockingStub.replaceGroup(req));
        Assertions.assertEquals("NOT_FOUND: Resources not found", statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"", String.format("resource with externalId %s and typeId %s not found",
                        firstGroupMemberOfGroup.getExternalId(), fakeTypeId)});
    }

    @Test
    public void replaceGroupFailOnFakeIdentityExternalIdTest() {
        String fakeExternalId = "fake_external_id";

        ReplaceGroupRequest req = ReplaceGroupRequest.newBuilder()
                .setGroup(IdentityCompositeId.newBuilder()
                        .setId(groupIdentity.getId())
                        .build())
                .addIdentities(IdentityCompositeId.newBuilder()
                        .setExternalIdentity(
                                ExternalIdentity.newBuilder()
                                        .setExternalId(fakeExternalId)
                                        .setTypeId(firstGroupMemberOfGroup.getType().getId())
                                        .build())
                )
                .build();

        StatusRuntimeException statusRuntimeException = Assertions.assertThrows(StatusRuntimeException.class,
                () -> identityGroupServiceBlockingStub.replaceGroup(req));
        Assertions.assertEquals("NOT_FOUND: Resources not found", statusRuntimeException.getMessage());
        assertMetadata(statusRuntimeException, 1,
                new String[]{"", String.format("resource with externalId %s and typeId %s not found",
                        fakeExternalId, firstGroupMemberOfGroup.getType().getId())});
    }
}
