package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class QuotaChangeRequestServicePropertyValidationTest extends BaseQuotaRequestTest {

    @Autowired
    private CampaignDao campaignDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
    }

    @Test
    public void quotaChangeRequestCanBeCreatedWithAdditionalProperties() {
        prepareCampaignResources();
        final Map<String, String> properties = new HashMap<String, String>() {{
            put("segment", "default");
        }};

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2), DiAmount.of(10L, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .additionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F);
        final DiQuotaChangeRequest createdRequest = requests.getFirst();

        assertEquals(createdRequest.getAdditionalProperties(), properties);

        updateHierarchy();

        final long id = createdRequest.getId();
        final DiQuotaChangeRequest request = dispenser().quotaChangeRequests().byId(id).get().perform();

        assertEquals(request.getAdditionalProperties(), properties);

        // Test update of additional properties

        properties.put("segment", "dev");

        final DiQuotaChangeRequest updatedRequest = dispenser().quotaChangeRequests()
                .byId(id)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .additionalProperties(properties)
                        .build())
                .performBy(AMOSOV_F);

        assertEquals(updatedRequest.getAdditionalProperties(), properties);

        final DiQuotaChangeRequest requestAfterUpdate = dispenser().quotaChangeRequests().byId(id).get().perform();

        assertEquals(requestAfterUpdate.getAdditionalProperties(), properties);
    }


    private void createService(final String key) {
        if (Hierarchy.get().getServiceReader().contains(key)) {
            return;
        }
        final Service ydb = serviceDao.create(Service.withKey(key)
                .withName(key)
                .withAbcServiceId(YDB_ABC_SERVICE_ID)
                .build());
        updateHierarchy();
        resourceDao.create(new Resource.Builder(STORAGE, ydb)
                .name("Storage")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.DEFAULT)
                .build());
        updateHierarchy();
    }

    private DiQuotaChangeRequest createRequestWithProperties(final String serviceKey, final Map<String, String> properties) {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        return dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(serviceKey, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .replaceAdditionalProperties(properties)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();
    }

    public void requestShouldContainAdditionalProperties(final String serviceKey, final String propertyKey) {
        createService(serviceKey);
        prepareCampaignResources();

        assertThrowsWithMessage(() -> createRequestWithProperties(serviceKey, Collections.emptyMap()),
                serviceKey + " request should contain additional properties '" + propertyKey + "'");

        final Map<String, String> properties = Collections.singletonMap(propertyKey, "My DB");

        final DiQuotaChangeRequest request = createRequestWithProperties(serviceKey, properties);

        assertThrowsWithMessage(() -> dispenser().quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .additionalProperties(Collections.emptyMap())
                        .build())
                .performBy(AMOSOV_F), serviceKey + " request should contain additional properties '" + propertyKey + "'");
    }

    @Test
    public void ydbRequestShouldContainAdditionalPropertyDatabaseName() {
        requestShouldContainAdditionalProperties(YDB, "databaseName");
    }

    @Test
    public void logbrokerRequestShouldContainAdditionalProperties() {
        requestShouldContainAdditionalProperties("logbroker", "account");
    }

    @Test
    public void ytRequestShouldNotContainUnknownAdditionalProperties() {
        createService("yt");
        prepareCampaignResources();
        assertThrowsWithMessage(() -> createRequestWithProperties("yt", ImmutableMap.of("foo", "bar")),
                "yt request can't contain properties 'foo'");

        createRequestWithProperties("yt", Collections.emptyMap());
        createRequestWithProperties("yt", ImmutableMap.of("pools", "42"));
        createRequestWithProperties("yt", ImmutableMap.of("accounts", "42"));
        createRequestWithProperties("yt", ImmutableMap.of("tabletCellBundles", "42"));
    }

    @Test
    public void sqsRequestShouldContainAdditionalProperties() {
        requestShouldContainAdditionalProperties("sqs", "account");
    }

    @Test
    public void qloudRequestShouldContainAdditionalPropertiesInstallationAndSegment() {
        createService(QLOUD);
        prepareCampaignResources();
        assertThrowsWithMessage(() -> createRequestWithProperties(QLOUD, Collections.emptyMap()),
                "qloud request should contain additional properties 'installation, segment, project'");
    }

    @Test
    public void qloudRequestShouldAllowOnlyCorrectSegmentInstallationPair() {
        createService(QLOUD);
        prepareCampaignResources();

        assertThrowsWithMessage(() -> createRequestWithProperties(QLOUD, ImmutableMap.of("installation", "qloud", "segment", "42", "project", "Yandex")),
                "Qloud request has unknown segment '42' for installation 'qloud'");

        assertThrowsWithMessage(() -> createRequestWithProperties(QLOUD, ImmutableMap.of("installation", "43", "segment", "april", "project", "Yandex")),
                "Qloud request has unknown installation '43'");

        final DiQuotaChangeRequest response = createRequestWithProperties(QLOUD, ImmutableMap.of("installation", "qloud-ext", "segment", "sox", "project", "Yandex"));
        assertEquals(ImmutableMap.of("installation", "qloud-ext", "segment", "sox", "project", "Yandex"), response.getAdditionalProperties());
    }

    @Test
    public void qloudRequestShouldContainsProject() {
        createService(QLOUD);

        prepareCampaignResources();
        assertThrowsWithMessage(() -> createRequestWithProperties(QLOUD, ImmutableMap.of("installation", "qloud", "segment", "42")),
                "qloud request should contain additional properties 'project'");
    }

    @Test
    public void nirvanaRequestShouldContainAdditionalProperties() {
        requestShouldContainAdditionalProperties(NIRVANA, "project");
    }

    @Test
    public void propertyValidationFailsOnEmptyValueString() {
        createService(NIRVANA);

        createService(YT);
        prepareCampaignResources();
        assertThrowsWithMessage(() -> createRequestWithProperties(NIRVANA, Collections.singletonMap("project", "")),
                "nirvana request should contain not empty additional properties 'project'");


        assertThrowsWithMessage(() -> createRequestWithProperties(YT, ImmutableMap.<String, String>builder()
                        .put("pools", "").put("accounts", "").put("tabletCellBundles", "some1").build()),
                "yt request should contain not empty additional properties 'pools, accounts");

        assertThrowsWithMessage(() -> createRequestWithProperties(YT, ImmutableMap.<String, String>builder()
                        .put("pools", "   ").put("accounts", "").put("tabletCellBundles", "some1").build()),
                "yt request should contain not empty additional properties 'pools, accounts'");
    }

    @Test
    public void propertyValidationFailsOnNullValueString() {
        createService(NIRVANA);
        createService(YT);
        prepareCampaignResources();

        assertThrowsWithMessage(() -> createRequestWithProperties(NIRVANA, Collections.singletonMap("project", null)),
                "nirvana request should contain not empty additional properties 'project'");


        final Map<String, String> map = new HashMap<>();
        map.put("pools", "");
        map.put("accounts", null);
        map.put("tabletCellBundles", "some1");

        assertThrowsWithMessage(() -> createRequestWithProperties(YT, map),
                "yt request should contain not empty additional properties 'pools, accounts");
    }

    @Test
    public void solomonRequestShouldContainAdditionalProjectProperty() {
        createService(SOLOMON);
        prepareCampaignResources();
        assertThrowsWithMessage(() -> createRequestWithProperties(SOLOMON, Collections.emptyMap()),
                "solomon request should contain additional properties 'project'");
    }

    @Test
    public void solomonRequestShouldNotContainUnknownAdditionalProperties() {
        createService(SOLOMON);
        prepareCampaignResources();
        assertThrowsWithMessage(() -> createRequestWithProperties(SOLOMON, ImmutableMap.of("foo", "bar")),
                "solomon request can't contain properties 'foo'");
    }

    @Test
    public void solomonRequestShouldContainOnlyValidProjects() {
        createService(SOLOMON);
        prepareCampaignResources();
        assertThrowsWithMessage(() -> createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "~test")),
                "Solomon quota request has wrong value for 'project' property." +
                        " Valid format is 'value1, value2, value3' where each value is a valid solomon project id");
        assertThrowsWithMessage(() -> createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "~test,-some")),
                "Solomon quota request has wrong value for 'project' property." +
                        " Valid format is 'value1, value2, value3' where each value is a valid solomon project id");
        assertThrowsWithMessage(() -> createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "test,")),
                "Solomon quota request has wrong value for 'project' property." +
                        " Valid format is 'value1, value2, value3' where each value is a valid solomon project id");
        assertThrowsWithMessage(() -> createRequestWithProperties(SOLOMON, ImmutableMap.of("project", ",test")),
                "Solomon quota request has wrong value for 'project' property." +
                        " Valid format is 'value1, value2, value3' where each value is a valid solomon project id");
        assertThrowsWithMessage(() -> createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "")),
                "solomon request should contain not empty additional properties 'project'");
        final HashMap<String, String> nullMap = new HashMap<>();
        nullMap.put("project", null);
        assertThrowsWithMessage(() -> createRequestWithProperties(SOLOMON, nullMap),
                "solomon request should contain not empty additional properties 'project'");
    }

    @Test
    public void solomonRequestWithValidProjects() {
        createService(SOLOMON);
        prepareCampaignResources();
        createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "test"));
        createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "test,some"));
        createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "test, some"));
        createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "test,  some"));
        createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "test-data, some-project"));
        createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "test_project"));
        createRequestWithProperties(SOLOMON, ImmutableMap.of("project", "test,some,more"));
    }

}
