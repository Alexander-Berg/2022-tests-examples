package ru.yandex.infra.auth.idm.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.Role;
import ru.yandex.infra.auth.RoleSubject;
import ru.yandex.infra.auth.RolesInfo;
import ru.yandex.infra.auth.idm.service.IdmLeaf;
import ru.yandex.infra.auth.idm.service.IdmName;
import ru.yandex.infra.auth.idm.service.IdmRole;
import ru.yandex.infra.auth.nanny.NannyRole;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.yp.client.api.AccessControl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdmApiServiceTest {
    private static final int roleNodesLimit = 10;
    private static final int retriesCount = 5;
    private static final String system = "system";
    private static final Duration retriesDelay = Duration.ofMillis(0);
    private static final RoleNodeInfo nodeWithHelp = new RoleNodeInfo(
            123,
            new IdmName("help"),
            "slug",
            "parent",
            "value_path",
            false,
            "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d");
    private static final String maintainerRole = "MAINTAINER";
    private static final Role role = new Role("role", "");
    private final RoleSubject userSubject = new RoleSubject("user", 0L,
            new Role("path.to.role", maintainerRole));
    private static final RoleSubject groupSubject = new RoleSubject("", 1L,
            new Role("path.to.role", maintainerRole));
    private final RolesInfo rolesInfo = new RolesInfo.Builder()
            .addRole(
                    maintainerRole,
                    new IdmLeaf(new IdmName("MAINTAINER"), new IdmName("maintainer"), "MAINTAINER.e41ae482-87f4ed32-cdb633f0-aba7dcb9"),
                    ImmutableList.of(new RolesInfo.RoleAce(
                            ImmutableSet.of(
                                    AccessControl.EAccessControlPermission.ACP_READ,
                                    AccessControl.EAccessControlPermission.ACA_WRITE),
                            Collections.emptyList())
                    )
            ).build();
    private DummyIdmApiImpl idmApi;
    private IdmApiService idmApiService;

    @BeforeEach
    void before() {
        idmApi = new DummyIdmApiImpl(retriesCount);
        idmApiService = new IdmApiService(idmApi, null, rolesInfo, system, roleNodesLimit, retriesCount,
                retriesDelay, Duration.ofSeconds(1), GaugeRegistry.EMPTY);
    }

    @Test
    void apiReturnsAllNodes() {
        RoleNodeInfo nodeWithUniqueId = new RoleNodeInfo(
                14,
                new IdmName("help"),
                "slug",
                "parent",
                "/Project1/Stage1/DEVELOPER/",
                false,
                "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d");
        RoleNodeInfo oldNode = new RoleNodeInfo(
                10,
                new IdmName("help"),
                "slug",
                "parent",
                "/Project2/OWNER/",
                false,
                "");

        final ArrayList<RoleNodeInfo> nodes = new ArrayList<>(Collections.nCopies(roleNodesLimit-1, nodeWithUniqueId));
        nodes.add(oldNode);

        idmApi.nodesToReturn = nodes;

        assertDoesNotThrow(() -> {
            Set<Role> roleNodes = idmApiService.getRoleNodes();
            // Only two role nodes + ROOT node are expected
            assertThat(roleNodes, hasSize(3));
            assertTrue(roleNodes.contains(Role.empty()));
            assertTrue(roleNodes.contains(new Role("Project1.Stage1", "DEVELOPER", "", "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d")));
            assertTrue(roleNodes.contains(new Role("Project2", "OWNER", "", "")));
        });
    }

    @Test
    void apiAlwaysReturnsMoreChunksThenExpected() {
        idmApi.getRoleNodesResponse = new CompletableFuture<>();
        var list = new ArrayList<>(Collections.nCopies(roleNodesLimit + 1, nodeWithHelp));
        idmApi.getRoleNodesResponse.complete(new RoleNodesResponse(10, null, 0, null, 11, list));

        assertThrows(IdmApiServiceError.class, () -> idmApiService.getRoleNodes());
    }

    @Test
    void apiAlwaysReturnErrorOptional() {
        idmApi.getRoleNodesResponse = new CompletableFuture<>();
        idmApi.getRoleNodesResponse.completeExceptionally(new IdmApiServiceError("Error"));

        assertThrows(IdmApiServiceError.class, () -> idmApiService.getRoleNodes());
    }

    @Test
    void apiOneTimeReturnsMoreChunksThenExpected() {
        idmApi.setFailChunkCount(1);

        List<RoleNodeInfo> nodes = new ArrayList<>(Collections.nCopies(roleNodesLimit, nodeWithHelp));
        idmApi.nodesToReturn = nodes;

        nodes.add(nodeWithHelp);
        idmApi.getRoleNodesFailFuture().complete(nodes);

        assertDoesNotThrow(() -> {
            Set<Role> roleNodes = idmApiService.getRoleNodes();
            // Only one role node + ROOT node are expected
            assertThat(roleNodes, hasSize(2));
        });
    }

    @Test
    void apiReturnsErrorButLessThanMaxRetriesTimes() {
        idmApi.setFailChunkCount(retriesCount - 1);

        List<RoleNodeInfo> nodes = new ArrayList<>(Collections.nCopies(roleNodesLimit, nodeWithHelp));
        idmApi.nodesToReturn = nodes;

        idmApi.getRoleNodesFailFuture().completeExceptionally(new IdmApiServiceError("Error"));

        assertDoesNotThrow(() -> {
            Set<Role> roleNodes = idmApiService.getRoleNodes();
            // Only one role node + ROOT node are expected
            assertThat(roleNodes, hasSize(2));
        });
    }

    private void generateNodes(int startId, int count) {
        ArrayList<RoleNodeInfo> nodes = new ArrayList<>();
        for (int i = startId; i < startId + count; i++) {
            nodes.add(new RoleNodeInfo(i, new IdmName("help"), "slug", "parent",
                    "value_path"+i, false, "uniqueId"+i));
        }
        idmApi.nodesToReturn = nodes;
        idmApi.fullChunkCount = count / roleNodesLimit + 1;
    }

    @Test
    void getRoleNodes3BatchesTest() {
        generateNodes(3, 35);
        assertDoesNotThrow(() -> {
            Set<Role> roleNodes = idmApiService.getRoleNodes();
            assertThat(roleNodes, hasSize(35 + 1));
            assertThat(idmApi.requestsWithLastKey, contains(12L, 22L, 32L));
        });
    }

    @Test
    void getRoleNodesSingleBatchWith10NodesTest() {
        generateNodes(3, 10);
        assertDoesNotThrow(() -> {
            Set<Role> roleNodes = idmApiService.getRoleNodes();
            assertThat(roleNodes, hasSize(10 + 1));
            assertThat(idmApi.requestsWithLastKey.size(), equalTo(0));
        });
    }

    @Test
    void getRoleNodes1BatchTest() {
        generateNodes(3, 5);
        assertDoesNotThrow(() -> {
            Set<Role> roleNodes = idmApiService.getRoleNodes();
            assertThat(roleNodes, hasSize(5 + 1));
            assertThat(idmApi.requestsWithLastKey.size(), equalTo(0));
        });
    }

    @Test
    void getRoleNodesHandleEmptyListTest() {
        idmApi.nodesToReturn = new ArrayList<>();
        assertDoesNotThrow(() -> {
            Set<Role> roleNodes = idmApiService.getRoleNodes();
            assertThat(roleNodes, hasSize(0));
            assertThat(idmApi.requestsWithLastKey.size(), equalTo(0));
        });
    }

    @Test
    void serverReturnsSuccessWhileRemoveRoleNode() {
        assertDoesNotThrow(() -> idmApiService.removeRoleNode(role));
    }

    @Test
    void serverReturnsClientErrorWhileRemoveRoleNode() {
        idmApi.getErrorResponse().complete(new HttpResponse(400, "ClientError"));
        idmApi.setErrorResponseCount(retriesCount);

        assertThrows(IdmApiServiceError.class, () -> idmApiService.removeRoleNode(role));
        assertThat(idmApi.getCurrentResponseCounter(), equalTo(1));
    }

    @Test
    void serverReturnsServerErrorLessThanMaxRetriesTimesWhileRemoveRoleNode() {
        idmApi.getErrorResponse().complete(new HttpResponse(500, "ServerError"));
        idmApi.setErrorResponseCount(retriesCount - 1);

        assertDoesNotThrow(() -> {
            idmApiService.removeRoleNode(role);
            assertThat(idmApi.getCurrentResponseCounter(), equalTo(retriesCount));
        });
    }

    @Test
    void serverReturnsServerErrorEqualThanMaxRetriesTimesWhileRemoveRoleNode() {
        idmApi.getErrorResponse().complete(new HttpResponse(500, "ServerError"));
        idmApi.setErrorResponseCount(retriesCount);

        assertThrows(IdmApiServiceError.class, () -> idmApiService.removeRoleNode(role));
        assertThat(idmApi.getCurrentResponseCounter(), equalTo(retriesCount));
    }

    private String pathBasedIdGenerator(BatchRequest.BatchBody body) {
        if (body instanceof AddNodeRequest.AddNodeRequestBody) {
            var nodeRequestBody = (AddNodeRequest.AddNodeRequestBody)body;
            return nodeRequestBody.getPath();
        }
        if (body instanceof AddSubjectRequest) {
            var subjectRequest = (AddSubjectRequest)body;
            return subjectRequest.getPath();
        }
        if (body == null) {
            return "empty";
        }
        return BatchRequest.UUID_REQUEST_ID_GENERATOR.apply(body);
    }

    @Test
    void convertRoleToBatchRequestTest() {

        BatchRequest.requestIdGenerator = this::pathBasedIdGenerator;

        Role roleProject = new Role("Project1", "", "", "");
        Role role1 = new Role("Project1", "OWNER", "", "");
        Role roleStage = new Role("Project1.Stage1", "", "", "");
        Role role2 = new Role("Project1.Stage1", "APPROVER", "", "");
        Role role3 = new Role("Project1.Stage2", "DEVELOPER", "", "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d");
        assertDoesNotThrow(() -> {
            idmApiService.addRoleNodes(List.of(roleProject, role1, roleStage, role2, role3), Collections.emptyMap());
        });

        var expectedRequestBodies = new ArrayList<AddNodeRequest.AddNodeRequestBody>();

        //roleProject
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("Project1"), IdmName.EMPTY_NAME, "Project1", system, "/ROOT/", ""));
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("Project role or Stage name"), IdmName.EMPTY_NAME, "Project1", system, "/ROOT/Project1/", ""));

        //role1
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("OWNER"), new IdmName("OWNER"), "OWNER", system, "/ROOT/Project1/Project1/", ""));

        //roleStage
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("Stage1"), IdmName.EMPTY_NAME, "Stage1", system, "/ROOT/Project1/Project1/", ""));
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("Stage role or box name"), IdmName.EMPTY_NAME, "Project1.Stage1", system, "/ROOT/Project1/Project1/Stage1/", ""));

        //role2
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("APPROVER"), new IdmName("APPROVER"), "APPROVER", system, "/ROOT/Project1/Project1/Stage1/Project1.Stage1/", ""));

        //role3
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("DEVELOPER"), new IdmName("DEVELOPER"), "DEVELOPER", system, "/ROOT/Project1/Project1/Stage2/Project1.Stage2/", "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d"));


        List<BatchRequest> expectedBatchRequests = expectedRequestBodies
                .stream()
                .map(AddNodeRequest::new)
                .collect(Collectors.toList());

        assertThat(idmApi.getLastBatchRequest(), equalTo(expectedBatchRequests));
    }

    @Test
    void convertNannyRolesToBatchRequestTest() {

        BatchRequest.requestIdGenerator = this::pathBasedIdGenerator;

        Role roleNanny = new Role("Project1." + Role.NANNY_ROLES_PARENT_NODE, "", "", "");
        Role roleService = new Role("Project1."+ Role.NANNY_ROLES_PARENT_NODE +".service1", "", "", "");
        Role role1 = new NannyRole("Project1."+ Role.NANNY_ROLES_PARENT_NODE +".service1", "owners", "uniqueId", Set.of("user1"), Set.of());
        Role role2 = new NannyRole("Project1."+ Role.NANNY_ROLES_PARENT_NODE +".service1", "developers", "", Set.of(), Set.of(5L));
        assertDoesNotThrow(() -> {
            idmApiService.addRoleNodes(List.of(roleNanny, roleService, role1, role2), Collections.emptyMap());
        });

        var expectedRequestBodies = new ArrayList<AddNodeRequest.AddNodeRequestBody>();

        //roleNanny
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName(Role.NANNY_ROLES_PARENT_NODE), IdmName.EMPTY_NAME, Role.NANNY_ROLES_PARENT_NODE, system, "/ROOT/Project1/Project1/", ""));
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("Service name"), IdmName.EMPTY_NAME, "Project1." + Role.NANNY_ROLES_PARENT_NODE, system, "/ROOT/Project1/Project1/" + Role.NANNY_ROLES_PARENT_NODE + "/", ""));

        //roleService
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("service1"), IdmName.EMPTY_NAME, "service1", system, "/ROOT/Project1/Project1/"+Role.NANNY_ROLES_PARENT_NODE+"/Project1."+Role.NANNY_ROLES_PARENT_NODE+"/", ""));
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("role"), IdmName.EMPTY_NAME, "Project1."+Role.NANNY_ROLES_PARENT_NODE+".service1", system,
                "/ROOT/Project1/Project1/"+Role.NANNY_ROLES_PARENT_NODE+"/Project1."+Role.NANNY_ROLES_PARENT_NODE+"/service1/",
                ""));

        //role1
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("owners"), new IdmName("owners"), "owners", system, "/ROOT/Project1/Project1/"+Role.NANNY_ROLES_PARENT_NODE+"/Project1."+Role.NANNY_ROLES_PARENT_NODE+"/service1/Project1."+Role.NANNY_ROLES_PARENT_NODE+".service1/", "uniqueId"));

        //role2
        expectedRequestBodies.add(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("developers"), new IdmName("developers"), "developers", system, "/ROOT/Project1/Project1/"+Role.NANNY_ROLES_PARENT_NODE+"/Project1."+Role.NANNY_ROLES_PARENT_NODE+"/service1/Project1."+Role.NANNY_ROLES_PARENT_NODE+".service1/", ""));

        List<BatchRequest> expectedBatchRequests = expectedRequestBodies
                .stream()
                .map(AddNodeRequest::new)
                .collect(Collectors.toList());

        //role subjects for "user1" and group = 5L
        expectedBatchRequests.add(new BatchedAddSubjectRequest(new AddUserSubjectRequest(system, "/Project1/"+Role.NANNY_ROLES_PARENT_NODE+"/service1/owners/", "user1")));
        expectedBatchRequests.add(new BatchedAddSubjectRequest(new AddGroupSubjectRequest(system, "/Project1/"+Role.NANNY_ROLES_PARENT_NODE+"/service1/developers/", 5L)));

        assertThat(idmApi.getLastBatchRequest(), equalTo(expectedBatchRequests));
    }

    @Test
    void serverReturnsSuccessWhileAddRoleNodes() {
        assertDoesNotThrow(() -> {
            idmApiService.addRoleNodes(List.of(role), Collections.emptyMap());
        });
    }

    @Test
    void serverReturnsClientErrorWhileAddRoleNodes() {
        BatchRequest.requestIdGenerator = this::pathBasedIdGenerator;

        idmApi.getErrorBatchResponse().complete(new BatchResponse.Builder()
                .addStatusCode(400)
                .addResponse(new BatchResponse.Response("/ROOT/role/", 400, "ClientError"))
                .addResponse(new BatchResponse.Response("/ROOT/role/Project role or Stage name/", 400, "ClientError"))
                .build());
        idmApi.setErrorResponseCount(retriesCount);

        assertDoesNotThrow(() -> {
            idmApiService.addRoleNodes(List.of(role), Collections.emptyMap());
        });
        assertThat(idmApi.getCurrentResponseCounter(), equalTo(1));
    }

    @Test
    void serverReturnsServerErrorLessThanMaxRetriesTimesWhileAddRoleNodes() {
        idmApi.getErrorBatchResponse().complete(new BatchResponse.Builder()
                .addStatusCode(500)
                .addResponse(new BatchResponse.Response("id", 500, "ServerError"))
                .build());
        idmApi.setErrorResponseCount(retriesCount - 1);

        assertDoesNotThrow(() -> {
            idmApiService.addRoleNodes(List.of(role), Collections.emptyMap());
        });
        assertThat(idmApi.getCurrentResponseCounter(), equalTo(retriesCount));
    }

    @Test
    void serverReturnsServerErrorEqualThanMaxRetriesTimesWhileAddRemoveRoleNodes() {
        idmApi.getErrorBatchResponse().complete(new BatchResponse.Builder()
                .addStatusCode(500)
                .addResponse(new BatchResponse.Response("id", 500, "ServerError"))
                .build());
        idmApi.setErrorResponseCount(retriesCount);

        assertThrows(IdmApiServiceError.class, () -> idmApiService.addRoleNodes(List.of(role), Collections.emptyMap()));
        assertThat(idmApi.getCurrentResponseCounter(), equalTo(retriesCount));
    }

    @Test
    void convertRoleSubjectToRequestTest() {
        assertDoesNotThrow(() -> {
            idmApiService.addRoleSubject(userSubject);
        });

        AddSubjectRequest expectedUserRequest = new AddUserSubjectRequest(
                system, IdmRole.createFromRole(userSubject.getRole()).getValuePath(), userSubject.getLogin());
        assertThat(idmApi.getLastAddSubjectRequest(), equalTo(expectedUserRequest));

        assertDoesNotThrow(() -> {
            idmApiService.addRoleSubject(groupSubject);
        });

        AddSubjectRequest expectedGroupRequest = new AddGroupSubjectRequest(
                system, IdmRole.createFromRole(groupSubject.getRole()).getValuePath(), groupSubject.getGroupId());
        assertThat(idmApi.getLastAddSubjectRequest(), equalTo(expectedGroupRequest));
    }

    @Test
    void serverReturnsSuccessWhileAddRoleSubject() {
        assertDoesNotThrow(() -> idmApiService.addRoleSubject(userSubject));
    }

    @Test
    void serverReturnsClientErrorWhileAddRoleSubject() {
        idmApi.getErrorResponse().complete(new HttpResponse(400, "ClientError"));
        idmApi.setErrorResponseCount(retriesCount);

        assertThrows(IdmApiServiceError.class, () -> idmApiService.addRoleSubject(userSubject));
        assertThat(idmApi.getCurrentResponseCounter(), equalTo(1));
    }

    @Test
    void serverReturnsServerErrorLessThanMaxRetriesTimesWhileAddRoleSubject() {
        idmApi.getErrorResponse().complete(new HttpResponse(500, "ServerError"));
        idmApi.setErrorResponseCount(retriesCount - 1);

        assertDoesNotThrow(() -> {
            idmApiService.addRoleSubject(userSubject);
            assertThat(idmApi.getCurrentResponseCounter(), equalTo(retriesCount));
        });
    }

    @Test
    void serverReturnsServerErrorEqualThanMaxRetriesTimesWhileAddRoleSubject() {
        idmApi.getErrorResponse().complete(new HttpResponse(500, "ServerError"));
        idmApi.setErrorResponseCount(retriesCount);

        assertThrows(IdmApiServiceError.class, () -> idmApiService.addRoleSubject(userSubject));
        assertThat(idmApi.getCurrentResponseCounter(), equalTo(retriesCount));
    }

    @Test
    void batchRequestWithRelocatedRolesTest() {

        BatchRequest.requestIdGenerator = this::pathBasedIdGenerator;

        assertDoesNotThrow(() -> {
            idmApiService.addRoleNodes(
                    List.of(
                        new Role("Project1.Stage1", "", "", "c65f9708-f8a2c60d-10112dbb-1c31717d"),
                        new Role("Project1.Stage1", "DEVELOPER", "", "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d")
                    ),
                    Map.of(
                        "c65f9708-f8a2c60d-10112dbb-1c31717d", new Role("PreviousProject.Stage1", "", "", "c65f9708-f8a2c60d-10112dbb-1c31717d"),
                        "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d", new Role("PreviousProject.Stage1", "DEVELOPER", "", "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d")
                    ));
        });

        List<BatchRequest> expectedBatchRequests = new ArrayList<>();

        expectedBatchRequests.add(new RemoveNodeRequest(system, "/ROOT/PreviousProject/PreviousProject/Stage1/"));

        expectedBatchRequests.add(new AddNodeRequest(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("Stage1"), IdmName.EMPTY_NAME, "Stage1", system, "/ROOT/Project1/Project1/", "c65f9708-f8a2c60d-10112dbb-1c31717d")));
        expectedBatchRequests.add(new AddNodeRequest(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("Stage role or box name"), IdmName.EMPTY_NAME, "Project1.Stage1", system, "/ROOT/Project1/Project1/Stage1/", "")));

        expectedBatchRequests.add(new AddNodeRequest(new AddNodeRequest.AddNodeRequestBody(
                new IdmName("DEVELOPER"), new IdmName("DEVELOPER"), "DEVELOPER", system, "/ROOT/Project1/Project1/Stage1/Project1.Stage1/", "DEVELOPER.c65f9708-f8a2c60d-10112dbb-1c31717d")));


        assertThat(idmApi.getLastBatchRequest(), equalTo(expectedBatchRequests));
    }

}
