package ru.yandex.intranet.imscore.grpc.identityGroup

import io.grpc.StatusRuntimeException
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import ru.yandex.intranet.imscore.IntegrationTest
import ru.yandex.intranet.imscore.grpc.common.TestData
import ru.yandex.intranet.imscore.proto.identity.ExternalIdentity
import ru.yandex.intranet.imscore.proto.identity.IdentityCompositeId
import ru.yandex.intranet.imscore.proto.identityGroup.*
import java.util.*

/**
 *  Exists identity relation with group tests.
 *
 * @author Mustakayev Marat <mmarat248@yandex-team.ru>
 */
@IntegrationTest
@TestPropertySource(properties = ["spring.liquibase.contexts=identity,identity-type-source,identity-type," +
    "identity-relation"])
open class ExistsInGroupTest {

    @GrpcClient("inProcess")
    private lateinit var identityGroupService: IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub

    @Test
    fun existsInGroupIndirectlyTest() {
        val req = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup3.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .build()
        val res: ExistsInGroupResponse = identityGroupService.existsInGroup(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.exists)
    }

    @Test
    fun existsInGroupIndirectlyByExternalIdentitiesTest() {
        val req = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(TestData.identityGroup2.externalId)
                        .setTypeId(TestData.identityGroup2.type.id)
                        .build()
                ).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(TestData.identity.externalId)
                        .setTypeId(TestData.identity.type.id)
                        .build()
                ).build()
            )
            .build()
        val res: ExistsInGroupResponse = identityGroupService.existsInGroup(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.exists)
    }

    @Test
    fun existsInGroupDirectlyTest() {
        val req = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup1.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        val res: ExistsInGroupResponse = identityGroupService.existsInGroup(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.exists)
    }

    @Test
    fun notExistsInGroupIndirectlyTest() {
        val req = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup4.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .build()
        val res: ExistsInGroupResponse = identityGroupService.existsInGroup(req)
        Assertions.assertNotNull(res)
        Assertions.assertFalse(res.exists)
    }

    @Test
    fun notExistsInGroupDirectlyTest() {
        val req = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup3.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        val res: ExistsInGroupResponse = identityGroupService.existsInGroup(req)
        Assertions.assertNotNull(res)
        Assertions.assertFalse(res.exists)
    }

    @Test
    fun existsInGroupOfFakeGroupTest() {
        val fakeIdentityId = UUID.randomUUID().toString()
        val req = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(fakeIdentityId).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.existsInGroup(req)
        }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", fakeIdentityId),
            statusRuntimeException.message
        )
    }

    @Test
    fun existsInGroupOfNotGroupTest() {
        val req = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup4.id).build()
            )
            .setOnlyDirectly(true)
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.existsInGroup(req)
        }
        Assertions.assertEquals(
            String.format("INVALID_ARGUMENT: %s is not a group", TestData.identity.id),
            statusRuntimeException.message
        )
    }

    @Test
    fun existsInGroupOfFakeIdentityTest() {
        val fakeIdentityId = UUID.randomUUID().toString()
        val req = ExistsInGroupRequest.newBuilder()
            .setGroup(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup1.id).build()
            )
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(fakeIdentityId).build()
            )
            .setOnlyDirectly(true)
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.existsInGroup(req)
        }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", fakeIdentityId),
            statusRuntimeException.message
        )
    }

}
