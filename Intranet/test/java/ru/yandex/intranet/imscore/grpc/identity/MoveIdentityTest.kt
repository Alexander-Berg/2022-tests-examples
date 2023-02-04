package ru.yandex.intranet.imscore.grpc.identity

import com.google.protobuf.Empty
import com.google.protobuf.FieldMask
import io.grpc.StatusRuntimeException
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import ru.yandex.intranet.imscore.IntegrationTest
import ru.yandex.intranet.imscore.grpc.GrpcHelperTest
import ru.yandex.intranet.imscore.grpc.common.TestData
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup1
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup2
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup3
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup4
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroupType
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityType
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityRequest
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityResponse
import ru.yandex.intranet.imscore.proto.identity.DeleteIdentityRequest
import ru.yandex.intranet.imscore.proto.identity.ExternalIdentity
import ru.yandex.intranet.imscore.proto.identity.GetIdentityRequest
import ru.yandex.intranet.imscore.proto.identity.GetIdentityResponse
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId
import ru.yandex.intranet.imscore.proto.identity.IdentityServiceGrpc
import ru.yandex.intranet.imscore.proto.identity.MoveIdentityRequest
import ru.yandex.intranet.imscore.proto.identityGroup.AddToGroupRequest
import ru.yandex.intranet.imscore.proto.identityGroup.ExistsInGroupRequest
import ru.yandex.intranet.imscore.proto.identityGroup.IdentityGroupServiceGrpc
import java.util.UUID

/**
 *
 * @author Mustakayev Marat <mmarat248@yandex-team.ru>
 */
@IntegrationTest
@TestPropertySource(properties = ["spring.liquibase.contexts=identity,identity-type-source,identity-type," +
    "identity-relation"])
open class MoveIdentityTest {

    @GrpcClient("inProcess")
    private lateinit var identityService: IdentityServiceGrpc.IdentityServiceBlockingStub

    @GrpcClient("inProcess")
    private lateinit var identityGroupService: IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub

    @Test
    fun moveIdentityByIdTest() {
        // check relation not exists
        var identity = identityService.getIdentity(
            GetIdentityRequest.newBuilder()
                .setIdentity(
                    IdentityCompositeId.newBuilder()
                        .setId(identityGroup3.id)
                        .build()
                )
                .build()
        ).identity
        Assertions.assertTrue(identity.parentId.isEmpty())
        val existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        var existsInGroup = identityGroupService.existsInGroup(existsInGroupReq).exists
        Assertions.assertFalse(existsInGroup)

        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        val res: Empty = identityService.moveIdentity(req)
        Assertions.assertNotNull(res)

        identity = identityService.getIdentity(
            GetIdentityRequest.newBuilder()
                .setIdentity(
                    IdentityCompositeId.newBuilder()
                        .setId(identityGroup3.id)
                        .build()
                )
                .build()
        ).identity
        Assertions.assertEquals(identityGroup4.id, identity.parentId)

        existsInGroup = identityGroupService.existsInGroup(existsInGroupReq).exists
        Assertions.assertTrue(existsInGroup)

        // delete parent
        identityService.deleteIdentity(
            DeleteIdentityRequest.newBuilder()
                .setIdentity(
                    IdentityCompositeId.newBuilder()
                        .setId(identityGroup4.id)
                        .build()
                )
                .build()
        )

        // check delete status of child
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) {
            identity = identityService.getIdentity(
                GetIdentityRequest.newBuilder()
                    .setIdentity(
                        IdentityCompositeId.newBuilder()
                            .setId(identityGroup3.id)
                            .build()
                    )
                    .build()
            ).identity
        }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", identityGroup3.id),
            statusRuntimeException.message
        )
    }

    @Test
    fun moveIdentityByExternalIdTest() {
        val identityCompositeId = IdentityCompositeId.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(identityGroup3.externalId)
                    .setTypeId(identityGroup3.type.id)
                    .build()
            )
            .build()
        // check relation not exists
        var identity = identityService.getIdentity(
            GetIdentityRequest.newBuilder()
                .setIdentity(identityCompositeId)
                .build()
        ).identity

        Assertions.assertTrue(identity.parentId.isEmpty())
        val existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setIdentity(identityCompositeId)
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        var existsInGroup = identityGroupService.existsInGroup(existsInGroupReq).exists
        Assertions.assertFalse(existsInGroup)

        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(identityCompositeId)
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        val res: Empty = identityService.moveIdentity(req)
        Assertions.assertNotNull(res)

        identity = identityService.getIdentity(
            GetIdentityRequest.newBuilder()
                .setIdentity(identityCompositeId)
                .build()
        ).identity
        Assertions.assertEquals(identityGroup4.id, identity.parentId)
        existsInGroup = identityGroupService.existsInGroup(existsInGroupReq).exists
        Assertions.assertTrue(existsInGroup)
    }

    @Test
    fun moveIdentityToAlreadyOwningGroupTest() {
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()
        var res = identityService.moveIdentity(req)
        Assertions.assertNotNull(res)

        res = identityService.moveIdentity(req)
        Assertions.assertNotNull(res)
    }

    @Test
    fun moveIdentityToAlreadyConnectedGroupTest() {
        // prepare initial data
        var identity = identityService.getIdentity(
            GetIdentityRequest.newBuilder()
                .setIdentity(
                    IdentityCompositeId.newBuilder()
                        .setId(TestData.identity.id)
                        .build()
                )
                .build()
        ).identity
        Assertions.assertTrue(identity.parentId.isEmpty())
        val existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        var existsInGroup = identityGroupService.existsInGroup(existsInGroupReq).exists
        Assertions.assertTrue(existsInGroup)


        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()
        val res = identityService.moveIdentity(req)
        Assertions.assertNotNull(res)

        identity = identityService.getIdentity(
            GetIdentityRequest.newBuilder()
                .setIdentity(
                    IdentityCompositeId.newBuilder()
                        .setId(TestData.identity.id)
                        .build()
                )
                .build()
        ).identity
        Assertions.assertEquals(identityGroup1.id, identity.parentId)

        existsInGroup = identityGroupService.existsInGroup(existsInGroupReq).exists
        Assertions.assertTrue(existsInGroup)

        // delete parent
        identityService.deleteIdentity(
            DeleteIdentityRequest.newBuilder()
                .setIdentity(
                    IdentityCompositeId.newBuilder()
                        .setId(identityGroup1.id)
                        .build()
                )
                .build()
        )

        // check delete status of child
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) {
            identity = identityService.getIdentity(
                GetIdentityRequest.newBuilder()
                    .setIdentity(
                        IdentityCompositeId.newBuilder()
                            .setId(TestData.identity.id)
                            .build()
                    )
                    .build()
            ).identity
        }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", TestData.identity.id),
            statusRuntimeException.message
        )
    }

    @Test
    fun moveIdentityWithoutParentUpdateParentModifiedAtTest() {
        val fieldMask = FieldMask.newBuilder()
            .addPaths("id")
            .addPaths("parent_id")
            .addPaths("external_id")
            .addPaths("type")
            .addPaths("created_at")
            .addPaths("modified_at")
            .addPaths("data")

        val getGroup1Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup1.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        val getGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getGroupResponse)
        val groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup1 = groupIdentity.modifiedAt

        val externalId = UUID.randomUUID().toString()
        val typeId = identityType.id

        val createReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(externalId)
                    .setTypeId(typeId)
                    .build()
            )
            .build()

        val createRes: CreateIdentityResponse = identityService.createIdentity(createReq)
        Assertions.assertNotNull(createRes)
        val createdIdentity = createRes.identity
        Assertions.assertNotNull(createdIdentity)

        val moveReq = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(createdIdentity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(groupIdentity.id).build()
            )
            .build()

        val moveRes = identityService.moveIdentity(moveReq)
        Assertions.assertNotNull(moveRes)

        val getUpdatedGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        val updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup1, updatedGroupIdentity.modifiedAt)
    }

    @Test
    fun moveIdentityWithoutParentUpdateAllGroupsUpModifiedAtTest() {
        val fieldMask = FieldMask.newBuilder()
            .addPaths("id")
            .addPaths("parent_id")
            .addPaths("external_id")
            .addPaths("type")
            .addPaths("created_at")
            .addPaths("modified_at")
            .addPaths("data")

        val getGroup1Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup1.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        var getGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getGroupResponse)
        var groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup1 = groupIdentity.modifiedAt

        val getGroup2Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup2.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup2Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup2 = groupIdentity.modifiedAt

        val getGroup3Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup3.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup3Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup3 = groupIdentity.modifiedAt

        val externalId = UUID.randomUUID().toString()
        val typeId = identityType.id

        val createReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(externalId)
                    .setTypeId(typeId)
                    .build()
            )
            .build()

        val createRes: CreateIdentityResponse = identityService.createIdentity(createReq)
        Assertions.assertNotNull(createRes)
        val createdIdentity = createRes.identity
        Assertions.assertNotNull(createdIdentity)

        val moveReq = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(createdIdentity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()

        val moveRes = identityService.moveIdentity(moveReq)
        Assertions.assertNotNull(moveRes)

        var getUpdatedGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        var updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup1, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup2Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup2, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup3Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup3, updatedGroupIdentity.modifiedAt)
    }

    @Test
    fun moveIdentityWithoutParentDontUpdateGroupsDownModifiedAtTest() {
        val fieldMask = FieldMask.newBuilder()
            .addPaths("id")
            .addPaths("parent_id")
            .addPaths("external_id")
            .addPaths("type")
            .addPaths("created_at")
            .addPaths("modified_at")
            .addPaths("data")

        val getGroup1Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup1.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        var getGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getGroupResponse)
        var groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup1 = groupIdentity.modifiedAt

        val getGroup2Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup2.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup2Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup2 = groupIdentity.modifiedAt

        val getGroup3Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup3.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup3Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup3 = groupIdentity.modifiedAt

        val externalId = UUID.randomUUID().toString()
        val typeId = identityType.id

        val createReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(externalId)
                    .setTypeId(typeId)
                    .build()
            )
            .build()

        val createRes: CreateIdentityResponse = identityService.createIdentity(createReq)
        Assertions.assertNotNull(createRes)
        val createdIdentity = createRes.identity
        Assertions.assertNotNull(createdIdentity)

        val moveReq = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(createdIdentity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .build()

        val moveRes = identityService.moveIdentity(moveReq)
        Assertions.assertNotNull(moveRes)

        var getUpdatedGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        var updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertEquals(oldModifiedAtIdentityGroup1, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup2Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertEquals(oldModifiedAtIdentityGroup2, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup3Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup3, updatedGroupIdentity.modifiedAt)
    }

    @Test
    fun moveIdentityWithParentUpdateParentModifiedAtTest() {
        val externalId = UUID.randomUUID().toString()
        val typeId = identityType.id

        val createReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(externalId)
                    .setTypeId(typeId)
                    .build()
            )
            .setParentId(IdentityCompositeId.newBuilder()
                .setId(identityGroup4.id)
                .build()
            )
            .build()

        val createRes: CreateIdentityResponse = identityService.createIdentity(createReq)
        Assertions.assertNotNull(createRes)
        val createdIdentity = createRes.identity
        Assertions.assertNotNull(createdIdentity)

        val fieldMask = FieldMask.newBuilder()
            .addPaths("id")
            .addPaths("parent_id")
            .addPaths("external_id")
            .addPaths("type")
            .addPaths("created_at")
            .addPaths("modified_at")
            .addPaths("data")

        val getGroup1Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup1.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        var getGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getGroupResponse)
        var groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup1 = groupIdentity.modifiedAt

        val getGroup4Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup4.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup4Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup4 = groupIdentity.modifiedAt

        val moveReq = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(createdIdentity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()

        val moveRes = identityService.moveIdentity(moveReq)
        Assertions.assertNotNull(moveRes)

        var getUpdatedGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        var updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup1, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup4Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup4, updatedGroupIdentity.modifiedAt)
    }

    @Test
    fun moveIdentityWithParentUpdateAllGroupsUpModifiedAtTest() {
        val externalId = UUID.randomUUID().toString()
        val typeId = identityType.id

        val createReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(externalId)
                    .setTypeId(typeId)
                    .build()
            )
            .setParentId(IdentityCompositeId.newBuilder()
                .setId(identityGroup4.id)
                .build()
            )
            .build()

        val createRes: CreateIdentityResponse = identityService.createIdentity(createReq)
        Assertions.assertNotNull(createRes)
        val createdIdentity = createRes.identity
        Assertions.assertNotNull(createdIdentity)

        val fieldMask = FieldMask.newBuilder()
            .addPaths("id")
            .addPaths("parent_id")
            .addPaths("external_id")
            .addPaths("type")
            .addPaths("created_at")
            .addPaths("modified_at")
            .addPaths("data")

        val getGroup1Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup1.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        var getGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getGroupResponse)
        var groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup1 = groupIdentity.modifiedAt

        val getGroup2Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup2.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup2Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup2 = groupIdentity.modifiedAt

        val getGroup3Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup3.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup3Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup3 = groupIdentity.modifiedAt

        val getGroup4Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup4.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup4Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup4 = groupIdentity.modifiedAt

        val groupTypeId = identityGroupType.id

        val create5GroupReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(UUID.randomUUID().toString())
                    .setTypeId(groupTypeId)
                    .build()
            )
            .build()
        val create5GroupRes: CreateIdentityResponse = identityService.createIdentity(create5GroupReq)
        Assertions.assertNotNull(create5GroupRes)
        val created5IGroup = create5GroupRes.identity
        Assertions.assertNotNull(created5IGroup)

        val create6GroupReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(UUID.randomUUID().toString())
                    .setTypeId(groupTypeId)
                    .build()
            )
            .build()
        val create6GroupRes: CreateIdentityResponse = identityService.createIdentity(create6GroupReq)
        Assertions.assertNotNull(create6GroupRes)
        val created6IGroup = create6GroupRes.identity
        Assertions.assertNotNull(created6IGroup)

        val addTo5GroupReq = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(created5IGroup.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        identityGroupService.addToGroup(addTo5GroupReq)

        val addTo6GroupReq = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(created6IGroup.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(created5IGroup.id).build()
            )
            .build()
        identityGroupService.addToGroup(addTo6GroupReq)

        val getGroup5Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(created5IGroup.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup5Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity

        val oldModifiedAtIdentityGroup5 = groupIdentity.modifiedAt

        val getGroup6Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(created6IGroup.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup6Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity

        val oldModifiedAtIdentityGroup6 = groupIdentity.modifiedAt

        val moveReq = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(createdIdentity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()

        val moveRes = identityService.moveIdentity(moveReq)
        Assertions.assertNotNull(moveRes)

        var getUpdatedGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        var updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup1, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup2Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup2, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup3Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup3, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup4Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup4, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup5Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup5, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup6Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup6, updatedGroupIdentity.modifiedAt)
    }

    @Test
    fun moveIdentityWithParentDontUpdateGroupsDownModifiedAtTest() {
        val fieldMask = FieldMask.newBuilder()
            .addPaths("id")
            .addPaths("parent_id")
            .addPaths("external_id")
            .addPaths("type")
            .addPaths("created_at")
            .addPaths("modified_at")
            .addPaths("data")

        val getGroup1Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup1.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        var getGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getGroupResponse)
        var groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup1 = groupIdentity.modifiedAt

        val getGroup2Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup2.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup2Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup2 = groupIdentity.modifiedAt

        val getGroup3Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup3.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup3Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup3 = groupIdentity.modifiedAt

        val getGroup4Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup4.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        getGroupResponse = identityService.getIdentity(getGroup4Request)
        Assertions.assertNotNull(getGroupResponse)
        groupIdentity = getGroupResponse.identity
        Assertions.assertNotNull(groupIdentity)

        val oldModifiedAtIdentityGroup4 = groupIdentity.modifiedAt

        val groupTypeId = identityGroupType.id

        val create5GroupReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(UUID.randomUUID().toString())
                    .setTypeId(groupTypeId)
                    .build()
            )
            .build()
        val create5GroupRes: CreateIdentityResponse = identityService.createIdentity(create5GroupReq)
        Assertions.assertNotNull(create5GroupRes)
        val created5IGroup = create5GroupRes.identity
        Assertions.assertNotNull(created5IGroup)

        val create6GroupReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(UUID.randomUUID().toString())
                    .setTypeId(groupTypeId)
                    .build()
            )
            .build()
        val create6GroupRes: CreateIdentityResponse = identityService.createIdentity(create6GroupReq)
        Assertions.assertNotNull(create6GroupRes)
        val created6IGroup = create6GroupRes.identity
        Assertions.assertNotNull(created6IGroup)

        val addTo5GroupReq = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(created5IGroup.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        identityGroupService.addToGroup(addTo5GroupReq)

        val addTo6GroupReq = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(created6IGroup.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(created5IGroup.id).build()
            )
            .build()
        identityGroupService.addToGroup(addTo6GroupReq)

        val externalId = UUID.randomUUID().toString()
        val typeId = identityType.id

        val createReq = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setExternalId(externalId)
                    .setTypeId(typeId)
                    .build()
            )
            .setParentId(IdentityCompositeId.newBuilder()
                .setId(created6IGroup.id)
                .build()
            )
            .build()

        val createRes: CreateIdentityResponse = identityService.createIdentity(createReq)
        Assertions.assertNotNull(createRes)
        val createdIdentity = createRes.identity
        Assertions.assertNotNull(createdIdentity)

        val getGroup5Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(created5IGroup.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        var identityResponse = identityService.getIdentity(getGroup5Request)
        Assertions.assertNotNull(identityResponse)
        var identity = identityResponse.identity
        Assertions.assertNotNull(identity)
        val oldModifiedAtIdentityGroup5 = identity.modifiedAt

        val getGroup6Request = GetIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder()
                    .setId(created6IGroup.id)
                    .build()
            )
            .setFieldMask(fieldMask)
            .build()

        identityResponse = identityService.getIdentity(getGroup6Request)
        Assertions.assertNotNull(identityResponse)
        identity = identityResponse.identity
        Assertions.assertNotNull(identity)
        val oldModifiedAtIdentityGroup6 = identity.modifiedAt

        val moveReq = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(createdIdentity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .build()

        val moveRes = identityService.moveIdentity(moveReq)
        Assertions.assertNotNull(moveRes)

        var getUpdatedGroupResponse: GetIdentityResponse = identityService.getIdentity(getGroup1Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        var updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertEquals(oldModifiedAtIdentityGroup1, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup2Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertEquals(oldModifiedAtIdentityGroup2, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup3Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup3, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup4Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertEquals(oldModifiedAtIdentityGroup4, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup5Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertEquals(oldModifiedAtIdentityGroup5, updatedGroupIdentity.modifiedAt)

        getUpdatedGroupResponse = identityService.getIdentity(getGroup6Request)
        Assertions.assertNotNull(getUpdatedGroupResponse)
        updatedGroupIdentity = getUpdatedGroupResponse.identity
        Assertions.assertNotNull(updatedGroupIdentity)
        Assertions.assertNotEquals(oldModifiedAtIdentityGroup6, updatedGroupIdentity.modifiedAt)
    }

    @Test
    open fun moveIdentityFailOnWrongUUIDTest() {
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId("NOT-UUID").build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) { identityService.moveIdentity(req) }
        Assertions.assertEquals(
            "INVALID_ARGUMENT: identity_id_oneof is invalid",
            statusRuntimeException.message
        )
        GrpcHelperTest.assertMetadata(statusRuntimeException, "identity.identity_id_oneof.id", "Value is not a UUID")
    }

    @Test
    open fun moveIdentityFailOnFakeIdentityTest() {
        val fakeIdentityId = UUID.randomUUID().toString()
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(fakeIdentityId).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) { identityService.moveIdentity(req) }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", fakeIdentityId),
            statusRuntimeException.message
        )
    }

    @Test
    open fun moveIdentityFailOnFakeIdentityExternalIdTest() {
        val fakeIdentityExternalId = "fake_external_id"
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(fakeIdentityExternalId)
                        .setTypeId(identityGroup3.type.id)
                        .build()
                ).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) { identityService.moveIdentity(req) }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with externalId %s and typeId %s not found",
                fakeIdentityExternalId, identityGroup3.type.id),
            statusRuntimeException.message
        )
    }

    @Test
    open fun moveIdentityFailOnFakeIdentityTypeIdTest() {
        val fakeIdentityTypeId = "fake_type_id"
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup3.externalId)
                        .setTypeId(fakeIdentityTypeId)
                        .build()
                ).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) { identityService.moveIdentity(req) }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with externalId %s and typeId %s not found",
                identityGroup3.externalId, fakeIdentityTypeId),
            statusRuntimeException.message
        )
    }

    @Test
    open fun moveIdentityFailOnFakeGroupTest() {
        val fakeGroupId = UUID.randomUUID().toString()
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(fakeGroupId).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) { identityService.moveIdentity(req) }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", fakeGroupId),
            statusRuntimeException.message
        )
    }

    @Test
    open fun moveIdentityFailOnFakeGroupExternalIdTest() {
        val fakeIdentityExternalId = "fake_external_id"
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(fakeIdentityExternalId)
                        .setTypeId(identityGroup1.type.id)
                        .build()
                ).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) { identityService.moveIdentity(req) }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with externalId %s and typeId %s not found",
                fakeIdentityExternalId, identityGroup1.type.id),
            statusRuntimeException.message
        )
    }

    @Test
    open fun moveIdentityFailOnFakeGroupTypeIdTest() {
        val fakeIdentityTypeId = "fake_type_id"
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup1.externalId)
                        .setTypeId(fakeIdentityTypeId)
                        .build()
                ).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(
            StatusRuntimeException::class.java
        ) { identityService.moveIdentity(req) }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with externalId %s and typeId %s not found",
                identityGroup1.externalId, fakeIdentityTypeId),
            statusRuntimeException.message
        )
    }

    @Test
    fun moveIdentityToNotGroupTest() {
        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityService.moveIdentity(req)
        }
        Assertions.assertEquals(
            String.format("INVALID_ARGUMENT: %s is not a group", TestData.identity.id),
            statusRuntimeException.message
        )
    }

    @Test
    fun moveIdentityDeleteOldOwnershipTest() {
        val identity = identityService.getIdentity(
            GetIdentityRequest.newBuilder()
                .setIdentity(
                    IdentityCompositeId.newBuilder()
                        .setId(TestData.identityGroup4SubGroup1.id)
                        .build()
                )
                .build()
        ).identity
        Assertions.assertTrue(identity.parentId.isNotBlank())
        val existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup4SubGroup1.id).build()
            )
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        var existsInGroup = identityGroupService.existsInGroup(existsInGroupReq).exists
        Assertions.assertTrue(existsInGroup)

        val req = MoveIdentityRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup4SubGroup1.id).build()
            )
            .setToGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .build()
        val res = identityService.moveIdentity(req)
        Assertions.assertNotNull(res)

        existsInGroup = identityGroupService.existsInGroup(existsInGroupReq).exists
        Assertions.assertFalse(existsInGroup)
    }

}
