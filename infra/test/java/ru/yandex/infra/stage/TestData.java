package ru.yandex.infra.stage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.typesafe.config.ConfigValueFactory;

import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.ProjectMeta;
import ru.yandex.infra.controller.dto.RelationMeta;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.deployunit.DeployUnitTimeline;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.ClusterAndType;
import ru.yandex.infra.stage.dto.Condition;
import ru.yandex.infra.stage.dto.CoredumpConfig;
import ru.yandex.infra.stage.dto.DeployProgress;
import ru.yandex.infra.stage.dto.DeployReadyCriterion;
import ru.yandex.infra.stage.dto.DeploySpeed;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DeployUnitStatus;
import ru.yandex.infra.stage.dto.DockerImageContents;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.dto.DynamicResourceMeta;
import ru.yandex.infra.stage.dto.DynamicResourceSpec;
import ru.yandex.infra.stage.dto.HorizontalPodAutoscalerMeta;
import ru.yandex.infra.stage.dto.LogbrokerCommunalTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerConfig;
import ru.yandex.infra.stage.dto.LogbrokerDestroyPolicy;
import ru.yandex.infra.stage.dto.LogbrokerTopicConfig;
import ru.yandex.infra.stage.dto.LogbrokerTopicDescription;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;
import ru.yandex.infra.stage.dto.McrsUnitSpec;
import ru.yandex.infra.stage.dto.NetworkDefaults;
import ru.yandex.infra.stage.dto.PodAgentConfig;
import ru.yandex.infra.stage.dto.ReplicaSetDeploymentStrategy;
import ru.yandex.infra.stage.dto.ReplicaSetUnitSpec;
import ru.yandex.infra.stage.dto.ReplicaSetUnitStatus;
import ru.yandex.infra.stage.dto.RuntimeDeployControls;
import ru.yandex.infra.stage.dto.SandboxResourceInfo;
import ru.yandex.infra.stage.dto.Secret;
import ru.yandex.infra.stage.dto.SecretRef;
import ru.yandex.infra.stage.dto.SecretSelector;
import ru.yandex.infra.stage.dto.SecuritySettings;
import ru.yandex.infra.stage.dto.SidecarVolumeSettings;
import ru.yandex.infra.stage.dto.StageSpec;
import ru.yandex.infra.stage.dto.StageStatus;
import ru.yandex.infra.stage.dto.TvmApp;
import ru.yandex.infra.stage.dto.TvmClient;
import ru.yandex.infra.stage.dto.TvmConfig;
import ru.yandex.infra.stage.dto.datamodel.AntiaffinityConstraint;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.SandboxResourceMeta;
import ru.yandex.infra.stage.protobuf.Converter;
import ru.yandex.infra.stage.yp.Retainment;
import ru.yandex.infra.stage.yp.Yson;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.DynamicResource;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TProjectStatus;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetStatus;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.client.pods.EVolumeMountMode;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TMountedVolume;
import ru.yandex.yp.client.pods.TWorkload;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yt.ytree.TAttributeDictionary;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACA_WRITE;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACP_READ;

// Some non-default protos.
// Use these constants if you need some placeholder without regards to its contents.
// If you need some specific DTO - define it in place.
public class TestData {
    public static final DownloadableResource DOWNLOADABLE_RESOURCE = new DownloadableResource("url", Checksum.EMPTY);
    public static final DownloadableResource DOWNLOADABLE_RESOURCE2 = new DownloadableResource("rbtorrent:321", Checksum.EMPTY);
    public static final DownloadableResource DOWNLOADABLE_SBR_RESOURCE = new DownloadableResource("sbr:123", Checksum.EMPTY);
    public static final Map<String, String> SANDBOX_ATTRIBUTES = ImmutableMap.of("attribute1", "True");
    public static final int TASK_ID = 123;
    public static final int RESOURCE_ID = 456;
    public static final SandboxResourceMeta RESOURCE_META = new SandboxResourceMeta(TASK_ID, RESOURCE_ID, SANDBOX_ATTRIBUTES);
    public static final Map<String, String> SANDBOX_RESOLVED_RESOURCES = ImmutableMap.of("sbr:123", "rbtorrent:321");
    public static final List<String> DYNAMIC_RESOURCE_URLS = ImmutableList.of("rbtorrent:123123", "sbr:123");
    public static final List<String> DYNAMIC_RESOURCE_RESOLVED_URLS = ImmutableList.of("rbtorrent:123123", "rbtorrent:321");

    public static final String DEFAULT_REGISTRY_HOST = "https://registry.yandex.net";
    public static final String DEFAULT_IMAGE_NAME =
            "qe_quality/nirvana_sandbox_processor_nirvana_sandbox_processor_standalone";
    public static final String DEFAULT_IMAGE_TAG = "1.189";
    public static final String DEFAULT_IMAGE_HASH = "sha256:bc746bc0e7e829f47ed7afb3fb0ed269722dadf5ee46cfbeecac2dd8e509dccc";

    public static final DockerImageDescription DOCKER_IMAGE_DESCRIPTION = new DockerImageDescription(
            DEFAULT_REGISTRY_HOST, DEFAULT_IMAGE_NAME, DEFAULT_IMAGE_TAG);
    public static final DockerImageDescription DOCKER_IMAGE_WITHOUT_HOST_DESCRIPTION = new DockerImageDescription(
            null, DEFAULT_IMAGE_NAME, DEFAULT_IMAGE_TAG);

    public static final ReplicaSetDeploymentStrategy REPLICA_SET_DEPLOYMENT_STRATEGY = new ReplicaSetDeploymentStrategy(1,
            2,
            3,
            4,
            5,
            6,
            Optional.of(new DeploySpeed(11, 22)),
            Optional.of(new DeployReadyCriterion(Optional.of("AUTO"), Optional.of(111), Optional.of(222))));

    public static final DockerImageContents
            DOCKER_IMAGE_CONTENTS = new DockerImageContents(DOCKER_IMAGE_DESCRIPTION,
            ImmutableList.of(DOWNLOADABLE_RESOURCE),
            ImmutableList.of("/bin/sh", "-c", "java $QLOUD_JAVA_OPTIONS -jar /runtime.jar"), emptyList(),
            Optional.empty(), Optional.empty(),
            Optional.empty(), ImmutableMap.of("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
            , Optional.of(DEFAULT_IMAGE_HASH));

    public static final DockerImageContents DOCKER_IMAGE_CONTENTS_WITH_EMPTY_DIGEST = new DockerImageContents(
            DOCKER_IMAGE_CONTENTS.getDescription(),
            DOCKER_IMAGE_CONTENTS.getLayers(),
            DOCKER_IMAGE_CONTENTS.getCommand(),
            DOCKER_IMAGE_CONTENTS.getEntryPoint(),
            DOCKER_IMAGE_CONTENTS.getUser(),
            DOCKER_IMAGE_CONTENTS.getGroup(),
            DOCKER_IMAGE_CONTENTS.getWorkingDir(),
            DOCKER_IMAGE_CONTENTS.getEnvironment(),
            Optional.empty());

    public static final SidecarVolumeSettings SIDECAR_VOLUME_SETTINGS = new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO);

    private static Converter createConverterWithFlags(boolean useExtendedReplicaSetStatuses,
                                                      boolean addDeployUnitTimelineStatuses) {
        return new Converter(
                ConfigValueFactory.fromMap(ImmutableMap.of(
                                Converter.PROTO_CONVERTER_CONFIG_KEY, ImmutableMap.of(
                                        Converter.WRITE_FAILURE_CONDITION_CONFIG_KEY, true),
                                Converter.PROJECT_PROMISE_CONFIG_KEY, ImmutableMap.of(
                                        Converter.USE_EXTENDED_REPLICA_SET_STATUSES_CONFIG_KEY, useExtendedReplicaSetStatuses,
                                        Converter.ADD_DEPLOY_UNIT_TIMELINE_STATUSES_CONFIG_KEY, addDeployUnitTimelineStatuses)
                        )
                ).toConfig()
        );
    }

    public static final Converter CONVERTER = createConverterWithFlags(true, true);
    public static final Converter CONVERTER_WITHOUT_REPLICA_SET_STATUSES = createConverterWithFlags(false, true);
    public static final Converter CONVERTER_WITHOUT_DEPLOY_TIMELINE_STATUSES = createConverterWithFlags(true, false);

    public static final PodAgentConfig POD_AGENT_CONFIG = new PodAgentConfig(Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD)));
    public static final Function<DataModel.TPodSpec.TPodAgentDeploymentMeta, PodAgentConfig> POD_AGENT_CONFIG_EXTRACTOR = podAgentDeploymentMeta -> POD_AGENT_CONFIG;
    public static final LogbrokerTopicRequest DEFAULT_LOGBROKER_TOPIC_REQUEST = LogbrokerCommunalTopicRequest.INSTANCE;
    public static final LogbrokerConfig LOGBROKER_CONFIG = new LogbrokerConfig(
            Optional.empty(),
            LogbrokerConfig.SidecarBringupMode.DEFAULT,
            Optional.empty(),
            Optional.empty(),
            new LogbrokerDestroyPolicy(),
            DEFAULT_LOGBROKER_TOPIC_REQUEST,
            Optional.empty()
    );

    public static final SecuritySettings SECURITY_SETTINGS = new SecuritySettings(true, true);
    public static final LogbrokerTopicDescription LOGBROKER_TOPIC_DESCRIPTION = new LogbrokerTopicDescription(2010000, "test/logs");
    public static final AllComputeResources EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST = new AllComputeResources(
            0, 0, AllComputeResources.UNKNOWN_DISK_CAPACITY, 0
    );
    public static final SecretRef SECRET_REF = new SecretRef("id", "version");
    public static final String DELEGATION_TOKEN = "delegation_token";
    public static final Secret SECRET = new Secret(SECRET_REF, DELEGATION_TOKEN);
    public static final String SECRET_ALIAS = "secret_alias";
    public static final String SECRET_KEY = "secret_key";
    public static final SecretSelector SECRET_SELECTOR = new SecretSelector(SECRET_ALIAS, SECRET_KEY);
    public static final LogbrokerTopicConfig DEFAULT_LOGBROKER_TOPIC_CONFIG = new LogbrokerTopicConfig(
            LOGBROKER_TOPIC_DESCRIPTION,
            SECRET_SELECTOR
    );
    public static final AccessControl.TAccessControlEntry ACL_WRITE_ENTRY = createACLEntry("user", ACA_WRITE);
    public static final AccessControl.TAccessControlEntry ACL_READ_ENTRY = createACLEntry("user", ACP_READ);
    public static final String DEFAULT_ITYPE = "itype";
    public static final String DEFAULT_WORKLOAD_ITYPE = "workload_itype";
    public static final String DEFAULT_WORKLOAD_ID = "workload_id_1";

    public static final String SERVICE_NAME = "serviceName";
    public static final CoredumpConfig COREDUMP_CONFIG_WITH_AGGREGATION = new CoredumpConfig(2, 100, 50, 3600, true,
            null, Optional.of(SERVICE_NAME), Optional.of("prod"), Optional.empty());
    public static final CoredumpConfig COREDUMP_CONFIG_WITHOUT_AGGREGATION = new CoredumpConfig(3, 200, 60, 3600,
            false, null, Optional.of(SERVICE_NAME), Optional.empty(), Optional.empty());
    public static final CoredumpConfig COREDUMP_CONFIG_WITH_AGGREGATION_WITHOUT_SERVICE = new CoredumpConfig(2, 100, 50, 3600, true,
            null, Optional.empty(), Optional.of("prod"), Optional.empty());
    public static final Acl STAGE_ACL = new Acl(ImmutableList.of(ACL_WRITE_ENTRY));
    public static final RuntimeDeployControls RUNTIME_DEPLOY_CONTROLS = new RuntimeDeployControls(emptyMap(), emptyMap(), emptyMap());
    public static final Acl PROJECT_ACL = new Acl(ImmutableList.of(ACL_READ_ENTRY));
    public static final String ABC_ACCOUNT_ID = "abc:service:1234";
    public static final String PROJECT_ABC_ACCOUNT_ID = "abc:service:4444";
    public static final String DEFAULT_BOX_ID = "box_id_1";
    public static final TvmApp TVM_SRC = new TvmApp(1, "src");
    public static final TvmApp TVM_DST = new TvmApp(2, "dst");
    public static final TvmClient TVM_CLIENT = new TvmClient(SECRET_SELECTOR, TVM_SRC, ImmutableList.of(TVM_DST), "");
    public static final TvmClient TVM_CLIENT_WITH_NO_DESTINATIONS = TVM_CLIENT.withNoDestinations();

    public static final String BLACKBOX_ENVIRONMENT = "test";
    public static final TvmConfig TVM_CONFIG = new TvmConfig(TvmConfig.Mode.ENABLED, BLACKBOX_ENVIRONMENT,
            ImmutableList.of(TVM_CLIENT), OptionalInt.of(4000), OptionalInt.of(333), OptionalInt.of(30),
            Optional.of(new DownloadableResource("foo", new Checksum("tvmPatcherTest", Checksum.Type.MD5))),
            OptionalInt.of(2020703), OptionalInt.of(2012028), OptionalInt.of(12510),
            Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD)), false);
    public static final TvmConfig TVM_CONFIG_WITH_NO_DESTINATIONS = TVM_CONFIG.withClients(ImmutableList.of(TVM_CLIENT_WITH_NO_DESTINATIONS));

    public static final String NETWORK_ID = "networkId";
    public static final NetworkDefaults NETWORK_DEFAULTS = new NetworkDefaults(
            NETWORK_ID,
            false,
            false,
            Optional.empty(),
            emptyList()
    );
    public static final NetworkDefaults EMPTY_NETWORK_DEFAULTS = NETWORK_DEFAULTS.withNetworkId("");
    public static final NetworkDefaults NETWORK_DEFAULTS_WITH_ADDRESS_OVERRIDE = NETWORK_DEFAULTS.toBuilder()
            .withNetworkId("networkId2")
            .withOverrideIp6AddressRequests(true)
            .build();

    public static final NetworkDefaults NETWORK_DEFAULTS_WITH_SUBNET_OVERRIDE = NETWORK_DEFAULTS.toBuilder()
            .withNetworkId("networkId3")
            .withOverrideIp6SubnetRequests(true)
            .build();

    public static final DataModel.TPodSpec.TDiskVolumeRequest USED_BY_INFRA_VOLUME_REQUEST = DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
            .setId("root")
            .setStorageClass("hdd")
            .setQuotaPolicy(DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy.newBuilder()
                    .setCapacity(2000000000)
                    .build())
            .setLabels(TAttributeDictionary.newBuilder()
                    .addAttributes(PodSpecUtils.USED_BY_INFRA_LABEL)
                    .build())
            .build();

    public static final DataModel.TPodSpec.TDiskVolumeRequest NOT_USED_BY_INFRA_VOLUME_REQUEST = DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
            .setId("non_root")
            .setStorageClass("hdd")
            .setQuotaPolicy(DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy.newBuilder()
                    .setCapacity(2000000000)
                    .build())
            .setLabels(TAttributeDictionary.newBuilder()
                    .build())
            .build();

    public static final DataModel.TPodSpec.TDiskVolumeRequest NOT_USED_BY_INFRA_VOLUME_REQUEST_2 = DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
            .setId("non_root_2")
            .setStorageClass("hdd")
            .setQuotaPolicy(DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy.newBuilder()
                    .setCapacity(1000000000)
                    .build())
            .setLabels(TAttributeDictionary.newBuilder()
                    .build())
            .build();


    public static final DataModel.TPodSpec.TResourceRequests VALID_POD_RESOURCES =
            DataModel.TPodSpec.TResourceRequests.newBuilder()
                    .setMemoryGuarantee(100)
                    .setMemoryLimit(100)
                    .setVcpuLimit(100)
                    .build();

    public static final DataModel.TPodSpec POD_SPEC = makePodSpecWithDefaults(
            DataModel.TPodSpec.newBuilder()
                    .addDiskVolumeRequests(USED_BY_INFRA_VOLUME_REQUEST)
                    .setResourceRequests(VALID_POD_RESOURCES)
                    .build());

    public static final DataModel.TPodSpec POD_SPEC_MULTIPLE_USER_DISKS = makePodSpecWithDefaults(
            DataModel.TPodSpec.newBuilder()
                    .addDiskVolumeRequests(USED_BY_INFRA_VOLUME_REQUEST)
                    .addDiskVolumeRequests(NOT_USED_BY_INFRA_VOLUME_REQUEST)
                    .setResourceRequests(VALID_POD_RESOURCES)
                    .build());

    public static final int MAX_UNAVAILABLE = 1;
    public static final TReplicaSetSpec REPLICA_SET_SPEC = TReplicaSetSpec.newBuilder()
            .setAccountId(ABC_ACCOUNT_ID)
            .setDeploymentStrategy(TReplicaSetSpec.TDeploymentStrategy.newBuilder()
                    .setMaxUnavailable(MAX_UNAVAILABLE))
            .setReplicaCount(1)
            .setPodTemplateSpec(TPodTemplateSpec.newBuilder()
                    .setSpec(POD_SPEC)
                    .build())
            .build();
    public static final TMultiClusterReplicaSetSpec MULTI_CLUSTER_REPLICA_SET_SPEC =
            TMultiClusterReplicaSetSpec.newBuilder()
                    .setAccountId(ABC_ACCOUNT_ID)
                    .setPodTemplateSpec(TPodTemplateSpec.newBuilder()
                            .setSpec(POD_SPEC)
                            .build())
                    .setDeploymentStrategy(TMultiClusterReplicaSetSpec.TDeploymentStrategy.newBuilder()
                            .setMaxUnavailable(MAX_UNAVAILABLE))
                    .build();

    public static final TMultiClusterReplicaSetSpec MULTI_CLUSTER_REPLICA_SET_SPEC_MULTIPLE_USER_DISKS =
            TMultiClusterReplicaSetSpec.newBuilder()
                    .setAccountId(ABC_ACCOUNT_ID)
                    .setPodTemplateSpec(TPodTemplateSpec.newBuilder()
                            .setSpec(POD_SPEC_MULTIPLE_USER_DISKS)
                            .build())
                    .setDeploymentStrategy(TMultiClusterReplicaSetSpec
                            .TDeploymentStrategy
                            .newBuilder()
                            .build())
                    .build();

    public static final McrsUnitSpec MCRS_UNIT_SPEC = new McrsUnitSpec(MULTI_CLUSTER_REPLICA_SET_SPEC, POD_AGENT_CONFIG_EXTRACTOR);
    public static final McrsUnitSpec MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS = new McrsUnitSpec(MULTI_CLUSTER_REPLICA_SET_SPEC_MULTIPLE_USER_DISKS, POD_AGENT_CONFIG_EXTRACTOR);
    public static final String CLUSTER = "sas-test";
    public static final ReplicaSetUnitSpec REPLICA_SET_UNIT_SPEC = new ReplicaSetUnitSpec(REPLICA_SET_SPEC,
            ImmutableMap.of(
                    CLUSTER, new ReplicaSetUnitSpec.PerClusterSettings(Either.left(100), Optional.empty())
            ),
            POD_AGENT_CONFIG_EXTRACTOR);
    public static final DeployUnitSpec.DeploySettings EMPTY_DEPLOY_SETTINGS =
            new DeployUnitSpec.DeploySettings(List.of(new DeployUnitSpec.DeploySettings.ClusterSettings("sas-test",
                    true)), TDeployUnitSpec.TDeploySettings.EDeployStrategy.PARALLEL);
    public static final SandboxResourceInfo EMPTY_RESOURCE_INFO = new SandboxResourceInfo(-1, emptyMap()); // >=0 breaks tests
    public static final long DEFAULT_SIDECAR_REVISION = 123;
    public static final SandboxResourceInfo DEFAULT_SIDECAR_RESOURCE_INFO = new SandboxResourceInfo(DEFAULT_SIDECAR_REVISION, emptyMap());
    public static final DeployUnitSpec DEPLOY_UNIT_SPEC = new DeployUnitSpec(1, 0, NETWORK_DEFAULTS,
            Optional.of(TVM_CONFIG), REPLICA_SET_UNIT_SPEC, emptyMap(), emptyMap(), emptyMap(), emptyMap(),
            LOGBROKER_CONFIG, Optional.of(SECURITY_SETTINGS),false, false, emptyMap(),
            Optional.of(EMPTY_RESOURCE_INFO), Optional.of(EMPTY_RESOURCE_INFO), Optional.of(EMPTY_RESOURCE_INFO),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    public static final String ENDPOINT_SET_ID = "endpoint_set_id";
    public static final String REPLICA_SET_ID = "replica_set_id";
    public static final String HORIZONTAL_POD_AUTOSCALER_ID = "horizontal_pod_autoscaler_id";
    public static final ReplicaSetUnitStatus REPLICA_SET_UNIT_STATUS_DETAILS = new ReplicaSetUnitStatus(ImmutableMap.of(
            CLUSTER, new ReplicaSetUnitStatus.PerClusterStatus(REPLICA_SET_ID, List.of(ENDPOINT_SET_ID), HORIZONTAL_POD_AUTOSCALER_ID, Optional.of(TReplicaSetStatus.newBuilder().setRevisionId("123").build()))
    ));
    public static final ReplicaSetUnitStatus REPLICA_SET_UNIT_STATUS_DETAILS_WITHOUT_RAW_STATUS = new ReplicaSetUnitStatus(ImmutableMap.of(
            CLUSTER, new ReplicaSetUnitStatus.PerClusterStatus(REPLICA_SET_ID, List.of(ENDPOINT_SET_ID),
                    HORIZONTAL_POD_AUTOSCALER_ID, Optional.empty())
    ));
    public static final DeployProgress DEPLOY_PROGRESS = new DeployProgress(1, 2, 3);
    public static final Condition CONDITION1 = new Condition(Condition.Status.TRUE, "a", "b",
            Instant.ofEpochSecond(100500));
    public static final Condition CONDITION2 = new Condition(Condition.Status.FALSE, "c", "d",
            Instant.ofEpochSecond(100600));
    public static final Condition CONDITION3 = new Condition(Condition.Status.FALSE, "e", "f",
            Instant.ofEpochSecond(100700));

    public static final DeployUnitTimeline DEPLOY_UNIT_TIMELINE = new DeployUnitTimeline(
            1000,
            Instant.ofEpochMilli(1),
            CONDITION1
    );

    public static final DeployUnitStatus DEPLOY_UNIT_STATUS = new DeployUnitStatus(
            CONDITION2, CONDITION3,
            DEPLOY_UNIT_SPEC,
            REPLICA_SET_UNIT_STATUS_DETAILS, DEPLOY_PROGRESS, "itype",
            DEPLOY_UNIT_TIMELINE
    );

    public static final String DEPLOY_UNIT_ID = "deploy_unit_id";
    public static final String DYNAMIC_RESOURCE_ID = "dynamic_resource_id";
    public static final DynamicResourceSpec DYNAMIC_RESOURCE_SPEC = new DynamicResourceSpec(DEPLOY_UNIT_ID,
            DynamicResource.TDynamicResourceSpec.newBuilder()
                    .setRevision(1)
                    .addDeployGroups(DynamicResource.TDynamicResourceSpec.DeployGroup.newBuilder()
                            .setMark("all")
                            .addAllUrls(DYNAMIC_RESOURCE_URLS)
                            .setStorageOptions(DataModel.TPodDynamicResourceSpec.TStorageOptions.newBuilder()
                                    .setBoxRef(DEFAULT_BOX_ID)
                            )
                    )
                    .setUpdateWindow(2)
                    .build());

    public static final StageSpec STAGE_SPEC = new StageSpec(ImmutableMap.of(
            DEPLOY_UNIT_ID, DEPLOY_UNIT_SPEC
    ), ABC_ACCOUNT_ID, 100500, false, emptyMap(), emptyMap());
    public static final StageSpec STAGE_SPEC_WITH_DR = new StageSpec(ImmutableMap.of(
            DEPLOY_UNIT_ID, DEPLOY_UNIT_SPEC
    ), ABC_ACCOUNT_ID, 100500, false, ImmutableMap.of(DYNAMIC_RESOURCE_ID, DYNAMIC_RESOURCE_SPEC),
            emptyMap());

    public static final TStageSpec PROTO_STAGE_SPEC = CONVERTER.toProto(STAGE_SPEC);
    public static final StageStatus STAGE_STATUS = new StageStatus(ImmutableMap.of(
            DEPLOY_UNIT_ID, DEPLOY_UNIT_STATUS
    ), emptyMap(), 100500, CONDITION1, 1);
    public static final String PROJECT_ID = "project-id";
    public static final Try<YpObject<StageMeta, TStageSpec, TStageStatus>> STAGE = createStageWithAcl(STAGE_ACL);
    public static final Try<YpObject<SchemaMeta, TProjectSpec, TProjectStatus>> PROJECT =
            createProjectWithAcl(PROJECT_ID, PROJECT_ACL);

    public static final AntiaffinityConstraint CONSTRAINT = AntiaffinityConstraint.dc(10);

    public static final ClusterAndType CLUSTER_AND_TYPE = ClusterAndType.perClusterInstance("sas",
            YpObjectType.REPLICA_SET);
    public static final long META_TIMESTAMP = 10L;

    private static DataModel.TPodSpec makePodSpecWithDefaults(DataModel.TPodSpec podSpec) {
        String json = Yson.protoAsJson(podSpec);
        DataModel.TPodSpec.Builder builder = DataModel.TPodSpec.newBuilder();
        try {
            JsonFormat.parser().merge(json, builder);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        return builder.build();
    }

    public static TWorkload createWorkloadWithLogs(String workloadId, String boxId) {
        return TWorkload.newBuilder().setId(workloadId).setTransmitLogs(true).setBoxRef(boxId).build();
    }

    public static TWorkload createWorkloadWithoutLogs(String workloadId, String boxId) {
        return TWorkload.newBuilder().setId(workloadId).setBoxRef(boxId).build();
    }

    public static TWorkload createWorkload(String workloadId, String boxId, boolean withLogs) {
        if (withLogs) {
            return createWorkloadWithLogs(workloadId, boxId);
        }
        return createWorkloadWithoutLogs(workloadId, boxId);
    }

    public static TBox.Builder createBox(String boxId) {
        return TBox.newBuilder().setId(boxId);
    }

    public static void addVolumeToBox(TBox.Builder box, String volumeId, String volumeMountPoint) {
        box.addVolumes(TMountedVolume.newBuilder()
                        .setMode(EVolumeMountMode.EVolumeMountMode_READ_WRITE)
                        .setMountPoint(volumeMountPoint)
                        .setVolumeRef(volumeId))
                .build();
    }

    public static AccessControl.TAccessControlEntry createACLEntry(Iterable<String> subjects,
                                                                   AccessControl.EAccessControlPermission permission) {
        return AccessControl.TAccessControlEntry.newBuilder()
                .addAllSubjects(subjects)
                .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                .addPermissions(permission)
                .build();
    }

    public static AccessControl.TAccessControlEntry createACLEntry(String subject,
                                                                   AccessControl.EAccessControlPermission permission) {
        return createACLEntry(List.of(subject), permission);
    }

    public static Try<YpObject<SchemaMeta, TProjectSpec, TProjectStatus>> createProjectWithAcl(String id, Acl acl) {
        return Try.success(new YpObject.Builder<SchemaMeta, TProjectSpec, TProjectStatus>()
                .setSpecAndTimestamp(TProjectSpec.newBuilder()
                        .setAccountId(PROJECT_ABC_ACCOUNT_ID)
                        .build(), 1)
                .setMeta(new SchemaMeta(id, acl, "", "", 0))
                .build());
    }

    public static Try<YpObject<StageMeta, TStageSpec, TStageStatus>> createStageWithAcl(Acl acl) {
        return createStageWithAclAndProject(acl, PROJECT_ID);
    }

    public static Try<YpObject<StageMeta, TStageSpec, TStageStatus>> createStageWithAclAndProject(Acl acl,
                                                                                                  String projectId) {
        return Try.success(new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                .setSpecAndTimestamp(PROTO_STAGE_SPEC, 1)
                .setStatus(TStageStatus.getDefaultInstance())
                .setMeta(new StageMeta(DEFAULT_STAGE_ID, acl, DEFAULT_STAGE_FQID, DEFAULT_STAGE_UUID, 0, projectId))
                .setMetaTimestamp(META_TIMESTAMP)
                .build());
    }

    public static String composeStageFqid(String stageId, String uuid) {
        return String.format("yp|sas-test|stage|%s|%s", stageId, uuid);
    }

    public static final String DEFAULT_STAGE_ID = "stage_id";
    public static final String DEFAULT_STAGE_UUID = "d7c28a6b-a59aedd2-25eb200a-301468c4";
    public static final String DEFAULT_STAGE_FQID = composeStageFqid(DEFAULT_STAGE_ID, DEFAULT_STAGE_UUID);
    public static final StageContext DEFAULT_STAGE_CONTEXT = new StageContext(DEFAULT_STAGE_FQID,
            DEFAULT_STAGE_ID, 100500, "abc:111", STAGE_ACL, 1, "project_id", emptyMap(), emptyMap(),
            RUNTIME_DEPLOY_CONTROLS, Map.of("key1", "value1", "key2", "value2"), GlobalContext.EMPTY);

    public static final DeployUnitContext DEFAULT_UNIT_CONTEXT = new DeployUnitContext(
            DEFAULT_STAGE_CONTEXT,
            DEPLOY_UNIT_SPEC,
            DEPLOY_UNIT_ID,
            DEFAULT_STAGE_CONTEXT.getStageId() + "." + DEPLOY_UNIT_ID,
            emptyMap(),
            Optional.of(DEFAULT_LOGBROKER_TOPIC_CONFIG),
            emptyMap());
    public static final Retainment RETAINMENT = new Retainment(true, "just because");

    public static final Acl RW_ACL = new Acl(ImmutableList.of(TestData.ACL_READ_ENTRY, TestData.ACL_WRITE_ENTRY));

    public static final SchemaMeta SCHEMA_META = new SchemaMeta("Id1", RW_ACL, "Fqid2", "d7c28a6b-a59aedd2-25eb200a-301468c4", 1624442967346635L);
    public static final RelationMeta RELATION_META = new RelationMeta("Id1", RW_ACL, "Fqid2", "d7c28a6b-a59aedd2-25eb200a-301468c4",
            1625160065514838L,
            "yp|sas-test|stage|vzstage5|1d457f2b-84fb4d5e-c7c2624-9d12e8b9",
            "yp|sas-test|replica_set|vzstage5.deployUnit|169fcfcb-14d12709-2afd6b66-dbffa2aa");
    public static final ProjectMeta PROJECT_META = new ProjectMeta("ProjectId1", RW_ACL, "Fqid2", "d7c28a6b-a59aedd2-25eb200a-301468c4", 1624442967346635L, "user1");
    public static final StageMeta STAGE_META = new StageMeta("StageId1", RW_ACL, "Fqid2", "d7c28a6b-a59aedd2-25eb200a-301468c4", 1624442967346635L, "ProjectId1", ABC_ACCOUNT_ID);
    public static final DynamicResourceMeta DYNAMIC_RESOURCE_META = new DynamicResourceMeta("panefgen-dr2-stage.DeployUnit1.TestRes1", RW_ACL,
            "yp|sas-test|dynamic_resource|panefgen-dr2-stage.DeployUnit1.TestRes1|5a180970-611e8d88-69ac1303-2a10f053",
            "5a180970-611e8d88-69ac1303-2a10f053", 123, "PodSetId");
    public static final HorizontalPodAutoscalerMeta HORIZONTAL_POD_AUTOSCALER_META = new HorizontalPodAutoscalerMeta(
            "danibw-test-cpu-load.DeployUnit1", RW_ACL,
            "yp|iva|horizontal_pod_autoscaler|test-cpu-load.DeployUnit1|399c1-36154a54-30179523-64b51f6e",
            "399c1-36154a54-30179523-64b51f6e", 456, "rsId");
}
