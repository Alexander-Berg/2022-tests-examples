package ru.yandex.qe.dispenser.integration;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignResource;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.PersonAffiliation;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.Quota;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.QuotaSpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceSegmentation;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignResourceDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao;
import ru.yandex.qe.dispenser.domain.dao.quota.QuotaDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dictionaries.impl.FrontDictionariesManager;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.domain.hierarchy.Session;
import ru.yandex.qe.dispenser.domain.index.LongIndexBase;
import ru.yandex.qe.dispenser.domain.tracker.TrackerManagerImpl;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.quota.request.ticket.QuotaChangeRequestTicketManager;
import ru.yandex.qe.dispenser.ws.quota.request.workflow.QuotaRequestWorkflowManager;
import ru.yandex.qe.dispenser.ws.quota.request.workflow.ResourceWorkflow;
import ru.yandex.qe.dispenser.ws.quota.request.workflow.context.CreateRequestContext;
import ru.yandex.qe.dispenser.ws.quota.request.workflow.context.RequestContext;
import ru.yandex.qe.dispenser.ws.quota.request.workflow.context.UpdateRequestContext;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.TEST_BIG_ORDER_DATE;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class TrackerIntegrationTest extends BaseExternalApiTest {
    @Autowired
    private TrackerManagerImpl trackerManager;

    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;
    private QuotaChangeRequest req;

    @Autowired
    private QuotaDao quotaDao;

    @Autowired
    private ProjectDao projectDao;

    @Autowired
    private QuotaRequestWorkflowManager workflowManager;

    @Autowired
    private QuotaChangeRequestTicketManager ticketManager;

    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;

    @Autowired
    private CampaignResourceDao campaignResourceDao;

    @Autowired
    private FrontDictionariesManager frontDictionariesManager;

    @Autowired
    private PersonDao personDao;

    private Resource storage;
    private Project yandex;
    private Resource ytCpu;
    private Service nirvana;
    private Campaign campaign;
    private BotCampaignGroup botCampaignGroup;
    private BigOrder bigOrder;

    @BeforeAll
    public void beforeClass() {
        bigOrderManager.clear();
        bigOrder = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE));
    }

    @BeforeEach
    @Override
    public void setUp() throws URISyntaxException {
        super.setUp();

        final Campaign.BigOrder campaignBigOrder = new Campaign.BigOrder(bigOrder.getId(), TEST_BIG_ORDER_DATE);
        campaignBigOrder.setId(1L);
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrder).build());
        Map<Long, BigOrder> orderById = bigOrderManager.getByIds(campaign.getBigOrders().stream()
                        .map(Campaign.BigOrder::getBigOrderId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(BigOrder::getId, s -> s));
        botCampaignGroup = botCampaignGroupDao.create(BotCampaignGroup.builder()
                .setKey("test_group")
                .setName("test group")
                .setActive(true)
                .setBotPreOrderIssueKey("DISPENSERREQ-42")
                .setCampaigns(List.of(campaign.forBot(orderById)))
                .setBigOrders(new ArrayList<>(orderById.values()))
                .build());

        final Hierarchy cache = Hierarchy.get();

        nirvana = cache.getServiceReader().read("nirvana");
        ytCpu = cache.getResourceReader().read(new Resource.Key("yt-cpu", nirvana));
        storage = cache.getResourceReader().read(new Resource.Key("storage", nirvana));
        final QuotaSpec ytCpuSpec = cache.getQuotaSpecReader().readSingle(ytCpu);
        yandex = projectDao.createIfAbsent(Project.withKey("ya").name("Ya").abcServiceId(42).build());

        projectDao.attach(personDao.readPersonByLogin("keyd"), yandex, Role.PROCESS_RESPONSIBLE);
        projectDao.attach(personDao.readPersonByLogin("whistler"), yandex, Role.RESPONSIBLE);
        projectDao.attach(personDao.readPersonByLogin("lyadzhin"), yandex, Role.MEMBER);

        quotaDao.applyChanges(ImmutableMap.of(
                Quota.Key.totalOf(ytCpuSpec, yandex), 200_000L
        ), Collections.emptyMap(), Collections.emptyMap());

        final Project testProject = projectDao.create(Project.withKey("test")
                .name("test")
                .description("test")
                .abcServiceId(123)
                .parent(yandex)
                .build());

        final Person binarycat = new Person("binarycat", 1120000000011199L, false, false, false, PersonAffiliation.YANDEX);
        projectDao.attach(binarycat, testProject, Role.RESPONSIBLE);

        updateHierarchy();

        prepareCampaignResources();

        final QuotaChangeRequest request = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                .project(testProject)
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).segments(Collections.emptySet()).amount(1_000L).build()
                ))
                .author(binarycat)
                .description("for test")
                .calculations("2-3")
                .created(0)
                .updated(0)
                .status(QuotaChangeRequest.Status.NEW)
                .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                .chartLinks(Collections.emptyList())
                .cost(42d)
                .requestOwningCost(0L)
                .build());

        final String ticketForQuotaChangeRequest = ticketManager.createTicketForQuotaChangeRequest(request);

        req = request.copyBuilder()
                .trackerIssueKey(ticketForQuotaChangeRequest)
                .build();
    }

    @AfterEach
    public void clearDictionaryCache() {
        frontDictionariesManager.clearCache();
    }

    @Test
    public void testTicketWorkflow() {

        final Issue issue = trackerManager.getIssue(req.getTrackerIssueKey());
        assertNotNull(issue);
        issue.update(IssueUpdate.type("task").build());

        updateHierarchy();

        final long originCommentCount = trackerManager.getComments(req.getTrackerIssueKey()).stream().count();

        final ResourceWorkflow workflow = workflowManager.getResourceWorkflow();

        final BodyUpdate bodyUpdate = new BodyUpdate.BodyUpdateBuilder().build();

        final Person amosovf = new Person("amosov-f", 1120000000022901L, false, false, false, PersonAffiliation.YANDEX);
        final UpdateRequestContext context = new UpdateRequestContext(
                amosovf, req, Collections.singletonList(new QuotaChangeRequest.ChangeAmount(
                new QuotaChangeRequest.ChangeKey(null, storage, Collections.emptySet()), 100L)
        ), bodyUpdate);
        workflow.updateRequest(req, context, false);

        final long updatedCommentCount = trackerManager.getComments(req.getTrackerIssueKey()).stream().count();
        assertEquals(1, updatedCommentCount - originCommentCount);

        Issue updatedIssue = trackerManager.getIssue(req.getTrackerIssueKey());
        final String description = updatedIssue.getDescription().get();

        assertTrue(description.contains("Nirvana: Storage"));
        assertTrue(description.contains("Storage: 100 B"));

        updateHierarchy();

        workflow.setStatus(req, QuotaChangeRequest.Status.APPLIED, new RequestContext(amosovf, req), false);

        updatedIssue = trackerManager.getIssue(req.getTrackerIssueKey());
        assertEquals("closed", updatedIssue.getStatus().getKey());
    }

    @Test
    public void uniqueTicketMustCreatedAndRelinked() {

        final String originIssueKey = req.getTrackerIssueKey();
        assertNotNull(originIssueKey);

        //empty ticket
        final QuotaChangeRequest request = quotaChangeRequestDao.read(req.getId()).copyBuilder()
                .trackerIssueKey(null)
                .build();
        quotaChangeRequestDao.update(request);

        final String ticketForQuotaChangeRequest = ticketManager.createTicketForQuotaChangeRequest(request);

        assertEquals(originIssueKey, ticketForQuotaChangeRequest);
    }

    @Test
    public void resourcePreOrderRequestShouldHaveComments() {
        final ResourceWorkflow workflow = workflowManager.getResourceWorkflow();

        final Person person = new Person("whistler", 1120000000022037L, false, false, false, PersonAffiliation.YANDEX);

        final ImmutableList<QuotaChangeRequest.Change> changes = ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(storage).segments(Collections.emptySet())
                        .amount(1_000L).order(new QuotaChangeRequest.BigOrder(bigOrder.getId(),
                                bigOrder.getDate(), true)).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).segments(Collections.emptySet())
                        .amount(1_000L).order(new QuotaChangeRequest.BigOrder(bigOrder.getId(),
                                bigOrder.getDate(), true)).build()
        );

        final CreateRequestContext ctx = new CreateRequestContext(person, yandex, changes,
                new Body.BodyBuilder()
                        .projectKey(yandex.getPublicKey())
                        .description("")
                        .calculations("")
                        .changes(nirvana.getKey(), storage.getPublicKey(), bigOrder.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(), null, QuotaChangeRequest.Campaign.from(campaign), botCampaignGroup);

        QuotaChangeRequest request = workflow.createRequest(ctx).getValue().iterator().next();
        final String trackerIssueKey = request.getTrackerIssueKey();
        Issue issue = trackerManager.getIssue(trackerIssueKey);
        issue = issue.update(IssueUpdate.type("request").build());

        final RequestContext context = new RequestContext(person, yandex, Collections.singleton(nirvana), null,
                QuotaChangeRequest.Campaign.from(campaign), botCampaignGroup);
        request = workflow.setStatus(request, QuotaChangeRequest.Status.CONFIRMED, context, false);

        issue = trackerManager.getIssue(trackerIssueKey);
        Comment lastComment = issue.getComments().toList().last();
        assertEquals("Заявка переведена в статус %%CONFIRMED%%", lastComment.getText().get());

        request = workflow.setStatus(request, QuotaChangeRequest.Status.NEW, context, false);

        issue = trackerManager.getIssue(trackerIssueKey);
        lastComment = issue.getComments().toList().last();
        assertEquals("Заявка переведена в статус %%NEW%%\n\nИзмененная заявка ожидает рассмотрения.\n\nUpdated request awaits your decision.", lastComment.getText().get());
    }

    private void prepareCampaignResources() {
        clearDictionaryCache();
        TransactionWrapper.INSTANCE.execute(() -> {
            Set<Resource> resources = Hierarchy.get().getResourceReader().getAll();
            Set<Campaign> campaigns = campaignDao.getAll();
            campaigns.forEach(campaign -> {
                List<Long> bigOrders = campaign.getBigOrders().stream()
                        .map(LongIndexBase::getId).collect(Collectors.toList());
                resources.forEach(resource -> {
                    List<Segmentation> segmentations = Hierarchy.get()
                            .getResourceSegmentationReader().getResourceSegmentations(resource)
                            .stream().map(ResourceSegmentation::getSegmentation).collect(Collectors.toList());
                    List<CampaignResource.Segmentation> segmentationsSettings = new ArrayList<>();
                    segmentations.forEach(segmentation -> {
                        Set<Segment> segments = Hierarchy.get().getSegmentReader().get(segmentation);
                        segmentationsSettings.add(new CampaignResource.Segmentation(segmentation.getId(),
                                segments.stream().map(LongIndexBase::getId).collect(Collectors.toList())));
                    });
                    List<CampaignResource.SegmentsBigOrders> segmentBigOrders = prepareSegmentBigOrders(segmentations,
                            bigOrders);
                    CampaignResource campaignResource = new CampaignResource(campaign.getId(), resource.getId(),
                            false, false, null, new CampaignResource.Settings(
                                    segmentationsSettings, bigOrders, segmentBigOrders));
                    Session.ERROR.remove();
                    campaignResourceDao.getByResourceId(campaignResource.getResourceId()).stream()
                            .filter(r -> r.getCampaignId() == campaignResource.getCampaignId())
                            .forEach(r -> campaignResourceDao.delete(r));
                    campaignResourceDao.create(campaignResource);
                });
            });
        });
    }

    private List<CampaignResource.SegmentsBigOrders> prepareSegmentBigOrders(final List<Segmentation> segmentations,
                                                                             final List<Long> bigOrders) {
        List<CampaignResource.SegmentsBigOrders> result = new ArrayList<>();
        prepareSegmentsRecursive(result, segmentations, bigOrders, 0, Collections.emptyList());
        return result;
    }

    private void prepareSegmentsRecursive(final List<CampaignResource.SegmentsBigOrders> result,
                                          final List<Segmentation> segmentations,
                                          final List<Long> bigOrders,
                                          final int segmentationIndex,
                                          final List<Long> segmentsList) {
        if (segmentationIndex < segmentations.size()) {
            Segmentation segmentation = segmentations.get(segmentationIndex);
            Set<Segment> segments = Hierarchy.get().getSegmentReader().get(segmentation);
            segments.forEach(segment -> {
                List<Long> newSegmentsList = new ArrayList<>(segmentsList);
                newSegmentsList.add(segment.getId());
                prepareSegmentsRecursive(result, segmentations, bigOrders, segmentationIndex + 1,
                        newSegmentsList);
            });
        } else {
            result.add(new CampaignResource.SegmentsBigOrders(segmentsList, bigOrders));
        }
    }

}
