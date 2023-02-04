package ru.yandex.qe.dispenser.ws.logic;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSetAmountResult;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignUpdate;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.ProjectRole;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.delivery.DeliveryDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceMappingDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.resources_model.ResourcesModelMappingDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentReader;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceReader;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.domain.resources_model.ExternalResource;
import ru.yandex.qe.dispenser.domain.resources_model.InternalResource;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDelivery;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDeliveryContext;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDeliveryResolveStatus;
import ru.yandex.qe.dispenser.domain.resources_model.ResourceModelMappingTarget;
import ru.yandex.qe.dispenser.domain.resources_model.ResourcesModelMapping;
import ru.yandex.qe.dispenser.standalone.MockDApi;
import ru.yandex.qe.dispenser.ws.bot.Provider;
import ru.yandex.qe.dispenser.ws.quota.request.SetResourceAmountBody;
import ru.yandex.qe.dispenser.ws.reqbody.SetResourceAmountBodyOptional;
import ru.yandex.qe.hitman.tvm.TvmConstants;
import ru.yandex.startrek.client.model.CommentCreate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest.Status.COMPLETED;
import static ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest.Status.CONFIRMED;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class ResourcePreorderRequestAllocationTest extends BaseResourcePreorderTest {

    @Autowired
    private ResourceMappingDao resourceMappingDao;
    @Autowired
    private SegmentationDao segmentationDao;
    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Value("${abc.resource.yp.resourceType}")
    private Long resourceType;
    @Autowired
    private ResourcesModelMappingDao resourcesModelMappingDao;
    @Autowired
    private MockDApi mockDApi;
    @Autowired
    private DeliveryDao deliveryDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentDao segmentDao;
    private Campaign aggregatedCampaign;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        resourceMappingDao.clear();
        mockDApi.reset();
        deliveryDao.clear();
        aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("test-aggregated")
                .setName("test-aggregated")
                .setType(Campaign.Type.AGGREGATED).build());
    }

    @AfterEach
    public void reset() {
        mockDApi.reset();
    }

    public static Object[][] quotaKeysBuilders() {
        return new Object[][]{
                {Role.QUOTA_MANAGER}, {Role.STEWARD}
        };
    }

    @NotNull
    private DiQuotaChangeRequest.ProviderAllocationInfo allocatableProviderInfo(String key) {
        return new DiQuotaChangeRequest.ProviderAllocationInfo(key, Set.of(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA), List.of(), false, false);
    }
    @NotNull
    private DiQuotaChangeRequest.ProviderAllocationInfo allocatableProviderInfo(String key, boolean allocating, String... notes) {
        return new DiQuotaChangeRequest.ProviderAllocationInfo(key, Set.of(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA), List.of(notes), allocating, false);
    }


    @NotNull
    private DiQuotaChangeRequest.ProviderAllocationInfo notAllocatableProviderInfo(String key, boolean allocating, String... notes) {
        return new DiQuotaChangeRequest.ProviderAllocationInfo(key, Set.of(), Arrays.asList(notes), allocating,
                false);
    }

    @MethodSource("quotaKeysBuilders")
    @ParameterizedTest
    public void userWithRoleShouldCanSendReadyForAllocationRequest(final Role requesterRole) {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final QuotaChangeRequest.BigOrder requestBigOrder = new QuotaChangeRequest.BigOrder(bigOrder.getBigOrderId(), bigOrder.getDate(), true);
        final Service service = serviceDao.read(SQS);

        final Service updatedService = Service.withKey(service.getKey())
                .withName(service.getName())
                .withPriority(service.getPriority())
                .withAbcServiceId(service.getAbcServiceId())
                .withId(service.getId())
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(service.getSettings().accountActualValuesInQuotaDistribution())
                        .usesProjectHierarchy(service.getSettings().usesProjectHierarchy())
                        .manualQuotaAllocation(true)
                        .build())
                .build();

        serviceDao.update(updatedService);
        sqsYtCpu = new Resource.Builder(sqsYtCpu.getKey().getPublicKey(), updatedService)
                .id(sqsYtCpu.getId())
                .name(sqsYtCpu.getName())
                .description(sqsYtCpu.getDescription())
                .type(sqsYtCpu.getType())
                .mode(sqsYtCpu.getMode())
                .group(sqsYtCpu.getGroup())
                .priority(sqsYtCpu.getPriority())
                .build();
        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .project(Hierarchy.get().getProjectReader().read(DEFAULT))
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder().order(requestBigOrder).resource(sqsYtCpu).segments(Collections.emptySet()).amount(15_000).build()
                )));

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, new SetResourceAmountBody(ImmutableList.of(
                        new SetResourceAmountBody.Item(requestId, null, ImmutableList.of(
                                new SetResourceAmountBody.ChangeBody(SQS, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT), null)
                        ), "")
                )), DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);


        assertThrowsForbiddenWithMessage(() -> {
            createAuthorizedLocalClient(BINARY_CAT)
                    .path("/v1/resource-preorder/" + requestId + "/request-allocation")
                    .post(null, DiQuotaChangeRequest.class);
        }, "Only request author or quota manager can allocate quota");


        final ProjectRole role = roleDao.getAll().stream()
                .filter(r -> r.getKey().equals(requesterRole.getKey())).findFirst().get();
        final Hierarchy hierarchyImpl = this.hierarchy.get();
        projectDao.attachAll(
                Collections.singleton(hierarchyImpl.getPersonReader().readPersonByLogin(BINARY_CAT.getLogin())),
                Collections.emptySet(),
                hierarchyImpl.getProjectReader().read(YANDEX),
                role.getId());

        updateHierarchy();

        final DiQuotaChangeRequest newRequest = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/" + requestId + "/request-allocation")
                .post(null, DiQuotaChangeRequest.class);
    }


    @Test
    public void allocationRequestShouldAffectTicketForSolomonRequests() {
        final DiQuotaChangeRequest request = getDiQuotaChangeRequest(Provider.SOLOMON.getServiceKey(), QLOUD_ABC_SERVICE_ID);

        final Map<String, Object> issueFields = trackerManager.getIssueFields(Objects.requireNonNull(request.getTrackerIssueKey()));
        final String responsible = "guschin";

        assertEquals(responsible, issueFields.get("assignee"));
        assertTrue(Arrays.asList((Object[]) issueFields.get("followers")).contains(AMOSOV_F.getLogin()));

        final List<CommentCreate> issueComments = trackerManager.getIssueComments(request.getTrackerIssueKey());

        final CommentCreate comment = Iterables.getLast(issueComments);
        assertEquals(Collections.emptyList(), comment.getSummonees());
        assertEquals("staff:amosov-f запросил выдачу квоты\n" +
                "Провайдер: solomon\n" +
                "#|\n" +
                "||  | Storage ||\n" +
                "|| Январь 2020 | 512 KiB ||\n" +
                "|#\n" +
                "\n" +
                "<{Техническая информация для кого:guschin:\n" +
                "После выдачи квоты в Solomon это надо будет отметить в самой заявке:\n" +
                "%%curl --request PATCH --url https://dispenser-dev.yandex-team.ru/api/v1/resource-preorder/quotaState \n" +
                "--header 'accept: application/json' \n" +
                "--header 'authorization: OAuth ***' \n" +
                "--header 'content-type: application/json' \n" +
                "--data '{\"updates\":[{\"requestId\":" + request.getId() + ",\"ticketKey\":null,\"changes\":[{\"serviceKey\":\"solomon\",\"bigOrderId\":" + bigOrderOne.getId() + ",\"resourceKey\":\"storage\",\"segmentKeys\":[],\"amountReady\":null,\"amountAllocated\":{\"value\":524288,\"unit\":\"BYTE\"}}],\"comment\":\"\"}]}'%%\n" +
                "Если всё пройдёт успешно, сюда придёт робот и сообщит о том, что квота выдана}>", comment.getComment().get());
    }

    @Test
    public void allocationRequestShouldAffectTicketForYtRequests() {
        final DiQuotaChangeRequest request = getDiQuotaChangeRequest(Provider.YT.getServiceKey(), QLOUD_ABC_SERVICE_ID);

        final Map<String, Object> issueFields = trackerManager.getIssueFields(Objects.requireNonNull(request.getTrackerIssueKey()));
        final String responsible = "andozer";

        assertEquals(responsible, issueFields.get("assignee"));
        assertTrue(Arrays.asList((Object[]) issueFields.get("followers")).contains(AMOSOV_F.getLogin()));

        final List<CommentCreate> issueComments = trackerManager.getIssueComments(request.getTrackerIssueKey());

        final CommentCreate comment = Iterables.getLast(issueComments);
        assertEquals(Collections.singletonList(responsible), comment.getSummonees());
        assertEquals(comment.getComment().get(), "staff:amosov-f запросил выдачу квоты\n" +
                "Провайдер: yt\n" +
                "#|\n" +
                "||  | Storage ||\n" +
                "|| Январь 2020 | 512 KiB ||\n" +
                "|#\n" +
                "\n" +
                "<{Техническая информация для кого:andozer:\n" +
                "После выдачи квоты в YT это надо будет отметить в самой заявке:\n" +
                "%%curl --request PATCH --url https://dispenser-dev.yandex-team.ru/api/v1/resource-preorder/quotaState \n" +
                "--header 'accept: application/json' \n" +
                "--header 'authorization: OAuth ***' \n" +
                "--header 'content-type: application/json' \n" +
                "--data '{\"updates\":[{\"requestId\":" + request.getId() + ",\"ticketKey\":null,\"changes\":[{\"serviceKey\":\"yt\",\"bigOrderId\":" + bigOrderOne.getId() + ",\"resourceKey\":\"storage\",\"segmentKeys\":[],\"amountReady\":null,\"amountAllocated\":{\"value\":524288,\"unit\":\"BYTE\"}}],\"comment\":\"\"}]}'%%\n" +
                "Если всё пройдёт успешно, сюда придёт робот и сообщит о том, что квота выдана}>");
    }

    @Test
    public void allocationRequestShouldAffectTicketForRTMRRequests() {
        final DiQuotaChangeRequest request = getDiQuotaChangeRequest(Provider.RTMR.getServiceKey(), GENCFG_ABC_SERVICE_ID);

        final Map<String, Object> issueFields = trackerManager.getIssueFields(Objects.requireNonNull(request.getTrackerIssueKey()));
        final String responsible = "slonnn";

        assertEquals(responsible, issueFields.get("assignee"));
        assertTrue(Arrays.asList((Object[]) issueFields.get("followers")).contains(AMOSOV_F.getLogin()));

        final List<CommentCreate> issueComments = trackerManager.getIssueComments(request.getTrackerIssueKey());

        final CommentCreate comment = Iterables.getLast(issueComments);
        assertEquals(Collections.singletonList(responsible), comment.getSummonees());
        assertEquals(comment.getComment().get(), "staff:amosov-f запросил выдачу квоты\n" +
                "Провайдер: rtmr\n" +
                "#|\n" +
                "||  | Storage ||\n" +
                "|| Январь 2020 | 512 KiB ||\n" +
                "|#\n" +
                "\n" +
                "<{Техническая информация для кого:slonnn:\n" +
                "После выдачи квоты в RTMR это надо будет отметить в самой заявке:\n" +
                "%%curl --request PATCH --url https://dispenser-dev.yandex-team.ru/api/v1/resource-preorder/quotaState \n" +
                "--header 'accept: application/json' \n" +
                "--header 'authorization: OAuth ***' \n" +
                "--header 'content-type: application/json' \n" +
                "--data '{\"updates\":[{\"requestId\":" + request.getId() + ",\"ticketKey\":null,\"changes\":[{\"serviceKey\":\"rtmr\",\"bigOrderId\":" + bigOrderOne.getId() + ",\"resourceKey\":\"storage\",\"segmentKeys\":[],\"amountReady\":null,\"amountAllocated\":{\"value\":524288,\"unit\":\"BYTE\"}}],\"comment\":\"\"}]}'%%\n" +
                "Если всё пройдёт успешно, сюда придёт робот и сообщит о том, что квота выдана}>");
    }

    private DiQuotaChangeRequest getDiQuotaChangeRequest(final String serviceKey, final Integer abcServiceId) {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        Service service = getService(serviceKey, abcServiceId);

        serviceDao.attachAdmin(service, personDao.read(LOTREK.getLogin()));

        final Resource storage = resourceDao.create(new Resource.Builder(STORAGE, service)
                .name("Storage")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.DEFAULT)
                .build());
        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .changes(Collections.singletonList(QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(storage)
                        .order(bigOrder)
                        .segments(Collections.emptySet())
                        .amount(1024 * 1024).build()))
        );

        DiQuotaChangeRequest changeRequest = dispenser()
                .quotaChangeRequests()
                .byId(requestId)
                .createTicket()
                .performBy(AMOSOV_F);

        waitForComment(changeRequest.getTrackerIssueKey(), 0);

        final DiSetAmountResult updateReadyResult = createAuthorizedLocalClient(LOTREK)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, new SetResourceAmountBody(ImmutableList.of(
                        new SetResourceAmountBody.Item(requestId, null, ImmutableList.of(
                                new SetResourceAmountBody.ChangeBody(service.getKey(), bigOrder.getBigOrderId(), STORAGE, Collections.emptySet(), DiAmount.of(512, DiUnit.KIBIBYTE), null)
                        ), "")
                )), DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, updateReadyResult);
        waitForComment(changeRequest.getTrackerIssueKey(), 1);

        DiQuotaChangeRequest post = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/request-allocation")
                .post(null, DiQuotaChangeRequest.class);
        waitForComment(changeRequest.getTrackerIssueKey(), 2);
        return post;
    }

    @NotNull
    private Service getService(String serviceKey, Integer abcServiceId) {
        try {
            Service service = serviceDao.read(serviceKey);

            serviceDao.update(Service.copyOf(service)
                    .withSettings(Service.Settings.builder()
                            .manualQuotaAllocation(true)
                            .build())
                    .build());
        } catch (EmptyResultDataAccessException e) {
            serviceDao.create(Service.withKey(serviceKey)
                    .withName(serviceKey)
                    .withAbcServiceId(abcServiceId)
                    .withSettings(Service.Settings.builder()
                            .manualQuotaAllocation(true)
                            .build())
                    .build());
        }
        updateHierarchy();

        return serviceDao.read(serviceKey);
    }

    private Resource createResource(Service service, String key, DiResourceType type) {
        final Resource resource = resourceDao.create(new Resource.Builder(key, service)
                .name(key)
                .type(type)
                .mode(DiQuotingMode.DEFAULT)
                .build());
        return resource;
    }

    private void waitForComment(String trackerIssueKey, Integer expected) {
        assertNotNull(trackerIssueKey);
        for (int i = 0; i < 1000; i++) {
            if (trackerManager.getVersion(trackerIssueKey).equals(expected)) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }

        throw new IllegalStateException("Tracker job takes too long.");
    }

    @Test
    public void providerAdminCanSetAllocatedResourceInUnconfirmedRequestsForSpecialProviders() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        Service logbroker = getService(Provider.LOGBROKER.getServiceKey(), 1);
        Service rtmr = getService(Provider.RTMR.getServiceKey(), 2);
        Service rtmrMirror = getService(Provider.RTMR_MIRROR.getServiceKey(), 3);
        Service rtmrProcessing = getService(Provider.RTMR_PROCESSING.getServiceKey(), 4);

        List<Resource> resources = new ArrayList<>();
        for (Service service : List.of(logbroker, rtmr, rtmrMirror, rtmrProcessing)) {
            serviceDao.attachAdmin(service, personDao.read(LOTREK.getLogin()));
            Resource resource = createResource(service, "storage", DiResourceType.STORAGE);
            resources.add(resource);
        }
        updateHierarchy();

        List<QuotaChangeRequest.Change> changes = resources.stream()
                .map(resource -> QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(resource)
                        .order(bigOrder)
                        .segments(Collections.emptySet())
                        .amount(1024 * 1024)
                        .build())
                .collect(Collectors.toList());

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.READY_FOR_REVIEW)
                .changes(changes)
        );

        DiQuotaChangeRequest changeRequest = dispenser()
                .quotaChangeRequests()
                .byId(requestId)
                .createTicket()
                .performBy(AMOSOV_F);

        waitForComment(changeRequest.getTrackerIssueKey(), 0);

        QuotaChangeRequest request = requestDao.read(requestId);

        int c = 0;
        for (QuotaChangeRequest.Status status : EnumSet.of(QuotaChangeRequest.Status.READY_FOR_REVIEW,
                QuotaChangeRequest.Status.APPROVED, QuotaChangeRequest.Status.CONFIRMED)) {
            requestDao.update(request.copyBuilder()
                    .status(status)
                    .build());

            List<SetResourceAmountBody.ChangeBody> bodies = new ArrayList<>();
            for (Resource resource : resources) {
                bodies.add(new SetResourceAmountBody.ChangeBody(resource.getService().getKey(), bigOrder.getBigOrderId(),
                        resource.getPublicKey(), Collections.emptySet(),
                        DiAmount.of(512 + c, DiUnit.KIBIBYTE), DiAmount.of(512 + c, DiUnit.KIBIBYTE)));
            }
            DiSetAmountResult updateReadyResult = null;
            try {
                updateReadyResult = createAuthorizedLocalClient(LOTREK)
                        .path("/v1/resource-preorder/quotaState")
                        .invoke(HttpMethod.PATCH, new SetResourceAmountBody(ImmutableList.of(
                                new SetResourceAmountBody.Item(requestId, null, bodies, "")
                        )), DiSetAmountResult.class);
            } catch (Exception e) {
                fail("Error for request status " + status, e);
            }
            assertEquals(DiSetAmountResult.SUCCESS, updateReadyResult);
            c++;
        }
        waitForComment(changeRequest.getTrackerIssueKey(), c);
    }

    @Test
    public void providerAdminCanCompleteUnconfirmedRequestsForSpecialProviders() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        Service logbroker = getService(Provider.LOGBROKER.getServiceKey(), 1);

        serviceDao.attachAdmin(logbroker, personDao.read(LOTREK.getLogin()));
        Resource resource = createResource(logbroker, "storage", DiResourceType.STORAGE);
        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.READY_FOR_REVIEW)
                .changes(List.of(QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(resource)
                        .order(bigOrder)
                        .segments(Collections.emptySet())
                        .amount(1024 * 1024)
                        .build()))
        );

        DiQuotaChangeRequest changeRequest = dispenser()
                .quotaChangeRequests()
                .byId(requestId)
                .createTicket()
                .performBy(AMOSOV_F);

        waitForComment(changeRequest.getTrackerIssueKey(), 0);

        DiSetAmountResult updateReadyResult = createAuthorizedLocalClient(LOTREK)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, new SetResourceAmountBodyOptional(List.of(
                        new SetResourceAmountBodyOptional.Item(requestId, null, null, null)),
                        SetResourceAmountBodyOptional.UpdateFor.BOTH), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS, updateReadyResult);

        waitForComment(changeRequest.getTrackerIssueKey(), 2);
        changeRequest = dispenser()
                .quotaChangeRequests()
                .byId(requestId)
                .get()
                .perform();

        assertEquals(COMPLETED, changeRequest.getStatus());
    }

    @Test
    public void nonQloudAndNonGencfgResourcesCantBeManualAllocatedInYp() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final QuotaChangeRequest.BigOrder requestBigOrder = new QuotaChangeRequest.BigOrder(bigOrder.getBigOrderId(), bigOrder.getDate(), true);

        final String serviceKey = "saas";

        final Service service = serviceDao.create(Service.withKey(serviceKey)
                .withName(serviceKey)
                .withAbcServiceId(1)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 2)
                .build());

        final Resource resource = resourceDao.create(new Resource.Builder(CPU, service)
                .name("CPU")
                .type(DiResourceType.PROCESSOR)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(resource, ImmutableSet.of(locations));
        createProject("custom", YANDEX, WHISTLER.getLogin());

        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .project(Hierarchy.get().getProjectReader().read("custom"))
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder)
                                .resource(resource)
                                .segments(Collections.singleton(sas))
                                .amount(15_000)
                                .amountReady(10_000)
                                .build()
                )));

        dispenser().quotaChangeRequests()
                .byId(requestId)
                .createTicket()
                .performBy(AMOSOV_F);

        final Set<DiQuotaChangeRequest.Permission> permissions = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class).getPermissions();

        assertFalse(permissions.contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA_IN_YP));
        assertTrue(permissions.contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/request-allocation")
                .post(null, DiQuotaChangeRequest.class);
    }

    @Test
    public void newStyleYpResourcesCanBeAllocatedThroughFormApi() {
        Service yp = serviceDao.read("yp");

        serviceDao.update(Service.copyOf(yp)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        final Segmentation ypSegmentation = segmentationDao.create(new Segmentation.Builder("ypSegment")
                .name("ypSegment")
                .description("ypSegment")
                .build());

        updateHierarchy();

        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 2)
                .build());

        final Segment dev = segmentDao.create(new Segment.Builder("dev", ypSegmentation)
                .name("dev")
                .description("dev")
                .priority((short) 2)
                .build());

        final Resource cpu = resourceDao.create(new Resource.Builder("cpu_segmented", yp)
                .name("CPU")
                .type(DiResourceType.PROCESSOR)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        final Resource ram = resourceDao.create(new Resource.Builder("ram_segmented", yp)
                .name("RAM")
                .type(DiResourceType.MEMORY)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        final Resource hdd = resourceDao.create(new Resource.Builder("hdd_segmented", yp)
                .name("HDD")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        final Resource ssd = resourceDao.create(new Resource.Builder("ssd_segmented", yp)
                .name("SSD")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        final Resource gpu = resourceDao.create(new Resource.Builder("gpu_segmented", yp)
                .name("GPU")
                .type(DiResourceType.ENUMERABLE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        final Resource ioSsd = resourceDao.create(new Resource.Builder("io_ssd", yp)
                .name("GPU")
                .type(DiResourceType.BINARY_TRAFFIC)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        final Resource ioHdd = resourceDao.create(new Resource.Builder("io_hdd", yp)
                .name("GPU")
                .type(DiResourceType.BINARY_TRAFFIC)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, ImmutableSet.of(locations, ypSegmentation));
        resourceSegmentationDao.setSegmentations(ram, ImmutableSet.of(locations, ypSegmentation));
        resourceSegmentationDao.setSegmentations(hdd, ImmutableSet.of(locations, ypSegmentation));
        resourceSegmentationDao.setSegmentations(ssd, ImmutableSet.of(locations, ypSegmentation));
        resourceSegmentationDao.setSegmentations(gpu, ImmutableSet.of(locations, ypSegmentation));
        resourceSegmentationDao.setSegmentations(ioSsd, ImmutableSet.of(locations, ypSegmentation));
        resourceSegmentationDao.setSegmentations(ioHdd, ImmutableSet.of(locations, ypSegmentation));

        createProject("custom", YANDEX, WHISTLER.getLogin());

        BigOrder bigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2077, 1, 1)));

        final Campaign newCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setId(42L)
                .setKey("foo-42")
                .setName("foo-42")
                .setStartDate(LocalDate.now())
                .setBigOrders(Arrays.asList(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2077, 1, 1))
                ))
                .build());

        prepareCampaignResources();
        updateHierarchy();

        final Campaign.BigOrder bigOrder1 = newCampaign.getBigOrders().get(0);
        final Campaign.BigOrder bigOrder2 = newCampaign.getBigOrders().get(1);
        final QuotaChangeRequest.BigOrder requestBigOrder1 = new QuotaChangeRequest.BigOrder(bigOrder1.getBigOrderId(), bigOrder1.getDate(), true);
        final QuotaChangeRequest.BigOrder requestBigOrder2 = new QuotaChangeRequest.BigOrder(bigOrder2.getBigOrderId(), bigOrder2.getDate(), true);

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .project(Hierarchy.get().getProjectReader().read("custom"))
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder2)
                                .resource(cpu)
                                .segments(ImmutableSet.of(sas, dev))
                                .amount(9_000)
                                .amountReady(9_000)
                                .build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder1)
                                .resource(cpu)
                                .segments(ImmutableSet.of(sas, dev))
                                .amount(10_000)
                                .amountReady(10_000)
                                .build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder1)
                                .resource(ram)
                                .segments(ImmutableSet.of(sas, dev))
                                .amount(1_073_741_824L)
                                .amountReady(1_073_741_824L)
                                .build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder1)
                                .resource(hdd)
                                .segments(ImmutableSet.of(sas, dev))
                                .amount(1_099_511_627_776L)
                                .amountReady(1_099_511_627_776L)
                                .build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder1)
                                .resource(ssd)
                                .segments(ImmutableSet.of(sas, dev))
                                .amount(1_099_511_627_776L)
                                .amountReady(1_099_511_627_776L)
                                .build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder1)
                                .resource(gpu)
                                .segments(ImmutableSet.of(sas, dev))
                                .amount(10_000)
                                .amountReady(10_000)
                                .build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder1)
                                .resource(ioSsd)
                                .segments(ImmutableSet.of(sas, dev))
                                .amount(1_048_576L)
                                .amountReady(1_048_576L)
                                .build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .order(requestBigOrder1)
                                .resource(ioHdd)
                                .segments(ImmutableSet.of(sas, dev))
                                .amount(1_048_576L)
                                .amountReady(1_048_576L)
                                .build()
                )));

        final String trackerIssueKey = dispenser().quotaChangeRequests()
                .byId(requestId)
                .createTicket()
                .performBy(AMOSOV_F).getTrackerIssueKey();

        final Response response = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/hooks/form-preorder")
                .query("reqId", 42)
                .header(TvmConstants.TVM_SERVICE_HEADER_NAME, "FORMS")
                .post("{\"id\": \"42\", \"user\": \"" + AMOSOV_F.getLogin() + "\", \"service\": \"YP\", \"ticket\": \"qux/" + trackerIssueKey + "\", \"quota\": \"loc:SAS-seg:dev-cpu:10-mem:1-hdd:1-ssd:1-gpu_q:10-io_ssd:1-io_hdd:1\", \"allocateAll\": \"Нет\", \"comment\": \"foo\", \"advance-ticket\": \"\", \"new-resources\":\"Да\"}");

        assertEquals(200, response.getStatus());

        final DiQuotaChangeRequest req = dispenser().quotaChangeRequests()
                .byId(requestId)
                .get()
                .perform();

        final Map<String, DiQuotaChangeRequest.Change> changeFromBO1ByResourceKey = req.getChanges().stream()
                .filter(c -> c.getOrder().getId() == bigOrder1.getBigOrderId())
                .collect(Collectors.toMap(c -> c.getResource().getKey(), c -> c));

        final Map<String, DiQuotaChangeRequest.Change> changeFromBO2ByResourceKey = req.getChanges().stream()
                .filter(c -> c.getOrder().getId() == bigOrder2.getBigOrderId())
                .collect(Collectors.toMap(c -> c.getResource().getKey(), c -> c));

        assertEquals(DiAmount.of(10_000, DiUnit.PERMILLE_CORES), changeFromBO1ByResourceKey.get(cpu.getPublicKey()).getAmountAllocated());
        assertEquals(DiAmount.of(1_073_741_824L, DiUnit.BYTE), changeFromBO1ByResourceKey.get(ram.getPublicKey()).getAmountAllocated());
        assertEquals(DiAmount.of(1_099_511_627_776L, DiUnit.BYTE), changeFromBO1ByResourceKey.get(hdd.getPublicKey()).getAmountAllocated());
        assertEquals(DiAmount.of(1_099_511_627_776L, DiUnit.BYTE), changeFromBO1ByResourceKey.get(ssd.getPublicKey()).getAmountAllocated());
        assertEquals(DiAmount.of(10_000, DiUnit.PERMILLE), changeFromBO1ByResourceKey.get(gpu.getPublicKey()).getAmountAllocated());
        assertEquals(DiAmount.of(1_048_576L, DiUnit.BINARY_BPS), changeFromBO1ByResourceKey.get(ioHdd.getPublicKey()).getAmountAllocated());
        assertEquals(DiAmount.of(1_048_576L, DiUnit.BINARY_BPS), changeFromBO1ByResourceKey.get(ioSsd.getPublicKey()).getAmountAllocated());

        assertEquals(DiAmount.of(0, DiUnit.PERMILLE_CORES), changeFromBO2ByResourceKey.get(cpu.getPublicKey()).getAmountAllocated());


    }

    @Test
    public void allProviderResourcesCanBeAllocatedThroughFormApi() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final Service yp = hierarchy.get().getServiceReader().read(YP);
        final Resource ypCpu = hierarchy.get().getResourceReader().read(new Resource.Key(SEGMENT_CPU, yp));
        final Resource ypHdd = hierarchy.get().getResourceReader().read(new Resource.Key(SEGMENT_HDD, yp));
        final SegmentReader segmentReader = hierarchy.get().getSegmentReader();
        final Segment dc1 = segmentReader.read(DC_SEGMENT_1);
        final Segment s1 = segmentReader.read(SEGMENT_SEGMENT_1);

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ypCpu).order(bigOrder).segments(ImmutableSet.of(dc1, s1)).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ypHdd).order(bigOrder).segments(ImmutableSet.of(dc1, s1)).amount(20_000).build()
        )));

        final String trackerIssueKey = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .createTicket()
                .performBy(AMOSOV_F).getTrackerIssueKey();

        Response response = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/hooks/form-preorder")
                .query("reqId", 43)
                .header(TvmConstants.TVM_SERVICE_HEADER_NAME, "FORMS")
                .post("{\"id\": \"42\", \"user\": \"" + AMOSOV_F.getLogin() + "\", \"service\": \"YP\", \"ticket\": \"qux/" + trackerIssueKey + "\", \"quota\": \"  \", \"allocateAll\": \"Да\", \"comment\": \"foo\", \"advance-ticket\": \"\", \"new-resources\":\"Нет\"}");


        assertEquals(200, response.getStatus());

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform();

        assertEquals(CONFIRMED, request.getStatus());

        Map<String, DiQuotaChangeRequest.Change> changeByResource = request
                .getChanges().stream()
                .collect(Collectors.toMap(c -> c.getResource().getKey(), Function.identity()));

        DiQuotaChangeRequest.Change ytCpuChange = changeByResource.get(YT_CPU);

        assertEquals(15, DiUnit.COUNT.convert(ytCpuChange.getAmount()));
        assertEquals(0, DiUnit.COUNT.convert(ytCpuChange.getAmountReady()));
        assertEquals(0, DiUnit.COUNT.convert(ytCpuChange.getAmountAllocated()));

        DiQuotaChangeRequest.Change ypCpuChange = changeByResource.get(SEGMENT_CPU);

        assertEquals(10, DiUnit.CORES.convert(ypCpuChange.getAmount()));
        assertEquals(10, DiUnit.CORES.convert(ypCpuChange.getAmountReady()));
        assertEquals(10, DiUnit.CORES.convert(ypCpuChange.getAmountAllocated()));

        DiQuotaChangeRequest.Change ypHddChange = changeByResource.get(SEGMENT_HDD);

        assertEquals(20_000, DiUnit.BYTE.convert(ypHddChange.getAmount()));
        assertEquals(20_000, DiUnit.BYTE.convert(ypHddChange.getAmountReady()));
        assertEquals(20_000, DiUnit.BYTE.convert(ypHddChange.getAmountAllocated()));

        response = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/hooks/form-preorder")
                .query("reqId", 44)
                .header(TvmConstants.TVM_SERVICE_HEADER_NAME, "FORMS")
                .post("{\"id\": \"42\", \"user\": \"" + AMOSOV_F.getLogin() + "\", \"service\": \"Nirvana\", \"ticket\": \"qux/" + trackerIssueKey + "\", \"quota\": \"  \", \"allocateAll\": \"Yes\", \"comment\": \"foo\", \"advance-ticket\": \"\", \"new-resources\":\"Нет\"}");

        assertEquals(200, response.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform();

        assertEquals(COMPLETED, request.getStatus());

        changeByResource = request
                .getChanges().stream()
                .collect(Collectors.toMap(c -> c.getResource().getKey(), Function.identity()));

        ytCpuChange = changeByResource.get(YT_CPU);

        assertEquals(15, DiUnit.COUNT.convert(ytCpuChange.getAmount()));
        assertEquals(15, DiUnit.COUNT.convert(ytCpuChange.getAmountReady()));
        assertEquals(15, DiUnit.COUNT.convert(ytCpuChange.getAmountAllocated()));

        ypCpuChange = changeByResource.get(SEGMENT_CPU);

        assertEquals(10, DiUnit.CORES.convert(ypCpuChange.getAmount()));
        assertEquals(10, DiUnit.CORES.convert(ypCpuChange.getAmountReady()));
        assertEquals(10, DiUnit.CORES.convert(ypCpuChange.getAmountAllocated()));

        ypHddChange = changeByResource.get(SEGMENT_HDD);

        assertEquals(20_000, DiUnit.BYTE.convert(ypHddChange.getAmount()));
        assertEquals(20_000, DiUnit.BYTE.convert(ypHddChange.getAmountReady()));
        assertEquals(20_000, DiUnit.BYTE.convert(ypHddChange.getAmountAllocated()));

    }

    @Test
    public void advanceForAllDatescanbeSettedThroughFormApi() {
        campaignDao.clear();
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setBigOrders(Arrays.asList(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2),
                        new Campaign.BigOrder(bigOrderThree.getId(), TEST_BIG_ORDER_DATE_3)
                ))
                .build());
        aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("test-aggregated")
                .setName("test-aggregated")
                .setBigOrders(Arrays.asList(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2),
                        new Campaign.BigOrder(bigOrderThree.getId(), TEST_BIG_ORDER_DATE_3)
                ))
                .setType(Campaign.Type.AGGREGATED)
                .build());


        final Map<Long, Campaign.BigOrder> bigOrderById = aggregatedCampaign.getBigOrders().stream()
                .collect(Collectors.toMap(Campaign.BigOrder::getBigOrderId, Function.identity()));

        final Service yp = hierarchy.get().getServiceReader().read(YP);

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());

        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 2)
                .build());

        final Resource cpu = resourceDao.create(new Resource.Builder("cpu_segmented", yp)
                .name("Cpu")
                .type(DiResourceType.PROCESSOR)
                .mode(DiQuotingMode.DEFAULT)
                .build());

        final Segmentation segments = segmentationDao.create(new Segmentation.Builder("segments")
                .name("segments")
                .description("Segments")
                .build());

        updateHierarchy();

        final Segment dev = segmentDao.create(new Segment.Builder("dev", segments)
                .name("dev")
                .description("dev")
                .priority((short) 1)
                .build());

        final Segment def = segmentDao.create(new Segment.Builder("default", segments)
                .name("default")
                .description("default")
                .priority((short) 2)
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, ImmutableSet.of(locations, segments));

        updateHierarchy();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu).order(bigOrderById.get(bigOrderOne.getId())).segments(ImmutableSet.of(sas, dev)).amount(15_000).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu).order(bigOrderById.get(bigOrderTwo.getId())).segments(ImmutableSet.of(sas, dev)).amount(10_000).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu).order(bigOrderById.get(bigOrderThree.getId())).segments(ImmutableSet.of(sas, dev)).amount(20_000).build()
                )));

        final String trackerIssueKey = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .createTicket()
                .performBy(AMOSOV_F).getTrackerIssueKey();

        Response response = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/hooks/form-preorder")
                .query("reqId", 42)
                .header(TvmConstants.TVM_SERVICE_HEADER_NAME, "FORMS")
                .post("{\"id\": \"42\", \"user\": \"" + AMOSOV_F.getLogin() + "\", \"service\": \"YP\", \"ticket\": \"qux/" + trackerIssueKey + "\", \"quota\": \"loc:SAS-seg:dev-cpu:20-mem:0-hdd:0-ssd:0-gpu_q:0-io_ssd:0-io_hdd:0\", \"allocateAll\": \"Нет\", \"comment\": \"foo\", \"advance-ticket\": \"\", \"new-resources\":\"Да\"}");


        assertEquals(200, response.getStatus());

        DiQuotaChangeRequest request = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform();

        assertEquals(CONFIRMED, request.getStatus());

        Map<Long, DiQuotaChangeRequest.Change> changeByResource = request
                .getChanges().stream()
                .collect(Collectors.toMap(c -> c.getOrder().getId(), Function.identity()));

        DiQuotaChangeRequest.Change bo1Change = changeByResource.get(bigOrderOne.getId());

        assertEquals(15, DiUnit.CORES.convert(bo1Change.getAmount()));
        assertEquals(0, DiUnit.CORES.convert(bo1Change.getAmountReady()));
        assertEquals(15, DiUnit.CORES.convert(bo1Change.getAmountAllocated()));

        DiQuotaChangeRequest.Change bo2Change = changeByResource.get(bigOrderTwo.getId());

        assertEquals(10, DiUnit.CORES.convert(bo2Change.getAmount()));
        assertEquals(0, DiUnit.CORES.convert(bo2Change.getAmountReady()));
        assertEquals(5, DiUnit.CORES.convert(bo2Change.getAmountAllocated()));

        DiQuotaChangeRequest.Change bo3Change = changeByResource.get(bigOrderThree.getId());

        assertEquals(20, DiUnit.CORES.convert(bo3Change.getAmount()));
        assertEquals(0, DiUnit.CORES.convert(bo3Change.getAmountReady()));
        assertEquals(0, DiUnit.CORES.convert(bo3Change.getAmountAllocated()));


        response = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/hooks/form-preorder")
                .query("reqId", 43)
                .header(TvmConstants.TVM_SERVICE_HEADER_NAME, "FORMS")
                .post("{\"id\": \"42\", \"user\": \"" + AMOSOV_F.getLogin() + "\", \"service\": \"YP\", \"ticket\": \"qux/" + trackerIssueKey + "\", \"quota\": \"loc:SAS-seg:dev-cpu:20-mem:0-hdd:0-ssd:0-gpu_q:0-io_ssd:0-io_hdd:0\", \"allocateAll\": \"Нет\", \"comment\": \"foo\", \"advance-ticket\": \"\", \"new-resources\":\"Да\"}");

        assertEquals(200, response.getStatus());

        request = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform();

        assertEquals(CONFIRMED, request.getStatus());

        changeByResource = request
                .getChanges().stream()
                .collect(Collectors.toMap(c -> c.getOrder().getId(), Function.identity()));

        bo1Change = changeByResource.get(bigOrderOne.getId());

        assertEquals(15, DiUnit.CORES.convert(bo1Change.getAmount()));
        assertEquals(0, DiUnit.CORES.convert(bo1Change.getAmountReady()));
        assertEquals(15, DiUnit.CORES.convert(bo1Change.getAmountAllocated()));

        bo2Change = changeByResource.get(bigOrderTwo.getId());

        assertEquals(10, DiUnit.CORES.convert(bo2Change.getAmount()));
        assertEquals(0, DiUnit.CORES.convert(bo2Change.getAmountReady()));
        assertEquals(10, DiUnit.CORES.convert(bo2Change.getAmountAllocated()));

        bo3Change = changeByResource.get(bigOrderThree.getId());

        assertEquals(20, DiUnit.CORES.convert(bo3Change.getAmount()));
        assertEquals(0, DiUnit.CORES.convert(bo3Change.getAmountReady()));
        assertEquals(15, DiUnit.CORES.convert(bo3Change.getAmountAllocated()));
    }

    @Test
    public void availableProvidersShouldBeInRequest() {
        Service rtmr = serviceDao.create(Service.withKey("rtmr")
                .withName("rtmr")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());
        Service solomon = serviceDao.create(Service.withKey("solomon")
                .withName("solomon")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());
        Service yt = serviceDao.create(Service.withKey("yt")
                .withName("yt")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        Map<Service, Resource> resourceByService = new HashMap<>();

        final List<Service> services = List.of(rtmr, solomon, yt);
        for (Service service : services) {
            serviceDao.update(Service.copyOf(service)
                    .withSettings(
                            Service.Settings.builder()
                                    .manualQuotaAllocation(true)
                                    .build())
                    .build());
        }
        updateHierarchy();

        final ServiceReader serviceReader = Hierarchy.get().getServiceReader();
        for (Service service : services) {

            Resource resource = new Resource.Builder("cpu_test", serviceReader.read(service.getKey()))
                    .description("cpu")
                    .name("cpu")
                    .noGroup()
                    .priority(42)
                    .mode(DiQuotingMode.SYNCHRONIZATION)
                    .type(DiResourceType.PROCESSOR)
                    .build();

            resource = resourceDao.create(resource);
            resourceByService.put(service, resource);
        }
        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resourceByService.get(rtmr))
                                .amount(1_000_000).order(bigOrder).segments(Set.of()).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resourceByService.get(solomon))
                                .amount(1_000_000).order(bigOrder).segments(Set.of()).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resourceByService.get(yt))
                                .amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);

        assertEquals(Set.of(), fetchedRequest.getProvidersToAllocate());

        final DiSetAmountResult setAmountResult = setRequestAmount(requestId, SetResourceAmountBodyOptional.UpdateFor.READY, AMOSOV_F);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("rtmr"),
                allocatableProviderInfo("solomon"),
                allocatableProviderInfo("yt")), fetchedRequest.getProvidersToAllocate());

        fetchedRequest = allocateProvider(requestId, "rtmr", AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("solomon"),
                allocatableProviderInfo("yt"),
                notAllocatableProviderInfo("rtmr", true, "Quota allocation already requested")), fetchedRequest.getProvidersToAllocate());

        final QuotaChangeRequest request = requestDao.read(requestId);
        final QuotaChangeRequest.Change rtmrChange = Iterables.find(request.getChanges(), c -> c.getResource().getService().equals(rtmr));

        assertEquals(rtmrChange.getAmount(), rtmrChange.getAmountReady());
        assertEquals(rtmrChange.getAmount(), rtmrChange.getAmountAllocating());
        assertEquals(0L, rtmrChange.getAmountAllocated());
    }

    @Test
    public void allocationIsAvailableOnlyIfNotAllocating() {
        Service rtmr = serviceDao.create(Service.withKey("rtmr")
                .withName("rtmr")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();
        Resource resource = resourceDao.create(new Resource.Builder("cpu_test", rtmr)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource)
                                .amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);

        assertEquals(Set.of(), fetchedRequest.getProvidersToAllocate());

        DiSetAmountResult setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(new SetResourceAmountBody.ChangeBody(rtmr.getKey(), bigOrder.getBigOrderId(),
                                        resource.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null)),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("rtmr")), fetchedRequest.getProvidersToAllocate());
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        fetchedRequest = allocateProvider(requestId, "rtmr", AMOSOV_F);
        assertEquals(Set.of(notAllocatableProviderInfo("rtmr", true, "Quota allocation already requested")), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final QuotaChangeRequest request = requestDao.read(requestId);
        final QuotaChangeRequest.Change saasChange = Iterables.find(request.getChanges(), c -> c.getResource().getService().equals(rtmr));

        assertEquals(500_000, saasChange.getAmountReady());
        assertEquals(saasChange.getAmountReady(), saasChange.getAmountAllocating());
        assertEquals(0L, saasChange.getAmountAllocated());

        setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(new SetResourceAmountBody.ChangeBody(rtmr.getKey(), bigOrder.getBigOrderId(),
                                        resource.getPublicKey(), Set.of(), DiAmount.of(700, DiUnit.CORES), null)),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(notAllocatableProviderInfo("rtmr", true, "Quota allocation already requested")), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));
    }

    @Test
    public void requestCanBeAllocatedBothInResourcesModelAndByResponsibleRequest() {
        campaignDao.partialUpdate(aggregatedCampaign, CampaignUpdate.builder()
                .setBigOrders(List.of(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2)
                ))
                .build());
        aggregatedCampaign = campaignDao.read(aggregatedCampaign.getId());

        final Campaign.BigOrder bigOrder1 = aggregatedCampaign.getBigOrders().get(0);
        final Campaign.BigOrder bigOrder2 = aggregatedCampaign.getBigOrders().get(1);

        Service rtmr = serviceDao.create(Service.withKey("rtmr")
                .withName("rtmr")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", rtmr)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource ram = resourceDao.create(new Resource.Builder("ram_test", rtmr)
                .description("ram")
                .name("ram")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.MEMORY)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        resourceSegmentationDao.setSegmentations(ram, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder2).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(man)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(ram)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpu)
                .campaignId(aggregatedCampaign.getId())
                .addSegments(Set.of(sas))
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(UUID.randomUUID())
                        .externalResourceBaseUnitKey("cores")
                        .numerator(1L)
                        .denominator(1000L)
                        .build()
                )
        );

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("rtmr")), fetchedRequest.getProvidersToAllocate());
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        fetchedRequest = allocateProvider(requestId, "rtmr", AMOSOV_F);
        assertEquals(Set.of(notAllocatableProviderInfo("rtmr", true, "Quota allocation already requested")), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final QuotaChangeRequest request = requestDao.read(requestId);
        final Map<Boolean, List<QuotaChangeRequest.Change>> changesByMapping = request.getChanges()
                .stream()
                .collect(Collectors.partitioningBy(c -> c.getResource().equals(cpu) && c.getSegments().equals(Set.of(sas))));
        changesByMapping.get(true)
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(500_000, c.getAmountAllocated());
                });

        changesByMapping.get(false)
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(0, c.getAmountAllocated());
                });

        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertTrue(fetchedRequest.isShowAllocationNote());
        assertEquals(Set.of(notAllocatableProviderInfo("rtmr", true, "Quota allocation already requested")), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE));

        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/reset-allocation-note")
                .invoke("POST", null, Response.class);

        assertEquals(200, response.getStatus());
        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertFalse(fetchedRequest.isShowAllocationNote());
    }

    @Test
    public void requestCanBeAllocatedBothInResourcesModelAfterResponsibleRequest() {
        campaignDao.partialUpdate(aggregatedCampaign, CampaignUpdate.builder()
                .setBigOrders(List.of(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2)
                ))
                .build());
        aggregatedCampaign = campaignDao.read(aggregatedCampaign.getId());

        final Campaign.BigOrder bigOrder1 = aggregatedCampaign.getBigOrders().get(0);
        final Campaign.BigOrder bigOrder2 = aggregatedCampaign.getBigOrders().get(1);

        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).amountAllocating(500_000).order(bigOrder2).segments(Set.of(man)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpu)
                .campaignId(aggregatedCampaign.getId())
                .addSegments(Set.of(sas))
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(UUID.randomUUID())
                        .externalResourceBaseUnitKey("cores")
                        .numerator(1L)
                        .denominator(1000L)
                        .build()
                )
        );

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("saas", true)), fetchedRequest.getProvidersToAllocate());
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        fetchedRequest = allocateProvider(requestId, "saas", AMOSOV_F);
        assertEquals(Set.of(notAllocatableProviderInfo("saas", true, "Quota allocation already requested")), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final QuotaChangeRequest request = requestDao.read(requestId);
        final Map<Boolean, List<QuotaChangeRequest.Change>> changesByMapping = request.getChanges()
                .stream()
                .collect(Collectors.partitioningBy(c -> c.getResource().equals(cpu) && c.getSegments().equals(Set.of(sas))));
        changesByMapping.get(true)
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(500_000, c.getAmountAllocated());
                });

        changesByMapping.get(false)
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(0, c.getAmountAllocated());
                });
    }

    @Test
    public void unresolvedRequestDeliveryCanBeRetriedManually() {
        campaignDao.partialUpdate(aggregatedCampaign, CampaignUpdate.builder()
                .setBigOrders(List.of(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2)
                ))
                .build());
        aggregatedCampaign = campaignDao.read(aggregatedCampaign.getId());

        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder1 = aggregatedCampaign.getBigOrders().get(0);
        final Campaign.BigOrder bigOrder2 = aggregatedCampaign.getBigOrders().get(1);

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).amountAllocating(500_000).order(bigOrder1).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).amountAllocating(500_000).order(bigOrder2).segments(Set.of(sas)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);
        final UUID externalResourceId = UUID.randomUUID();
        final String externalUnitKey = "cores";
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpu)
                .campaignId(aggregatedCampaign.getId())
                .addSegments(Set.of(sas))
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(externalResourceId)
                        .externalResourceBaseUnitKey(externalUnitKey)
                        .numerator(1L)
                        .denominator(1000L)
                        .build()
                )
        );

        final Person author = Hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        for (Campaign.BigOrder bigOrder : List.of(bigOrder1, bigOrder2)) {
            deliveryDao.create(QuotaRequestDelivery.builder()
                    .id(UUID.randomUUID())
                    .authorId(author.getId())
                    .authorUid(author.getUid())
                    .abcServiceId(project.getAbcServiceId())
                    .resolved(false)
                    .campaignId(aggregatedCampaign.getId())
                    .createdAt(Instant.now())
                    .quotaRequestId(requestId)
                    .providerId(saas.getId())
                    .addInternalResource(InternalResource.builder()
                            .resourceId(cpu.getId())
                            .amount(500_000)
                            .bigOrderId(bigOrder.getBigOrderId())
                            .addSegmentId(sas.getId())
                            .build())
                    .addExternalResource(ExternalResource.builder()
                            .amount(500)
                            .resourceId(externalResourceId)
                            .bigOrderId(bigOrder.getBigOrderId())
                            .unitKey(externalUnitKey)
                            .build())
                            .resolveStatus(QuotaRequestDeliveryResolveStatus.IN_PROCESS)
                    .build());
        }

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("saas")), fetchedRequest.getProvidersToAllocate());
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        fetchedRequest = allocateProvider(requestId, "saas", AMOSOV_F);
        assertTrue(fetchedRequest.getProvidersToAllocate().isEmpty());

        final QuotaChangeRequest request = requestDao.read(requestId);
        request.getChanges()
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(500_000, c.getAmountAllocated());
                });
        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertTrue(fetchedRequest.getProvidersToAllocate().isEmpty());

        final List<QuotaRequestDelivery> unresolved = deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(requestId,
                saas.getId());
        assertTrue(unresolved.isEmpty());
    }

    @Test
    public void resourceModelAllocationWillSaveUnresolvedDeliveryOnErrorResponse() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder).segments(Set.of(sas)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);
        final UUID externalResourceId = UUID.randomUUID();
        final String externalUnitKey = "cores";
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpu)
                .campaignId(aggregatedCampaign.getId())
                .addSegments(Set.of(sas))
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(externalResourceId)
                        .externalResourceBaseUnitKey(externalUnitKey)
                        .numerator(1L)
                        .denominator(1000L)
                        .build()
                )
        );

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("saas")), fetchedRequest.getProvidersToAllocate());

        mockDApi.setDeliveryProcessor(deliveryRequestDto -> {
            throw new ClientErrorException(400);
        });
        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F), 500,
                "Unknown error from resources model service. Status - 400: no response");
        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertFalse(fetchedRequest.getProvidersToAllocate().isEmpty());

        final QuotaChangeRequest request = requestDao.read(requestId);
        request.getChanges()
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(0L, c.getAmountAllocated());
                });

        final List<QuotaRequestDelivery> unresolved = deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(requestId,
                saas.getId());
        assertFalse(unresolved.isEmpty());
    }

    @Test
    public void resourceModelAllocationWillDeliverUnresolvedAndNewQuota() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(700_000).amountAllocating(500_000).order(bigOrder).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(700_000).order(bigOrder).segments(Set.of(man)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        final UUID externalResourceId = UUID.randomUUID();
        final String externalUnitKey = "cores";
        final Person author = Hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        for (Segment dc : List.of(sas, man)) {
            resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                    .resource(cpu)
                    .campaignId(aggregatedCampaign.getId())
                    .addSegments(Set.of(dc))
                    .target(ResourceModelMappingTarget.builder()
                            .externalResourceId(externalResourceId)
                            .externalResourceBaseUnitKey(externalUnitKey)
                            .numerator(1L)
                            .denominator(1000L)
                            .build()
                    )
            );
        }

        deliveryDao.create(QuotaRequestDelivery.builder()
                .id(UUID.randomUUID())
                .authorId(author.getId())
                .authorUid(author.getUid())
                .abcServiceId(project.getAbcServiceId())
                .resolved(false)
                .campaignId(aggregatedCampaign.getId())
                .createdAt(Instant.now())
                .quotaRequestId(requestId)
                .providerId(saas.getId())
                .addInternalResource(InternalResource.builder()
                        .resourceId(cpu.getId())
                        .amount(500_000)
                        .bigOrderId(bigOrder.getBigOrderId())
                        .addSegmentId(sas.getId())
                        .build())
                .addExternalResource(ExternalResource.builder()
                        .amount(500)
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder.getBigOrderId())
                        .unitKey(externalUnitKey)
                        .build())
                        .resolveStatus(QuotaRequestDeliveryResolveStatus.IN_PROCESS)
                .build());

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("saas")), fetchedRequest.getProvidersToAllocate());

        fetchedRequest = allocateProvider(requestId, "saas", AMOSOV_F);
        assertTrue(fetchedRequest.getProvidersToAllocate().isEmpty());

        final QuotaChangeRequest request = requestDao.read(requestId);
        request.getChanges()
                .forEach(c -> {
                    assertEquals(700_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(700_000, c.getAmountAllocated());
                });

        final List<QuotaRequestDelivery> deliveries = deliveryDao.readAll();
        assertEquals(2, deliveries.size());
        assertTrue(deliveries.stream().allMatch(QuotaRequestDelivery::isResolved));
    }

    @Test
    public void ydbResourceModelAllocationWillMapRpsToCores() {
        Service ydb = serviceDao.create(Service.withKey("ydb")
                .withName("ydb")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("ydbResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource rps = resourceDao.create(new Resource.Builder("ydb_ru-rps", ydb)
                .description("rps")
                .name("rps")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.ENUMERABLE)
                .build());
        Resource userpoolCores = resourceDao.create(new Resource.Builder("ydb_ru-userpool_cores", ydb)
                .description("cores")
                .name("cores")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(rps)
                                .amount(1_000_000).amountReady(1000_000).order(bigOrder).segments(Set.of()).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(userpoolCores)
                                .amount(1_000_000).amountReady(1000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        final UUID externalResourceId = UUID.randomUUID();
        final String externalUnitKey = "cores";
        final Person author = Hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(rps)
                .campaignId(aggregatedCampaign.getId())
        );
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(userpoolCores)
                .campaignId(aggregatedCampaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(externalResourceId)
                        .externalResourceBaseUnitKey(externalUnitKey)
                        .numerator(1L)
                        .denominator(4000L)
                        .build()
                )
        );

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("ydb")), fetchedRequest.getProvidersToAllocate());

        fetchedRequest = allocateProvider(requestId, "ydb", AMOSOV_F);
        assertTrue(fetchedRequest.getProvidersToAllocate().isEmpty());

        final QuotaChangeRequest request = requestDao.read(requestId);
        request.getChanges()
                .forEach(c -> {
                    assertEquals(1_000_000L, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(1_000_000L, c.getAmountAllocated());
                });

        final List<QuotaRequestDelivery> deliveries = deliveryDao.readAll();
        assertEquals(1, deliveries.size());
        final QuotaRequestDelivery delivery = Iterables.getOnlyElement(deliveries);
        assertTrue(delivery.isResolved());
        assertEquals(List.of(
                new ExternalResource(externalResourceId, bigOrder.getBigOrderId(), 251L, externalUnitKey,
                        null, null, null)
        ), delivery.getExternalResources());
        assertEquals(Set.of(
                new InternalResource(rps.getId(), Set.of(), bigOrder.getBigOrderId(), 1_000_000L),
                new InternalResource(userpoolCores.getId(), Set.of(), bigOrder.getBigOrderId(), 1_002_000L)
        ), Set.copyOf(delivery.getInternalResources()));
    }

    @Test
    public void resourceModelAllocationDryRunShouldWork() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(700_000).amountAllocated(200_000)
                                .amountAllocating(200_000).order(bigOrder).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(700_000).amountAllocated(200_000)
                                .amountAllocating(200_000).order(bigOrder).segments(Set.of(man)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        final UUID externalResourceId = UUID.randomUUID();
        final String externalUnitKey = "cores";
        final Person author = Hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        for (Segment dc : List.of(sas, man)) {
            resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                    .resource(cpu)
                    .campaignId(aggregatedCampaign.getId())
                    .addSegments(Set.of(dc))
                    .target(ResourceModelMappingTarget.builder()
                            .externalResourceId(externalResourceId)
                            .externalResourceBaseUnitKey(externalUnitKey)
                            .numerator(1L)
                            .denominator(1000L)
                            .build()
                    )
            );
        }

        QuotaRequestDeliveryContext.View deliveryContext = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/provider-allocation/" + saas.getKey() + "/_dry-run")
                .post(null, QuotaRequestDeliveryContext.View.class);

        QuotaRequestDelivery.View quotaRequestDelivery = deliveryContext.getQuotaRequestDelivery();
        assertEquals(List.of(new ExternalResource(externalResourceId, bigOrder.getBigOrderId(), 1000, externalUnitKey,
                        null, null, null)),
                quotaRequestDelivery.getExternalResources());
        assertEquals(
                Set.of(
                        new InternalResource(cpu.getId(), Set.of(man.getId()), bigOrder.getBigOrderId(), 500_000),
                        new InternalResource(cpu.getId(), Set.of(sas.getId()), bigOrder.getBigOrderId(), 500_000)
                ), Set.copyOf(quotaRequestDelivery.getInternalResources()));
        assertFalse(quotaRequestDelivery.isResolved());
        assertNull(quotaRequestDelivery.getResolvedAt());
        assertTrue(quotaRequestDelivery.getDeliveryResults().isEmpty());

        deliveryContext = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/provider-allocation/" + saas.getKey() + "/_dry-run")
                .query("all", true)
                .post(null, QuotaRequestDeliveryContext.View.class);

        quotaRequestDelivery = deliveryContext.getQuotaRequestDelivery();
        assertEquals(List.of(new ExternalResource(externalResourceId, bigOrder.getBigOrderId(), 1600, externalUnitKey,
                        null, null, null)),
                quotaRequestDelivery.getExternalResources());
        assertEquals(
                Set.of(
                        new InternalResource(cpu.getId(), Set.of(man.getId()), bigOrder.getBigOrderId(), 800_000),
                        new InternalResource(cpu.getId(), Set.of(sas.getId()), bigOrder.getBigOrderId(), 800_000)
                ), Set.copyOf(quotaRequestDelivery.getInternalResources()));
        assertFalse(quotaRequestDelivery.isResolved());
        assertNull(quotaRequestDelivery.getResolvedAt());
        assertTrue(quotaRequestDelivery.getDeliveryResults().isEmpty());
    }

    @Test
    public void resourceModelAllocationWillShowRetryErrorsOnlyIfNoNewQuota() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(700_000).amountAllocating(500_000).order(bigOrder).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(700_000).order(bigOrder).segments(Set.of(man)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        final UUID externalResourceId = UUID.randomUUID();
        final String externalUnitKey = "cores";
        final Person author = Hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        for (Segment dc : List.of(sas, man)) {
            resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                    .resource(cpu)
                    .campaignId(aggregatedCampaign.getId())
                    .addSegments(Set.of(dc))
                    .target(ResourceModelMappingTarget.builder()
                            .externalResourceId(externalResourceId)
                            .externalResourceBaseUnitKey(externalUnitKey)
                            .numerator(1L)
                            .denominator(1000L)
                            .build()
                    )
            );
        }

        final UUID foreverUnresoulvedDeliveryId = UUID.randomUUID();
        deliveryDao.create(QuotaRequestDelivery.builder()
                .id(foreverUnresoulvedDeliveryId)
                .authorId(author.getId())
                .authorUid(author.getUid())
                .abcServiceId(project.getAbcServiceId())
                .resolved(false)
                .campaignId(aggregatedCampaign.getId())
                .createdAt(Instant.now())
                .quotaRequestId(requestId)
                .providerId(saas.getId())
                .addInternalResource(InternalResource.builder()
                        .resourceId(cpu.getId())
                        .amount(500_000)
                        .bigOrderId(bigOrder.getBigOrderId())
                        .addSegmentId(sas.getId())
                        .build())
                .addExternalResource(ExternalResource.builder()
                        .amount(500)
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder.getBigOrderId())
                        .unitKey(externalUnitKey)
                        .build())
                .resolveStatus(QuotaRequestDeliveryResolveStatus.IN_PROCESS)
                .build());

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("saas")), fetchedRequest.getProvidersToAllocate());

        mockDApi.setDeliveryProcessor(req -> {
            if (req.getDeliveryId().equals(foreverUnresoulvedDeliveryId.toString())) {
                throw new RuntimeException("something gone wrong");
            } else {
                return mockDApi.defaultDelivery(req);
            }
        });

        fetchedRequest = allocateProvider(requestId, "saas", AMOSOV_F);
        assertFalse(fetchedRequest.getProvidersToAllocate().isEmpty());

        final QuotaChangeRequest request = requestDao.read(requestId);
        final Map<Segment, List<QuotaChangeRequest.Change>> changesBySegment = request.getChanges()
                .stream()
                .collect(Collectors.groupingBy(c -> c.getSegments().iterator().next()));
        changesBySegment.get(sas).forEach(c -> {
            assertEquals(700_000, c.getAmountReady());
            assertEquals(c.getAmountReady(), c.getAmountAllocating());
            assertEquals(200_000, c.getAmountAllocated());
        });
        changesBySegment.get(man).forEach(c -> {
            assertEquals(700_000, c.getAmountReady());
            assertEquals(c.getAmountReady(), c.getAmountAllocating());
            assertEquals(700_000, c.getAmountAllocated());
        });

        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F), 500, "Unknown error from resources model service");
    }

    @Test
    public void amountAllocatedCannotBeDecreased() {
        nirvana = Service.copyOf(nirvana)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build();
        serviceDao.update(nirvana);
        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu)
                                .amount(1_000_000)
                                .amountReady(1_000_000)
                                .amountAllocated(500_000)
                                .amountAllocating(500_000)
                                .order(bigOrder)
                                .segments(Set.of())
                                .build()
                ))
        );


        Response setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(new SetResourceAmountBody.ChangeBody(nirvana.getKey(), bigOrder.getBigOrderId(),
                                        ytCpu.getPublicKey(), Set.of(), null, DiAmount.of(499_000, ytCpu.getType().getBaseUnit()))),
                                null)
                        )));
        final DiSetAmountResult.Errors errors = setAmountResult.readEntity(DiSetAmountResult.Errors.class);
        assertFalse(errors.getErrors().isEmpty());
        final DiSetAmountResult.Errors.Item item = Iterables.getOnlyElement(errors.getErrors());
        assertTrue(item.getMessage().contains("Amount allocated"));
    }

    private DiQuotaChangeRequest getRequest(long requestId, DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class);
    }

    private DiQuotaChangeRequest createRequestTicket(long requestId, DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/quota-requests/" + requestId + "/create-ticket")
                .post(null, DiQuotaChangeRequest.class);
    }

    private DiSetAmountResult setRequestAmount(long requestId, SetResourceAmountBodyOptional.UpdateFor updateFor, DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke("PATCH", new SetResourceAmountBodyOptional(List.of(
                        new SetResourceAmountBodyOptional.Item(requestId, null, null, null)
                ), updateFor), DiSetAmountResult.class);
    }

    private DiQuotaChangeRequest allocateProvider(long requestId, String providerKey, DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/resource-preorder/" + requestId + "/provider-allocation/" + providerKey)
                .post(null, DiQuotaChangeRequest.class);
    }
}

