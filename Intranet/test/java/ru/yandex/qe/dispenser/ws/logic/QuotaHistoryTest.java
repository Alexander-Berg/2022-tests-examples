package ru.yandex.qe.dispenser.ws.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaHistoryEvent;
import ru.yandex.qe.dispenser.api.v1.DiQuotaMaxDeltaUpdate;
import ru.yandex.qe.dispenser.api.v1.DiQuotaMaxUpdate;
import ru.yandex.qe.dispenser.api.v1.DiSignedAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.response.DiListRelativePageResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.dao.history.quota.QuotaHistoryDao;
import ru.yandex.qe.dispenser.ws.reqbody.MaxValueBody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.param.RelativePageParam.FROM_ID;
import static ru.yandex.qe.dispenser.ws.param.RelativePageParam.PAGE_ORDER;
import static ru.yandex.qe.dispenser.ws.param.RelativePageParam.PAGE_SIZE;

public class QuotaHistoryTest extends BusinessLogicTestBase {

    public static final GenericType<DiListRelativePageResponse<DiQuotaHistoryEvent>> RESPONSE_TYPE = new GenericType<DiListRelativePageResponse<DiQuotaHistoryEvent>>() {
    };

    @Autowired
    private QuotaHistoryDao quotaHistoryDao;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        quotaHistoryDao.clear();
    }

    private void syncState(final String service,
                           final String project,
                           final String resource,
                           final String quotaSpec,
                           final Collection<String> segments,
                           final DiAmount max,
                           final DiAmount ownMax,
                           final DiPerson person) {
        dispenser().service(service)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState
                        .forResource(resource)
                        .forProject(project)
                        .forKey(quotaSpec)
                        .withSegments(segments)
                        .withMax(max)
                        .withOwnMax(ownMax)
                        .build())
                .performBy(person);
    }

    private void changeMax(final String service,
                           final String project,
                           final String resource,
                           final String quotaSpec,
                           final Collection<String> segments,
                           final DiAmount max,
                           final DiAmount ownMax,
                           final DiPerson person,
                           final String comment,
                           final String ticketKey) {
        dispenser().service(service)
                .updateMax()
                .quotas()
                .description(comment)
                .ticketKey(ticketKey)
                .updateMax(DiQuotaMaxUpdate
                        .forResource(resource)
                        .forProject(project)
                        .withQuotaSpecKey(quotaSpec)
                        .withSegmentKeys(segments)
                        .withMax(max)
                        .withOwnMax(ownMax)
                        .build())
                .performBy(person);
    }

    private void changeDeltaMax(final String service,
                                final String project,
                                final String resource,
                                final String quotaSpec,
                                final Collection<String> segments,
                                final DiSignedAmount maxDelta,
                                final DiSignedAmount ownMaxDelta,
                                final DiPerson person,
                                final String comment,
                                final String ticketKey) {
        dispenser().service(service)
                .updateMax()
                .deltas()
                .description(comment)
                .ticketKey(ticketKey)
                .updateMax(DiQuotaMaxDeltaUpdate
                        .forResource(resource)
                        .forProject(project)
                        .withQuotaSpecKey(quotaSpec)
                        .withSegmentKeys(segments)
                        .withMaxDelta(maxDelta)
                        .withOwnMaxDelta(ownMaxDelta)
                        .build())
                .performBy(person);
    }

    private void changeQuota(final String service,
                             final String project,
                             final String resource,
                             final String quotaSpec,
                             final DiPerson person,
                             final MaxValueBody maxValueBody) {
        createAuthorizedLocalClient(person)
                .path(String.format("/v1/quotas/%s/%s/%s/%s", project, service, resource, quotaSpec))
                .post(maxValueBody);
    }

    private static List<DiQuotaHistoryEvent> asList(final DiListRelativePageResponse<DiQuotaHistoryEvent> history) {
        return new ArrayList<>(history.getResults());
    }

    private DiListRelativePageResponse<DiQuotaHistoryEvent> historyByQuotaAsc(final String service,
                                                                              final String project,
                                                                              final String resource,
                                                                              final String quotaSpec,
                                                                              final Collection<String> segments,
                                                                              final DiPerson person) {
        return historyByQuota(service, project, resource, quotaSpec, segments, person, Collections.singletonMap(PAGE_ORDER, "ASC"));
    }

    private DiListRelativePageResponse<DiQuotaHistoryEvent> historyByQuota(final String service,
                                                                           final String project,
                                                                           final String resource,
                                                                           final String quotaSpec,
                                                                           final Collection<String> segments,
                                                                           final DiPerson person,
                                                                           final Map<String, Object> params) {
        final WebClient client = createAuthorizedLocalClient(person)
                .path("/v1/history/quotas/" + project + "/" + service + "/" + resource + "/" + quotaSpec)
                .query("segments", segments);

        params.forEach(client::query);
        return client.get(RESPONSE_TYPE);
    }

    private DiListRelativePageResponse<DiQuotaHistoryEvent> history(final DiPerson person,
                                                                    final Map<String, Object> params) {
        final WebClient client = createAuthorizedLocalClient(person)
                .path("/v1/history/quotas/");

        params.forEach(client::query);
        return client.get(RESPONSE_TYPE);
    }

    @Test
    public void historyWillBeRecordedWithSyncStateMethod() {
        final List<String> segments = Arrays.asList(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        syncState(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(60, DiUnit.CORES), DiAmount.of(30, DiUnit.CORES), SLONNN);

        DiListRelativePageResponse<DiQuotaHistoryEvent> events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertNull(events.getNextPageUrl());
        assertEquals(1, events.size());

        final DiQuotaHistoryEvent first = events.getFirst();
        assertEquals(first.getPerson(), SLONNN.getLogin());
        assertEquals(0L, first.getOldOwnMax());
        assertEquals(30_000L, first.getNewOwnMax());

        assertEquals(70_000L, first.getOldMax());
        assertEquals(60_000L, first.getNewMax());

        syncState(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(60, DiUnit.CORES), DiAmount.of(30, DiUnit.CORES), SLONNN);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(1, events.size());

        syncState(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(70, DiUnit.CORES), DiAmount.of(30, DiUnit.CORES), SLONNN);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(2, events.size());

        List<DiQuotaHistoryEvent> list = events.stream().collect(Collectors.toList());

        final DiQuotaHistoryEvent second = list.get(1);

        assertEquals(60_000L, second.getOldMax());
        assertEquals(70_000L, second.getNewMax());
        assertEquals(second.getOldOwnMax(), second.getNewOwnMax());

        syncState(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(70, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES), SLONNN);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(3, events.size());

        list = events.stream().collect(Collectors.toList());

        final DiQuotaHistoryEvent third = list.get(2);

        assertEquals(third.getOldMax(), third.getNewMax());
        assertEquals(30_000L, third.getOldOwnMax());
        assertEquals(0L, third.getNewOwnMax());
    }

    @Test
    public void historyWillBeRecordedWithChangeMaxMethod() {
        final List<String> segments = Arrays.asList(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        final String ticket = "ticket";
        final String comment = "comment";
        changeMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(60, DiUnit.CORES), DiAmount.of(30, DiUnit.CORES), SLONNN, comment, ticket);

        DiListRelativePageResponse<DiQuotaHistoryEvent> events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertNull(events.getNextPageUrl());
        assertEquals(1, events.size());

        final DiQuotaHistoryEvent first = events.getFirst();
        assertEquals(first.getPerson(), SLONNN.getLogin());
        assertEquals(comment, first.getComment());
        assertEquals(ticket, first.getIssueKey());
        assertEquals(0L, first.getOldOwnMax());
        assertEquals(30_000L, first.getNewOwnMax());

        assertEquals(70_000L, first.getOldMax());
        assertEquals(60_000L, first.getNewMax());

        changeMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(60, DiUnit.CORES), DiAmount.of(30, DiUnit.CORES), SLONNN, comment, ticket);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(1, events.size());

        changeMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(70, DiUnit.CORES), DiAmount.of(30, DiUnit.CORES), SLONNN, comment, ticket);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(2, events.size());

        List<DiQuotaHistoryEvent> list = events.stream().collect(Collectors.toList());

        final DiQuotaHistoryEvent second = list.get(1);

        assertEquals(60_000L, second.getOldMax());
        assertEquals(70_000L, second.getNewMax());
        assertEquals(second.getOldOwnMax(), second.getNewOwnMax());

        changeMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(70, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES), SLONNN, comment, ticket);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(3, events.size());

        list = events.stream().collect(Collectors.toList());

        final DiQuotaHistoryEvent third = list.get(2);

        assertEquals(third.getOldMax(), third.getNewMax());
        assertEquals(30_000L, third.getOldOwnMax());
        assertEquals(0L, third.getNewOwnMax());
    }

    @Test
    public void historyWillBeRecordedWithChangeDeltaMaxMethod() {
        final List<String> segments = Arrays.asList(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        final String ticket = "ticket";
        final String comment = "comment";
        changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiSignedAmount.negative(10, DiUnit.CORES), DiSignedAmount.positive(30, DiUnit.CORES), SLONNN, comment, ticket);

        DiListRelativePageResponse<DiQuotaHistoryEvent> events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertNull(events.getNextPageUrl());
        assertEquals(1, events.size());

        final DiQuotaHistoryEvent first = events.getFirst();
        assertEquals(first.getPerson(), SLONNN.getLogin());
        assertEquals(comment, first.getComment());
        assertEquals(ticket, first.getIssueKey());
        assertEquals(0L, first.getOldOwnMax());
        assertEquals(30_000L, first.getNewOwnMax());

        assertEquals(70_000L, first.getOldMax());
        assertEquals(60_000L, first.getNewMax());

        changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiSignedAmount.positive(0, DiUnit.CORES), DiSignedAmount.positive(0, DiUnit.CORES), SLONNN, comment, ticket);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(1, events.size());

        changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiSignedAmount.positive(10, DiUnit.CORES), DiSignedAmount.positive(0, DiUnit.CORES), SLONNN, comment, ticket);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(2, events.size());

        List<DiQuotaHistoryEvent> list = events.stream().collect(Collectors.toList());

        final DiQuotaHistoryEvent second = list.get(1);

        assertEquals(60_000L, second.getOldMax());
        assertEquals(70_000L, second.getNewMax());
        assertEquals(second.getOldOwnMax(), second.getNewOwnMax());

        changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiSignedAmount.positive(0, DiUnit.CORES), DiSignedAmount.negative(30, DiUnit.CORES), SLONNN, comment, ticket);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(3, events.size());

        list = events.stream().collect(Collectors.toList());

        final DiQuotaHistoryEvent third = list.get(2);

        assertEquals(third.getOldMax(), third.getNewMax());
        assertEquals(30_000L, third.getOldOwnMax());
        assertEquals(0L, third.getNewOwnMax());
    }

    @Test
    public void historyWillBeRecordedWithUpdateQuotaMethod() {
        final Set<String> segments = Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        final MaxValueBody maxValueBody = new MaxValueBody();
        maxValueBody.setSegments(segments);
        maxValueBody.setUnit(DiUnit.CORES);

        maxValueBody.setMaxValue(60);
        maxValueBody.setOwnMaxValue(30);
        changeQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, SLONNN, maxValueBody);

        DiListRelativePageResponse<DiQuotaHistoryEvent> events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertNull(events.getNextPageUrl());
        assertEquals(1, events.size());

        final DiQuotaHistoryEvent first = events.getFirst();
        assertEquals(first.getPerson(), SLONNN.getLogin());
        assertEquals(0L, first.getOldOwnMax());
        assertEquals(30_000L, first.getNewOwnMax());

        assertEquals(70_000L, first.getOldMax());
        assertEquals(60_000L, first.getNewMax());

        maxValueBody.setMaxValue(60);
        maxValueBody.setOwnMaxValue(30);
        changeQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, SLONNN, maxValueBody);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(1, events.size());

        maxValueBody.setMaxValue(70);
        maxValueBody.setOwnMaxValue(30);
        changeQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, SLONNN, maxValueBody);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(2, events.size());

        List<DiQuotaHistoryEvent> list = events.stream().collect(Collectors.toList());

        final DiQuotaHistoryEvent second = list.get(1);

        assertEquals(60_000L, second.getOldMax());
        assertEquals(70_000L, second.getNewMax());
        assertEquals(second.getOldOwnMax(), second.getNewOwnMax());

        maxValueBody.setMaxValue(70);
        maxValueBody.setOwnMaxValue(1);
        changeQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, SLONNN, maxValueBody);

        events = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(3, events.size());

        list = events.stream().collect(Collectors.toList());

        final DiQuotaHistoryEvent third = list.get(2);

        assertEquals(third.getOldMax(), third.getNewMax());
        assertEquals(30_000L, third.getOldOwnMax());
        assertEquals(1_000L, third.getNewOwnMax());
    }

    @Test
    public void eventsCanBeFilteredByPerformer() {
        final Set<String> segments = Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        final String comment = "foo";
        final String ticket = "bar";
        final DiSignedAmount maxDelta = DiSignedAmount.positive(DiAmount.of(0, DiUnit.CORES));
        changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, maxDelta, DiSignedAmount.positive(DiAmount.of(1, DiUnit.CORES)),
                SLONNN, comment, ticket);
        changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, maxDelta, DiSignedAmount.positive(DiAmount.of(1, DiUnit.CORES)),
                DiPerson.login(ZOMB_MOBSEARCH), comment, ticket);
        final DiListRelativePageResponse<DiQuotaHistoryEvent> history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN,
                ImmutableMap.of("performer", ZOMB_MOBSEARCH));

        assertEquals(1, history.size());
        final DiQuotaHistoryEvent first = history.getFirst();
        assertEquals(ZOMB_MOBSEARCH, first.getPerson());
    }

    @Test
    public void eventsCanBeFilteredByDate() {
        final Set<String> segments = Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        final String comment = "foo";
        final String ticket = "bar";
        final DiSignedAmount maxDelta = DiSignedAmount.positive(DiAmount.of(0, DiUnit.CORES));

        final int updateCount = 3;
        for (int i = 0; i < updateCount; i++) {
            changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, maxDelta, DiSignedAmount.positive(DiAmount.of(1, DiUnit.CORES)),
                    SLONNN, comment, ticket);
        }

        DiListRelativePageResponse<DiQuotaHistoryEvent> history = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        assertEquals(updateCount, history.size());
        final List<DiQuotaHistoryEvent> historyList = asList(history);

        final DiQuotaHistoryEvent second = historyList.get(1);
        final long id = second.getId();

        history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN,
                ImmutableMap.of("from", second.getUpdated(), "to", second.getUpdated()));

        assertEquals(1, history.size());

        assertEquals(history.getFirst().getId(), id);
    }

    @Test
    public void historyEventCanBeFetchedById() {
        final List<String> segments = Arrays.asList(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        syncState(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                DiAmount.of(60, DiUnit.CORES), DiAmount.of(30, DiUnit.CORES), SLONNN);
        final DiListRelativePageResponse<DiQuotaHistoryEvent> history = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);
        final long eventId = history.getFirst().getId();

        final DiQuotaHistoryEvent event = createAuthorizedLocalClient(SLONNN)
                .path("/v1/history/quotas/events/" + eventId)
                .get(DiQuotaHistoryEvent.class);
        assertEquals(event.getId(), eventId);
    }

    @Test
    public void relativePaginationOnHistoryShouldWork() {
        final Set<String> segments = Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        final String comment = "foo";
        final String ticket = "bar";
        final DiSignedAmount maxDelta = DiSignedAmount.positive(DiAmount.of(0, DiUnit.CORES));

        final int updateCount = 3;
        for (int i = 0; i < updateCount; i++) {
            changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, maxDelta, DiSignedAmount.positive(DiAmount.of(1, DiUnit.CORES)),
                    SLONNN, comment, ticket);
        }

        DiListRelativePageResponse<DiQuotaHistoryEvent> history = historyByQuotaAsc(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN);

        final List<DiQuotaHistoryEvent> events = asList(history);
        final DiQuotaHistoryEvent first = history.getFirst();
        final DiQuotaHistoryEvent second = events.get(1);

        assertEquals(updateCount, history.size());

        history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN,
                ImmutableMap.of(PAGE_ORDER, "ASC",FROM_ID, first.getId(), PAGE_SIZE, updateCount - 1));

        assertNotNull(history.getNextPageUrl());
        assertTrue(history.getNextPageUrl().toASCIIString().contains("/api/v1/history/quotas/"));
        assertEquals(updateCount - 1, history.size());

        history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN,
                ImmutableMap.of(PAGE_ORDER, "ASC",FROM_ID, first.getId(), PAGE_SIZE, updateCount));

        assertNull(history.getNextPageUrl());
        assertEquals(updateCount, history.size());

        history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN,
                ImmutableMap.of(PAGE_ORDER, "ASC", FROM_ID, second.getId(), PAGE_SIZE, updateCount));

        assertNull(history.getNextPageUrl());
        assertEquals(updateCount - 1, history.size());
        assertEquals(history.getFirst().getId(), second.getId());

        history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, SLONNN,
                ImmutableMap.of(PAGE_ORDER, "DESC",FROM_ID, second.getId(), PAGE_SIZE, updateCount - 1));

        assertNull(history.getNextPageUrl());
        assertEquals(updateCount - 1, history.size());
        final List<DiQuotaHistoryEvent> historyEventList = asList(history);
        assertEquals(historyEventList.get(0).getId(), second.getId());
        assertEquals(historyEventList.get(1).getId(), first.getId());
    }

    @Test
    public void historyForFilterShouldBeRecordedAndCanBeFetched() {
        final String comment = "foo";
        final String ticket = "bar";
        final DiSignedAmount maxDelta = DiSignedAmount.positive(DiAmount.of(0, DiUnit.CORES));

        for (final String ss : Collections.singletonList(SEGMENT_SEGMENT_1)) {
            for (final String ds : Arrays.asList(DC_SEGMENT_1, DC_SEGMENT_2)) {
                changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, Sets.newHashSet(ss, ds), DiSignedAmount.negative(DiAmount.of(1, DiUnit.CORES)),
                    maxDelta, SLONNN, comment, ticket);
            }
        }

        final ImmutableMap<String, Object> params = ImmutableMap.of("project", YANDEX, "service", YP,
                "resource", "/" + YP + "/" + SEGMENT_CPU, "leaf", false);
        DiListRelativePageResponse<DiQuotaHistoryEvent> history = history(SLONNN,
                params);

        assertEquals(2, history.size());

        final ImmutableMap<String, Object> paramsWithSegments = ImmutableMap.<String, Object>builder()
                .putAll(params)
                .put("segment", Arrays.asList(SEGMENT_SEGMENT_1, DC_SEGMENT_1))
                .build();
        history = history(SLONNN, paramsWithSegments);

        assertEquals(1, history.size());
    }

    @Test
    public void fromIdDefaultValueDependsOnOrder() {
        final List<String> segments = Arrays.asList(SEGMENT_SEGMENT_1, DC_SEGMENT_1);
        final String ticket = "ticket";
        final String comment = "comment";
        final int historySize = 3;
        for (int i = 0; i < historySize; i++) {
            changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments,
                    DiSignedAmount.positive(1, DiUnit.CORES), DiSignedAmount.positive(0, DiUnit.CORES), SLONNN, comment, ticket);
        }
        DiListRelativePageResponse<DiQuotaHistoryEvent> history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, AMOSOV_F,
                ImmutableMap.of(PAGE_ORDER, "ASC", PAGE_SIZE, historySize));
        final List<DiQuotaHistoryEvent> events = asList(history);
        final long firstId = events.get(0).getId();
        final long lastId = events.get(historySize - 1).getId();

        history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, AMOSOV_F,
                ImmutableMap.of(PAGE_ORDER, "DESC", PAGE_SIZE, 1));

        assertEquals(1, history.size());
        DiQuotaHistoryEvent event = history.getFirst();
        assertEquals(event.getId(), lastId);

        history = historyByQuota(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, segments, AMOSOV_F,
                ImmutableMap.of(PAGE_ORDER, "ASC", PAGE_SIZE, 1));

        assertEquals(1, history.size());
        event = history.getFirst();
        assertEquals(event.getId(), firstId);

        history = history(AMOSOV_F, ImmutableMap.of(PAGE_ORDER, "DESC", PAGE_SIZE, 1));

        assertEquals(1, history.size());
        event = history.getFirst();
        assertEquals(event.getId(), lastId);

        history = history(AMOSOV_F, ImmutableMap.of(PAGE_ORDER, "ASC", PAGE_SIZE, 1));

        assertEquals(1, history.size());
        event = history.getFirst();
        assertEquals(event.getId(), firstId);
    }

    @Test
    public void humanReadableErrorAppearsWhenIncorrectInstantParamIsProvided() {
        final Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("from", "foo")
                .build();

        assertThrowsWithMessage(() -> history(AMOSOV_F, params), 400, "Incorrect time format for value 'foo'. Expected format: yyyy-MM-dd'T'HH:mm:ss'Z'");
    }
}
