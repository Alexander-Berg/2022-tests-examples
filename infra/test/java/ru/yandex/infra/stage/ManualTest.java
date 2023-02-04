package ru.yandex.infra.stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.infra.controller.yp.CreateObjectRequest;
import ru.yandex.infra.controller.yp.ObjectBuilderDescriptor;
import ru.yandex.infra.controller.yp.Paths;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.controller.yp.YpObjectRepository;
import ru.yandex.infra.stage.docker.DockerInfoJSONParseException;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.dto.SecretSelector;
import ru.yandex.infra.stage.dto.StageSpec;
import ru.yandex.infra.stage.dto.TvmApp;
import ru.yandex.infra.stage.dto.TvmClient;
import ru.yandex.infra.stage.dto.TvmConfig;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.util.StringUtils;
import ru.yandex.infra.stage.yp.AsyncYpClientsMap;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeProtoUtils;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.Autogen.TSchemaMeta;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TDockerImageDescription;
import ru.yandex.yp.client.api.THostInfraInfo;
import ru.yandex.yp.client.api.TLogrotateConfig;
import ru.yandex.yp.client.api.TMonitoringInfo;
import ru.yandex.yp.client.api.TMonitoringUnistatEndpoint;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.client.api.TNetworkDefaults;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.client.pods.EWorkloadTargetState;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TLivenessCheck;
import ru.yandex.yp.client.pods.TMutableWorkload;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResourceGang;
import ru.yandex.yp.client.pods.TRootfsVolume;
import ru.yandex.yp.client.pods.TUtilityContainer;
import ru.yandex.yp.client.pods.TWorkload;
import ru.yandex.yp.model.YpAccessControlAction;
import ru.yandex.yp.model.YpAccessControlPermission;
import ru.yandex.yp.model.YpCheckObjectPermissions;
import ru.yandex.yp.model.YpCheckedObjectPermissions;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpPayload;
import ru.yandex.yp.model.YpPayloadFormat;
import ru.yandex.yp.model.YpSelectStatement;
import ru.yandex.yp.model.YpSelectedObjects;
import ru.yandex.yp.model.YpTypedId;
import ru.yandex.yt.ytree.TAttributeDictionary;

import static java.util.Collections.emptyMap;
import static ru.yandex.infra.controller.util.ConfigUtils.token;
import static ru.yandex.infra.controller.util.YpUtils.CommonSelectors.SPEC_STATUS_META;
import static ru.yandex.infra.controller.util.YsonUtils.payloadToYson;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.literalEnvVar;

// Some code for manual testing, not intended to be run in CI yet
@Disabled
class ManualTest {
    private static final Logger LOG = LoggerFactory.getLogger(ManualTest.class);
    private static final int TOTAL_STAGES = 20000;
    private static final int TOTAL_ACTIONS = 1000000;
    private static final int WRITER_THREADS = 20;
    private static final String TVM_SECRET_KEY = "tvm_secret1";     // key in yav
    private static final String TVM_SECRET_NAME = "tvm_secret_name";
    public static final String ROOT_BOX = "root_box";

    private YpRawObjectService ypClient;
    private String stageId;
    private String subjectUser;
    private boolean useDockerImage;
    private String cluster;
    private YpObjectRepository<StageMeta, TStageSpec, TStageStatus> repository;
    private Optional<String> rsTvmToken;
    private boolean transmitLogs;

    @BeforeEach
    void before() {
        String configPath = System.getenv("CONFIG_PATH");
        Config config = ConfigFactory.parseFile(new File(configPath),
                ConfigParseOptions.defaults().setAllowMissing(false))
                .withFallback(ConfigFactory.load("application_defaults.conf"));
        Config testSettings = config.getConfig("test_settings");
        if (testSettings.hasPath("tvm")) {
            // token configuration is described here
            // https://wiki.yandex-team.ru/deploy/docs/scenarii-ispolzovanija-dctl/#copy-stage
            // different DU require different tokens
            rsTvmToken = Optional.of(token(testSettings.getString("tvm.rs_token_file")));
        } else {
            rsTvmToken = Optional.empty();
        }

        stageId = System.getenv("STAGE_ID");
        subjectUser = System.getenv("SUBJECT_USER");
        useDockerImage = Boolean.parseBoolean(System.getenv("USE_DOCKER_IMAGE"));
        transmitLogs = Boolean.parseBoolean(System.getenv("TRANSMIT_LOGS"));

        boolean isReadonlyMode = config.getBoolean("readonly_mode");
        AsyncYpClientsMap clientsMap = ConfigUtils.ypClientMap(config.getConfig("yp"), GaugeRegistry.EMPTY, isReadonlyMode);
        ypClient = clientsMap.getMultiClusterClient();
        cluster = clientsMap.getClusters().iterator().next();

        repository = ConfigUtils.ypStageRepository(ypClient, config, 1000,
                () -> 0L, "not_stagectl_epoch", Optional.of("stagectl_vcs"), GaugeRegistry.EMPTY, isReadonlyMode);
    }

    @Test
    void getStageDockerImages() throws ExecutionException, InterruptedException {
        CompletableFuture<List<List<YpPayload>>> future =
                ypClient.selectObjects(YpSelectStatement.builder(YpObjectType.STAGE, YpPayloadFormat.YSON)
                                .addSelector(Paths.ID)
                                .addSelector(Paths.SPEC)
                                .build(),
                        p -> p).thenApply(YpSelectedObjects::getResults);
        List<List<YpPayload>> lists = future.get();
        ObjectBuilderDescriptor<TSchemaMeta, SchemaMeta> builderDescriptor = new ObjectBuilderDescriptor<>(
                TStageSpec::newBuilder, TStageStatus::newBuilder, SchemaMeta::fromProto,
                TSchemaMeta.getDefaultInstance());
        Map<String, StageSpec> map = new HashMap<>();
        lists.forEach(l -> {
            TStageSpec message =
                    (TStageSpec) YTreeProtoUtils.unmarshal(payloadToYson(l.get(1)),
                            builderDescriptor.createSpecBuilder());
            StageSpec stageSpec = TestData.CONVERTER.fromProto(message);

            if (stageSpec.getDeployUnits().values().stream().anyMatch(s -> !s.getImagesForBoxes().isEmpty())) {
                map.put(payloadToYson(l.get(0)).stringValue(), stageSpec);
            }
        });

        startCheck(map);
    }

    class Attempt {
        DockerImageDescription description;
        List<String> command;
        List<String> entryPoint;
        String response;
        int attempts;
        private final CountDownLatch latch;

        Attempt(DockerImageDescription description, CountDownLatch latch) {
            this.description = description;
            this.attempts = 3;
            this.latch = latch;
        }

        public synchronized int getAttempts() {
            return attempts;
        }

        public synchronized void decrementAttempts() {
            attempts--;
            if (attempts == 0) {
                latch.countDown();
            }
        }

        public synchronized void setResult(Triple<String, List<String>, List<String>> t) {
            this.command = t.getMiddle();
            this.entryPoint = t.getRight();
            this.response = t.getLeft();
            latch.countDown();
        }

        private boolean noResult() {
            return command == null;
        }

        private boolean bad() {
            return command != null && entryPoint != null && !command.isEmpty() && !entryPoint.isEmpty();
        }

        private boolean veryBad() {
            return bad() && !sameElementsLists(command, entryPoint);
        }

        @Override
        public String toString() {
            return "description=" + String.format("name=%s&tag=%s", description.getName(), description.getTag()) +
                    ", command=" + command +
                    ", entryPoint=" + entryPoint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Attempt attempt = (Attempt) o;
            return description.equals(attempt.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(description);
        }
    }

    private void startCheck(Map<String, StageSpec> stages) throws InterruptedException {
        DefaultAsyncHttpClient httpClient = new DefaultAsyncHttpClient();
        Set<DockerImageDescription> imageDescriptions = stages.values().stream()
                .flatMap(s -> s.getDeployUnits().values().stream())
                .flatMap(du -> du.getImagesForBoxes().values().stream())
                .collect(Collectors.toSet());
        Map<DockerImageDescription, Attempt> descriptionAttemptMap = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(imageDescriptions.size());
        imageDescriptions.forEach(d -> descriptionAttemptMap.put(d, new Attempt(d, latch)));
        Semaphore semaphore = new Semaphore(20);
        for (DockerImageDescription imageDescription : imageDescriptions) {
            getDockerImages(descriptionAttemptMap, imageDescription, httpClient, semaphore);
        }

        Map<DockerImageDescription, List<String>> descriptionToStageIds = new HashMap<>();
        for (Map.Entry<String, StageSpec> stageEntry : stages.entrySet()) {
            stageEntry.getValue().getDeployUnits().values().stream()
                    .flatMap(du -> du.getImagesForBoxes().values().stream())
                    .forEach(d -> descriptionToStageIds.computeIfAbsent(d, x -> new ArrayList<>()).add(stageEntry.getKey()));
        }

        latch.await();


        descriptionAttemptMap.values().stream().filter(Attempt::noResult).forEach(a -> {
            System.out.printf("For dockerDescriptions %s, stages %s - no response\n",
                    String.format("https://dockinfo.yandex-team.ru/api/docker/resolve?registryUrl=%s/%s&tag=%s",
                            a.description.getRegistryHost(), a.description.getName(), a.description.getTag()),
                    descriptionToStageIds.get(a.description));
        });
        System.out.println();
        System.out.println("Bad configurations[command and entryPoint Both are not empty]: " + (int) descriptionAttemptMap.values().stream().filter(Attempt::bad).count());
        descriptionAttemptMap.values().stream().filter(Attempt::bad).forEach(System.out::println);
        System.out.println();
        System.out.println("Very Bad configurations[command and entryPoint Both are not empty and not equals]: " + (int) descriptionAttemptMap.values().stream().filter(Attempt::veryBad).count());
        descriptionAttemptMap.values().stream().filter(Attempt::veryBad).forEach(a -> System.out.println(a + " used " +
                "by " + descriptionToStageIds.get(a.description)));
        System.out.println();
        System.out.println("Bad Stages ");
        descriptionAttemptMap.values().stream().filter(Attempt::veryBad).forEach(a -> {
            List<String> stageIds = descriptionToStageIds.get(a.description);
            for (String id : stageIds) {
                StageSpec stageSpec = stages.get(id);
                boolean badStage = stageSpec.getDeployUnits().values().stream().flatMap(
                        du -> du.getDetails().getPodSpec().getPodAgentPayload().getSpec().getWorkloadsList().stream())
                        .anyMatch(wl -> wl.getStart().getCommandLine().isEmpty());
                if (badStage) {
                    System.out.println(id + " -> " + a);
                }
            }
        });
    }

    private void getDockerImages(Map<DockerImageDescription, Attempt> result, DockerImageDescription description,
                                 AsyncHttpClient httpClient, Semaphore semaphore) {
        if (result.get(description).getAttempts() == 0) {
            return;
        }
        result.get(description).decrementAttempts();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        get2(description, httpClient).whenComplete((p, e) -> {
            semaphore.release();
            if (p != null) {
                Attempt attempt = result.get(description);
                attempt.setResult(p);
            }
            if (e != null) {
                getDockerImages(result, description, httpClient, semaphore);
            }
        });
    }

    private boolean sameElementsLists(List<String> l1, List<String> l2) {
        if (l1.size() != l2.size()) {
            return false;
        }
        Collections.sort(l1);
        Collections.sort(l2);
        for (int i = 0; i < l1.size(); i++) {
            if (!l1.get(i).equals(l2.get(i))) {
                return false;
            }
        }
        return true;
    }


    public CompletableFuture<Triple<String, List<String>, List<String>>> get2(DockerImageDescription description,
                                                                              AsyncHttpClient httpClient) {
        try {
            String url = String.format("https://dockinfo.yandex-team.ru/api/docker/resolve?registryUrl=%s/%s&tag=%s",
                    description.getRegistryHost(), description.getName(), description.getTag());
            Request request = httpClient
                    .prepareGet(url)
                    .build();
            return httpClient.executeRequest(request).toCompletableFuture().thenApply(this::parseResponse2);
        } catch (Exception e) {
            CompletableFuture<Triple<String, List<String>, List<String>>> result = new CompletableFuture<>();
            result.completeExceptionally(e);
            return result;
        }
    }

    private Triple<String, List<String>, List<String>> parseResponse2(Response response) {
        if (response.getStatusCode() != 200) {
            throw new RuntimeException(String.format("Invalid response for docker_info request. Code %d",
                    response.getStatusCode()));
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(response.getResponseBodyAsBytes());
            JsonNode dockerConfig = node.get("dockerConfig");
            List<String> command = new ArrayList<>();
            dockerConfig.get("command").forEach(commandNode -> command.add(commandNode.textValue()));
            List<String> entryPoint = new ArrayList<>();
            dockerConfig.get("entryPoint").forEach(commandNode -> entryPoint.add(commandNode.textValue()));
            return ImmutableTriple.of(new String(response.getResponseBodyAsBytes()), command, entryPoint);
        } catch (Exception e) {
            throw new DockerInfoJSONParseException(String.format("Invalid docker_info JSON. %s", e.getMessage()));
        }
    }

    @Test
    void deployStageToYp() throws Exception {
        setTestStageSpec(repository, stageId, subjectUser, useDockerImage, cluster, transmitLogs);
    }

    @Test
    void loadTest() throws Exception {
        for (int i = 0; i < TOTAL_STAGES; ++i) {
            LOG.info("Removing {}", i);
            repository.removeObject(stageId + i).get();
        }

        List<Thread> threads = new ArrayList<>();
        for (int threadCount = 0; threadCount < WRITER_THREADS; ++threadCount) {
            int threadNumber = threadCount;
            int threadStagesCount = TOTAL_STAGES / WRITER_THREADS;
            int stageNumberStart = threadStagesCount * threadNumber;
            threads.add(new Thread(() -> {
                for (int i = 0; i < TOTAL_ACTIONS / WRITER_THREADS; ++i) {
                    if (i % 100 == 0) {
                        LOG.info("Thread {} iteration number {}", threadNumber, i);
                    }
                    try {
                        int stageNumber = ThreadLocalRandom.current().nextInt(threadStagesCount) + stageNumberStart;
                        setTestStageSpec(repository, stageId + stageNumber, subjectUser, useDockerImage, cluster,
                                transmitLogs);
                        Thread.sleep(10);
                    } catch (Exception e) {
                        LOG.error("Unhandled exception {}", ExceptionUtils.getAllMessages(e));
                    }
                }
            }));
        }
        threads.forEach(Thread::start);
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void cleanUpAfterLoadTest() throws Exception {
        for (int i = 0; i < TOTAL_STAGES; ++i) {
            LOG.info("Removing {}", i);
            repository.removeObject(stageId + i).get();
        }
    }

    @Test
    void listStages() throws Exception {
        Map<String, Try<YpObject<StageMeta, TStageSpec, TStageStatus>>> objects =
                repository.selectObjects(SPEC_STATUS_META, emptyMap()).get().getObjects();
        System.out.println("Stages:" + objects.size());

        Set<String> allUsers = new HashSet<>();

        objects.forEach((key, value) -> {
            Set<String> users = value.get().getMeta().getAcl().getEntries().stream()
                    .flatMap(item -> item.getSubjectsList().stream())
                    .filter(item -> !item.startsWith("abc:"))
                    .filter(item -> !item.equals("robot-drug-deploy"))
                    .filter(item -> !item.equals("robot-rcc"))
                    .collect(Collectors.toSet());
            if (users.isEmpty()) {
                return;
            }

            value.get().getSpec().getDeployUnitsMap().entrySet()
                    .forEach(entry -> {
                        String networkProject = entry.getValue().getNetworkDefaults().getNetworkId();
                        if (networkProject.isEmpty()) {
                            return;
                        }
                        List<YpCheckObjectPermissions> checks = users.stream()
                                .map(user -> new YpCheckObjectPermissions(new YpTypedId(networkProject,
                                        YpObjectType.NETWORK_PROJECT), user, YpAccessControlPermission.USE))
                                .collect(Collectors.toList());

                        try {
                            List<YpCheckedObjectPermissions> resultList = ypClient.checkObjectPermissions(checks).get();
                            for (int i = 0; i < checks.size(); ++i) {
                                YpCheckObjectPermissions check = checks.get(i);
                                YpCheckedObjectPermissions result = resultList.get(i);
                                if (result.getAction() == YpAccessControlAction.DENY) {
                                    System.out.println(String.format("User '%s' denied for project '%s', unit '%s', " +
                                                    "stage '%s'",
                                            check.getSubject(), check.getId().getId(), entry.getKey(), key));
                                    allUsers.add(check.getSubject());
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        });

        System.out.println("Users with insufficient permissions: " + StringUtils.joinStrings(allUsers));
    }

    void setTestStageSpec(YpObjectRepository<StageMeta, TStageSpec, TStageStatus> repository, String stageId,
                          String subjectUser, boolean useDockerImage, String cluster, boolean transmitLogs) throws Exception {
        TStageSpec spec = stageSpec(useDockerImage, cluster, transmitLogs);
        if (repository.getObject(stageId, SPEC_STATUS_META).get().isEmpty()) {
            Acl acl = new Acl(ImmutableList.of(
                    AccessControl.TAccessControlEntry.newBuilder()
                            .addAllPermissions(ImmutableList.of(
                                    AccessControl.EAccessControlPermission.ACA_WRITE,
                                    AccessControl.EAccessControlPermission.ACP_READ
                            ))
                            .addAllSubjects(ImmutableList.of(subjectUser, "robot-drug-deploy"))
                            .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                            .build()
            ));

            CreateObjectRequest<TStageSpec> request = new CreateObjectRequest.Builder<>(spec)
                    .setAcl(acl)
                    .build();
            LOG.info("Stage {} does not exist, will create", stageId);
            repository.createObject(stageId, request).get();
        } else {
            repository.updateObjectSpec(stageId, spec).get();
        }
    }

    TStageSpec stageSpec(boolean useDockerImage, String cluster, boolean transmitLogs) {
        TNetworkDefaults networkDefaults = TNetworkDefaults.newBuilder()
                .setNetworkId("_YA_DEPLOY_NETS_")
                .build();

        TDeployUnitSpec.Builder rsUnitSpec = TDeployUnitSpec.newBuilder()
                .putLogrotateConfigs(ROOT_BOX,
                        TLogrotateConfig.newBuilder().setRawConfig("Test-config\nasd\nasdasd").build())
                .setNetworkDefaults(networkDefaults)
                .setReplicaSet(TDeployUnitSpec.TReplicaSetDeploy.newBuilder()
                        .setReplicaSetTemplate(TReplicaSetSpec.newBuilder()
                                .setPodTemplateSpec(TPodTemplateSpec.newBuilder()
                                        .setSpec(podSpec(transmitLogs))
                                        .build())
                                .setDeploymentStrategy(TReplicaSetSpec.TDeploymentStrategy.newBuilder()
                                        .setMaxUnavailable(1)
                                        .build())
                                .build())
                        .putAllPerClusterSettings(ImmutableMap.of(
                                cluster, TDeployUnitSpec.TReplicaSetDeploy.TPerClusterSettings.newBuilder()
                                        .setPodCount(1)
                                        .build()
                        ))
                        .build());
        rsTvmToken.ifPresent(token -> rsUnitSpec.setTvmConfig(TestData.CONVERTER.toProto(new TvmConfig(
                TvmConfig.Mode.ENABLED, "ProdYaTeam",
                ImmutableList.of(new TvmClient(new SecretSelector(TVM_SECRET_NAME, TVM_SECRET_KEY),
                        // app ids must exist in tvm scope and a secret for source must be present in pod spec
                        new TvmApp(2014986, ""), ImmutableList.of(new TvmApp(2015014, "")), "")),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), Optional.empty(),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), Optional.empty(), false))));

        if (useDockerImage) {
            rsUnitSpec.putImagesForBoxes("docker_box", TDockerImageDescription.newBuilder()
                    .setRegistryHost("registry.yandex.net")
                    .setName("qe_iss/iss-probe")
                    .setTag("1.1.55.61849dd")
                    .build());
            rsUnitSpec.getReplicaSetBuilder().getReplicaSetTemplateBuilder().getPodTemplateSpecBuilder().getSpecBuilder()
                    .getPodAgentPayloadBuilder().getSpecBuilder()
                    .addBoxes(TBox.newBuilder()
                            .setId("docker_box")
                            .build())
                    .getWorkloadsBuilderList().get(0)
                    .setBoxRef("docker_box")
                    .addEnv(literalEnvVar("QLOUD_LOGGER_STDERR_PARSER", "line"))
                    .addEnv(literalEnvVar("QLOUD_SLB_IPV4", "NONE"))
                    .addEnv(literalEnvVar("QLOUD_LOGGER_STDERR", "true"))
                    .addEnv(literalEnvVar("QLOUD_LOGGER_STDOUT_PARSER", "linlinee"))
                    .addEnv(literalEnvVar("QLOUD_DRIVER", "porto"))
                    .addEnv(literalEnvVar("QLOUD_SLB_IPV6", "NONE"))
                    .addEnv(literalEnvVar("QLOUD_JAVA_OPTIONS", "-Xmx512m -XX:-HeapDumpOnOutOfMemoryError -Djava.net" +
                            ".preferIPv6Addresses=true -Drequester.issUrl=thrift://iss3.yandex-team.ru:9090 " +
                            "-Drequester.loopbackUrl=https://iss-probe.qloud.yandex-team.ru -Drequester" +
                            ".targetHosts=[\"sas1-3954.search.yandex.net\"] -agentlib:jdwp=transport=dt_socket," +
                            "server=y,suspend=n,address=5005"))
                    .addEnv(literalEnvVar("QLOUD_LOGGER_STDOUT", "true"))
                    .setStart(TUtilityContainer.newBuilder()
                            .setCommandLine("/bin/sh -c 'java $QLOUD_JAVA_OPTIONS -jar /runtime.jar'")
                            .build());
        }


        TStageSpec.Builder builder = TStageSpec.newBuilder()
                .putDeployUnits("RS", rsUnitSpec.build())
                // just to test mcrs, does not include any patching features
                .putDeployUnits("MCRS", TDeployUnitSpec.newBuilder()
                        .setNetworkDefaults(networkDefaults)
                        .setMultiClusterReplicaSet(TDeployUnitSpec.TMultiClusterReplicaSetDeploy.newBuilder()
                                .setReplicaSet(TMultiClusterReplicaSetSpec.newBuilder()
                                        .setPodTemplateSpec(TPodTemplateSpec.newBuilder()
                                                .setSpec(podSpec(transmitLogs))
                                                .build())
                                        .setDeploymentStrategy(TMultiClusterReplicaSetSpec.TDeploymentStrategy.newBuilder()
                                                .setMaxUnavailable(1)
                                                .build())
                                        .addAllClusters(ImmutableList.of(
                                                TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                                                        .setCluster(cluster)
                                                        .setSpec(TMultiClusterReplicaSetSpec.TReplicaSetSpecPreferences.newBuilder()
                                                                .setReplicaCount(1)
                                                                .build())
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())
                // For a local yp master, create an account via
                // ya tool yp create --address localhost:<port> --config '{enable_ssl=%false}' account
                // and put its id here.
                .setAccountId("abc:service:3494")
                .setRevision(ThreadLocalRandom.current().nextInt(100500));
        return builder.build();
    }

    DataModel.TPodSpec podSpec(boolean transmitLogs) {
        String workloadId = "StartHook";
        DataModel.TPodSpec.Builder result = DataModel.TPodSpec.newBuilder()
                .setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setSpec(TPodAgentSpec.newBuilder()
                                .setResources(TResourceGang.newBuilder()
                                        .addLayers(TLayer.newBuilder()
                                                .setId("base_layer")
                                                .setChecksum("EMPTY:")
                                                .setUrl("https://proxy.sandbox.yandex-team.ru/725288970")
                                                .build())
                                        .addLayers(TLayer.newBuilder()
                                                .setId("server6")
                                                .setChecksum("EMPTY:")
                                                .setUrl("rbtorrent:9a0884822929f02b472589abcbd7a0367eadc2bc")
                                                .build())
                                        .build())
                                .addWorkloads(TWorkload.newBuilder()
                                        .setId(workloadId)
                                        .setStart(TUtilityContainer.newBuilder()
                                                .setCommandLine("python /opt/server6.py 8000")
                                                .build())
                                        .setLivenessCheck(TLivenessCheck.newBuilder()
                                                .setContainer(TUtilityContainer.newBuilder()
                                                        .setCommandLine("bash -c 'exit 0'")
                                                        .build())
                                                .build())
                                        .setBoxRef(ROOT_BOX)
                                        .setTransmitLogs(transmitLogs)
                                        .build())
                                .addMutableWorkloads(TMutableWorkload.newBuilder()
                                        .setWorkloadRef(workloadId)
                                        .setTargetState(EWorkloadTargetState.EWorkloadTarget_ACTIVE)
                                        .build())
                                .addBoxes(TBox.newBuilder()
                                        .setId(ROOT_BOX)
                                        .setRootfs(TRootfsVolume.newBuilder()
                                                .addLayerRefs("base_layer")
                                                .addLayerRefs("server6")
                                                .build())
                                        .setBindSkynet(true)
                                        .build())
                                .build())
                        .build())
                .setResourceRequests(DataModel.TPodSpec.TResourceRequests.newBuilder()
                        .setMemoryGuarantee(PodSpecUtils.GIGABYTE)
                        .setMemoryLimit(PodSpecUtils.GIGABYTE)
                        .setVcpuGuarantee(200)
                        .setVcpuLimit(200)
                        .build())
                .addDiskVolumeRequests(DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
                        .setId("disk_allocation")
                        .setStorageClass("hdd")
                        .setQuotaPolicy(DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy.newBuilder()
                                .setCapacity(5 * PodSpecUtils.GIGABYTE)
                                .build())
                        .setLabels(TAttributeDictionary.newBuilder()
                                .addAttributes(PodSpecUtils.USED_BY_INFRA_LABEL)
                                .build())
                        .build())
                .setHostInfra(THostInfraInfo.newBuilder()
                        .setMonitoring(TMonitoringInfo.newBuilder()
                                .addUnistats(TMonitoringUnistatEndpoint.newBuilder()
                                        .setPort(8080)
                                        .setWorkloadId(workloadId)
                                        .setPath("/unistat")
                                        .build())
                                .build())
                        .build());
        // https://yav.yandex-team.ru/secret/sec-01dhkkd895q5ay4338dyagem43/explore/versions
        rsTvmToken.ifPresent(token -> result.putSecrets(TVM_SECRET_NAME, DataModel.TPodSpec.TSecret.newBuilder()
                .setSecretId("sec-01dhkkd895q5ay4338dyagem43")
                .setSecretVersion("ver-01dm08p6wczedjegc98qntgzj9")
                .setDelegationToken(token)
                .build()));
        return result.build();
    }
}
