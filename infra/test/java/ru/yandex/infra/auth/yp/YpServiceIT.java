package ru.yandex.infra.auth.yp;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.Metrics;
import ru.yandex.infra.auth.Role;
import ru.yandex.infra.auth.RoleSubject;
import ru.yandex.infra.auth.RolesInfo;
import ru.yandex.infra.auth.utils.ConfigUtils;
import ru.yandex.infra.controller.dto.NannyServiceMeta;
import ru.yandex.infra.controller.dto.ProjectMeta;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.infra.controller.yp.LabelBasedRepository;
import ru.yandex.infra.controller.yp.ObjectBuilderDescriptor;
import ru.yandex.infra.controller.yp.Selector;
import ru.yandex.infra.controller.yp.YpObjectSettings;
import ru.yandex.infra.controller.yp.YpTransactionClient;
import ru.yandex.infra.controller.yp.YpTransactionClientImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.yp.YpInstance;
import ru.yandex.yp.YpRawClientBuilder;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.Autogen.TProjectMeta;
import ru.yandex.yp.client.api.Autogen.TSchemaMeta;
import ru.yandex.yp.client.api.Autogen.TStageMeta;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TNannyServiceSpec;
import ru.yandex.yp.client.api.TNannyServiceStatus;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TProjectStatus;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.model.YpAccessControlAction;
import ru.yandex.yp.model.YpAccessControlPermission;
import ru.yandex.yp.model.YpCheckObjectPermissions;
import ru.yandex.yp.model.YpCheckedObjectPermissions;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpTypedId;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.typesafe.config.impl.ConfigImpl.emptyConfig;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.auth.Role.ROLE_NAME_DELIMITER;
import static ru.yandex.infra.auth.yp.YpGroupsHelper.IDM_GROUP_PREFIX;
import static ru.yandex.infra.auth.yp.YpGroupsHelper.SYSTEM_IDM_LABEL_VALUE;
import static ru.yandex.infra.auth.yp.YpGroupsHelper.SYSTEM_LABEL_KEY;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.createAccount;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.createApprovalPolicy;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.createGroup;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.createProject;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.createStage;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.createUser;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.generateACE;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.getMeta;
import static ru.yandex.infra.auth.yp.YpServiceTestUtils.isExist;
import static ru.yandex.infra.auth.yp.YpUtils.generateACE;
import static ru.yandex.infra.controller.testutil.FutureUtils.get5s;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.cleanup;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.getLabelsYson;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.getSpec;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACA_ROOT_SSH_ACCESS;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACA_SSH_ACCESS;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACA_WRITE;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACP_READ;
import static ru.yandex.yp.model.YpObjectType.ACCOUNT;
import static ru.yandex.yp.model.YpObjectType.APPROVAL_POLICY;
import static ru.yandex.yp.model.YpObjectType.GROUP;
import static ru.yandex.yp.model.YpObjectType.PROJECT;
import static ru.yandex.yp.model.YpObjectType.STAGE;
import static ru.yandex.yp.model.YpObjectType.USER;

public class YpServiceIT {
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String TEST_ROBOT_NAME = "test-robot";
    private static final String DEPLOY_GROUP_ACCOUNT = "test-robot";
    private static final ObjectBuilderDescriptor<TSchemaMeta, SchemaMeta> GROUP_DESCRIPTOR =
            new ObjectBuilderDescriptor<>(DataModel.TGroupSpec::newBuilder, DataModel.TGroupStatus::newBuilder,
                    SchemaMeta::fromProto, TSchemaMeta.getDefaultInstance());
    private static final AccessControl.TAccessControlEntry ROOT_ACE = generateACE("root", List.of(ACP_READ, ACA_WRITE));
    private static final String DEVELOPER_ROLE = "DEVELOPER";
    private static final String SYSTEM_DEVELOPER_ROLE = "SYSTEM_DEVELOPER";
    private static final String DEFAULT_PROJECT = "test-project";
    private static final String DEFAULT_STAGE = "test-stage";
    private static final String DEFAULT_STAGE_FULL_PATH = unionWithRoleDelimiter(DEFAULT_PROJECT, DEFAULT_STAGE);
    private static final String SYSTEM_DEPLOY_LABEL_VALUE = "deploy";
    private static final String DEPLOY_GROUP_PREFIX = SYSTEM_DEPLOY_LABEL_VALUE + ":";
    private static final String PROJECT_GROUP = DEPLOY_GROUP_PREFIX + DEFAULT_PROJECT;
    private static final String STAGE_GROUP = DEPLOY_GROUP_PREFIX + DEFAULT_STAGE_FULL_PATH;
    private static final String PROJECT_DEVELOPER_ROLE_GROUP = unionWithRoleDelimiter(PROJECT_GROUP, DEVELOPER_ROLE);
    private static final String PROJECT_SYSTEM_DEVELOPER_ROLE_GROUP =
            unionWithRoleDelimiter(PROJECT_GROUP, SYSTEM_DEVELOPER_ROLE);
    private static final String DEFAULT_MEMBER = "test-member";
    private static final String EMPTY_MEMBER = "";
    private static final String DEFAULT_IDM_GROUP = "idm:123";
    private static final String EPHEMERAL_PATH_BOXES_PREFIX = "/access/deploy/box/";
    private static final String EPHEMERAL_DEFAULT_BOX_PATH = EPHEMERAL_PATH_BOXES_PREFIX + "default";
    private static final String EPHEMERAL_SYSTEM_BOX_PATH = EPHEMERAL_PATH_BOXES_PREFIX + "system";
    private static final String EPHEMERAL_APPROVERS_PATH = "/access/deploy/approvers";
    private static final String EPHEMERAL_MANDATORY_APPROVERS_PATH = "/access/deploy/mandatory_approvers";
    private static final String USE_UUID_SINCE = "2100-01-01 00:00:00";

    private static final Long DEFAULT_GROUP_ID = 123L;
    private static final Long EMPTY_GROUP_ID = 0L;
    private static final Map<String, Object> DEPLOY_GROUP_LABELS = Map.of(
            "system", "deploy",
            "project", DEFAULT_PROJECT
    );
    private static final Map<String, Object> IDM_GROUP_LABELS = Map.of("system", "idm");
    private static final Map<String, String> DEPLOY_GROUP_ALL_PROJECTS_LABELS = Map.of(
            "system", "deploy"
    );

    private YpRawObjectService masterYpClient;
    private YpTransactionClient ypTransactionClient;
    private YpRawObjectService slaveYpClient;
    private LabelBasedRepository<StageMeta, TStageSpec, TStageStatus> stageRepository;
    private LabelBasedRepository<NannyServiceMeta, TNannyServiceSpec, TNannyServiceStatus> nannyServiceRepository;
    private LabelBasedRepository<ProjectMeta, TProjectSpec, TProjectStatus> projectRepository;
    private LabelBasedRepository<SchemaMeta, DataModel.TGroupSpec, DataModel.TGroupStatus> groupRepository;
    private LabelBasedRepository<SchemaMeta, DataModel.TGroupSpec, DataModel.TGroupStatus> slaveGroupRepository;
    private YpService ypService;
    private int retryCount;
    private Duration retryRate;
    GaugeRegistry gaugeRegistry = GaugeRegistry.EMPTY;

    private static String unionWithRoleDelimiter(String first, String second) {
        StringJoiner joiner = new StringJoiner(ROLE_NAME_DELIMITER);
        joiner.add(first).add(second);
        return joiner.toString();
    }


    @BeforeEach
    void before() {
        HostAndPort masterHostPort = HostAndPort.fromString(System.getenv("YP_MASTER_GRPC_INSECURE_ADDR_1"));
        YpInstance masterInstance = new YpInstance(masterHostPort.getHost(), masterHostPort.getPort());
        masterYpClient = new YpRawClientBuilder(masterInstance, () -> "fake_token")
                .setUseMasterDiscovery(false)
                .setUsePlaintext(true)
                .build()
                .objectService();

        stageRepository = ConfigUtils.ypStageRepository(masterYpClient, DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE,
                emptyConfig("{}"), gaugeRegistry);
        nannyServiceRepository =
                ConfigUtils.ypNannyServiceRepository(masterYpClient, DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE, gaugeRegistry);
        projectRepository =
                ConfigUtils.ypProjectRepository(masterYpClient, DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE, gaugeRegistry);
        groupRepository = ConfigUtils.ypGroupRepository(masterYpClient, DEFAULT_PAGE_SIZE, gaugeRegistry);
        ypTransactionClient = new YpTransactionClientImpl(masterYpClient);

        final YpGroupsClientImpl masterGroupsClient = new YpGroupsClientImpl(groupRepository, TEST_ROBOT_NAME, DEFAULT_PAGE_SIZE);

        Config rolesConfig = ConfigFactory.load("roles_to_permissions.conf");
        RolesInfo rolesInfo = RolesInfo.fromConfig(rolesConfig);

        prepareYpObjects(masterYpClient);

        retryCount = 5;
        retryRate = Duration.ofMillis(50);

        var ypCacheSettings = Map.of(YpObjectSettings.TYPE_USED_AS_KEY_WITH_DEFAULT_SETTINGS,
                new YpObjectSettings.Builder().setWatches(false).build());

        ypService = new YpServiceImpl(
                new YpObjectsTreeGetterImpl(stageRepository, nannyServiceRepository, projectRepository, groupRepository,
                        new Metrics(new MetricRegistry(), 1),
                        USE_UUID_SINCE, ypCacheSettings, gaugeRegistry, false),
                new YpObjectPermissionsUpdaterImpl<>(ypTransactionClient, stageRepository),
                new YpObjectPermissionsUpdaterImpl<>(ypTransactionClient, projectRepository),
                new YpObjectPermissionsUpdaterImpl<>(ypTransactionClient, nannyServiceRepository),
                new YpClients(ypTransactionClient, masterGroupsClient),
                emptyMap(),
                rolesInfo,
                rolesConfig.getConfig("roles_tree.root"),
                retryRate,
                retryCount,
                new MetricRegistry(),
                new Metrics(new MetricRegistry(), 1),
                SYSTEM_DEPLOY_LABEL_VALUE);
    }

    void createYpServiceWithSlaveClient() {
        HostAndPort slaveHostPort = HostAndPort.fromString(System.getenv("YP_MASTER_GRPC_INSECURE_ADDR_2"));

        YpInstance slaveInstance = new YpInstance(slaveHostPort.getHost(), slaveHostPort.getPort());
        slaveYpClient = new YpRawClientBuilder(slaveInstance, () -> "fake_token")
                .setUseMasterDiscovery(false)
                .setUsePlaintext(true)
                .build()
                .objectService();

        slaveGroupRepository = ConfigUtils.ypGroupRepository(slaveYpClient, DEFAULT_PAGE_SIZE, gaugeRegistry);
        YpClients slaveClient = new YpClients(new YpTransactionClientImpl(slaveYpClient),
                new YpGroupsClientImpl(slaveGroupRepository, TEST_ROBOT_NAME, DEFAULT_PAGE_SIZE));

        prepareYpObjects(slaveYpClient);
        Config rolesConfig = ConfigFactory.load("roles_to_permissions.conf");
        RolesInfo rolesInfo = RolesInfo.fromConfig(rolesConfig);
        var ypCacheSettings = Map.of(YpObjectSettings.TYPE_USED_AS_KEY_WITH_DEFAULT_SETTINGS,
                new YpObjectSettings.Builder().setWatches(false).build());

        ypService = new YpServiceImpl(
                new YpObjectsTreeGetterImpl(stageRepository, nannyServiceRepository, projectRepository, groupRepository,
                        new Metrics(new MetricRegistry(), 1),
                        USE_UUID_SINCE, ypCacheSettings, gaugeRegistry, false),
                new YpObjectPermissionsUpdaterImpl<>(ypTransactionClient, stageRepository),
                new YpObjectPermissionsUpdaterImpl<>(ypTransactionClient, projectRepository),
                new YpObjectPermissionsUpdaterImpl<>(ypTransactionClient, nannyServiceRepository),
                new YpClients(ypTransactionClient, new YpGroupsClientImpl(groupRepository, TEST_ROBOT_NAME, DEFAULT_PAGE_SIZE)),
                Map.of("sas", slaveClient),
                rolesInfo,
                rolesConfig.getConfig("roles_tree.root"),
                retryRate,
                retryCount,
                new MetricRegistry(),
                new Metrics(new MetricRegistry(), 1),
                SYSTEM_DEPLOY_LABEL_VALUE);

    }

    private static void prepareYpObjects(YpRawObjectService ypClient) {
        try {
            cleanup(ypClient, YpObjectType.STAGE);
            cleanup(ypClient, YpObjectType.PROJECT);
            cleanup(ypClient, YpObjectType.GROUP, Set.of("superusers"));
            cleanup(ypClient, YpObjectType.USER, Set.of("root"));
            cleanup(ypClient, ACCOUNT, Set.of("tmp"));

            createUser(ypClient, TEST_ROBOT_NAME);
            createUser(ypClient, DEFAULT_MEMBER);
            createAccount(ypClient, DEPLOY_GROUP_ACCOUNT);
            createGroup(ypClient, DEFAULT_IDM_GROUP, emptySet(), IDM_GROUP_LABELS);

            while (!isExist(ypClient, TEST_ROBOT_NAME, USER)) {
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void getRolesEmpty() throws YpObjectsTreeGetterError {
        assertThat(ypService.getRoles(), containsInAnyOrder(
                new Role("", "SUPER_USER"),
                new Role("", "")));
    }

    @Test
    void getRoles() throws YpObjectsTreeGetterError {
        List<String> specificBoxTypes = List.of("my-box");
        createProject(masterYpClient, DEFAULT_PROJECT, specificBoxTypes);
        createStage(masterYpClient, DEFAULT_STAGE, DEFAULT_PROJECT);

        String aloneProject = "alone-project";
        createProject(masterYpClient, aloneProject, specificBoxTypes);

        String boxFullPath = unionWithRoleDelimiter(DEFAULT_STAGE_FULL_PATH, specificBoxTypes.get(0));

        assertThat(ypService.getRoles(), containsInAnyOrder(
                new Role("", "SUPER_USER"),
                new Role("", ""),
                new Role(aloneProject, ""),
                new Role(aloneProject, "OWNER"),
                new Role(aloneProject, "MAINTAINER"),
                new Role(aloneProject, "DEVELOPER"),
                new Role(aloneProject, "SYSTEM_DEVELOPER"),
                new Role(aloneProject, "ROOT_DEVELOPER"),
                new Role(aloneProject, "APPROVER"),
                new Role(aloneProject, "MANDATORY_APPROVER"),
                new Role(aloneProject, "DEPLOYER"),
                new Role(aloneProject, "RESPONSIBLE"),
                new Role(aloneProject, "VIEWER"),
                new Role(DEFAULT_PROJECT, ""),
                new Role(DEFAULT_PROJECT, "OWNER"),
                new Role(DEFAULT_PROJECT, "MAINTAINER"),
                new Role(DEFAULT_PROJECT, "DEVELOPER"),
                new Role(DEFAULT_PROJECT, "SYSTEM_DEVELOPER"),
                new Role(DEFAULT_PROJECT, "ROOT_DEVELOPER"),
                new Role(DEFAULT_PROJECT, "APPROVER"),
                new Role(DEFAULT_PROJECT, "MANDATORY_APPROVER"),
                new Role(DEFAULT_PROJECT, "DEPLOYER"),
                new Role(DEFAULT_PROJECT, "RESPONSIBLE"),
                new Role(DEFAULT_PROJECT, "VIEWER"),
                new Role(DEFAULT_STAGE_FULL_PATH, ""),
                new Role(DEFAULT_STAGE_FULL_PATH, "MAINTAINER"),
                new Role(DEFAULT_STAGE_FULL_PATH, "DEVELOPER"),
                new Role(DEFAULT_STAGE_FULL_PATH, "SYSTEM_DEVELOPER"),
                new Role(DEFAULT_STAGE_FULL_PATH, "ROOT_DEVELOPER"),
                new Role(DEFAULT_STAGE_FULL_PATH, "APPROVER"),
                new Role(DEFAULT_STAGE_FULL_PATH, "MANDATORY_APPROVER"),
                new Role(DEFAULT_STAGE_FULL_PATH, "DEPLOYER"),
                new Role(DEFAULT_STAGE_FULL_PATH, "RESPONSIBLE"),
                new Role(DEFAULT_STAGE_FULL_PATH, "VIEWER"),
                new Role(boxFullPath, ""),
                new Role(boxFullPath, "DEVELOPER"),
                new Role(boxFullPath, "ROOT_DEVELOPER")));
    }

    @Test
    void getRoleSubjectsEmpty() {
        assertThat(ypService.getRoleSubjects(), empty());
    }

    @Test
    void getRoleSubjects() {
        createProject(masterYpClient, DEFAULT_PROJECT);
        createStage(masterYpClient, DEFAULT_STAGE, DEFAULT_PROJECT);
        createGroup(groupRepository, unionWithRoleDelimiter(PROJECT_GROUP, "OWNER"), Set.of(DEFAULT_MEMBER,
                DEFAULT_IDM_GROUP),
                DEPLOY_GROUP_LABELS);
        createGroup(groupRepository, unionWithRoleDelimiter(STAGE_GROUP, "MAINTAINER"), Set.of(DEFAULT_MEMBER),
                DEPLOY_GROUP_LABELS);

        assertThat(ypService.getRoleSubjects(), containsInAnyOrder(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID, new Role(DEFAULT_PROJECT, "OWNER")),
                new RoleSubject(EMPTY_MEMBER, DEFAULT_GROUP_ID, new Role(DEFAULT_PROJECT, "OWNER")),
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID, new Role(DEFAULT_STAGE_FULL_PATH, "MAINTAINER"))));
    }

    @Test
    void getGroupWithPrefixEmpty() {
        assertThat(ypService.getGroupsWithPrefix(IDM_GROUP_PREFIX + "some-pref"), anEmptyMap());
    }

    @Test
    void getGroupWithEmptyPrefix() {
        assertThat(ypService.getGroupsWithPrefix(""), hasEntry("superusers", Set.of("root")));
    }

    @Test
    void getGroupWithPrefix() {
        createUser(masterYpClient, "test-member-2");
        createUser(masterYpClient, "test-member-3");
        String newGroupId = DEFAULT_IDM_GROUP + "11";

        createGroup(groupRepository, newGroupId, Set.of("test-member-2", "test-member-3"), IDM_GROUP_LABELS);

        createGroup(groupRepository, unionWithRoleDelimiter(PROJECT_GROUP, "OWNER"), Set.of(DEFAULT_MEMBER,
                newGroupId),
                DEPLOY_GROUP_LABELS);
        createGroup(groupRepository, unionWithRoleDelimiter(STAGE_GROUP, "MAINTAINER"), Set.of(DEFAULT_MEMBER),
                DEPLOY_GROUP_LABELS);

        Map<String, Set<String>> deployGroups = ypService.getGroupsWithPrefix(DEPLOY_GROUP_PREFIX);
        assertThat(deployGroups, hasEntry(unionWithRoleDelimiter(PROJECT_GROUP, "OWNER"), Set.of(DEFAULT_MEMBER,
                newGroupId)));
        assertThat(deployGroups, hasEntry(unionWithRoleDelimiter(STAGE_GROUP, "MAINTAINER"), Set.of(DEFAULT_MEMBER)));

        Map<String, Set<String>> groups = ypService.getGroupsWithPrefix(IDM_GROUP_PREFIX);
        assertThat(groups.entrySet(), hasSize(2));
        assertThat(groups, hasEntry(newGroupId, Set.of("test-member-2", "test-member-3")));
        assertThat(groups, hasEntry(DEFAULT_IDM_GROUP, emptySet()));
    }

    @Test
    void getGroupWithLabels() {
        createGroup(groupRepository, unionWithRoleDelimiter(PROJECT_GROUP, "OWNER"), Set.of(DEFAULT_MEMBER,
                DEFAULT_IDM_GROUP),
                DEPLOY_GROUP_LABELS);
        createGroup(groupRepository, unionWithRoleDelimiter(STAGE_GROUP, "MAINTAINER"), Set.of(DEFAULT_MEMBER),
                DEPLOY_GROUP_LABELS);
        createGroup(groupRepository, unionWithRoleDelimiter(PROJECT_GROUP, "DEVELOPER"), Set.of(DEFAULT_MEMBER),
                ImmutableMap.of("system", "deploy"));

        Map<String, Set<String>> deployGroups = ypService.getGroupsWithLabels(DEPLOY_GROUP_ALL_PROJECTS_LABELS);
        assertThat(deployGroups.entrySet(), hasSize(3));
        assertThat(deployGroups, hasEntry(unionWithRoleDelimiter(PROJECT_GROUP, "OWNER"), Set.of(DEFAULT_MEMBER,
                DEFAULT_IDM_GROUP)));
        assertThat(deployGroups, hasEntry(unionWithRoleDelimiter(STAGE_GROUP, "MAINTAINER"), Set.of(DEFAULT_MEMBER)));
        assertThat(deployGroups, hasEntry(unionWithRoleDelimiter(PROJECT_GROUP, "DEVELOPER"), Set.of(DEFAULT_MEMBER)));

        Map<String, Set<String>> projectGroups = ypService.getGroupsWithLabels(ImmutableMap.of(
                "system", "deploy", "project", DEFAULT_PROJECT));
        assertThat(projectGroups.entrySet(), hasSize(2));
        assertThat(projectGroups, hasEntry(unionWithRoleDelimiter(PROJECT_GROUP, "OWNER"), Set.of(DEFAULT_MEMBER,
                DEFAULT_IDM_GROUP)));
        assertThat(projectGroups, hasEntry(unionWithRoleDelimiter(STAGE_GROUP, "MAINTAINER"), Set.of(DEFAULT_MEMBER)));
    }

    @Test
    void addRoleSubject() {
        createProject(masterYpClient, DEFAULT_PROJECT);
        createStage(masterYpClient, DEFAULT_STAGE, DEFAULT_PROJECT);
        AccessControl.TAccessControlEntry projectDeveloperACE1 = generateACE(PROJECT_DEVELOPER_ROLE_GROUP,
                List.of(ACA_SSH_ACCESS),
                List.of(EPHEMERAL_DEFAULT_BOX_PATH));
        AccessControl.TAccessControlEntry projectDeveloperACE2 = generateACE(PROJECT_DEVELOPER_ROLE_GROUP,
                List.of(ACP_READ),
                emptyList());

        // TODO: refactor copy-paste
        // Check ACL for project.DEVELOPER for member
        assertDoesNotThrow(() -> ypService.addRoleSubject(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID, new Role(DEFAULT_PROJECT, DEVELOPER_ROLE))));
        assertThat(getSpec(masterYpClient, PROJECT_DEVELOPER_ROLE_GROUP, GROUP_DESCRIPTOR.createSpecBuilder(), GROUP),
                equalTo(DataModel.TGroupSpec.newBuilder()
                        .addMembers(DEFAULT_MEMBER)
                        .build()));
        assertThat(((TProjectMeta) getMeta(masterYpClient, TProjectMeta.newBuilder(), DEFAULT_PROJECT, PROJECT))
                        .getAclList(),
                containsInAnyOrder(ROOT_ACE, projectDeveloperACE1, projectDeveloperACE2));

        // Check ACL for project.DEVELOPER for group
        assertDoesNotThrow(() -> ypService.addRoleSubject(
                new RoleSubject(EMPTY_MEMBER, DEFAULT_GROUP_ID, new Role(DEFAULT_PROJECT, DEVELOPER_ROLE))));
        assertThat(getSpec(masterYpClient, PROJECT_DEVELOPER_ROLE_GROUP, GROUP_DESCRIPTOR.createSpecBuilder(), GROUP),
                equalTo(DataModel.TGroupSpec.newBuilder()
                        .addAllMembers(List.of(DEFAULT_IDM_GROUP, DEFAULT_MEMBER))
                        .build()));
        assertThat(((TProjectMeta) getMeta(masterYpClient, TProjectMeta.newBuilder(), DEFAULT_PROJECT, PROJECT))
                        .getAclList(),
                containsInAnyOrder(ROOT_ACE, projectDeveloperACE1, projectDeveloperACE2));

        // Check ACL for project.stage.DEVELOPER
        assertDoesNotThrow(() -> ypService.addRoleSubject(
                new RoleSubject(EMPTY_MEMBER, DEFAULT_GROUP_ID, new Role(DEFAULT_STAGE_FULL_PATH, DEVELOPER_ROLE))));
        assertThat(getSpec(masterYpClient, unionWithRoleDelimiter(STAGE_GROUP, DEVELOPER_ROLE),
                GROUP_DESCRIPTOR.createSpecBuilder(), GROUP),
                equalTo(DataModel.TGroupSpec.newBuilder()
                        .addMembers(DEFAULT_IDM_GROUP)
                        .build()));
        assertThat(((TStageMeta) getMeta(masterYpClient, TStageMeta.newBuilder(), DEFAULT_STAGE, STAGE)).getAclList(),
                containsInAnyOrder(
                        ROOT_ACE,
                        generateACE(unionWithRoleDelimiter(STAGE_GROUP, DEVELOPER_ROLE),
                                List.of(ACA_SSH_ACCESS),
                                List.of(EPHEMERAL_DEFAULT_BOX_PATH)),
                        generateACE(unionWithRoleDelimiter(STAGE_GROUP, DEVELOPER_ROLE),
                                List.of(ACP_READ),
                                emptyList())));

        // Check ACL for project1.SYSTEM_DEVELOPER
        assertDoesNotThrow(() -> ypService.addRoleSubject(
                new RoleSubject(EMPTY_MEMBER, DEFAULT_GROUP_ID, new Role(DEFAULT_PROJECT,
                        SYSTEM_DEVELOPER_ROLE))));
        assertThat(getSpec(masterYpClient, PROJECT_SYSTEM_DEVELOPER_ROLE_GROUP, GROUP_DESCRIPTOR.createSpecBuilder(),
                GROUP),
                equalTo(DataModel.TGroupSpec.newBuilder()
                        .addAllMembers(List.of(DEFAULT_IDM_GROUP))
                        .build()));
        assertThat(((TProjectMeta) getMeta(masterYpClient, TProjectMeta.newBuilder(), DEFAULT_PROJECT, PROJECT))
                        .getAclList(),
                containsInAnyOrder(
                        ROOT_ACE, projectDeveloperACE1, projectDeveloperACE2,
                        generateACE(PROJECT_SYSTEM_DEVELOPER_ROLE_GROUP,
                                List.of(ACA_SSH_ACCESS, ACA_ROOT_SSH_ACCESS),
                                List.of(EPHEMERAL_SYSTEM_BOX_PATH)),
                        generateACE(PROJECT_SYSTEM_DEVELOPER_ROLE_GROUP,
                                List.of(ACP_READ),
                                emptyList())));
    }

    @Test
    void addRoleSubjectWithSpecificBox() {
        createProject(masterYpClient, DEFAULT_PROJECT);
        createStage(masterYpClient, DEFAULT_STAGE, DEFAULT_PROJECT);

        String specificBoxId = "my-box";
        String specificBoxPath = unionWithRoleDelimiter(DEFAULT_STAGE_FULL_PATH, specificBoxId);
        assertDoesNotThrow(() -> ypService.addRoleSubject(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID, new Role(specificBoxPath, DEVELOPER_ROLE))));

        String specificBoxDeveloperGroup = unionWithRoleDelimiter(
                unionWithRoleDelimiter(STAGE_GROUP, specificBoxId),
                DEVELOPER_ROLE);

        assertThat(getSpec(masterYpClient, specificBoxDeveloperGroup, GROUP_DESCRIPTOR.createSpecBuilder(), GROUP),
                equalTo(DataModel.TGroupSpec.newBuilder()
                        .addMembers(DEFAULT_MEMBER)
                        .build()));

        assertThat(((TStageMeta) getMeta(masterYpClient, TStageMeta.newBuilder(), DEFAULT_STAGE, STAGE)).getAclList(),
                containsInAnyOrder(
                        ROOT_ACE,
                        generateACE(specificBoxDeveloperGroup, List.of(ACA_SSH_ACCESS),
                                List.of(EPHEMERAL_PATH_BOXES_PREFIX + specificBoxId)),
                        generateACE(specificBoxDeveloperGroup, List.of(ACP_READ),
                                emptyList())));
    }

    @Test
    void addRoleSubjectWhenObjectNotExist() {
        createYpServiceWithSlaveClient();

        // if we've failed to add ACL to object we should not add member to the group in master or slaves
        assertThrows(YpServiceException.class, () -> ypService.addRoleSubject(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID, new Role(DEFAULT_PROJECT, DEVELOPER_ROLE))));

        // so we check that group doesn`t exists in master
        assertThat(get5s(groupRepository.getObject(unionWithRoleDelimiter(PROJECT_GROUP, DEVELOPER_ROLE),
                new Selector.Builder().withMeta().build())), emptyOptional());

        assertFalse(isExist(slaveYpClient, unionWithRoleDelimiter(PROJECT_GROUP, DEVELOPER_ROLE), GROUP));

        assertThrows(YpServiceException.class, () -> ypService.addRoleSubject(
                new RoleSubject(EMPTY_MEMBER, DEFAULT_GROUP_ID, new Role(DEFAULT_STAGE_FULL_PATH, DEVELOPER_ROLE))));

        assertThat(get5s(groupRepository.getObject(unionWithRoleDelimiter(STAGE_GROUP, DEVELOPER_ROLE),
                new Selector.Builder().withMeta().build())), emptyOptional());

        assertFalse(isExist(slaveYpClient, unionWithRoleDelimiter(STAGE_GROUP, DEVELOPER_ROLE), GROUP));
    }

    @Test
    void removeRoleSubject() {
        createGroup(groupRepository, PROJECT_DEVELOPER_ROLE_GROUP, Set.of(DEFAULT_MEMBER, DEFAULT_IDM_GROUP),
                DEPLOY_GROUP_LABELS);

        assertDoesNotThrow(() -> ypService.removeRoleSubject(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID, new Role(DEFAULT_PROJECT, DEVELOPER_ROLE))));

        assertThat(getSpec(masterYpClient, PROJECT_DEVELOPER_ROLE_GROUP, GROUP_DESCRIPTOR.createSpecBuilder(), GROUP),
                equalTo(DataModel.TGroupSpec.newBuilder()
                        .addMembers(DEFAULT_IDM_GROUP)
                        .build()));
    }

    @Test
    void removeRoleSubjectWhenGroupNotExist() {
        assertDoesNotThrow(() -> ypService.removeRoleSubject(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID, new Role(DEFAULT_PROJECT, DEVELOPER_ROLE))));
    }

    @Test
    void addMembersToGroup() {
        createYpServiceWithSlaveClient();

        createGroup(groupRepository, PROJECT_DEVELOPER_ROLE_GROUP, Set.of(DEFAULT_MEMBER, DEFAULT_IDM_GROUP),
                DEPLOY_GROUP_LABELS);
        createGroup(slaveGroupRepository, PROJECT_DEVELOPER_ROLE_GROUP, Set.of(DEFAULT_MEMBER, DEFAULT_IDM_GROUP),
                DEPLOY_GROUP_LABELS);
        createUser(masterYpClient, "new-member");
        createUser(slaveYpClient, "new-member");

        assertDoesNotThrow(() -> ypService.addMembersToGroup(PROJECT_DEVELOPER_ROLE_GROUP, Set.of("new-member")));
        waitForAsync();

        for (YpRawObjectService client : List.of(masterYpClient, slaveYpClient)) {
            assertThat(((DataModel.TGroupSpec) getSpec(client, PROJECT_DEVELOPER_ROLE_GROUP,
                    GROUP_DESCRIPTOR.createSpecBuilder(), GROUP)).getMembersList(),
                    containsInAnyOrder(DEFAULT_MEMBER, DEFAULT_IDM_GROUP, "new-member"));
        }

        // Members do not change when try to add known member
        assertDoesNotThrow(() -> ypService.addMembersToGroup(PROJECT_DEVELOPER_ROLE_GROUP, Set.of("new-member")));

        for (YpRawObjectService client : List.of(masterYpClient, slaveYpClient)) {
            assertThat(((DataModel.TGroupSpec) getSpec(client, PROJECT_DEVELOPER_ROLE_GROUP,
                    GROUP_DESCRIPTOR.createSpecBuilder(), GROUP)).getMembersList(),
                    containsInAnyOrder(DEFAULT_MEMBER, DEFAULT_IDM_GROUP, "new-member"));
        }
    }

    @Test
    void addMembersToNotExistGroup() {
        createYpServiceWithSlaveClient();

        createProject(masterYpClient, DEFAULT_PROJECT);
        createProject(slaveYpClient, DEFAULT_PROJECT);
        createUser(masterYpClient, "new-member");
        createUser(slaveYpClient, "new-member");

        assertDoesNotThrow(() -> ypService.addMembersToGroup(PROJECT_DEVELOPER_ROLE_GROUP, Set.of("new-member")));
        waitForAsync();

        for (YpRawObjectService client : List.of(masterYpClient, slaveYpClient)) {
            assertThat(((DataModel.TGroupSpec) getSpec(client, PROJECT_DEVELOPER_ROLE_GROUP,
                    GROUP_DESCRIPTOR.createSpecBuilder(), GROUP)).getMembersList(),
                    containsInAnyOrder("new-member"));
            YTreeMapNode labels = getLabelsYson(client, PROJECT_DEVELOPER_ROLE_GROUP, GROUP);
            assertThat(labels.getString(SYSTEM_LABEL_KEY), equalTo(SYSTEM_DEPLOY_LABEL_VALUE));
        }

        String nonexistentGroup = DEFAULT_IDM_GROUP + "1";
        assertDoesNotThrow(() -> ypService.addMembersToGroup(nonexistentGroup, Set.of("new-member")));
        waitForAsync();

        for (YpRawObjectService client : List.of(masterYpClient, slaveYpClient)) {
            assertThat(((DataModel.TGroupSpec) getSpec(client, nonexistentGroup,
                    GROUP_DESCRIPTOR.createSpecBuilder(), GROUP)).getMembersList(),
                    containsInAnyOrder("new-member"));
            YTreeMapNode labels = getLabelsYson(client, nonexistentGroup, GROUP);
            assertThat(labels.getString(SYSTEM_LABEL_KEY), equalTo(SYSTEM_IDM_LABEL_VALUE));
        }
    }

    @Test
    void removeMembersFromGroup() {
        createYpServiceWithSlaveClient();

        createGroup(groupRepository, PROJECT_DEVELOPER_ROLE_GROUP, Set.of(DEFAULT_MEMBER, DEFAULT_IDM_GROUP),
                DEPLOY_GROUP_LABELS);
        createGroup(slaveGroupRepository, PROJECT_DEVELOPER_ROLE_GROUP, Set.of(DEFAULT_MEMBER, DEFAULT_IDM_GROUP),
                DEPLOY_GROUP_LABELS);

        assertDoesNotThrow(
                () -> ypService.removeMembersFromGroup(PROJECT_DEVELOPER_ROLE_GROUP, Set.of(DEFAULT_IDM_GROUP)));
        waitForAsync();

        for (YpRawObjectService client : List.of(masterYpClient, slaveYpClient)) {
            assertThat(((DataModel.TGroupSpec) getSpec(client, PROJECT_DEVELOPER_ROLE_GROUP,
                    GROUP_DESCRIPTOR.createSpecBuilder(), GROUP)).getMembersList(),
                    containsInAnyOrder(DEFAULT_MEMBER));
        }

        // Members do not change when try to remove unknown member
        assertDoesNotThrow(
                () -> ypService.removeMembersFromGroup(PROJECT_DEVELOPER_ROLE_GROUP, Set.of(DEFAULT_IDM_GROUP)));
        waitForAsync();

        for (YpRawObjectService client : List.of(masterYpClient, slaveYpClient)) {
            assertThat(((DataModel.TGroupSpec) getSpec(client, PROJECT_DEVELOPER_ROLE_GROUP,
                    GROUP_DESCRIPTOR.createSpecBuilder(), GROUP)).getMembersList(),
                    containsInAnyOrder(DEFAULT_MEMBER));
        }
    }

    @Test
    void removeMembersFromNotExistGroup() {
        assertDoesNotThrow(
                () -> ypService.removeMembersFromGroup(PROJECT_DEVELOPER_ROLE_GROUP, Set.of(DEFAULT_IDM_GROUP)));
    }

    @Test
    void approvalPolicyInheritAclTest() {
        String approvalPolicyId = DEFAULT_STAGE;

        createProject(masterYpClient, DEFAULT_PROJECT);
        createStage(masterYpClient, DEFAULT_STAGE, DEFAULT_PROJECT);
        createApprovalPolicy(masterYpClient, approvalPolicyId, DEFAULT_STAGE);

        String newStageId = DEFAULT_STAGE + "2";
        String newApprovalPolicyId = newStageId;
        createStage(masterYpClient, newStageId, DEFAULT_PROJECT);
        createApprovalPolicy(masterYpClient, newApprovalPolicyId, newStageId);
        assertDoesNotThrow(() -> ypService.addRoleSubject(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID,
                        new Role(unionWithRoleDelimiter(DEFAULT_PROJECT, newStageId), "OWNER"))));

        // Check APPROVER role
        assertDoesNotThrow(() -> ypService.addRoleSubject(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID, new Role(DEFAULT_STAGE_FULL_PATH, "APPROVER"))));
        waitForAsync();
        List<YpCheckedObjectPermissions> checkResults =
                get5s(masterYpClient.checkObjectPermissions(
                        Stream.of(approvalPolicyId, newApprovalPolicyId)
                                .map(approvalId -> new YpCheckObjectPermissions(
                                        new YpTypedId(approvalPolicyId, APPROVAL_POLICY),
                                        DEFAULT_MEMBER,
                                        YpAccessControlPermission.USE,
                                        EPHEMERAL_APPROVERS_PATH))
                                .collect(Collectors.toList())));

        assertThat(checkResults.get(0).getAction(), equalTo(YpAccessControlAction.ALLOW));
        assertThat(checkResults.get(1).getAction(), equalTo(YpAccessControlAction.ALLOW));

        // Check MANDATORY_APPROVER role
        assertDoesNotThrow(() -> ypService.addRoleSubject(
                new RoleSubject(DEFAULT_MEMBER, EMPTY_GROUP_ID,
                        new Role(DEFAULT_STAGE_FULL_PATH, "MANDATORY_APPROVER"))));
        waitForAsync();
        checkResults =
                get5s(masterYpClient.checkObjectPermissions(
                        Stream.of(approvalPolicyId, newApprovalPolicyId)
                                .map(approvalId -> new YpCheckObjectPermissions(
                                        new YpTypedId(approvalPolicyId, APPROVAL_POLICY),
                                        DEFAULT_MEMBER,
                                        YpAccessControlPermission.USE,
                                        EPHEMERAL_MANDATORY_APPROVERS_PATH))
                                .collect(Collectors.toList())));
        assertThat(checkResults.get(0).getAction(), equalTo(YpAccessControlAction.ALLOW));
        assertThat(checkResults.get(1).getAction(), equalTo(YpAccessControlAction.ALLOW));
    }

    void waitForAsync() {
        // Give some time for async queue to execute slave group update
        try {
            sleep(2000);
        } catch (InterruptedException ignored) {
        }
    }
}

