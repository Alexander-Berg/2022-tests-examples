package ru.yandex.intranet.imscore.grpc.identityGroup

import com.google.protobuf.Empty
import io.grpc.StatusRuntimeException
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import ru.yandex.intranet.imscore.IntegrationTest
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identity
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup1
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup2
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup3
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityType
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityRequest
import ru.yandex.intranet.imscore.proto.identity.ExternalIdentity
import ru.yandex.intranet.imscore.proto.identity.GetIdentityRequest
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId
import ru.yandex.intranet.imscore.proto.identity.IdentityServiceGrpc.IdentityServiceBlockingStub
import ru.yandex.intranet.imscore.proto.identityGroup.AddToGroupRequest
import ru.yandex.intranet.imscore.proto.identityGroup.ExistsInGroupRequest
import ru.yandex.intranet.imscore.proto.identityGroup.ExistsInGroupResponse
import ru.yandex.intranet.imscore.proto.identityGroup.IdentityGroupServiceGrpc
import ru.yandex.intranet.imscore.proto.identityGroup.RemoveFromGroupRequest
import java.util.UUID

/**
 * Remove identity relation tests.
 *
 * @author Mustakayev Marat <mmarat248@yandex-team.ru>
 */
@IntegrationTest
@TestPropertySource(properties = ["spring.liquibase.contexts=identity,identity-type-source,identity-type," +
    "identity-relation"])
open class RemoveFromGroupTest {
    @GrpcClient("inProcess")
    private lateinit var identityServiceBlockingStub: IdentityServiceBlockingStub
    @GrpcClient("inProcess")
    private lateinit var identityGroupService: IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub

    @Test
    fun removeFromGroupByIdTest() {
        var existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        var existsInGroupRes: ExistsInGroupResponse = identityGroupService.existsInGroup(existsInGroupReq)
        Assertions.assertNotNull(existsInGroupRes)
        Assertions.assertTrue(existsInGroupRes.exists)

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        val res: Empty = identityGroupService.removeFromGroup(req)
        Assertions.assertNotNull(res)

        existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        existsInGroupRes = identityGroupService.existsInGroup(existsInGroupReq)
        Assertions.assertNotNull(existsInGroupRes)
        Assertions.assertFalse(existsInGroupRes.exists)
    }

    @Test
    fun removeFromGroupByExternalIdTest() {
        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup2.externalId)
                        .setTypeId(identityGroup2.type.id)
                        .build()
                ).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup3.externalId)
                        .setTypeId(identityGroup3.type.id)
                        .build()
                ).build()
            )
            .build()
        val res = identityGroupService.removeFromGroup(req)
        Assertions.assertNotNull(res)
    }

    @Test
    fun removeFromGroupByMixedExternalIdWithIdTest() {
        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup2.externalId)
                        .setTypeId(identityGroup2.type.id)
                        .build()
                ).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .build()
        val res = identityGroupService.removeFromGroup(req)
        Assertions.assertNotNull(res)
    }

    @Test
    fun removeFromGroupWithOwnershipRemoveChildTest() {
        val createIdentity = identityServiceBlockingStub.createIdentity(
            CreateIdentityRequest.newBuilder()
                .setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setTypeId(identityType.id)
                        .build()
                )
                .setParentId(IdentityCompositeId.newBuilder()
                    .setId(identityGroup1.id)
                    .build())
                .build()
        )

        val identity = createIdentity.identity

        val getIdentityRequest = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identity.id)
                .build())
            .build()

        val getIdeResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertEquals(identity, getIdeResponse.identity)

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder()
                    .setId(identity.id)
                    .build()
            )
            .build()

        val res = identityGroupService.removeFromGroup(req)
        Assertions.assertNotNull(res)

        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityServiceBlockingStub.getIdentity(getIdentityRequest)
        }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", identity.id),
            statusRuntimeException.message
        )
    }

    @Test
    fun removeFromGroupWithMembershipDontRemoveMemberTest() {
        val createIdentity = identityServiceBlockingStub.createIdentity(
            CreateIdentityRequest.newBuilder()
                .setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setTypeId(identityType.id)
                        .build()
                )
                .build()
        )

        val identity = createIdentity.identity

        val getIdentityRequest = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identity.id)
                .build())
            .build()

        var getIdeResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertEquals(identity, getIdeResponse.identity)

        val addToGroupRequest = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        identityGroupService.addToGroup(addToGroupRequest)

        val existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        var existsInGroup = identityGroupService.existsInGroup(existsInGroupReq)
        Assertions.assertTrue(existsInGroup.exists)

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder()
                    .setId(identity.id)
                    .build()
            )
            .build()

        val res = identityGroupService.removeFromGroup(req)
        Assertions.assertNotNull(res)

        getIdeResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertEquals(identity, getIdeResponse.identity)

        existsInGroup = identityGroupService.existsInGroup(existsInGroupReq)
        Assertions.assertFalse(existsInGroup.exists)
    }

    @Test
    fun removeFromGroupUpdateGroupModifiedAtTest() {
        val getIdentityRequest = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup1.id)
                .build())
            .build()

        var getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertNotNull(getIdentityResponse)
        val group1 = getIdentityResponse.identity
        Assertions.assertNotNull(group1)

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder()
                    .setId(identity.id)
                    .build()
            )
            .build()
        val res = identityGroupService.removeFromGroup(req)
        Assertions.assertNotNull(res)

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)
        Assertions.assertNotEquals(group1.modifiedAt, getIdentityResponse.identity.modifiedAt)
    }

    @Test
    fun removeFromGroupUpdateAllGroupsUpModifiedAtTest() {
        val getIdentityRequest = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup1.id)
                .build())
            .build()

        var getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertNotNull(getIdentityResponse)
        val group1 = getIdentityResponse.identity
        Assertions.assertNotNull(group1)

        val getIdentity2Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup2.id)
                .build())
            .build()

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentity2Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group2 = getIdentityResponse.identity
        Assertions.assertNotNull(group2)

        val getIdentity3Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup3.id)
                .build())
            .build()

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentity3Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group3 = getIdentityResponse.identity
        Assertions.assertNotNull(group3)

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder()
                    .setId(identity.id)
                    .build()
            )
            .build()
        val res = identityGroupService.removeFromGroup(req)
        Assertions.assertNotNull(res)

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)
        Assertions.assertNotEquals(group1.modifiedAt, getIdentityResponse.identity.modifiedAt)

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentity2Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)
        Assertions.assertNotEquals(group2.modifiedAt, getIdentityResponse.identity.modifiedAt)

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentity3Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)
        Assertions.assertNotEquals(group3.modifiedAt, getIdentityResponse.identity.modifiedAt)
    }

    @Test
    fun removeFromGroupDontUpdateGroupsDownModifiedAtTest() {
        val getIdentityRequest = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup1.id)
                .build())
            .build()

        var getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertNotNull(getIdentityResponse)
        val group1 = getIdentityResponse.identity
        Assertions.assertNotNull(group1)

        val getIdentity2Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup2.id)
                .build())
            .build()

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentity2Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group2 = getIdentityResponse.identity
        Assertions.assertNotNull(group2)

        val getIdentity3Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup3.id)
                .build())
            .build()

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentity3Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group3 = getIdentityResponse.identity
        Assertions.assertNotNull(group3)

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder()
                    .setId(identityGroup2.id)
                    .build()
            )
            .build()
        val res = identityGroupService.removeFromGroup(req)
        Assertions.assertNotNull(res)

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentityRequest)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)
        Assertions.assertEquals(group1.modifiedAt, getIdentityResponse.identity.modifiedAt)

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentity2Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)
        Assertions.assertEquals(group2.modifiedAt, getIdentityResponse.identity.modifiedAt)

        getIdentityResponse = identityServiceBlockingStub.getIdentity(getIdentity3Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)
        Assertions.assertNotEquals(group3.modifiedAt, getIdentityResponse.identity.modifiedAt)
    }

    @Test
    fun removeFromGroupFailOnFakeGroupIdTest() {
        val fakeId = UUID.randomUUID().toString()

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(fakeId).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.removeFromGroup(req)
        }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", fakeId),
            statusRuntimeException.message
        )
    }

    @Test
    fun removeFromGroupFailOnFakeGroupTypeIdTest() {
        val fakeTypeId = "fake_type_id"

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup2.externalId)
                        .setTypeId(fakeTypeId)
                        .build()
                ).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.removeFromGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: resource with externalId %s and typeId %s not found",
                identityGroup2.externalId, fakeTypeId),
            statusRuntimeException.message
        )
    }

    @Test
    fun removeFromGroupFailOnFakeGroupExternalIdTest() {
        val fakeExternalId = "fake_external_id"

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(fakeExternalId)
                        .setTypeId(identityGroup2.type.id)
                        .build()
                ).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.removeFromGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: resource with externalId %s and typeId %s not found",
                fakeExternalId, identityGroup2.type.id),
            statusRuntimeException.message
        )
    }

    @Test
    fun removeFromGroupFailOnFakeIdentityIdTest() {
        val fakeId = UUID.randomUUID().toString()

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(fakeId).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.removeFromGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: Resources not found"),
            statusRuntimeException.message
        )
    }

    @Test
    fun removeFromGroupFailOnFakeIdentityTypeIdTest() {
        val fakeTypeId = "fake_type_id"

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identity.externalId)
                        .setTypeId(fakeTypeId)
                        .build()
                ).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.removeFromGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: Resources not found"),
            statusRuntimeException.message
        )
    }

    @Test
    fun removeFromGroupFailOnFakeIdentityExternalIdTest() {
        val fakeTypeId = "fake_type_id"

        val req = RemoveFromGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(fakeTypeId)
                        .setTypeId(identity.type.id)
                        .build()
                ).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.removeFromGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: Resources not found"),
            statusRuntimeException.message
        )
    }

}
