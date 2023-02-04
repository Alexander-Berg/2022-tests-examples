package ru.yandex.intranet.imscore.grpc.identityGroup

import com.google.protobuf.Empty
import io.grpc.StatusRuntimeException
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import ru.yandex.intranet.imscore.IntegrationTest
import ru.yandex.intranet.imscore.grpc.GrpcHelperTest.startThreadsSimultaneously
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup1
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup1WithData
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup2
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup3
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroup4
import ru.yandex.intranet.imscore.grpc.common.TestData.Companion.identityGroupType
import ru.yandex.intranet.imscore.proto.identity.CreateIdentityRequest
import ru.yandex.intranet.imscore.proto.identity.ExternalIdentity
import ru.yandex.intranet.imscore.proto.identity.GetIdentityRequest
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId
import ru.yandex.intranet.imscore.proto.identity.IdentityServiceGrpc
import ru.yandex.intranet.imscore.proto.identityGroup.AddToGroupRequest
import ru.yandex.intranet.imscore.proto.identityGroup.ExistsInGroupRequest
import ru.yandex.intranet.imscore.proto.identityGroup.ExistsInGroupResponse
import ru.yandex.intranet.imscore.proto.identityGroup.IdentityGroupServiceGrpc
import java.util.UUID
import java.util.concurrent.CyclicBarrier

/**
 * Add identity relation tests.
 *
 * @author Mustakayev Marat <mmarat248@yandex-team.ru>
 */
@IntegrationTest
@TestPropertySource(properties = ["spring.liquibase.contexts=identity,identity-type-source,identity-type," +
    "identity-relation"])
open class AddToGroupTest {

    @GrpcClient("inProcess")
    private lateinit var identityGroupService: IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub

    @GrpcClient("inProcess")
    private lateinit var identityService: IdentityServiceGrpc.IdentityServiceBlockingStub

    @Test
    fun addToGroupByIdTest() {
        var existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        var existsInGroupRes: ExistsInGroupResponse = identityGroupService.existsInGroup(existsInGroupReq)
        Assertions.assertNotNull(existsInGroupRes)
        Assertions.assertFalse(existsInGroupRes.exists)

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        val res: Empty = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)

        existsInGroupReq = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        existsInGroupRes = identityGroupService.existsInGroup(existsInGroupReq)
        Assertions.assertNotNull(existsInGroupRes)
        Assertions.assertTrue(existsInGroupRes.exists)
    }

    @Test
    fun addToGroupMultipleIdentitysByIdTest() {
        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        val res: Empty = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)
    }

    @Test
    fun addToGroupMultipleIdentitysByExtIdTest() {
        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder()
                    .setExternalIdentity(
                        ExternalIdentity.newBuilder()
                            .setExternalId(identityGroup3.externalId)
                            .setTypeId(identityGroup3.type.id)
                            .build()
                    )
                    .build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder()
                    .setExternalIdentity(
                        ExternalIdentity.newBuilder()
                            .setExternalId(identityGroup4.externalId)
                            .setTypeId(identityGroup4.type.id)
                            .build()
                    )
                    .build()
            )
            .build()
        val res: Empty = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)
    }

    @Test
    fun addToGroupByExternalIdTest() {
        val req = AddToGroupRequest.newBuilder()
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
                        .setExternalId(identityGroup4.externalId)
                        .setTypeId(identityGroup4.type.id)
                        .build()
                ).build()
            )
            .build()
        val res = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)
    }

    @Test
    fun addToGroupByMixedExternalIdWithIdTest() {
        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup2.externalId)
                        .setTypeId(identityGroup2.type.id)
                        .build()
                ).build()
            )
            .build()
        val res = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)
    }

    // @Test
    fun noDeadlockOnSelectTest() {
        val req1 = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup2.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()

        val req2 = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1WithData.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup4.externalId)
                        .setTypeId(identityGroup4.type.id)
                ).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup3.externalId)
                        .setTypeId(identityGroup3.type.id)
                ).build()
            )
            .build()

        val threadsFunc = arrayOf(
            java.util.function.Function<CyclicBarrier, Runnable> { gate ->
                Runnable {
                    gate.await()
                    val res = identityGroupService.addToGroup(req1)
                    Assertions.assertNotNull(res)
                }
            },
            java.util.function.Function<CyclicBarrier, Runnable> { gate ->
                Runnable {
                    gate.await()
                    val res = identityGroupService.addToGroup(req2)
                    Assertions.assertNotNull(res)
                }
            }
        )
        startThreadsSimultaneously(threadsFunc)
    }

    @Test
    fun addToGroupByIdAlreadyExistsTest() {
        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup2.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .build()
        val res = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)
    }

    @Test
    fun addToGroupUpdateGroupModifiedAtTest() {
        val createIdentityRequest = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setTypeId(identityGroupType.id)
                    .build()
            )
            .build()

        val identityResponse = identityService.createIdentity(createIdentityRequest)
        Assertions.assertNotNull(identityResponse)
        val identity = identityResponse.identity
        Assertions.assertNotNull(identity)

        val getIdentityRequest = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup1.id)
                .build())
            .build()

        var getIdentityResponse = identityService.getIdentity(getIdentityRequest)
        Assertions.assertNotNull(getIdentityResponse)
        val group1 = getIdentityResponse.identity
        Assertions.assertNotNull(group1)

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        val res = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)

        getIdentityResponse = identityService.getIdentity(getIdentityRequest)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)

        Assertions.assertNotEquals(group1.modifiedAt, getIdentityResponse.identity.modifiedAt)
    }

    @Test
    fun addToGroupUpdateAllGroupsUpModifiedAtTest() {
        val createIdentityRequest = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setTypeId(identityGroupType.id)
                    .build()
            )
            .build()

        val identityResponse = identityService.createIdentity(createIdentityRequest)
        Assertions.assertNotNull(identityResponse)
        val identity = identityResponse.identity
        Assertions.assertNotNull(identity)

        val getIdentity1Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup1.id)
                .build())
            .build()

        var getIdentityResponse = identityService.getIdentity(getIdentity1Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group1 = getIdentityResponse.identity
        Assertions.assertNotNull(group1)

        val getIdentity2Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup2.id)
                .build())
            .build()

        getIdentityResponse = identityService.getIdentity(getIdentity2Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group2 = getIdentityResponse.identity
        Assertions.assertNotNull(group2)

        val getIdentity3Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup3.id)
                .build())
            .build()

        getIdentityResponse = identityService.getIdentity(getIdentity3Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group3 = getIdentityResponse.identity
        Assertions.assertNotNull(group3)

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        val res = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)

        getIdentityResponse = identityService.getIdentity(getIdentity1Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)

        Assertions.assertNotEquals(group1.modifiedAt, getIdentityResponse.identity.modifiedAt)

        getIdentityResponse = identityService.getIdentity(getIdentity2Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)

        Assertions.assertNotEquals(group2.modifiedAt, getIdentityResponse.identity.modifiedAt)

        getIdentityResponse = identityService.getIdentity(getIdentity3Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)

        Assertions.assertNotEquals(group3.modifiedAt, getIdentityResponse.identity.modifiedAt)
    }

    @Test
    fun addToGroupDontUpdateGroupsDownModifiedAtTest() {
        val createIdentityRequest = CreateIdentityRequest.newBuilder()
            .setExternalIdentity(
                ExternalIdentity.newBuilder()
                    .setTypeId(identityGroupType.id)
                    .build()
            )
            .build()

        val identityResponse = identityService.createIdentity(createIdentityRequest)
        Assertions.assertNotNull(identityResponse)
        val identity = identityResponse.identity
        Assertions.assertNotNull(identity)

        val getIdentity1Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup1.id)
                .build())
            .build()

        var getIdentityResponse = identityService.getIdentity(getIdentity1Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group1 = getIdentityResponse.identity
        Assertions.assertNotNull(group1)

        val getIdentity2Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup2.id)
                .build())
            .build()

        getIdentityResponse = identityService.getIdentity(getIdentity2Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group2 = getIdentityResponse.identity
        Assertions.assertNotNull(group2)

        val getIdentity3Request = GetIdentityRequest.newBuilder()
            .setIdentity(IdentityCompositeId.newBuilder()
                .setId(identityGroup3.id)
                .build())
            .build()

        getIdentityResponse = identityService.getIdentity(getIdentity3Request)
        Assertions.assertNotNull(getIdentityResponse)
        val group3 = getIdentityResponse.identity
        Assertions.assertNotNull(group3)

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup3.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identity.id).build()
            )
            .build()
        val res = identityGroupService.addToGroup(req)
        Assertions.assertNotNull(res)

        getIdentityResponse = identityService.getIdentity(getIdentity1Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)

        Assertions.assertEquals(group1.modifiedAt, getIdentityResponse.identity.modifiedAt)

        getIdentityResponse = identityService.getIdentity(getIdentity2Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)

        Assertions.assertEquals(group2.modifiedAt, getIdentityResponse.identity.modifiedAt)

        getIdentityResponse = identityService.getIdentity(getIdentity3Request)
        Assertions.assertNotNull(getIdentityResponse)
        Assertions.assertNotNull(getIdentityResponse.identity)

        Assertions.assertNotEquals(group3.modifiedAt, getIdentityResponse.identity.modifiedAt)
    }

    @Test
    fun addToGroupFailOnFakeGroupIdTest() {
        val fakeId = UUID.randomUUID().toString()

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(fakeId).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.addToGroup(req)
        }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", fakeId),
            statusRuntimeException.message
        )
    }

    @Test
    fun addToGroupFailOnFakeGroupTypeIdTest() {
        val fakeTypeId = "fake_type_id"

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup2.externalId)
                        .setTypeId(fakeTypeId)
                        .build()
                ).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.addToGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: resource with externalId %s and typeId %s not found",
                identityGroup2.externalId, fakeTypeId),
            statusRuntimeException.message
        )
    }

    @Test
    fun addToGroupFailOnFakeGroupExternalIdTest() {
        val fakeExternalId = "fake_external_id"

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(fakeExternalId)
                        .setTypeId(identityGroup2.type.id)
                        .build()
                ).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.addToGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: resource with externalId %s and typeId %s not found",
                fakeExternalId, identityGroup2.type.id),
            statusRuntimeException.message
        )
    }

    @Test
    fun addToGroupFailOnFakeIdentityIdTest() {
        val fakeId = UUID.randomUUID().toString()

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(fakeId).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup4.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.addToGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: Resources not found"),
            statusRuntimeException.message
        )
    }

    @Test
    fun addToGroupFailOnFakeIdentityTypeIdTest() {
        val fakeTypeId = "fake_type_id"

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(identityGroup4.externalId)
                        .setTypeId(fakeTypeId)
                        .build()
                ).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.addToGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: Resources not found"),
            statusRuntimeException.message
        )
    }

    @Test
    fun addToGroupFailOnFakeIdentityExternalIdTest() {
        val fakeTypeId = "fake_type_id"

        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup1.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(fakeTypeId)
                        .setTypeId(identityGroup4.type.id)
                        .build()
                ).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.addToGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "NOT_FOUND: Resources not found"),
            statusRuntimeException.message
        )
    }

    @Test
    fun addIdentityGroupToThemselfFailTest() {
        val req = AddToGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(identityGroup2.id).build()
            )
            .addIdentities(
                IdentityCompositeId.newBuilder().setId(identityGroup2.id).build()
            )
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.addToGroup(req)
        }
        Assertions.assertEquals(
            String.format(
                "INVALID_ARGUMENT: Cycle detect for identity with id " + identityGroup2.id),
            statusRuntimeException.message
        )
    }

}
