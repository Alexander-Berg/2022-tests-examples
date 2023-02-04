package ru.yandex.infra.auth.yp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.infra.auth.Metrics;
import ru.yandex.infra.auth.Role;
import ru.yandex.infra.auth.RoleSubject;
import ru.yandex.infra.auth.RolesInfo;
import ru.yandex.infra.auth.TreeNode;
import ru.yandex.infra.auth.idm.service.IdmLeaf;
import ru.yandex.infra.auth.idm.service.IdmName;
import ru.yandex.infra.auth.tasks.RoleSubjectCacheUpdater;
import ru.yandex.infra.auth.yp.YpObjectsTreeGetter.TreeNodeWithTimestamp;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.yp.DummyYpObjectRepository;
import ru.yandex.infra.controller.yp.DummyYpTransactionClient;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.model.YpObjectType;

import static java.lang.Thread.sleep;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.stream.Stream.concat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.auth.Role.ROLE_NAME_DELIMITER;
import static ru.yandex.infra.controller.testutil.FutureUtils.get5s;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACA_CREATE;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACA_USE;

class YpServiceTest {
    public static final String DEVELOPER_ROLE = "DEVELOPER";
    public static final String SYSTEM_DEVELOPER_ROLE = "SYSTEM_DEVELOPER";
    public static final String SYSTEM_BOX = "system-box";
    public static final String COMMON_BOX = "box";
    private static final String ROOT_CONFIG_PATH = "roles_tree.root";
    private static final String DEFAULT_PROJECT = "project1";
    private static final String DEFAULT_STAGE = "stage1";
    private static final String DEFAULT_SPECIFIC_BOX = "box1";
    private static final String DEFAULT_ROLE = "OWNER";
    private static final String DEPLOY_SYSTEM_NAME = "deploy-testing";
    private static final String DEPLOY_GROUP_PREFIX = DEPLOY_SYSTEM_NAME + ":";
    private static final String IDM_GROUP_PREFIX = "idm:";
    private static final String GROUP_ID = "idm:1";
    private final String projectPath = DEFAULT_PROJECT;
    private final String stagePath = projectPath + ROLE_NAME_DELIMITER + DEFAULT_STAGE;
    private final String boxPath = stagePath + ROLE_NAME_DELIMITER + DEFAULT_SPECIFIC_BOX;
    private final int maxRetriesCount = 5;

    private DummyYpObjectTreeGetter ypObjectTreeGetter;
    private DummyYpObjectPermissionsUpdater ypStagesUpdater;
    private DummyYpObjectPermissionsUpdater ypProjectsUpdater;
    private DummyYpObjectPermissionsUpdater ypNannyServiceUpdater;
    private DummyYpGroupsClient masterYpGroupsClient;
    private DummyYpObjectRepository<StageMeta, TStageSpec, TStageStatus> stageRepository;
    private Map<String, YpGroupsClient> slavesYpGroupsClient;
    private DummyYpGroupsClient slaveYpGroupsClientSAS;
    private Map<String, YpClients> slaveYpClients;
    private YpClients masterYpClients;
    private YpService ypService;
    private RolesInfo rolesInfo;
    private MetricRegistry metricsRegistry;

    private void checkAddedRoleSubjectForObject(DummyYpObjectPermissionsUpdater updater,
                                                String objectId, RoleSubject roleSubject, String boxId) {
        String updatedGroup = YpServiceReadOnlyImpl.getYpGroupName(roleSubject.getRole(), DEPLOY_SYSTEM_NAME);
        String addedSubject = roleSubject.isPersonal()
                ? roleSubject.getLogin()
                : IDM_GROUP_PREFIX + roleSubject.getGroupId();

        // Give some time for async queue to execute slave group update
        waitForAsync();
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastUpdatedGroup, equalTo(updatedGroup));
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastMembers, containsInAnyOrder(addedSubject));
        });

        assertThat(updater.lastUpdatedObjectId, equalTo(objectId));
        assertThat(updater.lastAddedSubjectId, equalTo(updatedGroup));
    }

    @BeforeEach
    void before() {
        ypStagesUpdater = new DummyYpObjectPermissionsUpdater();
        ypProjectsUpdater = new DummyYpObjectPermissionsUpdater();
        ypNannyServiceUpdater = new DummyYpObjectPermissionsUpdater();
        ypObjectTreeGetter = new DummyYpObjectTreeGetter();
        masterYpGroupsClient = new DummyYpGroupsClient();
        slaveYpGroupsClientSAS = new DummyYpGroupsClient();
        slavesYpGroupsClient = Map.of("sas", slaveYpGroupsClientSAS);
        slaveYpClients = Map.of("sas", new YpClients(new DummyYpTransactionClient(), slaveYpGroupsClientSAS));
        stageRepository = new DummyYpObjectRepository<>();
        masterYpClients = new YpClients(new DummyYpTransactionClient(), masterYpGroupsClient, null, stageRepository, null, null, null);
        Config config = ConfigFactory.load("roles_to_permissions.conf");
        Config rootConfig = config.getConfig(ROOT_CONFIG_PATH);
        rolesInfo = RolesInfo.fromConfig(config);
        metricsRegistry = new MetricRegistry();
        Metrics metrics = new Metrics(metricsRegistry, 1);

        ypService = new YpServiceImpl(ypObjectTreeGetter, ypStagesUpdater, ypProjectsUpdater, ypNannyServiceUpdater,
                masterYpClients, slaveYpClients, rolesInfo, rootConfig,
                Duration.ofMillis(10), maxRetriesCount, metricsRegistry, metrics, DEPLOY_SYSTEM_NAME);
    }

    @Test
    void systemNameWithoutColon() {

        Config config = ConfigFactory.load("roles_to_permissions.conf");
        Config rootConfig = config.getConfig(ROOT_CONFIG_PATH);
        rolesInfo = RolesInfo.fromConfig(config);

        Assertions.assertThrows(RuntimeException.class, () -> {
            ypService = new YpServiceImpl(ypObjectTreeGetter, ypStagesUpdater, ypProjectsUpdater, ypNannyServiceUpdater,
                    masterYpClients, slaveYpClients, rolesInfo, rootConfig,
                    Duration.ofMillis(10), maxRetriesCount, new MetricRegistry(),
                    new Metrics(new MetricRegistry(), 1), "systemName:withColon");
        });
    }

    @Test
    void emptyTreeConvertTest() {
        assertDoesNotThrow(() -> {
            Set<Role> roles = ypService.getRoles();
            assertThat(roles, empty());
        });
    }

    @Test
    void onlyRootConvertTest() {
        ypObjectTreeGetter.root =
                new TreeNodeWithTimestamp(
                        new TreeNode.Builder().makeConvertibleToRole().build(),
                        0L, null);

        assertDoesNotThrow(() -> {
            Set<Role> roles = ypService.getRoles();
            Set<Role> resultRoles = ImmutableSet.of(
                    new Role("", ""),
                    new Role("", "SUPER_USER")
            );

            assertThat(roles, equalTo(resultRoles));
        });
    }

    @Test
    void fullTreeConvertTest() {
        ypObjectTreeGetter.root = new TreeNodeWithTimestamp(new TreeNode.Builder()
                .withChild(new TreeNode.Builder().withName("alone-project").makeConvertibleToRole())
                .withChild(new TreeNode.Builder()
                        .withName("project")
                        .withChild(new TreeNode.Builder()
                                .withName("stage")
                                .withChild(new TreeNode.Builder()
                                        .withName("first-user-box")
                                        .makeConvertibleToRole())
                                .withChild(new TreeNode.Builder()
                                        .withName("second-user-box")
                                        .makeConvertibleToRole())
                                .makeConvertibleToRole())
                        .makeConvertibleToRole()
                ).makeConvertibleToRole()
                .build(), 0L, null);

        assertDoesNotThrow(() -> {
            Set<Role> roles = ypService.getRoles();
            Set<Role> resultRoles = ImmutableSet.of(
                    new Role("", ""),
                    new Role("", "SUPER_USER"),
                    new Role("alone-project", ""),
                    new Role("alone-project", "OWNER"),
                    new Role("alone-project", "MAINTAINER"),
                    new Role("alone-project", "DEVELOPER"),
                    new Role("alone-project", "ROOT_DEVELOPER"),
                    new Role("alone-project", "SYSTEM_DEVELOPER"),
                    new Role("alone-project", "APPROVER"),
                    new Role("alone-project", "MANDATORY_APPROVER"),
                    new Role("alone-project", "DEPLOYER"),
                    new Role("alone-project", "RESPONSIBLE"),
                    new Role("alone-project", "VIEWER"),
                    new Role("project", ""),
                    new Role("project", "OWNER"),
                    new Role("project", "MAINTAINER"),
                    new Role("project", "DEVELOPER"),
                    new Role("project", "ROOT_DEVELOPER"),
                    new Role("project", "SYSTEM_DEVELOPER"),
                    new Role("project", "APPROVER"),
                    new Role("project", "MANDATORY_APPROVER"),
                    new Role("project", "DEPLOYER"),
                    new Role("project", "RESPONSIBLE"),
                    new Role("project", "VIEWER"),
                    new Role("project.stage", ""),
                    new Role("project.stage", "MAINTAINER"),
                    new Role("project.stage", "DEVELOPER"),
                    new Role("project.stage", "ROOT_DEVELOPER"),
                    new Role("project.stage", "SYSTEM_DEVELOPER"),
                    new Role("project.stage", "APPROVER"),
                    new Role("project.stage", "MANDATORY_APPROVER"),
                    new Role("project.stage", "DEPLOYER"),
                    new Role("project.stage", "RESPONSIBLE"),
                    new Role("project.stage", "VIEWER"),
                    new Role("project.stage.first-user-box", ""),
                    new Role("project.stage.first-user-box", "DEVELOPER"),
                    new Role("project.stage.first-user-box", "ROOT_DEVELOPER"),
                    new Role("project.stage.second-user-box", ""),
                    new Role("project.stage.second-user-box", "DEVELOPER"),
                    new Role("project.stage.second-user-box", "ROOT_DEVELOPER")
            );

            assertThat(roles, containsInAnyOrder(resultRoles.toArray()));
        });
    }

    @Test
    void getRoleSubjectsWithEmptyListOfProjectsAndStagesTest() {
        masterYpGroupsClient.getGroupsWithPrefixResponse = completedFuture(Map.of(
                DEPLOY_GROUP_PREFIX + "group1.OWNER", Set.of("member1", "idm:2"),
                DEPLOY_GROUP_PREFIX + "group2.MAINTAINER", Set.of("member3")
        ));

        Set<RoleSubject> roleSubjects = ypService.getRoleSubjects();
        assertThat(roleSubjects, empty());
    }

    @Test
    void getRoleSubjectsTest() {
        var groups = Map.of(
                DEPLOY_GROUP_PREFIX + "group1.OWNER", Set.of("member1", "idm:2"),
                DEPLOY_GROUP_PREFIX + "group2.MAINTAINER", Set.of("member3"),
                DEPLOY_GROUP_PREFIX + "groupWithoutProject.OWNER", Set.of("member1"),
                DEPLOY_GROUP_PREFIX + "group1.missedStage.OWNER", Set.of("member1"),
                DEPLOY_GROUP_PREFIX + "group1.stage1.APPROVER", Set.of("member5")
        );
        ypObjectTreeGetter.root = new TreeNodeWithTimestamp(new TreeNode.Builder()
                .withChild(new TreeNode.Builder().withName("group1")
                        .withChild(new TreeNode.Builder()
                                .withName("stage1")
                                .makeConvertibleToRole())
                        .makeConvertibleToRole())
                .withChild(new TreeNode.Builder().withName("group2").makeConvertibleToRole())
                .build(), 0L, groups);

        Set<RoleSubject> roleSubjects = ypService.getRoleSubjects();
        assertEquals(Set.of(
                        new RoleSubject("member1", 0L, new Role("group1", "OWNER")),
                        new RoleSubject("member3", 0L, new Role("group2", "MAINTAINER")),
                        new RoleSubject("", 2L, new Role("group1", "OWNER")),
                        new RoleSubject("member5", 0L, new Role("group1.stage1", "APPROVER"))
                ),
                roleSubjects);
    }

    @Test
    void getRoleSubjectsErrorTest() {
        masterYpGroupsClient.getGroupsWithPrefixResponse = failedFuture(new RuntimeException());

        Set<RoleSubject> roleSubjects = ypService.getRoleSubjects();
        assertThat(roleSubjects, empty());
    }

    @Test
    void getRoleSubjectsInvalidResponseTest() {
        masterYpGroupsClient.getGroupsWithPrefixResponse = completedFuture(Map.of(
                "deplo:group1.OWNER", Set.of("member1", "idm:2"),
                "deplo:group2.MAINTAINER", Set.of("member3")
        ));
        Set<RoleSubject> roleSubjects = ypService.getRoleSubjects();
        assertThat(roleSubjects, empty());

        masterYpGroupsClient.getGroupsWithPrefixResponse = completedFuture(Map.of(
                "group1.OWNER", Set.of("member1", "idm:2"),
                "group2.MAINTAINER", Set.of("member3")
        ));
        roleSubjects = ypService.getRoleSubjects();
        assertThat(roleSubjects, empty());
    }

    private Map<Role, Set<String>> filterEmpty(Map<Role, Set<String>> roles) {
        return roles.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    void getRoleSubjectsFromCacheTest() {
        Map<String, Set<String>> groups = Map.of(
                DEPLOY_GROUP_PREFIX + "project_id_1.OWNER", Set.of("member1", "idm:2"),
                DEPLOY_GROUP_PREFIX + "project_id_2.MAINTAINER", Set.of(),
                DEPLOY_GROUP_PREFIX + "missed_project_id.OWNER", Set.of("member1"),
                DEPLOY_GROUP_PREFIX + "project_id_1.missedStage.MAINTAINER", Set.of("member1"),
                DEPLOY_GROUP_PREFIX + "project_id_1.stage1.APPROVER", Set.of("member5")
        );
        ypObjectTreeGetter.root = new YpObjectsTreeGetter.TreeNodeWithTimestamp(new TreeNode.Builder()
                .withChild(new TreeNode.Builder().withName("project_id_1")
                        .withChild(new TreeNode.Builder()
                                .withName("stage1")
                                .makeConvertibleToRole())
                        .makeConvertibleToRole())
                .withChild(new TreeNode.Builder().withName("project_id_2").makeConvertibleToRole())
                .build(), 0L, groups);

        RoleSubjectCacheUpdater updater = new RoleSubjectCacheUpdater(ypService, Duration.ofSeconds(30),
                Duration.ofSeconds(30), new MapGaugeRegistry());
        updater.updateRoleSubjectCache();

        assertEquals(Map.of(
                new Role("project_id_1", "OWNER"), Set.of("member1", "idm:2"),
                new Role("project_id_1.stage1", "APPROVER"), Set.of("member5")
        ), filterEmpty(updater.getRoleMembers("project_id_1")));

        assertEquals(Collections.emptyMap(), filterEmpty(updater.getRoleMembers("project_id_2")));

        Assertions.assertNull(updater.getRoleMembers("missed_project_id"));

        assertEquals(List.of("project_id_2"), updater.getProjectsWithoutOwner());

        updater.shutdown();
    }

    @Test
    void getRoleSubjectsFromRoleMembersTest() {
        Role role1 = new Role("project", "OWNER");
        Role role2 = new Role("project2.stage1", "APPROVER");

        var roleMembers = Map.of(
                role1, Set.of("user1", "idm:223", "user2"),
                role2, Set.of("user3"));

        Assertions.assertEquals(YpServiceReadOnlyImpl.getRoleSubjectsFromRoleMembers(roleMembers), Set.of(
                new RoleSubject("user1", 0L, role1),
                new RoleSubject("", 223L, role1),
                new RoleSubject("user2", 0L, role1),
                new RoleSubject("user3", 0L, role2)
        ));
    }

    @Test
    void getIdmGroupsErrorTest() {
        masterYpGroupsClient.getGroupsWithPrefixResponse = failedFuture(new RuntimeException());

        Map<String, Set<String>> roleSubjects = ypService.getGroupsWithPrefix(IDM_GROUP_PREFIX);
        assertEquals(roleSubjects, Collections.emptyMap());
    }

    @Test
    void getIdmGroupsInvalidResponseTest() {
        masterYpGroupsClient.getGroupsWithPrefixResponse = completedFuture(Map.of(
                "id:12", Set.of("member1", "member2"),
                "id:34", Set.of("member3")
        ));
        Set<RoleSubject> roleSubjects = ypService.getRoleSubjects();
        assertThat(roleSubjects, empty());

        masterYpGroupsClient.getGroupsWithPrefixResponse = completedFuture(Map.of(
                "12", Set.of("member1", "member2"),
                "34", Set.of("member3")
        ));
        roleSubjects = ypService.getRoleSubjects();
        assertThat(roleSubjects, empty());
    }

    @Test
    void addRoleSubjectForProjectTest() {
        addRoleSubjectForObjectTest(ypProjectsUpdater, projectPath, DEFAULT_PROJECT, DEFAULT_ROLE);
        addRoleSubjectForObjectTest(ypProjectsUpdater, projectPath, DEFAULT_PROJECT, DEVELOPER_ROLE, COMMON_BOX);
        addRoleSubjectForObjectTest(ypProjectsUpdater, projectPath, DEFAULT_PROJECT, SYSTEM_DEVELOPER_ROLE, SYSTEM_BOX);
    }

    @Test
    void addRoleSubjectForStageTest() {
        addRoleSubjectForObjectTest(ypStagesUpdater, stagePath, DEFAULT_STAGE, DEFAULT_ROLE);
        addRoleSubjectForObjectTest(ypStagesUpdater, stagePath, DEFAULT_STAGE, DEVELOPER_ROLE, COMMON_BOX);
        addRoleSubjectForObjectTest(ypStagesUpdater, stagePath, DEFAULT_STAGE, SYSTEM_DEVELOPER_ROLE, SYSTEM_BOX);
    }

    @Test
    void addRoleSubjectForBoxTest() {
        addRoleSubjectForObjectTest(ypStagesUpdater, boxPath, DEFAULT_STAGE, DEVELOPER_ROLE, DEFAULT_SPECIFIC_BOX);
    }

    @Test
    void addRoleSubjectWithInvalidPathTest() {
        masterYpGroupsClient.addMembersResponse = completedFuture(0);
        masterYpGroupsClient.existsResponse = completedFuture(true);
        ypStagesUpdater.addResponse = completedFuture(0);

        String invalidPath = boxPath + ROLE_NAME_DELIMITER + DEFAULT_SPECIFIC_BOX;
        RoleSubject rolePersonSubject = new RoleSubject("member", 0L, new Role(invalidPath, DEFAULT_ROLE));
        assertThrows(YpServiceException.class, () -> ypService.addRoleSubject(rolePersonSubject));
    }

    @Test
    void stopRetriesIfMissedObjectTest() {
        String projectId = "invalid_project";
        masterYpGroupsClient.existsResponse = completedFuture(true);
        masterYpGroupsClient.createdGroup.add(String.format("%s:%s.%s", DEPLOY_SYSTEM_NAME, projectId, DEFAULT_ROLE));
        masterYpGroupsClient.addMembersResponse = completedFuture(null);

        ypProjectsUpdater.addResponse = failedFuture(new MissedYpObjectException(projectId, YpObjectType.PROJECT));

        RoleSubject roleSubject = new RoleSubject("member", 0L, new Role(projectId, DEFAULT_ROLE));
        assertThrows(YpServiceException.class, () -> ypService.addRoleSubject(roleSubject));
        assertEquals(1, ypProjectsUpdater.addRoleCallsCount);
    }

    void addRoleSubjectForObjectTest(DummyYpObjectPermissionsUpdater updater, String subjectPath, String objectId,
                                     String role) {
        addRoleSubjectForObjectTest(updater, subjectPath, objectId, role, "");
    }

    void addRoleSubjectForObjectTest(DummyYpObjectPermissionsUpdater updater, String subjectPath, String objectId,
                                     String role, String boxId) {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).addMembersResponse = completedFuture(0);
            ((DummyYpGroupsClient) ypGroupsClient).existsResponse = completedFuture(true);
        });
        updater.addResponse = completedFuture(0);

        RoleSubject rolePersonSubject = new RoleSubject("member", 0L, new Role(subjectPath, role));

        String groupToCreate = DEPLOY_GROUP_PREFIX + rolePersonSubject.getRole().toString();
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).createdGroup.add(groupToCreate);
        });

        assertDoesNotThrow(() -> ypService.addRoleSubject(rolePersonSubject));
        checkAddedRoleSubjectForObject(updater, objectId, rolePersonSubject, boxId);

        RoleSubject roleGroupSubject = new RoleSubject("", 1L, new Role(subjectPath, role));
        assertDoesNotThrow(() -> ypService.addRoleSubject(roleGroupSubject));
        checkAddedRoleSubjectForObject(updater, objectId, roleGroupSubject, boxId);
    }

    @Test
    void removeRoleSubjectForProjectTest() {
        removeRoleSubjectForObjectTest(projectPath);
    }

    @Test
    void removeRoleSubjectForStageTest() {
        removeRoleSubjectForObjectTest(stagePath);
    }

    @Test
    void removeRoleSubjectForBoxTest() {
        removeRoleSubjectForObjectTest(boxPath);
    }

    @Test
    void removeRoleSubjectWithInvalidPathTest() {
        masterYpGroupsClient.removeMembersResponse = completedFuture(0);

        String invalidPath = boxPath + ROLE_NAME_DELIMITER + DEFAULT_SPECIFIC_BOX;
        RoleSubject rolePersonSubject = new RoleSubject("member", 0L, new Role(invalidPath, DEFAULT_ROLE));
        assertDoesNotThrow(() -> ypService.removeRoleSubject(rolePersonSubject));
    }

    void removeRoleSubjectForObjectTest(String subjectPath) {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).removeMembersResponse = completedFuture(0);
        });

        RoleSubject rolePersonSubject = new RoleSubject("member", 0L, new Role(subjectPath, DEFAULT_ROLE));

        String groupToUpdate = DEPLOY_GROUP_PREFIX + rolePersonSubject.getRole().toString();

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).createdGroup.add(groupToUpdate);
        });

        assertDoesNotThrow(() -> ypService.removeRoleSubject(rolePersonSubject));
        waitForAsync();

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastUpdatedGroup, equalTo(groupToUpdate));
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastMembers,
                    containsInAnyOrder(rolePersonSubject.getLogin()));
        });

        RoleSubject roleGroupSubject = new RoleSubject("", 1L, new Role(subjectPath, DEFAULT_ROLE));
        assertDoesNotThrow(() -> ypService.removeRoleSubject(roleGroupSubject));
        waitForAsync();

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastUpdatedGroup, equalTo(groupToUpdate));
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastMembers,
                    containsInAnyOrder(IDM_GROUP_PREFIX + roleGroupSubject.getGroupId()));
        });
    }

    @Test
    void updateRoleSubjectMissingACLTest() {
        ypProjectsUpdater.updateResponse = completedFuture(0);

        RoleSubject rolePersonSubject = new RoleSubject(
                "member", 0L, new Role(DEFAULT_PROJECT, DEFAULT_ROLE)
        );
        assertDoesNotThrow(() -> ypService.updateRoleSubject(rolePersonSubject));
        assertEquals(ypProjectsUpdater.lastAddedAces, emptyList());
    }

    @Test
    void updateRoleSubjectSameACLTest() {
        addRoleSubjectForObjectTest(ypProjectsUpdater, projectPath, DEFAULT_PROJECT, DEFAULT_ROLE);
        assertThat(ypProjectsUpdater.lastAddedAces,
                containsInAnyOrder(rolesInfo.getRoleAces(DEFAULT_ROLE).toArray())
        );

        ypProjectsUpdater.updateResponse = completedFuture(0);

        RoleSubject rolePersonSubject = new RoleSubject(
                "member", 0L, new Role(DEFAULT_PROJECT, DEFAULT_ROLE)
        );
        assertDoesNotThrow(() -> ypService.updateRoleSubject(rolePersonSubject));
        assertThat(ypProjectsUpdater.lastAddedAces,
                containsInAnyOrder(rolesInfo.getRoleAces(DEFAULT_ROLE).toArray())
        );
    }

    @Test
    void updateRoleSubjectNewACLTest() {
        addRoleSubjectForObjectTest(ypProjectsUpdater, projectPath, DEFAULT_PROJECT, DEFAULT_ROLE);
        assertThat(ypProjectsUpdater.lastAddedAces,
                containsInAnyOrder(rolesInfo.getRoleAces(DEFAULT_ROLE).toArray())
        );

        Config config = ConfigFactory.load("roles_to_permissions.conf");
        Config rootConfig = config.getConfig(ROOT_CONFIG_PATH);

        // overwrite rolesInfo
        Set<AccessControl.EAccessControlPermission> newPermissions = ImmutableSet.of(ACA_CREATE, ACA_USE);
        List<String> newAttributePaths = ImmutableList.of("/some/new/path");
        List<RolesInfo.RoleAce> roleAces = ImmutableList.of(
                new RolesInfo.RoleAce(newPermissions, newAttributePaths)
        );
        RolesInfo newRolesInfo = new RolesInfo.Builder().addRole(
                "OWNER",
                new IdmLeaf(new IdmName("t", "t"), new IdmName("t2", "t2")),
                roleAces
        ).build();

        ypService = new YpServiceImpl(ypObjectTreeGetter, ypStagesUpdater, ypProjectsUpdater, ypNannyServiceUpdater,
                masterYpClients, slaveYpClients, newRolesInfo, rootConfig,
                Duration.ofMillis(10), maxRetriesCount, new MetricRegistry(),
                new Metrics(new MetricRegistry(), 1), "deploy-testing");

        ypProjectsUpdater.updateResponse = completedFuture(0);

        RoleSubject rolePersonSubject = new RoleSubject(
                "member", 0L, new Role(DEFAULT_PROJECT, DEFAULT_ROLE)
        );
        assertDoesNotThrow(() -> ypService.updateRoleSubject(rolePersonSubject));
        assertThat(ypProjectsUpdater.lastAddedAces,
                containsInAnyOrder(newRolesInfo.getRoleAces(DEFAULT_ROLE).toArray())
        );
    }

    @Test
    void addMembersToIdmGroupWhenObjectExistTest() {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(
                ypGroupsClient -> {
                    ((DummyYpGroupsClient) ypGroupsClient).existsResponse = completedFuture(true);
                    ((DummyYpGroupsClient) ypGroupsClient).addMembersResponse = completedFuture(0);
                    ((DummyYpGroupsClient) ypGroupsClient).createdGroup.add(GROUP_ID);
                });

        assertDoesNotThrow(() -> ypService.addMembersToGroup(GROUP_ID, Set.of("member2")));
        waitForAsync();

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(
                ypGroupsClient -> {
                    assertThat(((DummyYpGroupsClient) ypGroupsClient).lastCheckedGroup, equalTo(GROUP_ID));
                    assertThat(((DummyYpGroupsClient) ypGroupsClient).lastUpdatedGroup, equalTo(GROUP_ID));
                    assertThat(((DummyYpGroupsClient) ypGroupsClient).lastMembers, containsInAnyOrder("member2"));
                    assertThat(((DummyYpGroupsClient) ypGroupsClient).lastLabels, anEmptyMap());
                });
    }

    @Test
    void addMembersToIdmGroupWhenObjectNotExistTest() {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(
                ypGroupsClient -> {
                    ((DummyYpGroupsClient) ypGroupsClient).existsResponse = completedFuture(false);
                    ((DummyYpGroupsClient) ypGroupsClient).addGroupResponse = completedFuture(0);
                    ((DummyYpGroupsClient) ypGroupsClient).addMembersResponse = completedFuture(0);
                });

        assertDoesNotThrow(() -> ypService.addMembersToGroup(GROUP_ID, Set.of("member1")));

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(
                ypGroupsClient -> {
                    assertThat(masterYpGroupsClient.lastCheckedGroup, equalTo(GROUP_ID));
                    assertThat(masterYpGroupsClient.lastAddedGroup, equalTo(GROUP_ID));
                    assertThat(masterYpGroupsClient.lastMembers, containsInAnyOrder("member1"));
                    assertThat(masterYpGroupsClient.lastLabels, hasEntry("system", "idm"));
                });

        String deployRootGroup = "deploy:SUPER_USER";
        assertDoesNotThrow(() -> ypService.addMembersToGroup(deployRootGroup, Set.of("member2")));

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(
                ypGroupsClient -> {
                    assertThat(masterYpGroupsClient.lastCheckedGroup, equalTo(deployRootGroup));
                    assertThat(masterYpGroupsClient.lastAddedGroup, equalTo(deployRootGroup));
                    assertThat(masterYpGroupsClient.lastMembers, containsInAnyOrder("member2"));
                    assertThat(masterYpGroupsClient.lastLabels, hasEntry("system", "deploy"));
                    assertThat(masterYpGroupsClient.lastLabels, not(hasKey("project")));
                });


        String deployProjectGroup = "deploy:my-project.MAINTAINER";
        assertDoesNotThrow(() -> ypService.addMembersToGroup(deployProjectGroup, Set.of("member2")));

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(
                ypGroupsClient -> {
                    assertThat(masterYpGroupsClient.lastCheckedGroup, equalTo(deployProjectGroup));
                    assertThat(masterYpGroupsClient.lastAddedGroup, equalTo(deployProjectGroup));
                    assertThat(masterYpGroupsClient.lastMembers, containsInAnyOrder("member2"));
                    assertThat(masterYpGroupsClient.lastLabels, hasEntry("system", "deploy"));
                });


        String deployStageGroup = "deploy:my-project.my-stage.MAINTAINER";
        assertDoesNotThrow(() -> ypService.addMembersToGroup(deployStageGroup, Set.of("member2")));

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(
                ypGroupsClient -> {
                    assertThat(masterYpGroupsClient.lastCheckedGroup, equalTo(deployStageGroup));
                    assertThat(masterYpGroupsClient.lastAddedGroup, equalTo(deployStageGroup));
                    assertThat(masterYpGroupsClient.lastMembers, containsInAnyOrder("member2"));
                    assertThat(masterYpGroupsClient.lastLabels, hasEntry("system", "deploy"));
                });
    }

    @Test
    void addMembersToIdmGroupErrorTest() {
        masterYpGroupsClient.existsResponse = failedFuture(new RuntimeException());
        assertThrows(YpServiceException.class, () -> ypService.addMembersToGroup(GROUP_ID, Set.of("member1")));

        masterYpGroupsClient.existsResponse = completedFuture(false);
        masterYpGroupsClient.addGroupResponse = failedFuture(new RuntimeException());
        assertThrows(YpServiceException.class, () -> ypService.addMembersToGroup(GROUP_ID, Set.of("member1")));

        masterYpGroupsClient.existsResponse = completedFuture(true);
        masterYpGroupsClient.addMembersResponse = failedFuture(new RuntimeException());
        assertThrows(YpServiceException.class, () -> ypService.addMembersToGroup(GROUP_ID, Set.of("member1")));
    }

    @Test
    void addMembersToIdmGroupRetriesTest() {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).existsResponse = completedFuture(false);
            ((DummyYpGroupsClient) ypGroupsClient).addGroupResponse = completedFuture(0);
            ((DummyYpGroupsClient) ypGroupsClient).errorResponse = failedFuture(new RuntimeException());
            ((DummyYpGroupsClient) ypGroupsClient).errorResponseCount = maxRetriesCount;
        });

        assertThrows(YpServiceException.class, () -> ypService.addMembersToGroup(GROUP_ID, Set.of("member1")));

        slavesYpGroupsClient.values().forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).errorResponseCount = maxRetriesCount - 1;
            ((DummyYpGroupsClient) ypGroupsClient).currentResponseCounter = 0;
        });

        // Slaves will be updated in backgroud, so we return true if master has been updated successfully
        assertDoesNotThrow(() -> ypService.addMembersToGroup(GROUP_ID, Set.of("member1")));

        masterYpGroupsClient.errorResponseCount = maxRetriesCount - 1;
        masterYpGroupsClient.currentResponseCounter = 0;

        assertDoesNotThrow(() -> ypService.addMembersToGroup(GROUP_ID, Set.of("member1")));
    }

    @Test
    void removeMembersFromIdmGroupWhenObjectExistTest() {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).removeMembersResponse = completedFuture(0);
            ((DummyYpGroupsClient) ypGroupsClient).existsResponse = completedFuture(true);
            ((DummyYpGroupsClient) ypGroupsClient).createdGroup.add(GROUP_ID);
        });

        assertDoesNotThrow(() -> ypService.removeMembersFromGroup(GROUP_ID, Set.of("member1")));
        waitForAsync();

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastUpdatedGroup, equalTo(GROUP_ID));
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastMembers, containsInAnyOrder("member1"));
        });
    }

    @Test
    void removeMembersFromIdmGroupWhenObjectDoesNotExistTest() {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).removeMembersResponse = completedFuture(0);
            ((DummyYpGroupsClient) ypGroupsClient).existsResponse = completedFuture(false);
            ((DummyYpGroupsClient) ypGroupsClient).createdGroup.add(GROUP_ID);
        });

        assertDoesNotThrow(() -> ypService.removeMembersFromGroup(GROUP_ID, Set.of("member1")));

        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastUpdatedGroup, nullValue());
            assertThat(((DummyYpGroupsClient) ypGroupsClient).lastMembers, nullValue());
        });
    }

    @Test
    void removeMembersToIdmGroupErrorTest() {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).existsResponse = completedFuture(true);
            ((DummyYpGroupsClient) ypGroupsClient).removeMembersResponse = failedFuture(new RuntimeException());
            ((DummyYpGroupsClient) ypGroupsClient).createdGroup.add(GROUP_ID);
        });

        assertThrows(YpServiceException.class, () -> ypService.removeMembersFromGroup(GROUP_ID, Set.of("member1")));

        masterYpGroupsClient.removeMembersResponse = completedFuture(0);

        assertDoesNotThrow(() -> ypService.removeMembersFromGroup(GROUP_ID, Set.of("member1")));
    }

    @Test
    void removeMembersFromIdmGroupRetriesTest() {
        concat(slavesYpGroupsClient.values().stream(), Stream.of(masterYpGroupsClient)).forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).existsResponse = completedFuture(true);
            ((DummyYpGroupsClient) ypGroupsClient).removeMembersResponse = completedFuture(0);
            ((DummyYpGroupsClient) ypGroupsClient).errorResponse = failedFuture(new RuntimeException());
            ((DummyYpGroupsClient) ypGroupsClient).errorResponseCount = maxRetriesCount;
            ((DummyYpGroupsClient) ypGroupsClient).createdGroup.add(GROUP_ID);
        });

        assertThrows(YpServiceException.class, () -> ypService.removeMembersFromGroup(GROUP_ID, Set.of("member1")));

        slavesYpGroupsClient.values().forEach(ypGroupsClient -> {
            ((DummyYpGroupsClient) ypGroupsClient).errorResponseCount = maxRetriesCount - 1;
            ((DummyYpGroupsClient) ypGroupsClient).currentResponseCounter = 0;
        });
        masterYpGroupsClient.currentResponseCounter = 0;

        assertThrows(YpServiceException.class, () -> ypService.removeMembersFromGroup(GROUP_ID, Set.of("member1")));

        masterYpGroupsClient.errorResponseCount = maxRetriesCount - 1;
        masterYpGroupsClient.currentResponseCounter = 0;

        assertDoesNotThrow(() -> ypService.removeMembersFromGroup(GROUP_ID, Set.of("member1")));
    }

    @ParameterizedTest
    @CsvSource({
            "3db48af3-1da77fb3-a4a2d58c-5fee03ee,MAINTAINER.3db48af3-1da77fb3-a4a2d58c-5fee03ee,1",//new scheme with uniqueId
            "'','',1",//old scheme without uuid in yp group names
            "anotherId,MAINTAINER.3db48af3-1da77fb3-a4a2d58c-5fee03ee,0",
            "3db48af3-1da77fb3-a4a2d58c-5fee03ee,MAINTAINER.anotherId,0",
            "3db48af3-1da77fb3-a4a2d58c-5fee03ee,'',0",
            "'',MAINTAINER.3db48af3-1da77fb3-a4a2d58c-5fee03ee,0",
    })
    void removeRolesFromStageAclTest(String stageUuid, String uniqueIdInRole, int expectedCallsCount) {
        StageMeta stageMeta = new StageMeta("stage1", new Acl(List.of()), "", stageUuid, 0, "project2");

        YpObject<StageMeta, TStageSpec, TStageStatus> object =
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setMeta(stageMeta)
                        .build();

        stageRepository.getResponses.put("stage1", CompletableFuture.completedFuture(Optional.of(object)));
        ypStagesUpdater.removeRolesResponse = completedFuture(null);
        get5s(ypService.removeRolesFromStageAcl("stage1", Set.of(new Role("project.stage1", "MAINTAINER", "", uniqueIdInRole))));
        assertEquals(expectedCallsCount, ypStagesUpdater.removeRolesCallsCount);
    }

    @Test
    void removeRolesFromStageAclRetriesTest() {
        var uuid = "3db48af3-1da77fb3-a4a2d58c-5fee03ee";
        StageMeta stageMeta = new StageMeta("stage1", new Acl(List.of()), "", uuid, 0, "project2");

        YpObject<StageMeta, TStageSpec, TStageStatus> object =
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setMeta(stageMeta)
                        .build();

        stageRepository.getResponses.put("stage1", CompletableFuture.completedFuture(Optional.of(object)));
        ypStagesUpdater.removeRolesResponse = failedFuture(new RuntimeException("Failed update acls..."));

        get5s(ypService.removeRolesFromStageAcl("stage1", Set.of(new Role("project.stage1", "MAINTAINER", "", "MAINTAINER." + uuid))));

        assertEquals(maxRetriesCount, ypStagesUpdater.removeRolesCallsCount);
    }

    @Test
    void removeRolesFromStageAclUuidNotMatchedTest() {
        var uuid = "3db48af3-1da77fb3-a4a2d58c-5fee03ee";
        StageMeta stageMeta = new StageMeta("stage1", new Acl(List.of()), "", "wrong_uuid", 0, "project2");

        YpObject<StageMeta, TStageSpec, TStageStatus> object =
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setMeta(stageMeta)
                        .build();

        stageRepository.getResponses.put("stage1", CompletableFuture.completedFuture(Optional.of(object)));
        ypStagesUpdater.removeRolesResponse = completedFuture(null);
        get5s(ypService.removeRolesFromStageAcl("stage1", Set.of(new Role("project.stage1", "MAINTAINER", "", "MAINTAINER." + uuid))));
        assertEquals(0, ypStagesUpdater.removeRolesCallsCount);
    }

    @Test
    void removeRolesFromStageAclNoStageTest() {
        stageRepository.getResponses.put("stage1", completedFuture(Optional.empty()));
        ypStagesUpdater.removeRolesResponse = completedFuture(null);
        get5s(ypService.removeRolesFromStageAcl("stage1", Set.of(new Role("project.stage1", "MAINTAINER"))));
        assertEquals(0, ypStagesUpdater.removeRolesCallsCount);
    }

    @Test
    void syncMembersToSlaveClustersTest() {
        masterYpGroupsClient.getGroupsWithPrefixResponse = completedFuture(Map.of(
                "idm:0", Set.of("member0"),
                "idm:1", Set.of("member1", "member2"),
                "idm:2", Set.of("member3")
        ));

        slavesYpGroupsClient.values().stream().forEach(client -> {
            var dummyClient = (DummyYpGroupsClient) client;
            final Map<String, Set<String>> slaveGroups = Map.of(
                    "idm:1", Set.of("member1x", "member2"),
                    "idm:2", Set.of("member3"),
                    "idm:3", Set.of("member4"));
            dummyClient.getGroupsWithPrefixResponse = completedFuture(slaveGroups);
            dummyClient.createdGroup.addAll(slaveGroups.keySet());
            dummyClient.updateMembersResponse = completedFuture(0);
            dummyClient.addGroupResponse = completedFuture(0);
            dummyClient.getMembersResponse = slaveGroups.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), new ArrayList<>(e.getValue())))
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        });

        final Map<String, String> labels = Map.of("system", "idm");
        assertDoesNotThrow(() -> ypService.syncGroupMembersToAllSlaveClusters(labels));
        waitForAsync();

        slavesYpGroupsClient.values().forEach(
                ypGroupsClient -> {
                    assertThat(((DummyYpGroupsClient) ypGroupsClient).lastMembersUpdatesByGroupId,
                            equalTo(ImmutableMap.of("idm:0", Set.of("member0"),
                                    "idm:1", Set.of("member1", "member2"))));
                    assertThat(((DummyYpGroupsClient) ypGroupsClient).lastLabels, equalTo(labels));
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
            "project,DEVELOPER,'',project.DEVELOPER",
            "projectA.stageB,DEVELOPER,'',projectA.stageB.DEVELOPER",

            "project,DEVELOPER,DEVELOPER.1d457f2b-84fb4d5e-c7c2624-9d12e8b9,project.DEVELOPER:1d457f2b-84fb4d5e-c7c2624-9d12e8b9",
            "projectA.stageB,DEVELOPER,DEVELOPER.1d457f2b-84fb4d5e-c7c2624-9d12e8b9,projectA.stageB.DEVELOPER:1d457f2b-84fb4d5e-c7c2624-9d12e8b9",

            "projectA."+Role.NANNY_ROLES_PARENT_NODE+".Service1,owners,owners.1d457f2b-84fb4d5e-c7c2624-9d12e8b9,Service1.owners:1d457f2b-84fb4d5e-c7c2624-9d12e8b9"
    })
    void getYpGroupNameTest(String rolePath, String leaf, String uniqueId, String ypGroupName) {
        Role role = new Role(rolePath, leaf, "", uniqueId);
        assertEquals(DEPLOY_GROUP_PREFIX + ypGroupName, YpServiceReadOnlyImpl.getYpGroupName(role, DEPLOY_SYSTEM_NAME));
    }

    @Test
    void removeExistingGroupOnBothClustersTest() {
        masterYpGroupsClient.createdGroup = Set.of("somegroup", "group1");
        masterYpGroupsClient.removeGroupResponse = CompletableFuture.completedFuture(null);

        slaveYpGroupsClientSAS.createdGroup = Set.of("group2", "group1");
        slaveYpGroupsClientSAS.removeGroupResponse = CompletableFuture.completedFuture(null);

        ypService.removeGroup("group1");

        assertEquals("group1", masterYpGroupsClient.lastRemovedGroup);
        assertEquals("group1", slaveYpGroupsClientSAS.lastRemovedGroup);
    }

    @Test
    void removeGroupShouldContinueAfterFailureTest() {
        masterYpGroupsClient.createdGroup = Set.of("group1");
        masterYpGroupsClient.removeGroupResponse = CompletableFuture.failedFuture(new RuntimeException("error"));
        slaveYpGroupsClientSAS.createdGroup = Set.of("group1");
        slaveYpGroupsClientSAS.removeGroupResponse = CompletableFuture.completedFuture(null);

        ypService.removeGroup("group1");

        assertEquals("group1", masterYpGroupsClient.lastRemovedGroup);
        assertEquals("group1", slaveYpGroupsClientSAS.lastRemovedGroup);
    }

    void waitForAsync() {
        // Give some time for async queue to execute slave group update
        try {
            int maxIterations = 100;
            while((int)metricsRegistry.getGauges().get(YpGroupsAsyncQueue.TOTAL_YP_ASYNC_QUEUE).getValue() != 0) {
                if (maxIterations-- == 0) {
                    throw new RuntimeException("Failed to wait for child task completion");
                }
                sleep(10);
            }
            sleep(10);
        } catch (InterruptedException ignored) {
        }
    }
}
