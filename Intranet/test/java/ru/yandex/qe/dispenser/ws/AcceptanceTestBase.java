package ru.yandex.qe.dispenser.ws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.qe.bus.test.AbstractBusJUnit5SpringContextTests;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaType;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.DiYandexGroupType;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.application.startup.InitialState;
import ru.yandex.qe.dispenser.client.v1.DiOAuthToken;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.impl.DispenserConfig;
import ru.yandex.qe.dispenser.client.v1.impl.SpyDispenserFactory;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignResource;
import ru.yandex.qe.dispenser.domain.EntitySpec;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.PersonAffiliation;
import ru.yandex.qe.dispenser.domain.PersonGroupMembership;
import ru.yandex.qe.dispenser.domain.PersonGroupMembershipType;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.ProjectRole;
import ru.yandex.qe.dispenser.domain.ProjectServiceMeta;
import ru.yandex.qe.dispenser.domain.Quota;
import ru.yandex.qe.dispenser.domain.QuotaSpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceGroup;
import ru.yandex.qe.dispenser.domain.ResourceSegmentation;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.YaGroup;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignResourceDao;
import ru.yandex.qe.dispenser.domain.dao.entity.spec.EntitySpecDao;
import ru.yandex.qe.dispenser.domain.dao.group.GroupDao;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao;
import ru.yandex.qe.dispenser.domain.dao.quota.QuotaDao;
import ru.yandex.qe.dispenser.domain.dao.quota.spec.QuotaSpecDao;
import ru.yandex.qe.dispenser.domain.dao.resource.group.ResourceGroupDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dictionaries.impl.FrontDictionariesManager;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.HierarchySupplier;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.domain.hierarchy.Session;
import ru.yandex.qe.dispenser.domain.index.LongIndexBase;
import ru.yandex.qe.dispenser.domain.util.ExRunnable;
import ru.yandex.qe.dispenser.standalone.MockAbcApi;
import ru.yandex.qe.dispenser.standalone.MockTrackerManager;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;

@ContextConfiguration({"classpath:spring/application-test-ctx.xml"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AcceptanceTestBase extends AbstractBusJUnit5SpringContextTests {
    private static final Log LOG = LogFactory.getLog(AcceptanceTestBase.class);

    protected static final String ZOMB_MOBSEARCH = "zomb-mobsearch";
    protected static final String ROBOT_DISPENSER = "robot-dispenser";

    protected static final DiPerson AMOSOV_F = DiPerson.login("amosov-f");
    protected static final DiPerson WHISTLER = DiPerson.login("whistler");
    protected static final DiPerson SANCHO = DiPerson.login("sancho");
    protected static final DiPerson TERRY = DiPerson.login("terry");
    protected static final DiPerson LYADZHIN = DiPerson.login("lyadzhin");
    protected static final DiPerson QDEEE = DiPerson.login("qdeee");
    protected static final DiPerson STERLIGOVAK = DiPerson.login("sterligovak");
    protected static final DiPerson SLONNN = DiPerson.login("slonnn");
    protected static final DiPerson BINARY_CAT = DiPerson.login("binarycat");
    protected static final DiPerson LOTREK = DiPerson.login("lotrek");
    protected static final DiPerson AGODIN = DiPerson.login("agodin");
    protected static final DiPerson WELVET = DiPerson.login("welvet");
    protected static final DiPerson DM_TIM = DiPerson.login("dm-tim");
    protected static final DiPerson AQRU = DiPerson.login("aqru");
    protected static final DiPerson ILLYUSION = DiPerson.login("illyusion");
    protected static final DiPerson VEGED = DiPerson.login("veged");
    protected static final DiPerson STARLIGHT = DiPerson.login("starlight");
    protected static final DiPerson BENDYNA = DiPerson.login("bendyna");
    protected static final DiPerson KEYD = DiPerson.login("keyd");

    protected static final String YANDEX = "yandex";
    protected static final String INFRA = "infra";
    protected static final String VERTICALI = "verticals";
    protected static final String DEFAULT = "default";
    protected static final String SEARCH = "search";
    protected static final String INFRA_SPECIAL = "infra-special";

    protected static final String NIRVANA = "nirvana";
    protected static final String STORAGE = "storage";
    protected static final String YT_CPU = "yt-cpu";
    protected static final String YT_GPU = "yt-gpu";
    protected static final String YT_FILE = "yt-file";

    protected static final String YT = "yt";

    protected static final String LOGFELLER = "logfeller";

    protected static final String DISTBUILD = "distbuild";

    protected static final String YP = "yp";
    protected static final String SEGMENT_CPU = "segment-cpu";
    protected static final String SEGMENT_STORAGE = "segment-storage";
    protected static final String SEGMENT_HDD = "segment-hdd";
    protected static final String YP_HDD_FILE = "yp-hdd-file";
    protected static final String GENCFG = "gencfg";
    protected static final String GENCFG_SEGMENT_CPU = "gc-segment-hdd";
    protected static final String MDB = "mdb";
    protected static final String YDB = "ydb";
    protected static final String SQS = "sqs";
    protected static final String MDS = "mds";
    protected static final String QLOUD = "qloud";
    protected static final String SOLOMON = "solomon";
    protected static final String SAAS = "saas";
    protected static final String STRM = "strm";
    protected static final String LOGBROKER = "logbroker";
    protected static final String RTMR_PROCESSING = "rtmr_processing";
    protected static final String RTMR_MIRROR = "rtmr_mirror";
    protected static final String SANDBOX = "sandbox";

    protected static final String SCRAPER = "scraper";
    protected static final String DOWNLOADS = "downloads";
    protected static final String DOWNLOADS_PER_HOUR = "downloads_per_hour";
    protected static final String DOWNLOADS_PER_DAY = "downloads_per_day";
    protected static final String TASK = "task";
    protected static final String TELLURIUM_PARALLEL = "tellurium-parallel";


    protected static final String CLUSTER_API = "cluster-api";
    protected static final String COMPUTER = "computer";
    protected static final String CPU = "cpu";
    protected static final String RAM = "ram";
    protected static final String HDD = "hdd";
    protected static final String SSD = "ssd";
    protected static final String NET = "net";
    protected static final String WORKLOAD = "workload";

    protected static final DiEntity UNIT_ENTITY = DiEntity.withKey("pool").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.anyOf(DiUnit.BYTE)).build();
    protected static final DiEntityUsage SINGLE_USAGE_UNIT_ENTITY = DiEntityUsage.singleOf(UNIT_ENTITY);

    public static final Integer TEST_ABC_SERVICE_ID = 123;
    protected static final Integer NIRVANA_ABC_SERVICE_ID = 961;
    protected static final Integer CLUSTER_API_ABC_SERVICE_ID = 1783;
    protected static final Integer SCRAPER_ABC_SERVICE_ID = 1534;
    protected static final Integer YP_ABC_SERVICE_ID = 1979;
    protected static final Integer GENCFG_ABC_SERVICE_ID = 1175;
    protected static final Integer MDB_ABC_SERVICE_ID = 2660;
    protected static final Integer YDB_ABC_SERVICE_ID = 3303;
    protected static final Integer MDS_ABC_SERVICE_ID = 895;
    protected static final Integer QLOUD_ABC_SERVICE_ID = 741;
    protected static final Integer SAAS_ABC_SERVICE_ID = 664;
    protected static final Integer LOGBROKER_ABC_SERVICE_ID = 679;
    protected static final Integer RTMR_PROCESSING_ABC_SERVICE_ID = 586;
    protected static final Integer RTMR_MIRROR_ABC_SERVICE_ID = 586;
    protected static final Integer SANDBOX_ABC_SERVICE_ID = 469;

    protected static final String DC_SEGMENTATION = "dc";
    protected static final String SEGMENT_SEGMENTATION = "segment_segmentations";
    protected static final String DC_SEGMENT_1 = "dc_1";
    protected static final String DC_SEGMENT_2 = "dc_2";
    protected static final String DC_SEGMENT_3 = "dc_3";
    protected static final String SEGMENT_SEGMENT_1 = "ss_1";
    protected static final String SEGMENT_SEGMENT_2 = "ss_2";
    protected static final String GENCFG_SEGMENT_SEGMENTATION = "gc_segment_segmentations";
    protected static final String GENCFG_SEGMENT_SEGMENT_1 = "gc_ss_1";
    protected static final String GENCFG_SEGMENT_SEGMENT_2 = "gc_ss_2";

    protected static final String RESOURCE_GROUP_STORAGE = "storages";
    protected static final String RESOURCE_GROUP_GRAPHS = "graphs";

    // TODO: use API for creation and removing
    @Autowired
    @Deprecated
    protected ProjectDao projectDao;
    @Autowired
    @Deprecated
    protected QuotaSpecDao quotaSpecDao;
    @Autowired
    @Deprecated
    protected EntitySpecDao entitySpecDao;
    @Autowired
    protected GroupDao groupDao;
    @Autowired
    protected CampaignDao campaignDao;
    @Autowired
    protected FrontDictionariesManager frontDictionariesManager;
    @Autowired
    protected CampaignResourceDao campaignResourceDao;

    @Autowired
    protected HierarchySupplier hierarchy;

    @Autowired
    protected MockAbcApi abcApi;

    @Autowired
    private MockTrackerManager trackerManager;

    @NotNull
    private final SpyDispenserFactory webClientFactory;
    @NotNull
    private final Dispenser dispenser;


    protected AcceptanceTestBase() {
        webClientFactory = createWebClientFactory(DispenserConfig.Environment.DEVELOPMENT);
        dispenser = webClientFactory.get();
    }

    @NotNull
    protected SpyDispenserFactory createWebClientFactory(@NotNull final DispenserConfig.Environment env) {
        final DiOAuthToken token = DiOAuthToken.of(ZOMB_MOBSEARCH);
        final String host = System.getProperty("dispenser.client.host");
        return new SpyDispenserFactory(host, token, env, () -> applicationContext);
    }

    @BeforeEach
    public void setUp() {
        reinitialize();
        updateHierarchy();
        trackerManager.clearIssues();
    }

    protected void reinitialize() {
        disableHierarchy();
        Assertions.assertEquals(200, createLocalClient().path("/admin/reinitialize").get().getStatus());
    }

    public static class SetUp extends InitialState {

        private Project yandex;
        private Project defaultProject;
        private Project infra;
        private Project verticali;
        private Project search;
        private Project infraSpecial;

        @Autowired
        private QuotaDao quotaDao;

        @Autowired
        protected SegmentDao segmentDao;

        @Autowired
        protected ResourceSegmentationDao resourceSegmentationDao;

        @Autowired
        protected SegmentationDao segmentationDao;

        @Autowired
        protected ResourceGroupDao resourceGroupDao;

        @Override
        public void configure() throws DuplicateKeyException {
            configureProjectRoles();
            configureUsers();
            configureGroups();
            configureProjects();
            configureAdmins();

            configureStateForNirvana();
            configureStateForClusterAPI();
            configureStateForScraper();
            configureStateForYPAndGenCfg();
            configureStateForDb();

            configureServiceTrusties();
        }

        private void configureProjectRoles() {
            projectRoleDao.create(new ProjectRole(Role.RESPONSIBLE.getKey(), ProjectRole.AbcRoleSyncType.NONE, null));
            projectRoleDao.create(new ProjectRole(Role.MEMBER.getKey(), ProjectRole.AbcRoleSyncType.NONE, null));
            projectRoleDao.create(new ProjectRole(Role.PROCESS_RESPONSIBLE.getKey(), ProjectRole.AbcRoleSyncType.NONE, null));
            projectRoleDao.create(new ProjectRole(Role.STEWARD.getKey(), ProjectRole.AbcRoleSyncType.RESPONSIBLE, null));
            projectRoleDao.create(new ProjectRole(Role.VS_LEADER.getKey(), ProjectRole.AbcRoleSyncType.ROLE, 1L));
            projectRoleDao.create(new ProjectRole(Role.QUOTA_MANAGER.getKey(), ProjectRole.AbcRoleSyncType.ROLE, 1553L));
            projectRoleDao.create(new ProjectRole(Role.RESOURCE_ORDER_MANAGER.getKey(), ProjectRole.AbcRoleSyncType.ROLE, 7163L));
        }

        private void configureUsers() {
            personDao.create(new Person(WHISTLER.getLogin(), 1120000000022037L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(LYADZHIN.getLogin(), 1120000000014351L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(AMOSOV_F.getLogin(), 1120000000022901L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(ZOMB_MOBSEARCH, 1120000000017942L, true, false, false, PersonAffiliation.EXTERNAL));
            personDao.create(new Person(SANCHO.getLogin(), 1120000000006106L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(STERLIGOVAK.getLogin(), 1120000000010239L, false, true, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(TERRY.getLogin(), 1120000000000194L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(SLONNN.getLogin(), 1120000000048006L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(LOTREK.getLogin(), 1120000000024781L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(BINARY_CAT.getLogin(), 1120000000011199L, false, true, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(QDEEE.getLogin(), 1120000000015512L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(WELVET.getLogin(), 1120000000025633L, false, true, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(DM_TIM.getLogin(), 1120000000016633L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(AQRU.getLogin(), 1120000000185973L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(ILLYUSION.getLogin(), 1120000000136164L, false, true, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(VEGED.getLogin(), 1120000000000087L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(STARLIGHT.getLogin(), 1120000000008384L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(ROBOT_DISPENSER, 1120000000128362L, true, false, false, PersonAffiliation.EXTERNAL));
            personDao.create(new Person(BENDYNA.getLogin(), 1120000000017427L, false, true, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(AGODIN.getLogin(), 1120000000001589L, false, false, false, PersonAffiliation.YANDEX));
            personDao.create(new Person(KEYD.getLogin(), 1120000000001493L, false, false, false, PersonAffiliation.YANDEX));
        }

        private void configureGroups() {
            groupDao.create(new YaGroup("yandex_infra_tech_tools_st_dev", DiYandexGroupType.DEPARTMENT, 49800L, false));
            final YaGroup yandexGroup = groupDao.create(new YaGroup("yandex", DiYandexGroupType.DEPARTMENT, 962L, false));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(WHISTLER.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(LYADZHIN.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(AMOSOV_F.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(SANCHO.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(STERLIGOVAK.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(TERRY.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(SLONNN.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(LOTREK.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(BINARY_CAT.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(QDEEE.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(WELVET.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(DM_TIM.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(AQRU.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(ILLYUSION.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(VEGED.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(STARLIGHT.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(BENDYNA.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
            personGroupMembershipDao.create(new PersonGroupMembership(personDao.readPersonByLogin(AGODIN.getLogin()), yandexGroup,
                    PersonGroupMembershipType.DEPARTMENT_ANCESTORS));
        }

        private void configureProjects() {
            yandex = projectDao.createIfAbsent(Project.withKey(YANDEX).name("Yandex").build());
            defaultProject = projectDao.createIfAbsent(Project.withKey(DEFAULT).name("Default").description("Default project for all Yandex users").parent(yandex).build());
            infra = projectDao.create(Project.withKey(INFRA).name("Infrastruktura").parent(yandex).build());
            verticali = projectDao.create(Project.withKey(VERTICALI).name("Verticali").parent(yandex).build());
            search = projectDao.create(Project.withKey(SEARCH).name("Search").parent(yandex).build());
            infraSpecial = projectDao.create(Project.withKey(INFRA_SPECIAL).name("Special infra projects").parent(search).build());
            Project dispenser = projectDao.create(Project.withKey("dispenser").name("Dispenser").parent(yandex).build());
            Project seeHardwareMoney = projectDao.create(Project.withKey("seehardwaremoney").name("See hardware money").parent(yandex).build());

            final Person whistler = personDao.readPersonByLogin(WHISTLER.getLogin());
            final Person lyadzhin = personDao.readPersonByLogin(LYADZHIN.getLogin());
            final Person keyd = personDao.readPersonByLogin(KEYD.getLogin());
            final Person amosovf = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            final Person zombMobsearch = personDao.readPersonByLogin(ZOMB_MOBSEARCH);

            projectDao.attach(keyd, yandex, Role.PROCESS_RESPONSIBLE);
            projectDao.attach(whistler, yandex, Role.RESPONSIBLE);
            projectDao.attach(lyadzhin, yandex, Role.MEMBER);
            projectDao.attach(groupDao.readYaGroupByUrl("yandex"), defaultProject, Role.MEMBER);
            projectDao.attach(amosovf, dispenser, Role.MEMBER);
            projectDao.attach(keyd, dispenser, Role.MEMBER);
            projectDao.attach(zombMobsearch, dispenser, Role.MEMBER);
            projectDao.attach(amosovf, seeHardwareMoney, Role.MEMBER);
            projectDao.attach(keyd, seeHardwareMoney, Role.MEMBER);
            projectDao.attach(zombMobsearch, seeHardwareMoney, Role.MEMBER);
        }

        private void configureAdmins() {
            final Person amosovf = personDao.readPersonByLogin(AMOSOV_F.getLogin());
            dispenserAdminsDao.setDispenserAdmins(Collections.singleton(amosovf));
        }


        private void configureServiceTrusties() {
            final Person zombMobsearch = personDao.readPersonByLogin(ZOMB_MOBSEARCH);
            for (final Service service : serviceDao.getAll()) {
                serviceDao.attachTrustee(service, zombMobsearch);
            }
        }

        private void configureStateForNirvana() {
            final Service nirvana = serviceDao.create(Service
                    .withKey(NIRVANA)
                    .withName("Nirvana")
                    .withAbcServiceId(NIRVANA_ABC_SERVICE_ID)
                    .withSettings(Service.Settings.builder()
                            .manualQuotaAllocation(true)
                            .build())
                    .build());
            final Person sancho = personDao.readPersonByLogin(SANCHO.getLogin());
            serviceDao.attachAdmin(nirvana, sancho);

            final ResourceGroup storageResourceGroup = resourceGroupDao.create(new ResourceGroup.Builder(RESOURCE_GROUP_STORAGE, nirvana)
                    .name("Nirvana: Storage")
                    .build());

            resourceGroupDao.create(new ResourceGroup.Builder(RESOURCE_GROUP_GRAPHS, nirvana)
                    .name("Nirvana: Graphs")
                    .build());

            projectDao.putProjectMeta(ProjectServiceMeta.emtpy(infra, nirvana));

            final Resource storage = resourceDao.create(new Resource.Builder(STORAGE, nirvana)
                    .name("Storage")
                    .type(DiResourceType.STORAGE)
                    .mode(DiQuotingMode.ENTITIES_ONLY)
                    .group(storageResourceGroup)
                    .build());
            final QuotaSpec storageQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder("storage", storage)
                    .description("Nirvana storage quota.")
                    .build());

            final EntitySpec nirvanaYtFile = EntitySpec.builder()
                    .withKey(YT_FILE)
                    .withDescription("Файлы в YT")
                    .overResource(storage)
                    .build();
            entitySpecDao.create(nirvanaYtFile);

            setMax(ImmutableMap.of(
                    Quota.Key.totalOf(storageQuotaSpec, yandex), 250L,
                    Quota.Key.totalOf(storageQuotaSpec, infra), 100L
            ));

            final Resource ytCpu = resourceDao.create(new Resource.Builder(YT_CPU, nirvana)
                    .name("YT CPU")
                    .type(DiResourceType.ENUMERABLE)
                    .mode(DiQuotingMode.SYNCHRONIZATION)
                    .build());
            final QuotaSpec ytCpuQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(YT_CPU, ytCpu)
                    .description("YT CPU quota")
                    .build());
            setMax(ImmutableMap.of(
                    Quota.Key.totalOf(ytCpuQuotaSpec, yandex), 100000L,
                    Quota.Key.totalOf(ytCpuQuotaSpec, infra), 40000L,
                    Quota.Key.totalOf(ytCpuQuotaSpec, search), 40000L,
                    Quota.Key.totalOf(ytCpuQuotaSpec, verticali), 20000L,
                    Quota.Key.totalOf(ytCpuQuotaSpec, infraSpecial), 5000L
            ));

            final Resource ytGpu = resourceDao.create(new Resource.Builder(YT_GPU, nirvana)
                    .name("YT GPU")
                    .type(DiResourceType.ENUMERABLE)
                    .mode(DiQuotingMode.SYNCHRONIZATION)
                    .build());
            final QuotaSpec ytGpuQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(YT_GPU, ytGpu)
                    .description("YT GPU quota")
                    .build());
            setMax(ImmutableMap.of(
                    Quota.Key.totalOf(ytGpuQuotaSpec, yandex), 100000L,
                    Quota.Key.totalOf(ytGpuQuotaSpec, infra), 40000L,
                    Quota.Key.totalOf(ytGpuQuotaSpec, search), 40000L,
                    Quota.Key.totalOf(ytGpuQuotaSpec, verticali), 20000L
            ));
        }


        private void configureStateForClusterAPI() {
            final Service clusterApi = serviceDao.create(Service.withKey(CLUSTER_API).withName("ClusterAPI").withAbcServiceId(CLUSTER_API_ABC_SERVICE_ID).build());
            final Person sterligovak = personDao.readPersonByLogin(STERLIGOVAK.getLogin());
            serviceDao.attachAdmin(clusterApi, sterligovak);

            final Resource computer = resourceDao.create(new Resource.Builder(COMPUTER, clusterApi)
                    .name("Computing resources")
                    .type(DiResourceType.MONEY)
                    .build());
            final QuotaSpec computerQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder("computer", computer)
                    .type(DiQuotaType.ABSOLUTE)
                    .description("Cluster API computing power in currency")
                    .build());
            setMax(ImmutableMap.of(
                    Quota.Key.totalOf(computerQuotaSpec, yandex), 50L,
                    Quota.Key.totalOf(computerQuotaSpec, infra), 30L,
                    Quota.Key.totalOf(computerQuotaSpec, defaultProject), 20L
            ));


            final Resource cpu = resourceDao.create(new Resource.Builder(CPU, clusterApi)
                    .name("CPU")
                    .type(DiResourceType.POWER)
                    .mode(DiQuotingMode.ENTITIES_ONLY)
                    .build());
            final Resource ram = resourceDao.create(new Resource.Builder(RAM, clusterApi)
                    .name("RAM")
                    .type(DiResourceType.MEMORY)
                    .mode(DiQuotingMode.ENTITIES_ONLY)
                    .build());
            final Resource hdd = resourceDao.create(new Resource.Builder(HDD, clusterApi)
                    .name("HDD")
                    .type(DiResourceType.MEMORY)
                    .mode(DiQuotingMode.ENTITIES_ONLY)
                    .build());
            final Resource net = resourceDao.create(new Resource.Builder(NET, clusterApi)
                    .name("NET")
                    .type(DiResourceType.TRAFFIC)
                    .mode(DiQuotingMode.ENTITIES_ONLY)
                    .build());
            final Resource ssd = resourceDao.create(new Resource.Builder(SSD, clusterApi)
                    .name("SSD")
                    .type(DiResourceType.MEMORY)
                    .mode(DiQuotingMode.SYNCHRONIZATION)
                    .build());

            quotaSpecDao.create(new QuotaSpec.Builder(CPU, cpu).description("CPU quota").build());
            quotaSpecDao.create(new QuotaSpec.Builder(RAM, ram).description("RAM quota").build());
            quotaSpecDao.create(new QuotaSpec.Builder(HDD, hdd).description("HDD quota").build());
            quotaSpecDao.create(new QuotaSpec.Builder(NET, net).description("NET quota").build());
            final QuotaSpec ssdQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(SSD, ssd).description("SSD quota").build());

            setMax(ImmutableMap.of(
                    Quota.Key.totalOf(ssdQuotaSpec, yandex), 50L,
                    Quota.Key.totalOf(ssdQuotaSpec, infra), 30L,
                    Quota.Key.totalOf(ssdQuotaSpec, defaultProject), 20L
            ));

            final EntitySpec workload = EntitySpec.builder()
                    .withKey(WORKLOAD)
                    .withDescription("Cluster computional tasks")
                    .overResource(cpu)
                    .overResource(ram)
                    .overResource(hdd)
                    .overResource(net)
                    .build();
            entitySpecDao.create(workload);
        }

        private void configureStateForScraper() {
            final Service scraper = serviceDao.create(Service.withKey(SCRAPER).withName("Scraper").withAbcServiceId(SCRAPER_ABC_SERVICE_ID).build());
            final Person terry = personDao.readPersonByLogin(TERRY.getLogin());
            serviceDao.attachAdmin(scraper, terry);

            final Resource downloads = resourceDao.create(new Resource.Builder(DOWNLOADS, scraper)
                    .name("Downloads")
                    .type(DiResourceType.ENUMERABLE)
                    .build());
            final QuotaSpec downloadsPerHourQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(DOWNLOADS_PER_HOUR, downloads)
                    .type(DiQuotaType.PERCENT)
                    .description("Scraper per hour downloads")
                    .build());
            final QuotaSpec downloadsPerDayQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(DOWNLOADS_PER_DAY, downloads)
                    .type(DiQuotaType.PERCENT)
                    .description("Scraper per day downloads")
                    .build());
            setMax(ImmutableMap.of(
                    Quota.Key.totalOf(downloadsPerHourQuotaSpec, yandex), 10000L,
                    Quota.Key.totalOf(downloadsPerHourQuotaSpec, infra), 1000L,
                    Quota.Key.totalOf(downloadsPerHourQuotaSpec, verticali), 5000L
            ));
            setMax(ImmutableMap.of(
                    Quota.Key.totalOf(downloadsPerDayQuotaSpec, yandex), 75000L,
                    Quota.Key.totalOf(downloadsPerDayQuotaSpec, infra), 9000L,
                    Quota.Key.totalOf(downloadsPerDayQuotaSpec, verticali), 45000L
            ));

            final Resource telluriumParallel = resourceDao.create(new Resource.Builder(TELLURIUM_PARALLEL, scraper)
                    .name("Задания в Tellurium")
                    .type(DiResourceType.ENUMERABLE)
                    .mode(DiQuotingMode.ENTITIES_ONLY)
                    .build());
            quotaSpecDao.create(new QuotaSpec.Builder(TELLURIUM_PARALLEL + "-quota", telluriumParallel).description("").build());
            final EntitySpec scraperTask = EntitySpec.builder()
                    .withKey(TASK)
                    .withDescription("Саббатч")
                    .overResource(telluriumParallel)
                    .expirable(true)
                    .build();
            entitySpecDao.create(scraperTask);
        }

        private void configureStateForYPAndGenCfg() {
            final Service.Settings settings = Service.Settings.builder()
                    .accountActualValuesInQuotaDistribution(true)
                    .requireZeroQuotaUsageForProjectDeletion(true)
                    .build();
            final Service yp = serviceDao.create(Service.withKey(YP)
                    .withName("YP")
                    .withAbcServiceId(YP_ABC_SERVICE_ID)
                    .withSettings(settings)
                    .build());
            final Person slonnn = personDao.readPersonByLogin(SLONNN.getLogin());
            serviceDao.attachAdmin(yp, slonnn);
            serviceDao.attachAdmin(yp, personDao.readPersonByLogin(ZOMB_MOBSEARCH));

            final Segmentation dcSegmentation = segmentationDao.create(new Segmentation.Builder(DC_SEGMENTATION)
                    .name("Data Centers")
                    .description("Data centers segmentation")
                    .build());

            final Segmentation anotherSegmentation = segmentationDao.create(new Segmentation.Builder(SEGMENT_SEGMENTATION)
                    .name("Another segmentations")
                    .description("")
                    .build());

            final Segment dcSegment1 = segmentDao.create(new Segment.Builder(DC_SEGMENT_1, dcSegmentation)
                    .name("LOCATION_1")
                    .description("Data center in location 1")
                    .priority((short) 1)
                    .build());

            final Segment dcSegment2 = segmentDao.create(new Segment.Builder(DC_SEGMENT_2, dcSegmentation)
                    .name("LOCATION_2")
                    .description("Data center in location 2")
                    .priority((short) 1)
                    .build());

            final Segment dcSegment3 = segmentDao.create(new Segment.Builder(DC_SEGMENT_3, dcSegmentation)
                    .name("LOCATION_3")
                    .description("Data center in location 3")
                    .priority((short) 1)
                    .build());

            final Segment totalDcSegment = Segment.totalOf(dcSegmentation);

            final Segment segmentSegment1 = segmentDao.create(new Segment.Builder(SEGMENT_SEGMENT_1, anotherSegmentation)
                    .name("Segment1")
                    .description("Segment1")
                    .priority((short) 1)
                    .build());

            final Segment segmentSegment2 = segmentDao.create(new Segment.Builder(SEGMENT_SEGMENT_2, anotherSegmentation)
                    .name("Segment2")
                    .description("Segment2")
                    .priority((short) 1)
                    .build());

            final Segment totalSegmentSegment = Segment.totalOf(anotherSegmentation);

            final Resource segmentCpu = resourceDao.create(new Resource.Builder(SEGMENT_CPU, yp)
                    .name("Segment CPU")
                    .type(DiResourceType.PROCESSOR)
                    .mode(DiQuotingMode.DEFAULT)
                    .build());

            resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                    segmentCpu, dcSegmentation
            ).build());

            resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                    segmentCpu, anotherSegmentation
            ).build());

            final QuotaSpec segmentCpuSpec = quotaSpecDao.create(new QuotaSpec.Builder(SEGMENT_CPU, segmentCpu)
                    .description("Segment CPU quota")
                    .build());

            setMax(ImmutableMap.<Quota.Key, Long>builder()
                    .put(Quota.Key.totalOf(segmentCpuSpec, yandex), 100000L)
                    .put(new Quota.Key(segmentCpuSpec, yandex, Sets.newHashSet(dcSegment1, segmentSegment1)), 70000L)
                    .put(new Quota.Key(segmentCpuSpec, yandex, Sets.newHashSet(dcSegment2, segmentSegment1)), 30000L)
                    .put(new Quota.Key(segmentCpuSpec, yandex, Sets.newHashSet(dcSegment1, totalSegmentSegment)), 70000L)
                    .put(new Quota.Key(segmentCpuSpec, yandex, Sets.newHashSet(dcSegment2, totalSegmentSegment)), 30000L)
                    .put(new Quota.Key(segmentCpuSpec, yandex, Sets.newHashSet(totalDcSegment, segmentSegment1)), 100000L)
                    .put(Quota.Key.totalOf(segmentCpuSpec, search), 40000L)
                    .put(Quota.Key.totalOf(segmentCpuSpec, verticali), 20000L)
                    .put(new Quota.Key(segmentCpuSpec, infra, Sets.newHashSet(dcSegment1, segmentSegment1)), 30000L)
                    .put(new Quota.Key(segmentCpuSpec, infra, Sets.newHashSet(dcSegment2, segmentSegment1)), 10000L)
                    .put(new Quota.Key(segmentCpuSpec, infra, Sets.newHashSet(dcSegment1, totalSegmentSegment)), 30000L)
                    .put(new Quota.Key(segmentCpuSpec, infra, Sets.newHashSet(dcSegment2, totalSegmentSegment)), 10000L)
                    .put(new Quota.Key(segmentCpuSpec, infra, Sets.newHashSet(totalDcSegment, segmentSegment1)), 40000L)
                    .put(Quota.Key.totalOf(segmentCpuSpec, infra), 40000L)
                    .build()
            );

            final Resource segmentStorage = resourceDao.create(new Resource.Builder(SEGMENT_STORAGE, yp)
                    .name("Segment Storage")
                    .type(DiResourceType.MEMORY)
                    .mode(DiQuotingMode.SYNCHRONIZATION)
                    .build());

            resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                    segmentStorage, dcSegmentation
            ).build());

            resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                    segmentStorage, anotherSegmentation
            ).build());

            final QuotaSpec segmentStorageSpec = quotaSpecDao.create(new QuotaSpec.Builder(SEGMENT_STORAGE, segmentStorage)
                    .description("Segment Storage quota")
                    .build());

            setMax(ImmutableMap.<Quota.Key, Long>builder()
                    .put(Quota.Key.totalOf(segmentStorageSpec, yandex), 1024L)
                    .put(new Quota.Key(segmentStorageSpec, yandex, Sets.newHashSet(dcSegment1, segmentSegment1)), 256L)
                    .put(new Quota.Key(segmentStorageSpec, yandex, Sets.newHashSet(dcSegment2, segmentSegment1)), 768L)
                    .put(new Quota.Key(segmentStorageSpec, yandex, Sets.newHashSet(dcSegment1, totalSegmentSegment)), 256L)
                    .put(new Quota.Key(segmentStorageSpec, yandex, Sets.newHashSet(dcSegment2, totalSegmentSegment)), 768L)
                    .put(new Quota.Key(segmentStorageSpec, yandex, Sets.newHashSet(totalDcSegment, segmentSegment1)), 1024L)
                    .put(Quota.Key.totalOf(segmentStorageSpec, verticali), 512L)
                    .put(new Quota.Key(segmentStorageSpec, verticali, Sets.newHashSet(dcSegment1, segmentSegment1)), 256L)
                    .put(new Quota.Key(segmentStorageSpec, verticali, Sets.newHashSet(dcSegment2, segmentSegment1)), 256L)
                    .put(new Quota.Key(segmentStorageSpec, verticali, Sets.newHashSet(dcSegment1, totalSegmentSegment)), 256L)
                    .put(new Quota.Key(segmentStorageSpec, verticali, Sets.newHashSet(dcSegment2, totalSegmentSegment)), 256L)
                    .put(new Quota.Key(segmentStorageSpec, verticali, Sets.newHashSet(totalDcSegment, segmentSegment1)), 512L)
                    .build()
            );

            final Resource segmentHdd = resourceDao.create(new Resource.Builder(SEGMENT_HDD, yp)
                    .name("Segment Storage")
                    .type(DiResourceType.MEMORY)
                    .mode(DiQuotingMode.ENTITIES_ONLY)
                    .build());

            resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                    segmentHdd, dcSegmentation
            ).build());
            resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                    segmentHdd, anotherSegmentation
            ).build());

            quotaSpecDao.create(new QuotaSpec.Builder(SEGMENT_HDD, segmentHdd)
                    .description("Segment HDD quota")
                    .build());

            final EntitySpec ypHddFile = EntitySpec.builder()
                    .withKey(YP_HDD_FILE)
                    .withDescription("Файлы на диске в YP")
                    .overResource(segmentHdd)
                    .build();
            entitySpecDao.create(ypHddFile);

            final Service.Settings genCfgSettings = Service.Settings.builder()
                    .accountActualValuesInQuotaDistribution(true)
                    .requireZeroQuotaUsageForProjectDeletion(true)
                    .build();

            final Service gencfg = serviceDao.create(Service.withKey(GENCFG)
                    .withName("GenCfg")
                    .withAbcServiceId(GENCFG_ABC_SERVICE_ID)
                    .withSettings(genCfgSettings)
                    .build());

            final Segmentation gcSegmentSegmentation = segmentationDao.create(new Segmentation.Builder(GENCFG_SEGMENT_SEGMENTATION)
                    .name("Gencfg segment segmentations")
                    .description("")
                    .build());

            final Segment gcSegmentSegment1 = segmentDao.create(new Segment.Builder(GENCFG_SEGMENT_SEGMENT_1, gcSegmentSegmentation)
                    .name("Gencfg Segment1")
                    .description("Gencfg Segment1")
                    .priority((short) 1)
                    .build());

            final Segment gcSegmentSegment2 = segmentDao.create(new Segment.Builder(GENCFG_SEGMENT_SEGMENT_2, gcSegmentSegmentation)
                    .name("Gencfg Segment2")
                    .description("Gencfg Segment2")
                    .priority((short) 1)
                    .build());

            final Resource gcSegmentCpu = resourceDao.create(new Resource.Builder(GENCFG_SEGMENT_CPU, gencfg)
                    .name("Gencfg Segment CPU")
                    .type(DiResourceType.PROCESSOR)
                    .mode(DiQuotingMode.SYNCHRONIZATION)
                    .build());

            resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                    gcSegmentCpu, dcSegmentation
            ).build());

            resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                    gcSegmentCpu, gcSegmentSegmentation
            ).build());

            final QuotaSpec gcSegmentCpuSpec = quotaSpecDao.create(new QuotaSpec.Builder(GENCFG_SEGMENT_CPU, gcSegmentCpu)
                    .description("Gencfg Segment CPU quota")
                    .build());
        }

        private void configureStateForDb() {
            final Service mdb = serviceDao.create(Service.withKey(MDB).withName("MDB").withAbcServiceId(MDB_ABC_SERVICE_ID).build());

            final Resource cpu = resourceDao.create(new Resource.Builder(CPU, mdb)
                    .name("CPU")
                    .type(DiResourceType.PROCESSOR)
                    .mode(DiQuotingMode.DEFAULT)
                    .build());

            final QuotaSpec cpuQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(CPU, cpu)
                    .description("MDB CPU quota")
                    .build());

            final Resource ram = resourceDao.create(new Resource.Builder(RAM, mdb)
                    .name("RAM")
                    .type(DiResourceType.MEMORY)
                    .mode(DiQuotingMode.DEFAULT)
                    .build());

            final QuotaSpec ramQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(RAM, ram)
                    .description("MDB RAM quota")
                    .build());

            final Service.Settings mdsSettings = Service.Settings.builder()
                    .usesProjectHierarchy(false)
                    .build();
            final Service mds = serviceDao.create(Service.withKey(MDS).withName("MDS").withSettings(mdsSettings).withAbcServiceId(MDS_ABC_SERVICE_ID).build());
            serviceDao.attachAdmin(mds, personDao.readPersonByLogin(AGODIN.getLogin()));

            final Resource hdd = resourceDao.create(new Resource.Builder(HDD, mds)
                    .name("HDD")
                    .type(DiResourceType.STORAGE)
                    .mode(DiQuotingMode.SYNCHRONIZATION)
                    .build());

            final QuotaSpec hddQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(HDD, hdd)
                    .description("MDS HDD quota")
                    .build());
        }

        private void configureStateForMds() {
            final Service mdb = serviceDao.create(Service.withKey(MDB).withName("MDB").withAbcServiceId(MDB_ABC_SERVICE_ID).build());

            final Resource cpu = resourceDao.create(new Resource.Builder(CPU, mdb)
                    .name("CPU")
                    .type(DiResourceType.PROCESSOR)
                    .mode(DiQuotingMode.DEFAULT)
                    .build());

            final QuotaSpec cpuQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(CPU, cpu)
                    .description("MDB CPU quota")
                    .build());

            final Resource ram = resourceDao.create(new Resource.Builder(RAM, mdb)
                    .name("RAM")
                    .type(DiResourceType.MEMORY)
                    .mode(DiQuotingMode.DEFAULT)
                    .build());

            final QuotaSpec ramQuotaSpec = quotaSpecDao.create(new QuotaSpec.Builder(RAM, ram)
                    .description("MDB RAM quota")
                    .build());
        }


        @NotNull
        private void setMax(@NotNull final Map<Quota.Key, Long> maxValues) {
            final Set<Quota.Key> keys = maxValues.keySet();

            final Map<Quota.Key, Long> ownMaxValues = keys.stream()
                    .filter(key -> key.getProject().isRealLeaf())
                    .collect(Collectors.toMap(Function.identity(), maxValues::get));

            quotaDao.applyChanges(maxValues, Collections.emptyMap(), ownMaxValues);
        }

    }

    @Override
    protected WebClient createLocalClient() {
        return webClientFactory.createConfiguredWebClient();
    }

    protected WebClient createAuthorizedLocalClient(@NotNull final String requestPerformerLogin, final String type) {
        return createLocalClient()
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + requestPerformerLogin)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(type);
    }

    protected WebClient createAuthorizedLocalClient(@NotNull final String requestPerformerLogin) {
        return createLocalClient()
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + requestPerformerLogin)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected WebClient createAuthorizedLocalClient(@NotNull final DiPerson requestPerformer) {
        return createAuthorizedLocalClient(requestPerformer.getLogin());
    }

    protected WebClient createAuthorizedLocalClient(@NotNull final DiPerson requestPerformer, final String type) {
        return createAuthorizedLocalClient(requestPerformer.getLogin(), type);
    }

    @NotNull
    protected Dispenser dispenser() {
        return dispenser;
    }

    protected void updateHierarchy() {
        hierarchy.update();
    }

    @Deprecated
    protected void disableHierarchy() {
        hierarchy.reset();
    }

    protected void runInTransaction(@NotNull final ExRunnable action) {
        TransactionWrapper.INSTANCE.execute(action);
    }


    protected void prepareCampaignResources() {
        clearDictionaryCache();
        TransactionWrapper.INSTANCE.execute(() -> {
            final Set<Resource> resources = Hierarchy.get().getResourceReader().getAll();
            final Set<Campaign> campaigns = campaignDao.getAll();
            campaigns.forEach(campaign -> {
                final List<Long> bigOrders = campaign.getBigOrders().stream()
                        .map(LongIndexBase::getId).collect(Collectors.toList());
                resources.forEach(resource -> {
                    final List<Segmentation> segmentations = Hierarchy.get()
                            .getResourceSegmentationReader().getResourceSegmentations(resource)
                            .stream().map(ResourceSegmentation::getSegmentation).collect(Collectors.toList());
                    final List<CampaignResource.Segmentation> segmentationsSettings = new ArrayList<>();
                    segmentations.forEach(segmentation -> {
                        final Set<Segment> segments = Hierarchy.get().getSegmentReader().get(segmentation);
                        segmentationsSettings.add(new CampaignResource.Segmentation(segmentation.getId(),
                                segments.stream().map(LongIndexBase::getId).collect(Collectors.toList())));
                    });
                    final List<CampaignResource.SegmentsBigOrders> segmentBigOrders = prepareSegmentBigOrders(segmentations, bigOrders);
                    final CampaignResource campaignResource = new CampaignResource(campaign.getId(), resource.getId(),
                            false, false, null, new CampaignResource.Settings(segmentationsSettings,
                            bigOrders, segmentBigOrders));
                    Session.ERROR.remove();
                    campaignResourceDao.getByResourceId(campaignResource.getResourceId()).stream()
                            .filter(r -> r.getCampaignId() == campaignResource.getCampaignId()).forEach(r -> campaignResourceDao.delete(r));
                    campaignResourceDao.create(campaignResource);
                });
            });
        });
    }

    protected List<CampaignResource.SegmentsBigOrders> prepareSegmentBigOrders(final List<Segmentation> segmentations,
                                                                             final List<Long> bigOrders) {
        final List<CampaignResource.SegmentsBigOrders> result = new ArrayList<>();
        prepareSegmentsRecursive(result, segmentations, bigOrders, 0, Collections.emptyList());
        return result;
    }

    private void prepareSegmentsRecursive(final List<CampaignResource.SegmentsBigOrders> result,
                                          final List<Segmentation> segmentations,
                                          final List<Long> bigOrders,
                                          final int segmentationIndex,
                                          final List<Long> segmentsList) {
        if (segmentationIndex < segmentations.size()) {
            final Segmentation segmentation = segmentations.get(segmentationIndex);
            final Set<Segment> segments = Hierarchy.get().getSegmentReader().get(segmentation);
            segments.forEach(segment -> {
                final List<Long> newSegmentsList = new ArrayList<>(segmentsList);
                newSegmentsList.add(segment.getId());
                prepareSegmentsRecursive(result, segmentations, bigOrders, segmentationIndex + 1, newSegmentsList);
            });
        } else {
            result.add(new CampaignResource.SegmentsBigOrders(segmentsList, bigOrders));
        }
    }

    @AfterEach
    public void clearDictionaryCache() {
        frontDictionariesManager.clearCache();
    }
}
