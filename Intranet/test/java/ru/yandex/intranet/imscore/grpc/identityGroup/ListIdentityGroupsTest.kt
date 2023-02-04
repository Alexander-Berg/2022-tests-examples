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
 *
 * @author Mustakayev Marat <mmarat248@yandex-team.ru>
 */
@IntegrationTest
@TestPropertySource(properties = ["spring.liquibase.contexts=identity,identity-type-source,identity-type," +
    "identity-relation"])
open class ListIdentityGroupsTest {

    @GrpcClient("inProcess")
    private lateinit var identityGroupService: IdentityGroupServiceGrpc.IdentityGroupServiceBlockingStub

    @Test
    fun listIdentityGroupsTest() {
        var req = ListIdentityGroupsRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setPageSize(1)
            .build()
        var res = identityGroupService.listIdentityGroups(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.identitiesList != null)
        Assertions.assertTrue(res.identitiesList.size == 1)
        Assertions.assertEquals(TestData.identityGroup1, res.identitiesList.first())

        // test next page
        req = ListIdentityGroupsRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setPageSize(1)
            .setPageToken(
                res.nextPageToken
            )
            .build()
        res = identityGroupService.listIdentityGroups(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.identitiesList != null)
        Assertions.assertTrue(res.identitiesList.size == 1)
        Assertions.assertEquals(TestData.identityGroup2, res.identitiesList.first())
    }

    @Test
    fun listIdentityGroupsByExternalIdTest() {
        var req = ListIdentityGroupsRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(TestData.identity.externalId)
                        .setTypeId(TestData.identity.type.id)
                        .build()
                ).build()
            )
            .setPageSize(1)
            .build()
        var res = identityGroupService.listIdentityGroups(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.identitiesList != null)
        Assertions.assertTrue(res.identitiesList.size == 1)
        Assertions.assertEquals(TestData.identityGroup1, res.identitiesList.first())

        // test next page
        req = ListIdentityGroupsRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setExternalIdentity(
                    ExternalIdentity.newBuilder()
                        .setExternalId(TestData.identity.externalId)
                        .setTypeId(TestData.identity.type.id)
                        .build()
                ).build()
            )
            .setPageSize(1)
            .setPageToken(
                res.nextPageToken
            )
            .build()
        res = identityGroupService.listIdentityGroups(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.identitiesList != null)
        Assertions.assertTrue(res.identitiesList.size == 1)
        Assertions.assertEquals(TestData.identityGroup2, res.identitiesList.first())
    }

    @Test
    fun listIdentityGroupsOfFakeIdentityTest() {
        val fakeIdentityId = UUID.randomUUID().toString()
        val req = ListIdentityGroupsRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(
                    fakeIdentityId
                ).build()
            )
            .setPageSize(1)
            .build()
        val statusRuntimeException = Assertions.assertThrows(StatusRuntimeException::class.java) {
            identityGroupService.listIdentityGroups(req)
        }
        Assertions.assertEquals(
            String.format("NOT_FOUND: resource with id %s not found", fakeIdentityId),
            statusRuntimeException.message
        )
    }

    @Test
    fun listIdentityDirectGroupsTest() {
        var req = ListIdentityGroupsRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setOnlyDirectly(true)
            .setPageSize(1)
            .build()
        var res = identityGroupService.listIdentityGroups(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.identitiesList != null)
        Assertions.assertTrue(res.identitiesList.size == 1)
        Assertions.assertEquals(TestData.identityGroup1, res.identitiesList.first())

        // test next page
        req = ListIdentityGroupsRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identity.id).build()
            )
            .setPageSize(1)
            .setOnlyDirectly(true)
            .setPageToken(
                res.nextPageToken
            )
            .build()
        res = identityGroupService.listIdentityGroups(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.identitiesList != null)
        Assertions.assertTrue(res.identitiesList.size == 0)
    }

    @Test
    fun listIdentityGroupsOfRootIdentityTest() {
        val req = ListIdentityGroupsRequest.newBuilder()
            .setIdentity(
                IdentityCompositeId.newBuilder().setId(TestData.identityGroup3.id).build()
            )
            .setPageSize(1)
            .build()
        val res = identityGroupService.listIdentityGroups(req)
        Assertions.assertNotNull(res)
        Assertions.assertTrue(res.identitiesList != null)
        Assertions.assertTrue(res.identitiesList.size == 0)
    }

}
