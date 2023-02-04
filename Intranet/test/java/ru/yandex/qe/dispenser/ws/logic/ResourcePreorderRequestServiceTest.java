package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSetAmountResult;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.MessageHelper;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.exception.SingleMessageException;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.quartz.trigger.QuartzTrackerCommentTrigger;
import ru.yandex.qe.dispenser.ws.QuotaRequestChangeValues;
import ru.yandex.qe.dispenser.ws.ResourcePreorderChangeManager;
import ru.yandex.qe.dispenser.ws.ResourcePreorderRequestUtils;
import ru.yandex.qe.dispenser.ws.api.model.distribution.DistributedQuota;
import ru.yandex.qe.dispenser.ws.api.model.distribution.DistributedQuotaDeltas;
import ru.yandex.qe.dispenser.ws.api.model.distribution.QuotaDistributionPlan;
import ru.yandex.qe.dispenser.ws.api.model.distribution.ResourceDistributionAlgorithm;
import ru.yandex.qe.dispenser.ws.common.model.response.ErrorResponse;
import ru.yandex.qe.dispenser.ws.param.DiExceptionMapper;
import ru.yandex.qe.dispenser.ws.quota.request.QuotaChangeRequestManager;
import ru.yandex.qe.dispenser.ws.quota.request.SetResourceAmountBody;
import ru.yandex.qe.dispenser.ws.quota.request.ticket.QuotaChangeRequestTicketManager;
import ru.yandex.qe.dispenser.ws.quota.request.workflow.context.PerformerContext;
import ru.yandex.qe.dispenser.ws.reqbody.DistributeQuotaBody;
import ru.yandex.qe.dispenser.ws.reqbody.SetResourceAmountBodyOptional;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignGroupBuilder;

public class ResourcePreorderRequestServiceTest extends BaseResourcePreorderTest {

    public static final Function<DiQuotaChangeRequest.Change, String> GET_RESOURCE_KEY = c -> c.getResource().getKey();

    @Autowired
    private QuotaChangeRequestDao requestDao;

    @Autowired
    private QuotaChangeRequestManager requestManager;

    @Autowired
    @Qualifier("errorMessageSource")
    private MessageSource errorMessageSource;

    @Autowired
    private PersonDao personDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @Autowired
    private MessageHelper messageHelper;

    private Resource ytGpu;
    private Service yp;
    private Resource segmentStorage;
    private Resource segmentHdd;
    private Set<Segment> segmentStorageSegments;
    private Set<Segment> segmentHddSegments;
    private @NotNull Service mdb;
    private @NotNull Resource cpu;
    private Campaign aggregatedCampaign;


    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        ytGpu = hierarchy.get().getResourceReader().read(new Resource.Key(YT_GPU, nirvana));

        yp = hierarchy.get().getServiceReader().read(YP);
        segmentStorage = hierarchy.get().getResourceReader().read(new Resource.Key(SEGMENT_STORAGE, yp));
        segmentStorageSegments = new HashSet<>(Arrays.asList(hierarchy.get().getSegmentReader().read(SEGMENT_SEGMENT_1),
                hierarchy.get().getSegmentReader().read(DC_SEGMENT_1)));
        segmentHdd = hierarchy.get().getResourceReader().read(new Resource.Key(SEGMENT_HDD, yp));
        segmentHddSegments = new HashSet<>(Arrays.asList(hierarchy.get().getSegmentReader().read(SEGMENT_SEGMENT_2),
                hierarchy.get().getSegmentReader().read(DC_SEGMENT_2)));

        mdb = hierarchy.get().getServiceReader().read(MDB);
        cpu = hierarchy.get().getResourceReader().read(new Resource.Key(CPU, mdb));
        aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("test-aggregated")
                .setName("test-aggregated")
                .setType(Campaign.Type.AGGREGATED).build());
        prepareCampaignResources();

    }

    @Test
    public void onChangeAmountShouldCheckRequestId() {

        final long validRequestId = createRequest(defaultBuilder());

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(-1L, null, Collections.emptyList(), ""),
                new SetResourceAmountBody.Item(validRequestId, null, Collections.emptyList(), "")
        ));

        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());
        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();

        assertEquals("Non-existing request ids", error.getMessage());
        assertEquals(Collections.singleton(-1L), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldCheckResourceIdentification() {
        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(1L, null, Collections.emptyList(), ""),
                new SetResourceAmountBody.Item(null, "foo", Collections.emptyList(), "")
        ));

        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(400, response.getStatus());
        assertTrue(SpyWebClient.lastResponse().contains("Updates must contain only request ids or only ticket keys"));
    }

    @Test
    public void amountBodyItemConstructorWillThrowOnIllegalParameters() {
        assertThrows(SingleMessageException.class, () -> new SetResourceAmountBody.Item(null, null, Collections.emptyList(), ""));
        assertThrows(SingleMessageException.class, () -> new SetResourceAmountBody.Item(42L, "42", Collections.emptyList(), ""));
    }

    @Test
    public void onChangeAmountShouldCheckWrongRequestStatus() {
        final Campaign.BigOrder bigOrder = campaign.getBigOrders().iterator().next();
        final long validRequestId = createRequest(defaultBuilder());
        final long rejectedRequestId = createRequest(defaultBuilder().status(QuotaChangeRequest.Status.REJECTED));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(validRequestId, null,
                        ImmutableList.of(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), null)),
                        ""),
                new SetResourceAmountBody.Item(
                        rejectedRequestId, null,
                        ImmutableList.of(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))),
                        ""
                )
        )
        );

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Wrong status of requests", error.getMessage());
        assertEquals(Collections.singleton(rejectedRequestId), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldCheckRequestStatusIfAnyAllocatingPresence() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final long validRequestId = createRequest(defaultBuilder(aggregatedCampaign));
        final long notConfirmedStatusRequestId = createRequest(defaultBuilder(aggregatedCampaign).status(QuotaChangeRequest.Status.NEW));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(validRequestId, null,
                        ImmutableList.of(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), null)),
                        ""),
                new SetResourceAmountBody.Item(
                        notConfirmedStatusRequestId, null,
                        ImmutableList.of(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))),
                        ""
                )
        )
        );

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("'CONFIRMED' status required for requests", error.getMessage());
        assertEquals(Collections.singleton(notConfirmedStatusRequestId), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldntCheckRequestStatusIfNotAllocatingPresence() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final long validRequestId = createRequest(defaultBuilder(aggregatedCampaign));
        final long notConfirmedStatusRequestId = createRequest(defaultBuilder(aggregatedCampaign).status(QuotaChangeRequest.Status.NEW));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(validRequestId, null,
                        ImmutableList.of(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), null)),
                        ""),
                new SetResourceAmountBody.Item(
                        notConfirmedStatusRequestId, null,
                        ImmutableList.of(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), null)),
                        ""
                )
        )
        );

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void onChangeAmountShouldCheckRequestStatusWithTicketKeys() {

        final String ticket = "Foo";
        final String invalidRequestTicket = "Bar";

        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(ticket));
        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(invalidRequestTicket).status(QuotaChangeRequest.Status.NEW));

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(null, ticket,
                        ImmutableList.of(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), null)),
                        ""),
                new SetResourceAmountBody.Item(null, invalidRequestTicket,
                        ImmutableList.of(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))),
                        ""
                )
        )
        );

        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("'CONFIRMED' status required for requests", error.getMessage());
        assertEquals(Collections.singleton(invalidRequestTicket), Sets.newHashSet(error.getProblemTicketKeys()));
    }

    @Test
    public void onChangeAmountShouldCheckUserIsServiceAdmin() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long validRequestNirvanaId = createRequest(defaultBuilder(aggregatedCampaign));
        final long validRequestMdbId = createRequest(defaultBuilder(aggregatedCampaign).changes(Collections.singletonList(
                QuotaChangeRequest.Change.newChangeBuilder().order(bigOrder).resource(cpu).segments(Collections.emptySet()).amount(20_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(validRequestMdbId, null, Collections.singletonList(
                        new SetResourceAmountBody.ChangeBody(MDB, bigOrder.getId(), CPU, Collections.emptySet(), DiAmount.of(1, DiUnit.CORES), null)
                ), ""),
                new SetResourceAmountBody.Item(validRequestNirvanaId, null, Collections.singletonList(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getId(), YT_CPU, Collections.emptySet(), DiAmount.of(1, DiUnit.COUNT), null)), "")
        ));

        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Only service's admin can modify requests", error.getMessage());
        assertEquals(ImmutableSet.of(validRequestMdbId, validRequestNirvanaId), Sets.newHashSet(error.getProblemRequestIds()));

        response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        error = result.getErrors().iterator().next();
        assertEquals("Only service's admin can modify requests", error.getMessage());
        assertEquals(ImmutableSet.of(validRequestMdbId), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldCheckUserIsServiceAdminWithTicketKeys() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String ticketKey = "foo";
        final String otherTicketKey = "bar";
        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(ticketKey));
        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(otherTicketKey).changes(Collections.singletonList(
                QuotaChangeRequest.Change.newChangeBuilder().resource(cpu).order(bigOrder).segments(Collections.emptySet()).amount(20_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(null, ticketKey, Collections.singletonList(
                        new SetResourceAmountBody.ChangeBody(MDB, bigOrder.getId(), CPU, Collections.emptySet(), DiAmount.of(1, DiUnit.CORES), null)
                ), ""),
                new SetResourceAmountBody.Item(null, otherTicketKey, Collections.singletonList(new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getId(), YT_CPU, Collections.emptySet(), DiAmount.of(1, DiUnit.COUNT), null)), "")
        ));

        Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Only service's admin can modify requests", error.getMessage());
        assertEquals(ImmutableSet.of(ticketKey, otherTicketKey), Sets.newHashSet(error.getProblemTicketKeys()));

        response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        error = result.getErrors().iterator().next();
        assertEquals("Only service's admin can modify requests", error.getMessage());
        assertEquals(ImmutableSet.of(ticketKey), Sets.newHashSet(error.getProblemTicketKeys()));
    }

    @Test
    public void onChangeAmountShouldCheckRequestResources() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), ""),
                new SetResourceAmountBody.Item(request2Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), "")
        ));

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Modified resources not presented in requests", error.getMessage());
        assertEquals(ImmutableSet.of(request1Id), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldCheckRequestResourcesWithTicketKeys() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String ticketKey = "foo";
        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(ticketKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build()
        )));

        final String otherTicketKey = "bar";
        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(otherTicketKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(null, ticketKey, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), ""),
                new SetResourceAmountBody.Item(null, otherTicketKey, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), "")
        ));

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Modified resources not presented in requests", error.getMessage());
        assertEquals(ImmutableSet.of(ticketKey), Sets.newHashSet(error.getProblemTicketKeys()));
    }

    @Test
    public void onChangeAmountShouldCheckAmountGreaterThanAllocated() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(1_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(1, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), ""),
                new SetResourceAmountBody.Item(request2Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), "")
        ));

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Amount allocated must be lesser than amount ordered and cannot be decreased or be greater than already allocating", error.getMessage());
        assertEquals(ImmutableSet.of(request1Id), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldCheckAmountGreaterThanAllocatedWithTicketKeys() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String ticketKey = "foo";
        final String otherTicketKey = "bar";
        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(ticketKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(1_000).build()
        )));

        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(otherTicketKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(null, ticketKey, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(1, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), ""),
                new SetResourceAmountBody.Item(null, otherTicketKey, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), "")
        ));

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Amount allocated must be lesser than amount ordered and cannot be decreased or be greater than already allocating", error.getMessage());
        assertEquals(ImmutableSet.of(ticketKey), Sets.newHashSet(error.getProblemTicketKeys()));
    }

    @Test
    public void onChangeAmountShouldCheckDecreasingAllocated() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet())
                        .amount(100_000).amountReady(50_000).amountAllocated(50_000).amountAllocating(50_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), null, DiAmount.of(10, DiUnit.COUNT))
                ), "")
        ));

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Amount allocated must be lesser than amount ordered and cannot be decreased or be greater than already allocating", error.getMessage());
        assertEquals(ImmutableSet.of(request1Id), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldCheckAmountAllocatedNotGreaterThanAllocating() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet())
                        .amount(100_000).amountReady(50_000).amountAllocated(50_000).amountAllocating(50_000).build()
        )));

        SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), null, DiAmount.of(60, DiUnit.COUNT))
                ), "")
        ));

        Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(200, response.getStatus());
        DiSetAmountResult result = response.readEntity(DiSetAmountResult.class);

        assertEquals("SUCCESS", result.getStatus()); //if allocated == allocating, these values will increase simultaneously

        final QuotaChangeRequest request = requestDao.read(request1Id);
        final QuotaChangeRequest.Change change = Iterables.getOnlyElement(request.getChanges());
        final Person amosovf = Hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        requestManager.setChangesAllocatingAmount(List.of(request), Map.of(change.getId(), 80_000L), new PerformerContext(amosovf));

        body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), null, DiAmount.of(70, DiUnit.COUNT))
                ), "")
        ));

        response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(200, response.getStatus());
        result = response.readEntity(DiSetAmountResult.class);

        assertEquals("SUCCESS", result.getStatus()); //ok, allocated < allocating

        body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), null, DiAmount.of(90, DiUnit.COUNT))
                ), "")
        ));

        response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors errors = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", errors.getStatus()); //not ok, allocated > allocating

        final DiSetAmountResult.Errors.Item error = errors.getErrors().iterator().next();
        assertEquals("Amount allocated must be lesser than amount ordered and cannot be decreased or be greater than already allocating", error.getMessage());
        assertEquals(ImmutableSet.of(request1Id), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldCheckAmountGreaterThanReady() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(1_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), ""),
                new SetResourceAmountBody.Item(request2Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(1, DiUnit.COUNT))
                ), "")
        ));

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Amount ready must be lesser than amount ordered", error.getMessage());
        assertEquals(ImmutableSet.of(request2Id), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onChangeAmountShouldCheckAmountGreaterThanReadyWithTicketKey() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        String ticketKey = "foo";
        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(ticketKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build()
        )));

        String otherTicketKey = "bar";
        createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(otherTicketKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(100_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(1_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(null, ticketKey, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                ), ""),
                new SetResourceAmountBody.Item(null, otherTicketKey, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(1, DiUnit.COUNT))
                ), "")
        ));

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Amount ready must be lesser than amount ordered", error.getMessage());
        assertEquals(ImmutableSet.of(otherTicketKey), Sets.newHashSet(error.getProblemTicketKeys()));
    }

    @Test
    public void requestAllocatedShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), null, DiAmount.of(1, DiUnit.COUNT))
                ), ""),
                new SetResourceAmountBody.Item(request2Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), null, DiAmount.of(2, DiUnit.COUNT))
                ), "")
        ));

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(0, request1DiChange1.getAmountReady().getValue());
        assertEquals(1, DiUnit.COUNT.convert(request1DiChange1.getAmountAllocated()));

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(0, request2CpuDiChange.getAmountReady().getValue());
        assertEquals(2, DiUnit.COUNT.convert(request2CpuDiChange.getAmountAllocated()));
    }

    @Test
    public void requestAllocatedShouldChangeAmountForOldCampaign() {
        campaignDao.delete(campaign);
        campaignDao.delete(aggregatedCampaign);
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setSingleProviderRequestModeEnabled(true)
                .build());
        aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setType(Campaign.Type.AGGREGATED)
                .setName("test-aggregated")
                .setKey("test-aggregated")
                .setSingleProviderRequestModeEnabled(true)
                .build());
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(null, null, YT_CPU, Collections.emptySet(), null, DiAmount.of(1, DiUnit.COUNT))
                ), ""),
                new SetResourceAmountBody.Item(request2Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(null, null, YT_CPU, Collections.emptySet(), null, DiAmount.of(2, DiUnit.COUNT))
                ), "")
        ));

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(0, request1DiChange1.getAmountReady().getValue());
        assertEquals(1, DiUnit.COUNT.convert(request1DiChange1.getAmountAllocated()));

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(0, request2CpuDiChange.getAmountReady().getValue());
        assertEquals(2, DiUnit.COUNT.convert(request2CpuDiChange.getAmountAllocated()));
    }

    @Test
    public void requestReadyShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT), null)
                ), ""),
                new SetResourceAmountBody.Item(request2Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(4, DiUnit.COUNT), null)
                ), "")
        ));

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, request1DiChange1.getResource().getKey());
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(3, DiUnit.COUNT.convert(request1DiChange1.getAmountReady()));
        assertEquals(0, request1DiChange1.getAmountAllocated().getValue());

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(c -> c.getResource().getKey(), Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(4, DiUnit.COUNT.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(0, request2CpuDiChange.getAmountAllocated().getValue());
    }

    @Test
    public void requestReadyShouldChangeAmountWithTicketKeys() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String ticketKey = "foo";
        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(ticketKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final String otherTicketKey = "bar";
        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(otherTicketKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(null, ticketKey, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT), null)
                ), ""),
                new SetResourceAmountBody.Item(null, otherTicketKey, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(4, DiUnit.COUNT), null)
                ), "")
        ));

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(3, DiUnit.COUNT.convert(request1DiChange1.getAmountReady()));
        assertEquals(0, request1DiChange1.getAmountAllocated().getValue());

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(4, DiUnit.COUNT.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(0, request2CpuDiChange.getAmountAllocated().getValue());
    }

    @Test
    public void requestReadyShouldChangeAmountForOldCampaign() {
        campaignDao.delete(campaign);
        campaignDao.delete(aggregatedCampaign);
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setSingleProviderRequestModeEnabled(true)
                .build());
        aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setType(Campaign.Type.AGGREGATED)
                .setKey("test-aggregated")
                .setName("test-aggregated")
                .setSingleProviderRequestModeEnabled(true)
                .build());

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(request1Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(null, null, YT_CPU, Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT), null)
                ), ""),
                new SetResourceAmountBody.Item(request2Id, null, ImmutableList.of(
                        new SetResourceAmountBody.ChangeBody(null, null, YT_CPU, Collections.emptySet(), DiAmount.of(4, DiUnit.COUNT), null)
                ), "")
        ));

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(3, DiUnit.COUNT.convert(request1DiChange1.getAmountReady()));
        assertEquals(0, request1DiChange1.getAmountAllocated().getValue());

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(4, DiUnit.COUNT.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(0, request2CpuDiChange.getAmountAllocated().getValue());
    }

    @Test
    public void allResourceAllocationShouldCompleteRequest() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final QuotaChangeRequest.BigOrder requestOrder = new QuotaChangeRequest.BigOrder(bigOrder.getBigOrderId(), bigOrder.getDate(), true);

        final Resource ram = resourceDao.create(new Resource.Builder(RAM, nirvana)
                .name(RAM)
                .type(DiResourceType.MEMORY)
                .mode(DiQuotingMode.DEFAULT)
                .build());
        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(ram)
                                .order(requestOrder).segments(Collections.emptySet()).amount(1024 * 1024).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu)
                                .order(requestOrder).segments(Collections.emptySet()).amount(20_000).build()
                ))
        );

        dispenser()
                .quotaChangeRequests()
                .byId(requestId)
                .createTicket()
                .performBy(AMOSOV_F);

        DiSetAmountResult updateReadyResult = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, new SetResourceAmountBody(ImmutableList.of(
                        new SetResourceAmountBody.Item(requestId, null, ImmutableList.of(
                                new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT), DiAmount.of(10, DiUnit.COUNT))
                        ), "First part")
                )), DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, updateReadyResult);

        DiQuotaChangeRequest request = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, request.getStatus());

        updateReadyResult = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, new SetResourceAmountBody(ImmutableList.of(
                        new SetResourceAmountBody.Item(requestId, null, ImmutableList.of(
                                new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(), DiAmount.of(20, DiUnit.COUNT), DiAmount.of(20, DiUnit.COUNT))
                        ), "All complete")
                )), DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, updateReadyResult);

        request = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class);

        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, request.getStatus());

        final Map<String, Object> issueFields = trackerManager.getIssueFields(request.getTrackerIssueKey());
        assertEquals("closed", issueFields.get("status"));
        assertEquals(QuotaChangeRequestTicketManager.FIXED_RESOLUTION_ID, issueFields.get("resolution"));

        final List<CommentCreate> issueComments = trackerManager.getIssueComments(request.getTrackerIssueKey());
        final Optional<CommentCreate> completeComment = issueComments.stream().filter(c -> c.getComment().get().contains("кем:sancho заявка переведена в статус %%Квота выдана%%")).findFirst();
        final CommentCreate lastComment = Iterables.getLast(issueComments);

        assertTrue(completeComment.isPresent());
        assertTrue(completeComment.get().getSummonees().isEmpty());
    }

    @Test
    public void requestOptionalReadyShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id, null, null, null),
                new SetResourceAmountBodyOptional.Item(request2Id, null, null, null)
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmountReady()));
        assertEquals(0, request1DiChange1.getAmountAllocated().getValue());

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(0, request2CpuDiChange.getAmountAllocated().getValue());

        final DiQuotaChangeRequest.Change request2GpuDiChange = request2DiChangeByKey.get(YT_GPU);

        assertEquals(10, DiUnit.COUNT.convert(request2GpuDiChange.getAmount()));
        assertEquals(10, DiUnit.COUNT.convert(request2GpuDiChange.getAmountReady()));
        assertEquals(0, request2GpuDiChange.getAmountAllocated().getValue());
    }

    @Test
    public void requestOptionalAllocatedShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id, null, null, null),
                new SetResourceAmountBodyOptional.Item(request2Id, null, null, null)
        ), SetResourceAmountBodyOptional.UpdateFor.ALLOCATED);

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(0, DiUnit.COUNT.convert(request1DiChange1.getAmountReady()));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmountAllocated()));

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(0, DiUnit.COUNT.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmountAllocated()));

        final DiQuotaChangeRequest.Change request2GpuDiChange = request2DiChangeByKey.get(YT_GPU);

        assertEquals(10, DiUnit.COUNT.convert(request2GpuDiChange.getAmount()));
        assertEquals(0, DiUnit.COUNT.convert(request2GpuDiChange.getAmountReady()));
        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmountAllocated()));
    }

    @Test
    public void requestOptionalBothShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id, null, null, null),
                new SetResourceAmountBodyOptional.Item(request2Id, null, null, null)
        ), SetResourceAmountBodyOptional.UpdateFor.BOTH);

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmountReady()));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmountAllocated()));

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmountAllocated()));

        final DiQuotaChangeRequest.Change request2GpuDiChange = request2DiChangeByKey.get(YT_GPU);

        assertEquals(10, DiUnit.COUNT.convert(request2GpuDiChange.getAmount()));
        assertEquals(10, DiUnit.COUNT.convert(request2GpuDiChange.getAmountReady()));
        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmountAllocated()));
    }

    @Test
    public void requestOptionalWithResourceReadyShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytGpu).order(bigOrder).segments(Collections.emptySet()).amount(10_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, YT_CPU, null, null)), ""),
                new SetResourceAmountBodyOptional.Item(request2Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, YT_CPU, null, null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final DiSetAmountResult result = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(YT_CPU, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmount()));
        assertEquals(15, DiUnit.COUNT.convert(request1DiChange1.getAmountReady()));
        assertEquals(0, DiUnit.COUNT.convert(request1DiChange1.getAmountAllocated()));

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(YT_CPU);

        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmount()));
        assertEquals(10, DiUnit.COUNT.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(0, DiUnit.COUNT.convert(request2CpuDiChange.getAmountAllocated()));

        final DiQuotaChangeRequest.Change request2GpuDiChange = request2DiChangeByKey.get(YT_GPU);

        assertEquals(10, DiUnit.COUNT.convert(request2GpuDiChange.getAmount()));
        assertEquals(0, DiUnit.COUNT.convert(request2GpuDiChange.getAmountReady()));
        assertEquals(0, DiUnit.COUNT.convert(request2CpuDiChange.getAmountAllocated()));
    }


    private void getResourceWithSegment() {


        updateHierarchy();
    }

    @Test
    public void requestOptionalWithSegmentsReadyShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).segments(segmentHddSegments).amount(10_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, null,
                                segmentStorageSegments.stream().map(Segment::getPublicKey).collect(Collectors.toSet()), null)), ""),
                new SetResourceAmountBodyOptional.Item(request2Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, null,
                                segmentStorageSegments.stream().map(Segment::getPublicKey).collect(Collectors.toSet()), null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final DiSetAmountResult result = createAuthorizedLocalClient(SLONNN)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(SEGMENT_STORAGE, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15_000, DiUnit.BYTE.convert(request1DiChange1.getAmount()));
        assertEquals(15_000, DiUnit.BYTE.convert(request1DiChange1.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request1DiChange1.getAmountAllocated()));

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(SEGMENT_STORAGE);
        assertEquals(10_000, DiUnit.BYTE.convert(request2CpuDiChange.getAmount()));
        assertEquals(10_000, DiUnit.BYTE.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request2CpuDiChange.getAmountAllocated()));

        final DiQuotaChangeRequest.Change request2GpuDiChange = request2DiChangeByKey.get(SEGMENT_HDD);
        assertEquals(10_000, DiUnit.BYTE.convert(request2GpuDiChange.getAmount()));
        assertEquals(0, DiUnit.BYTE.convert(request2GpuDiChange.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request2CpuDiChange.getAmountAllocated()));
    }

    @Test
    public void requestOptionalWithServiceKeyShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(bigOrder).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(ytCpu.getService().getKey(), null,
                        null, null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final DiSetAmountResult result = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);
        final Map<String, DiQuotaChangeRequest.Change> request1DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change ytCpuChange = request1DiChangeByKey.get(YT_CPU);
        final DiQuotaChangeRequest.Change segmentStorageChange = request1DiChangeByKey.get(SEGMENT_STORAGE);
        assertEquals(10_000L, DiUnit.BYTE.convert(segmentStorageChange.getAmount()));
        assertEquals(0L, DiUnit.BYTE.convert(segmentStorageChange.getAmountReady()));

        assertEquals(15_000L, DiUnit.PERMILLE.convert(ytCpuChange.getAmount()));
        assertEquals(15_000L, DiUnit.PERMILLE.convert(ytCpuChange.getAmountReady()));
    }

    @Test
    public void requestOptionalWithOrderIdShouldChangeAmount() {
        final Set<BigOrder> simpleBigOrders = bigOrderManager.getByIds(Set.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()));
        final List<Campaign.BigOrder> orders = simpleBigOrders.stream()
                .sorted(Comparator.comparing(BigOrder::getId))
                .map(Campaign.BigOrder::new)
                .collect(Collectors.toList());
        campaignDao.clear();
        botCampaignGroupDao.clear();
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setStatus(Campaign.Status.CLOSED)
                .setBigOrders(orders)
                .build());
        aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setStatus(Campaign.Status.CLOSED)
                .setBigOrders(orders)
                .setType(Campaign.Type.AGGREGATED)
                .setKey("aggregated-test")
                .setName("aggregated-test")
                .build());
        botCampaignGroup = botCampaignGroupDao.create(defaultCampaignGroupBuilder()
                .setBigOrders(new ArrayList<>(simpleBigOrders))
                .addCampaign(campaignDao.readForBot(Collections.singleton(aggregatedCampaign.getId())).values().iterator().next())
                .build());

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(orders.get(0)).segments(segmentStorageSegments).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(ytCpu).order(orders.get(1)).segments(Collections.emptySet()).amount(15_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, null,
                        null, orders.get(0).getBigOrderId())), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final DiSetAmountResult result = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);
        final Map<String, DiQuotaChangeRequest.Change> request1DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change ytCpuChange = request1DiChangeByKey.get(YT_CPU);
        final DiQuotaChangeRequest.Change segmentStorageChange = request1DiChangeByKey.get(SEGMENT_STORAGE);
        assertEquals(10_000L, DiUnit.BYTE.convert(segmentStorageChange.getAmount()));
        assertEquals(10_000L, DiUnit.BYTE.convert(segmentStorageChange.getAmountReady()));

        assertEquals(15_000L, DiUnit.PERMILLE.convert(ytCpuChange.getAmount()));
        assertEquals(0L, DiUnit.PERMILLE.convert(ytCpuChange.getAmountReady()));
    }

    @Test
    public void requestOptionalWithResourceAndSegmentsReadyShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(10_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, SEGMENT_STORAGE,
                                segmentStorageSegments.stream().map(Segment::getPublicKey).collect(Collectors.toSet()), null)), ""),
                new SetResourceAmountBodyOptional.Item(request2Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, SEGMENT_HDD,
                                segmentHddSegments.stream().map(Segment::getPublicKey).collect(Collectors.toSet()), null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final DiSetAmountResult result = createAuthorizedLocalClient(SLONNN)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(SEGMENT_STORAGE, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15_000, DiUnit.BYTE.convert(request1DiChange1.getAmount()));
        assertEquals(15_000, DiUnit.BYTE.convert(request1DiChange1.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request1DiChange1.getAmountAllocated()));

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(SEGMENT_STORAGE);
        assertEquals(10_000, DiUnit.BYTE.convert(request2CpuDiChange.getAmount()));
        assertEquals(0, DiUnit.BYTE.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request2CpuDiChange.getAmountAllocated()));

        final DiQuotaChangeRequest.Change request2GpuDiChange = request2DiChangeByKey.get(SEGMENT_HDD);
        assertEquals(10_000, DiUnit.BYTE.convert(request2GpuDiChange.getAmount()));
        assertEquals(10_000, DiUnit.BYTE.convert(request2GpuDiChange.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request2CpuDiChange.getAmountAllocated()));
    }

    @Test
    public void requestOptionalWithMixReadyShouldChangeAmount() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder(aggregatedCampaign).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentHddSegments).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(10_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, SEGMENT_STORAGE,
                                segmentStorageSegments.stream().map(Segment::getPublicKey).collect(Collectors.toSet()), null)), ""),
                new SetResourceAmountBodyOptional.Item(request2Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, null,
                                segmentHddSegments.stream().map(Segment::getPublicKey).collect(Collectors.toSet()), null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final DiSetAmountResult result = createAuthorizedLocalClient(SLONNN)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body, DiSetAmountResult.class);

        assertEquals(DiSetAmountResult.SUCCESS, result);

        final DiQuotaChangeRequest.Change request1DiChange1 = dispenser().quotaChangeRequests()
                .byId(request1Id)
                .get()
                .perform()
                .getChanges().iterator().next();

        assertEquals(SEGMENT_STORAGE, GET_RESOURCE_KEY.apply(request1DiChange1));
        assertEquals(15_000, DiUnit.BYTE.convert(request1DiChange1.getAmount()));
        assertEquals(15_000, DiUnit.BYTE.convert(request1DiChange1.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request1DiChange1.getAmountAllocated()));

        final Map<String, DiQuotaChangeRequest.Change> request2DiChangeByKey = dispenser().quotaChangeRequests()
                .byId(request2Id)
                .get()
                .perform()
                .getChanges().stream()
                .collect(Collectors.toMap(GET_RESOURCE_KEY, Function.identity()));

        final DiQuotaChangeRequest.Change request2CpuDiChange = request2DiChangeByKey.get(SEGMENT_STORAGE);
        assertEquals(10_000, DiUnit.BYTE.convert(request2CpuDiChange.getAmount()));
        assertEquals(10_000, DiUnit.BYTE.convert(request2CpuDiChange.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request2CpuDiChange.getAmountAllocated()));

        final DiQuotaChangeRequest.Change request2GpuDiChange = request2DiChangeByKey.get(SEGMENT_HDD);
        assertEquals(10_000, DiUnit.BYTE.convert(request2GpuDiChange.getAmount()));
        assertEquals(10_000, DiUnit.BYTE.convert(request2GpuDiChange.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(request2CpuDiChange.getAmountAllocated()));
    }

    @Test
    public void onOptionalChangeAmountShouldCheckRequestId() {
        requestDao.clear();

        final long inValidRequestId = 666L;

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(inValidRequestId,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, YT_CPU, null, null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Non-existing request ids", error.getMessage());
        assertEquals(Collections.singleton(inValidRequestId), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onOptionalChangeAmountShouldCheckTicketKey() {
        requestDao.clear();

        final String inValidRequestTicketKey = "foo";

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(null, inValidRequestTicketKey,
                        ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, YT_CPU, null, null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Unknown ticket keys", error.getMessage());
        assertEquals(Collections.singleton(inValidRequestTicketKey), Sets.newHashSet(error.getProblemTicketKeys()));
    }

    @Test
    public void onOptionalChangeAmountShouldCheckResourcesInRequest() {
        final Campaign.BigOrder bigOrder = campaign.getBigOrders().iterator().next();

        final long request1Id = createRequest(defaultBuilder().changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(15_000).build()
        )));

        final long request2Id = createRequest(defaultBuilder().changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentHddSegments).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).segments(segmentHddSegments).amount(10_000).build()
        )));

        final long request3Id = createRequest(defaultBuilder().changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(15_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(request1Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, SEGMENT_STORAGE,
                                Sets.newHashSet("some_wrong_segment"), null)), ""),
                new SetResourceAmountBodyOptional.Item(request2Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, null,
                                segmentStorageSegments.stream().map(Segment::getPublicKey).collect(Collectors.toSet()), null)), ""),
                new SetResourceAmountBodyOptional.Item(request3Id,
                        null, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, SEGMENT_HDD, null, null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final Response response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Modified resources not presented in requests", error.getMessage());
        assertEquals(new HashSet<>(Arrays.asList(request1Id, request2Id, request3Id)), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void onOptionalChangeAmountShouldCheckResourcesInRequestWithTicketKeys() {
        final String request1Key = "foo";
        final long request1Id = createRequest(defaultBuilder().trackerIssueKey(request1Key).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).segments(segmentStorageSegments).amount(15_000).build()
        )));

        final String request2Key = "bar";
        final long request2Id = createRequest(defaultBuilder().trackerIssueKey(request2Key).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).segments(segmentHddSegments).amount(10_000).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).segments(segmentHddSegments).amount(10_000).build()
        )));

        final String request3Key = "baz";
        final long request3Id = createRequest(defaultBuilder().trackerIssueKey(request3Key).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).segments(segmentStorageSegments).amount(15_000).build()
        )));

        final SetResourceAmountBodyOptional body = new SetResourceAmountBodyOptional(ImmutableList.of(
                new SetResourceAmountBodyOptional.Item(null,
                        request1Key, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, SEGMENT_STORAGE,
                        Sets.newHashSet("some_wrong_segment"), null)), ""),
                new SetResourceAmountBodyOptional.Item(null,
                        request2Key, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, null,
                        segmentStorageSegments.stream().map(Segment::getPublicKey).collect(Collectors.toSet()), null)), ""),
                new SetResourceAmountBodyOptional.Item(null,
                        request3Key, ImmutableList.of(new SetResourceAmountBodyOptional.ChangeBody(null, SEGMENT_HDD, null, null)), "")
        ), SetResourceAmountBodyOptional.UpdateFor.READY);

        final Response response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/resource-preorder/quotaStateOptional")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode(), response.getStatus());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();
        assertEquals("Modified resources not presented in requests", error.getMessage());
        assertEquals(Sets.newHashSet(request1Key, request2Key, request3Key), Sets.newHashSet(error.getProblemTicketKeys()));
    }

    private long setUpTrackerCommentTest(final DiPerson person) {
        campaignDao.getAllSorted(ImmutableSet.of(Campaign.Status.ACTIVE)).stream().findFirst().get();
        serviceDao.attachAdmin(serviceDao.read(NIRVANA), personDao.readPersonByLogin(person.getLogin()));

        createProject(TEST_PROJECT_KEY, YANDEX, person.getLogin());

        final DiQuotaChangeRequest quotaRequest = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300L, DiUnit.COUNT))
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(500L, DiUnit.GIBIBYTE))
                        .build(), aggregatedCampaign.getId())
                .performBy(person)
                .getFirst();

        updateHierarchy();

        requestDao.update(requestDao.read(quotaRequest.getId()).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        return quotaRequest.getId();
    }

    private void trackerCommentTestTemplate(final SetResourceAmountBody.ChangeBody[] changeBodies,
                                            final String comment,
                                            final String expectedComment,
                                            final DiPerson person) {
        final long id = setUpTrackerCommentTest(person);

        trackerCommentTestTemplate(changeBodies, comment, expectedComment, person, id);
    }

    private void trackerCommentTestTemplate(final SetResourceAmountBody.ChangeBody[] changeBodies,
                                            final String comment,
                                            final String expectedComment,
                                            final DiPerson person,
                                            final long requestId) {
        final ArrayList<SetResourceAmountBody.Item> updates = new ArrayList<>();
        final ArrayList<SetResourceAmountBody.ChangeBody> changes = new ArrayList<>(Arrays.asList(changeBodies));

        final SetResourceAmountBody.Item item = new SetResourceAmountBody.Item(requestId, null, changes, comment);

        updates.add(item);

        final SetResourceAmountBody setResourceAmountBody = new SetResourceAmountBody(updates);

        final ArgumentCaptor<String> givenComment = ArgumentCaptor.forClass(String.class);

        final QuartzTrackerCommentTrigger mock = mock(QuartzTrackerCommentTrigger.class);
        final ResourcePreorderChangeManager changeManager =
                new ResourcePreorderChangeManager(requestManager, requestDao, messageHelper, mock);

        final PerformerContext context = new PerformerContext(personDao.read(person.getLogin()));
        final QuotaRequestChangeValues changeValues = new ResourcePreorderRequestUtils(requestDao, campaignDao, errorMessageSource).getChanges(setResourceAmountBody, context);
        changeManager.postAmountCommentsToTracker(changeValues, context);

        verify(mock).run(any(), givenComment.capture());
        assertEquals(expectedComment, givenComment.getValue());
    }

    @Test
    public void trackerCommentShouldIgnoreReadyForAllocatedEqualsTest() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        trackerCommentTestTemplate(new SetResourceAmountBody.ChangeBody[]{
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(),
                                DiAmount.of(100L, DiUnit.COUNT), DiAmount.of(100L, DiUnit.COUNT)),
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(),
                                DiAmount.of(200L, DiUnit.COUNT), DiAmount.of(200L, DiUnit.COUNT))},
                "Test comment",
                "Ресурсы заявки были помечены кем:binarycat как выданные:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU | YT GPU ||\n" +
                        "|| Январь 2020 | 100 units | 200 units ||\n" +
                        "|#\n" +
                        "----\n" +
                        "Всего выдано по заявке:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU | YT GPU ||\n" +
                        "|| Январь 2020 | 100 units | 200 units ||\n" +
                        "|#\n" +
                        "----\n" +
                        "\n" +
                        "Комментарий: Test comment",
                BINARY_CAT);
    }

    @Test
    public void trackerCommentShouldShowReadyAndAllocatedForDiffResourcesTest() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        trackerCommentTestTemplate(new SetResourceAmountBody.ChangeBody[]{
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(),
                                DiAmount.of(100L, DiUnit.COUNT), null),
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(),
                                null, DiAmount.of(200L, DiUnit.COUNT))},
                "Test comment",
                "Ресурсы заявки были помечены кем:binarycat как готовые:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU ||\n" +
                        "|| Январь 2020 | 100 units ||\n" +
                        "|#\n" +
                        "Для получения ресурсов в виде квоты нужно нажать на кнопку «Получить квоту» в списке заявок в ABC.\n" +
                        "----\n" +
                        "Ресурсы заявки были помечены кем:binarycat как выданные:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT GPU ||\n" +
                        "|| Январь 2020 | 200 units ||\n" +
                        "|#\n" +
                        "----\n" +
                        "Всего выдано по заявке:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT GPU ||\n" +
                        "|| Январь 2020 | 200 units ||\n" +
                        "|#\n" +
                        "----\n" +
                        "\n" +
                        "Комментарий: Test comment",
                BINARY_CAT);
    }

    @Test
    public void trackerCommentShouldAddReadyAndAllocatedForNotEqualsTest() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        trackerCommentTestTemplate(new SetResourceAmountBody.ChangeBody[]{
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(),
                                DiAmount.of(25L, DiUnit.COUNT), DiAmount.of(50L, DiUnit.COUNT)),
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(),
                                null, DiAmount.of(200L, DiUnit.COUNT))},
                null,
                "Ресурсы заявки были помечены кем:binarycat как готовые:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU ||\n" +
                        "|| Январь 2020 | 25 units ||\n" +
                        "|#\n" +
                        "Для получения ресурсов в виде квоты нужно нажать на кнопку «Получить квоту» в списке заявок в ABC.\n" +
                        "----\n" +
                        "Ресурсы заявки были помечены кем:binarycat как выданные:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU | YT GPU ||\n" +
                        "|| Январь 2020 | 50 units | 200 units ||\n" +
                        "|#\n" +
                        "----\n" +
                        "Всего выдано по заявке:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU | YT GPU ||\n" +
                        "|| Январь 2020 | 50 units | 200 units ||\n" +
                        "|#\n" +
                        "----\n",
                BINARY_CAT);
    }

    @Test
    public void trackerReadyCommentShouldContainsInfoAboutNextAllocationStep() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final Service updatedService = Service.withKey(NIRVANA)
                .withName(nirvana.getName())
                .withPriority(nirvana.getPriority())
                .withAbcServiceId(nirvana.getAbcServiceId())
                .withId(nirvana.getId())
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(nirvana.getSettings().accountActualValuesInQuotaDistribution())
                        .usesProjectHierarchy(nirvana.getSettings().usesProjectHierarchy())
                        .manualQuotaAllocation(true)
                        .build())
                .build();

        serviceDao.update(updatedService);
        updateHierarchy();

        resourceDao.updateAll(
                resourceDao.getByService(updatedService).stream()
                        .map(r -> new Resource.Builder(r.getKey().getPublicKey(), updatedService)
                                .mode(r.getMode())
                                .type(r.getType())
                                .name(r.getName())
                                .id(r.getId())
                                .build()
                        ).collect(Collectors.toList())
        );


        trackerCommentTestTemplate(new SetResourceAmountBody.ChangeBody[]{
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(),
                                DiAmount.of(25L, DiUnit.COUNT), DiAmount.of(50L, DiUnit.COUNT)),
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_GPU, Collections.emptySet(),
                                null, DiAmount.of(200L, DiUnit.COUNT))},
                null,
                "Ресурсы заявки были помечены кем:binarycat как готовые:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU ||\n" +
                        "|| Январь 2020 | 25 units ||\n" +
                        "|#\n" +
                        "Для получения ресурсов в виде квоты нужно нажать на кнопку «Получить квоту» в списке заявок " +
                        "в ABC.\n" +
                        "----\n" +
                        "Ресурсы заявки были помечены кем:binarycat как выданные:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU | YT GPU ||\n" +
                        "|| Январь 2020 | 50 units | 200 units ||\n" +
                        "|#\n" +
                        "----\n" +
                        "Всего выдано по заявке:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU | YT GPU ||\n" +
                        "|| Январь 2020 | 50 units | 200 units ||\n" +
                        "|#\n" +
                        "----\n",
                BINARY_CAT);
    }

    @Test
    public void trackerReadyCommentShouldContainsInfoAboutNextAllocationStepIfStatusIsNotConfirmed() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final Service updatedService = Service.withKey(NIRVANA)
                .withName(nirvana.getName())
                .withPriority(nirvana.getPriority())
                .withAbcServiceId(nirvana.getAbcServiceId())
                .withId(nirvana.getId())
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(nirvana.getSettings().accountActualValuesInQuotaDistribution())
                        .usesProjectHierarchy(nirvana.getSettings().usesProjectHierarchy())
                        .manualQuotaAllocation(true)
                        .build())
                .build();

        serviceDao.update(updatedService);
        updateHierarchy();

        resourceDao.updateAll(
                resourceDao.getByService(updatedService).stream()
                        .map(r -> new Resource.Builder(r.getKey().getPublicKey(), updatedService)
                                .mode(r.getMode())
                                .type(r.getType())
                                .name(r.getName())
                                .id(r.getId())
                                .build()
                        ).collect(Collectors.toList())
        );

        final long id = setUpTrackerCommentTest(BINARY_CAT);
        requestDao.update(
                requestDao.read(id).copyBuilder()
                        .status(QuotaChangeRequest.Status.NEW)
                        .build()
        );

        trackerCommentTestTemplate(new SetResourceAmountBody.ChangeBody[]{
                        new SetResourceAmountBody.ChangeBody(NIRVANA, bigOrder.getBigOrderId(), YT_CPU, Collections.emptySet(),
                                DiAmount.of(25L, DiUnit.COUNT), null)
                },
                null,
                "Ресурсы заявки были помечены кем:amosov-f как готовые:\n" +
                        "Провайдер: Nirvana\n" +
                        "#|\n" +
                        "||  | YT CPU ||\n" +
                        "|| Январь 2020 | 25 units ||\n" +
                        "|#\n" +
                        "Для получения ресурсов в виде квоты нужно нажать на кнопку «Получить квоту» в списке заявок в ABC.\n" +
                        "Эта заявка находится в статусе %%Черновик%%, поэтому сейчас получить по ней квоту нельзя. Кнопка получения появится сразу как только заявка станет подтверждена. Это должен сделать ((https://wiki.yandex-team.ru/Intranet/abc/features/hardware/#planners ответственный)), скорее всего, он стоит исполнителем этого тикета. ((https://wiki.yandex-team.ru/Intranet/abc/features/hardware/#edit Инструкция)).\n",
                AMOSOV_F, id);
    }

    @Test
    public void quotaDistributionPlanMustCheckUserIsServiceTrustee() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getErrors().iterator().next();
        assertEquals("Only service trustees and admins may use this endpoint.", error);
    }

    @Test
    public void quotaDistributionMustCheckUserIsServiceTrustee() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getErrors().iterator().next();
        assertEquals("Only service trustees and admins may use this endpoint.", error);
    }

    @Test
    public void quotaDistributionMustPassReqId() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/distribute-quota")
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        final Map<?, ?> result = response.readEntity(Map.class);
        final String error = (String) result.get("description");
        assertEquals("No cgi param 'reqId' in request!", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateService() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                "unknown", bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("service").iterator().next();
        assertEquals("Service with key [unknown] is not found.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateBigOrder() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), 9999L, aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("orderId").iterator().next();
        assertEquals("Big-order with id [9999] is not found.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateCampaign() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), 9999L, "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("campaignId").iterator().next();
        assertEquals("Campaign with id [9999] is not found.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateResource() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change("unexpected",
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].resourceKey").iterator().next();
        assertEquals("Resource with key [unexpected] is not found.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateSegmentation() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        ImmutableList.of("not-needed"), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys").iterator().next();
        assertEquals("Resource is not segmented.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateSegments() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of("unexpected"), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys[0]").iterator().next();
        assertEquals("Segment with key [unexpected] is not found.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateSegmentsFullSet() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys").iterator().next();
        assertEquals("Aggregation segments are not allowed.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateSegmentsNoDuplicates() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys").iterator().next();
        assertEquals("Duplicated segment keys.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateSegmentsNoMoreThanOnePerSegmentation() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, SEGMENT_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys").iterator().next();
        assertEquals("There are multiple segments for the same segmentation: segment_segmentations: ss_1, ss_2.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateUnits() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].amountReady").iterator().next();
        assertEquals("Amount ready units does not match resource units.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateNoDuplicateChanges() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes").iterator().next();
        assertEquals("Duplicate quota distribution keys.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateChangesRequired() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                Collections.emptyList());
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes").iterator().next();
        assertEquals("Changes are required.", error);
    }

    @Test
    public void quotaDistributionMustValidateService() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                "unknown", bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("service").iterator().next();
        assertEquals("Service with key [unknown] is not found.", error);
    }

    @Test
    public void quotaDistributionMustValidateBigOrder() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), 9999L, aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("orderId").iterator().next();
        assertEquals("Big-order with id [9999] is not found.", error);
    }

    @Test
    public void quotaDistributionMustValidateCampaign() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), 9999L, "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("campaignId").iterator().next();
        assertEquals("Campaign with id [9999] is not found.", error);
    }

    @Test
    public void quotaDistributionMustValidateResource() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change("unexpected",
                        Collections.emptyList(), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].resourceKey").iterator().next();
        assertEquals("Resource with key [unexpected] is not found.", error);
    }

    @Test
    public void quotaDistributionMustValidateSegmentation() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                mdb.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(cpu.getPublicKey(),
                        ImmutableList.of("not-needed"), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys").iterator().next();
        assertEquals("Resource is not segmented.", error);
    }

    @Test
    public void quotaDistributionMustValidateSegments() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of("unexpected"), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys[0]").iterator().next();
        assertEquals("Segment with key [unexpected] is not found.", error);
    }

    @Test
    public void quotaDistributionMustValidateSegmentsFullSet() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys").iterator().next();
        assertEquals("Aggregation segments are not allowed.", error);
    }

    @Test
    public void quotaDistributionMustValidateSegmentsNoDuplicates() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys").iterator().next();
        assertEquals("Duplicated segment keys.", error);
    }

    @Test
    public void quotaDistributionMustValidateSegmentsNoMoreThanOnePerSegmentation() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, SEGMENT_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].segmentKeys").iterator().next();
        assertEquals("There are multiple segments for the same segmentation: segment_segmentations: ss_1, ss_2.", error);
    }

    @Test
    public void quotaDistributionMustValidateUnits() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.CORES))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes[0].amountReady").iterator().next();
        assertEquals("Amount ready units does not match resource units.", error);
    }

    @Test
    public void quotaDistributionMustValidateNoDuplicateChanges() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes").iterator().next();
        assertEquals("Duplicate quota distribution keys.", error);
    }

    @Test
    public void quotaDistributionMustValidateChangesRequired() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                Collections.emptyList());
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getFieldErrors().get("changes").iterator().next();
        assertEquals("Changes are required.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateNoRemaindersLeft() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getErrors().iterator().next();
        assertEquals("Resources can not be distributed without remainders, see details.", error);
    }

    @Test
    public void quotaDistributionMustValidateNoRemaindersLeft() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getErrors().iterator().next();
        assertEquals("Resources can not be distributed without remainders, see details.", error);
    }

    @Test
    public void quotaDistributionPlanBasicDistributionProportional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        assertEquals(1, result.getDistributedQuota().getUpdates().get(0).getChanges().size());
        assertEquals(result.getDistributedQuota().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().size());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(result.getDistributedQuotaDeltas().getUpdates().get(0).getRequestId(), requestOneId);
    }

    @Test
    public void quotaDistributionPlanBasicDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        assertEquals(1, result.getDistributedQuota().getUpdates().get(0).getChanges().size());
        assertEquals(result.getDistributedQuota().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().size());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(result.getDistributedQuotaDeltas().getUpdates().get(0).getRequestId(), requestOneId);
    }

    @Test
    public void quotaDistributionBasicDistributionProportional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
    }

    @Test
    public void quotaDistributionBasicDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
    }


    @Test
    public void quotaDistributionPlanBasicDistributionProportionalAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        assertEquals(1, result.getDistributedQuota().getUpdates().get(0).getChanges().size());
        assertEquals(result.getDistributedQuota().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountAllocated().getValue());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().size());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountAllocated().getValue());
        assertEquals(result.getDistributedQuotaDeltas().getUpdates().get(0).getRequestId(), requestOneId);
    }

    @Test
    public void quotaDistributionPlanBasicDistributionEvenAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        assertEquals(1, result.getDistributedQuota().getUpdates().get(0).getChanges().size());
        assertEquals(result.getDistributedQuota().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountAllocated().getValue());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().size());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountAllocated().getValue());
        assertEquals(result.getDistributedQuotaDeltas().getUpdates().get(0).getRequestId(), requestOneId);
    }

    @Test
    public void quotaDistributionBasicDistributionProportionalAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, requestOne.getStatus());
    }

    @Test
    public void quotaDistributionBasicDistributionEvenAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build()
        )));

        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, requestOne.getStatus());
    }

    @Test
    public void quotaDistributionPlanExampleDistributionProportional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(15, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(4, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExampleDistributionProportional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(15, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
    }

    @Test
    public void quotaDistributionPlanExampleDistributionProportionalAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(15, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE));
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE));
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(4, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExampleDistributionProportionalAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(15, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestOne.getStatus());
        final DiQuotaChangeRequest requestTwo = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestTwo.getStatus());
        final DiQuotaChangeRequest requestThree = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestThree.getStatus());
    }

    @Test
    public void quotaDistributionPlanExampleDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(65, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(28, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(27, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExampleDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(65, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
    }

    @Test
    public void quotaDistributionPlanExampleDistributionEvenAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(65, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE));
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE));
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(28, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(27, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExampleDistributionEvenAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(65, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestOne.getStatus());
        final DiQuotaChangeRequest requestTwo = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestTwo.getStatus());
        final DiQuotaChangeRequest requestThree = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, requestThree.getStatus());
    }

    @Test
    public void quotaDistributionPlanExampleDistributionEvenUnevenly() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(2, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(2, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(61, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(11, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(2, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
    }

    @Test
    public void quotaDistributionExampleDistributionEvenUnevenly() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(2, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(61, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(11, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
    }

    @Test
    public void quotaDistributionPlanExampleDistributionEvenAllocateUnevenly() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(2, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(2, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(61, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(61, DiUnit.MEBIBYTE));
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(11, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(11, DiUnit.MEBIBYTE));
        assertEquals(2, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(61, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(11, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
    }

    @Test
    public void quotaDistributionExampleDistributionEvenAllocateUnevenly() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(2, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(61, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(61, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(11, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(11, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestOne.getStatus());
        final DiQuotaChangeRequest requestTwo = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestTwo.getStatus());
        final DiQuotaChangeRequest requestThree = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestThree.getStatus());
    }

    @Test
    public void quotaDistributionPlanExampleDistributionEvenUnevenlyFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(2, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 2);
        assertNull(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(2, sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
    }

    @Test
    public void quotaDistributionExampleDistributionEvenUnevenlyFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(2, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
    }

    @Test
    public void quotaDistributionPlanExampleDistributionEvenAllocateUnevenlyFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(2, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 2);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 2);
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(2, sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 2);
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
    }

    @Test
    public void quotaDistributionExampleDistributionEvenAllocateUnevenlyFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(2, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestOne.getStatus());
        final DiQuotaChangeRequest requestTwo = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestTwo.getStatus());
        final DiQuotaChangeRequest requestThree = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestThree.getStatus());
    }

    @Test
    public void quotaDistributionPlanExactDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(150, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(100, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(40, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExactDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(150, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
    }

    @Test
    public void quotaDistributionPlanExactDistributionEvenAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(150, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE));
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE));
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(100, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(40, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExactDistributionEvenAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(150, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestTwoChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, requestOne.getStatus());
        final DiQuotaChangeRequest requestTwo = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, requestTwo.getStatus());
        final DiQuotaChangeRequest requestThree = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, requestThree.getStatus());
    }

    @Test
    public void quotaDistributionPlanOverflowDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(160, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getErrors().iterator().next();
        assertEquals("Resources can not be distributed without remainders, see details.", error);
    }

    @Test
    public void quotaDistributionOverflowDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE)).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE))
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(160, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getErrors().iterator().next();
        assertEquals("Resources can not be distributed without remainders, see details.", error);
    }

    @Test
    public void quotaDistributionPlanExampleDistributionProportionalFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1)
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(15728643L, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE) + 4);
        assertNull(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE) + 1);
        assertNull(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 3);
        assertNull(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(4, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExampleDistributionProportionalFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1)
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(15728643L, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE) + 4, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
    }

    @Test
    public void quotaDistributionPlanExampleDistributionProportionalAllocateFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1)
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(15728643L, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE) + 4);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE) + 4);
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE) + 1);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE) + 1);
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 3);
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE) + 4);
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(4, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE) + 1);
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExampleDistributionProportionalAllocateFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1)
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(15728643L, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE) + 4, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(70, DiUnit.MEBIBYTE) + 4, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(14, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestTwoChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(1, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestOne.getStatus());
        final DiQuotaChangeRequest requestTwo = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestTwo.getStatus());
        final DiQuotaChangeRequest requestThree = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestThree.getStatus());
    }

    @Test
    public void quotaDistributionPlanExampleDistributionEvenFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1)
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(68157443L, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE) + 4);
        assertNull(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE) + 1);
        assertNull(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertNull(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(28, DiUnit.MEBIBYTE) + 3);
        assertNull(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(27, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertNull(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated());
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExampleDistributionEvenFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1)
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(68157443L, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE) + 4, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
    }

    @Test
    public void quotaDistributionPlanExampleDistributionEvenAllocateFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1)
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(68157443L, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(3, result.getDistributedQuota().getUpdates().size());
        final List<DistributedQuota.Update> sortedUpdates = result.getDistributedQuota().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuota.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedUpdates.get(0).getChanges().size());
        assertEquals(sortedUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE) + 4);
        assertEquals(sortedUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE) + 4);
        assertEquals(1, sortedUpdates.get(1).getChanges().size());
        assertEquals(sortedUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE) + 1);
        assertEquals(sortedUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE) + 1);
        assertEquals(1, sortedUpdates.get(2).getChanges().size());
        assertEquals(sortedUpdates.get(2).getRequestId(), requestThreeId);
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(3, result.getDistributedQuotaDeltas().getUpdates().size());
        final List<DistributedQuotaDeltas.Update> sortedDeltaUpdates = result.getDistributedQuotaDeltas().getUpdates().stream()
                .sorted(Comparator.comparing(DistributedQuotaDeltas.Update::getRequestId)).collect(Collectors.toList());
        assertEquals(1, sortedDeltaUpdates.get(0).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(28, DiUnit.MEBIBYTE) + 3);
        assertEquals(sortedDeltaUpdates.get(0).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE) + 4);
        assertEquals(sortedDeltaUpdates.get(0).getRequestId(), requestOneId);
        assertEquals(1, sortedDeltaUpdates.get(1).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(27, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(1).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE) + 1);
        assertEquals(sortedDeltaUpdates.get(1).getRequestId(), requestTwoId);
        assertEquals(1, sortedDeltaUpdates.get(2).getChanges().size());
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountReady().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getChanges().get(0).getAmountAllocated().getValue(),
                DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE));
        assertEquals(sortedDeltaUpdates.get(2).getRequestId(), requestThreeId);
    }

    @Test
    public void quotaDistributionExampleDistributionEvenAllocateFractional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKeyOne = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyOne).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(60, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyTwo = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestTwoId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyTwo).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2)
                        .amountReady(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1).build()
        )));
        final String issueKeyThree = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestThreeId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKeyThree).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments)
                        .amount(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1)
                        .amountReady(DiUnit.BYTE.convert(0, DiUnit.MEBIBYTE)).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(68157443L, DiUnit.BYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestOneChangeOne));
        assertEquals(DiUnit.BYTE.convert(160, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE) + 4, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(88, DiUnit.MEBIBYTE) + 4, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestTwoChangeOne = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestTwoChangeOne));
        assertEquals(DiUnit.BYTE.convert(50, DiUnit.MEBIBYTE) + 2, DiUnit.BYTE.convert(requestTwoChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestTwoChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(37, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestTwoChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest.Change requestThreeChangeOne = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform()
                .getChanges().iterator().next();
        assertEquals(segmentStorage.getPublicKey(), GET_RESOURCE_KEY.apply(requestThreeChangeOne));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE) + 1, DiUnit.BYTE.convert(requestThreeChangeOne.getAmount()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountReady()));
        assertEquals(DiUnit.BYTE.convert(10, DiUnit.MEBIBYTE), DiUnit.BYTE.convert(requestThreeChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestOne.getStatus());
        final DiQuotaChangeRequest requestTwo = dispenser().quotaChangeRequests()
                .byId(requestTwoId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestTwo.getStatus());
        final DiQuotaChangeRequest requestThree = dispenser().quotaChangeRequests()
                .byId(requestThreeId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestThree.getStatus());
    }

    @Test
    public void quotaDistributionPlanMultiResourceDistributionProportional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentHdd.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_2, DC_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        assertEquals(2, result.getDistributedQuota().getUpdates().get(0).getChanges().size());
        assertEquals(result.getDistributedQuota().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(1).getAmountReady().getValue());
        assertNull(result.getDistributedQuota().getUpdates().get(0).getChanges().get(1).getAmountAllocated());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        assertEquals(2, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().size());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(result.getDistributedQuotaDeltas().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(1).getAmountReady().getValue());
        assertNull(result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(1).getAmountAllocated());
    }

    @Test
    public void quotaDistributionPlanMultiResourceDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentHdd.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_2, DC_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        assertEquals(2, result.getDistributedQuota().getUpdates().get(0).getChanges().size());
        assertEquals(result.getDistributedQuota().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(1).getAmountReady().getValue());
        assertNull(result.getDistributedQuota().getUpdates().get(0).getChanges().get(1).getAmountAllocated());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        assertEquals(2, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().size());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertNull(result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountAllocated());
        assertEquals(result.getDistributedQuotaDeltas().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(1).getAmountReady().getValue());
        assertNull(result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(1).getAmountAllocated());
    }

    @Test
    public void quotaDistributionMultiResourceDistributionProportional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentHdd.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_2, DC_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final List<DiQuotaChangeRequest.Change> requestOneChanges = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges();
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmountAllocated()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmountAllocated()));
    }

    @Test
    public void quotaDistributionMultiResourceDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentHdd.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_2, DC_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final List<DiQuotaChangeRequest.Change> requestOneChanges = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges();
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmountAllocated()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmountAllocated()));
    }

    @Test
    public void quotaDistributionPlanMultiResourceDistributionProportionalAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentHdd.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_2, DC_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        assertEquals(2, result.getDistributedQuota().getUpdates().get(0).getChanges().size());
        assertEquals(result.getDistributedQuota().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountAllocated().getValue());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(1).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(1).getAmountAllocated().getValue());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        assertEquals(2, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().size());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountAllocated().getValue());
        assertEquals(result.getDistributedQuotaDeltas().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(1).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(1).getAmountAllocated().getValue());
    }

    @Test
    public void quotaDistributionPlanMultiResourceDistributionEvenAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentHdd.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_2, DC_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final QuotaDistributionPlan result = response.readEntity(QuotaDistributionPlan.class);
        assertEquals(1, result.getDistributedQuota().getUpdates().size());
        assertEquals(2, result.getDistributedQuota().getUpdates().get(0).getChanges().size());
        assertEquals(result.getDistributedQuota().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(0).getAmountAllocated().getValue());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(1).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuota().getUpdates().get(0).getChanges().get(1).getAmountAllocated().getValue());
        assertEquals(1, result.getDistributedQuotaDeltas().getUpdates().size());
        assertEquals(2, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().size());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(0).getAmountAllocated().getValue());
        assertEquals(result.getDistributedQuotaDeltas().getUpdates().get(0).getRequestId(), requestOneId);
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(1).getAmountReady().getValue());
        assertEquals(1048576, result.getDistributedQuotaDeltas().getUpdates().get(0).getChanges().get(1).getAmountAllocated().getValue());
    }

    @Test
    public void quotaDistributionMultiResourceDistributionProportionalAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentHdd.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_2, DC_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final List<DiQuotaChangeRequest.Change> requestOneChanges = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges();
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmountReady()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmountAllocated()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmountReady()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, requestOne.getStatus());
    }

    @Test
    public void quotaDistributionMultiResourceDistributionEvenAllocate() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE)),
                        new DistributeQuotaBody.Change(segmentHdd.getPublicKey(),
                                ImmutableList.of(SEGMENT_SEGMENT_2, DC_SEGMENT_2), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final List<DiQuotaChangeRequest.Change> requestOneChanges = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges();
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmountReady()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(0).getAmountAllocated()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmountReady()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChanges.get(1).getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.COMPLETED, requestOne.getStatus());
    }

    @Test
    public void quotaDistributionSomeResourceDistributionProportional() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().stream().filter(c -> c.getResource().getKey().equals(segmentStorage.getPublicKey())).findFirst().get();
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestOne.getStatus());
    }

    @Test
    public void quotaDistributionSomeResourceDistributionEven() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).trackerIssueKey(issueKey).changes(ImmutableList.of(
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentStorage).order(bigOrder).segments(segmentStorageSegments).amount(1048576).build(),
                QuotaChangeRequest.Change.newChangeBuilder().resource(segmentHdd).order(bigOrder).segments(segmentHddSegments).amount(1048576).build()
        )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.DIVIDE_AMONG_ALL,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/distribute-quota")
                .replaceQueryParam("reqId", UUID.randomUUID().toString())
                .invoke(HttpMethod.POST, body);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final DiResponse result = response.readEntity(DiResponse.class);
        assertEquals(200, result.getStatus());
        final DiQuotaChangeRequest.Change requestOneChangeOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform()
                .getChanges().stream().filter(c -> c.getResource().getKey().equals(segmentStorage.getPublicKey())).findFirst().get();
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmount()));
        assertEquals(1048576, DiUnit.BYTE.convert(requestOneChangeOne.getAmountReady()));
        assertEquals(0, DiUnit.BYTE.convert(requestOneChangeOne.getAmountAllocated()));
        final DiQuotaChangeRequest requestOne = dispenser().quotaChangeRequests()
                .byId(requestOneId)
                .get()
                .perform();
        assertEquals(DiQuotaChangeRequest.Status.CONFIRMED, requestOne.getStatus());
    }

    @Test
    public void quotaDistributionPlanMustValidateStatus() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).status(QuotaChangeRequest.Status.CANCELLED)
                .trackerIssueKey(issueKey).changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder().order(bigOrder).resource(segmentStorage)
                                .segments(segmentStorageSegments).amount(1048576).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().order(bigOrder).resource(segmentHdd)
                                .segments(segmentHddSegments).amount(1048576).build()
                )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", false,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getErrors().iterator().next();
        assertEquals("Resources can not be distributed without remainders, see details.", error);
    }

    @Test
    public void quotaDistributionPlanMustValidateStatusAllocation() {
        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();
        final String issueKey = trackerManager.createIssues(IssueCreate.builder().queue("TEST").build());
        final long requestOneId = createRequest(defaultBuilder(aggregatedCampaign).status(QuotaChangeRequest.Status.NEW)
                .trackerIssueKey(issueKey).changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder().order(bigOrder).resource(segmentStorage)
                                .segments(segmentStorageSegments).amount(1048576).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().order(bigOrder).resource(segmentHdd)
                                .segments(segmentHddSegments).amount(1048576).build()
                )));
        final DistributeQuotaBody body = new DistributeQuotaBody(ResourceDistributionAlgorithm.PROPORTIONAL_TO_READINESS_RATIO,
                yp.getKey(), bigOrder.getBigOrderId(), aggregatedCampaign.getId(), "test", true,
                ImmutableList.of(new DistributeQuotaBody.Change(segmentStorage.getPublicKey(),
                        ImmutableList.of(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(1, DiUnit.MEBIBYTE))));
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quota-distribution-plan")
                .invoke(HttpMethod.GET, body);
        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final ErrorResponse result = response.readEntity(ErrorResponse.class);
        final String error = result.getErrors().iterator().next();
        assertEquals("Resources can not be distributed without remainders, see details.", error);
    }


    @Test
    public void onChangeAmountShouldThrowOnDuplicateRequestIdTest() {
        final long requestId = createRequest(defaultBuilder(aggregatedCampaign));

        final SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(
                new SetResourceAmountBody.Item(requestId, null, Collections.emptyList(), ""),
                new SetResourceAmountBody.Item(requestId, null, Collections.emptyList(), "")
        ));

        final Response response = createAuthorizedLocalClient(SANCHO)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);

        assertEquals(response.getStatus(), DiExceptionMapper.ExtraStatus.UNPROCESSABLE_ENTITY.getStatusCode());
        final DiSetAmountResult.Errors result = response.readEntity(DiSetAmountResult.Errors.class);

        assertEquals("ERROR", result.getStatus());

        assertEquals(1, result.getErrors().size());

        final DiSetAmountResult.Errors.Item error = result.getErrors().iterator().next();

        assertEquals("Duplicates", error.getMessage());
        assertEquals(Collections.singleton(requestId), Sets.newHashSet(error.getProblemRequestIds()));
    }

    @Test
    public void inInvactiveCampaignOnlyActiveBigOrdersCanBeIncreased() {
        campaignDao.clear();
        botCampaignGroupDao.clear();
        BigOrder bigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2119, 1, 1)));

        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("camp1")
                .setName("camp1")
                .setBigOrders(Arrays.asList(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrder.getId(), LocalDate.of(2119, 1, 1))
                ))
                .setStatus(Campaign.Status.CLOSED)
                .setAllowedRequestModificationWhenClosed(true)
                .build());

        final Campaign.BigOrder bo1 = campaign.getBigOrders().get(0);
        final Campaign.BigOrder bo2 = campaign.getBigOrders().get(1);

        final Campaign campaign2 = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("camp2")
                .setName("camp2")
                .setStartDate(TEST_BIG_ORDER_DATE.plus(3, ChronoUnit.MONTHS))
                .setStatus(Campaign.Status.CLOSED)
                .setBigOrders(Arrays.asList(bo2))
                .setStatus(Campaign.Status.ACTIVE)
                .build());

        prepareCampaignResources();

        botCampaignGroup = botCampaignGroupDao.create(defaultCampaignGroupBuilder()
                .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bo1.getBigOrderId())))
                .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bo2.getBigOrderId())))
                .addCampaign(campaignDao.readForBot(Collections.singleton(this.campaign.getId())).values().iterator().next())
                .addCampaign(campaignDao.readForBot(Collections.singleton(campaign2.getId())).values().iterator().next())
                .build());

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final long oldRequest = createRequest(defaultBuilder()
                .project(project)
                .campaign(QuotaChangeRequest.Campaign.from(campaign))
                .campaignType(campaign.getType())
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder().order(bo1).resource(ytCpu).segments(Collections.emptySet()).amount(10_000).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().order(bo2).resource(ytCpu).segments(Collections.emptySet()).amount(10_000).build()
                ))
                .status(QuotaChangeRequest.Status.NEW)
        );

        final long newRequest = createRequest(defaultBuilder()
                .project(project)
                .campaign(QuotaChangeRequest.Campaign.from(campaign2))
                .campaignType(campaign2.getType())
                .changes(Collections.singletonList(QuotaChangeRequest.Change.newChangeBuilder().order(bo2).resource(ytCpu).segments(Collections.emptySet()).amount(10_000).build()))
                .status(QuotaChangeRequest.Status.NEW)
        );

        dispenser().quotaChangeRequests()
                .byId(newRequest)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bo2.getBigOrderId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.COUNT))
                .build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(oldRequest)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bo1.getBigOrderId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.COUNT))
                        .changes(NIRVANA, YT_CPU, bo2.getBigOrderId(), Collections.emptySet(), DiAmount.of(5L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        dispenser().quotaChangeRequests()
                .byId(oldRequest)
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .changes(NIRVANA, YT_CPU, bo1.getBigOrderId(), Collections.emptySet(), DiAmount.of(3L, DiUnit.COUNT))
                        .changes(NIRVANA, YT_CPU, bo2.getBigOrderId(), Collections.emptySet(), DiAmount.of(30L, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        assertThrowsWithMessage(() -> {

            dispenser().quotaChangeRequests()
                    .byId(oldRequest)
                    .update(new BodyUpdate.BodyUpdateBuilder()
                            .changes(NIRVANA, YT_CPU, bo1.getBigOrderId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.COUNT))
                            .changes(NIRVANA, YT_CPU, bo2.getBigOrderId(), Collections.emptySet(), DiAmount.of(40L, DiUnit.COUNT))
                            .build())
                    .performBy(AMOSOV_F);


        }, "Can't increase amount for resource YT CPU ordered already in 2020-01-01");
    }
}
