package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.response.DiListPageResponse;
import ru.yandex.qe.dispenser.domain.Quota;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.quota.QuotaDao;
import ru.yandex.qe.dispenser.domain.index.LongIndexBase;
import ru.yandex.qe.dispenser.domain.util.Page;
import ru.yandex.qe.dispenser.domain.util.PageInfo;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.v3.QuotaFilterParamsImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class PaginationValidationTest extends BusinessLogicTestBase {
    public static final String TEST_DESCRIPTION = "For needs";
    public static final String TEST_CALCULATIONS = "Description for how we calculated required amounts";
    public static final String TEST_SUMMARY = "test";

    @Autowired
    private BigOrderManager bigOrderManager;
    @Autowired
    private CampaignDao campaignDao;

    @Test
    public void quotaV3ShouldReturnPage() {

        final DiListPageResponse<?> page = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v3/quotas")
                .get(DiListPageResponse.class);

        assertTrue(page.getTotalResultsCount() > 0);
        assertTrue(page.getTotalPagesCount() > 0);

        assertNotNull(page.getNextPageUrl());

        final DiListPageResponse<?> nextPage = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v3/quotas")
                .query("page", 2)
                .get(DiListPageResponse.class);

        assertNotNull(nextPage.getPreviousPageUrl());
    }

    @Autowired
    private QuotaDao quotaDao;

    @Test
    public void quotaV3ShouldBeSoredById() {
        final QuotaFilterParamsImpl quotaFilterParams = new QuotaFilterParamsImpl(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        final Page<Quota> quotaPage = quotaDao.readPage(quotaFilterParams, new PageInfo(2, 20));

        final List<Quota> manualSorting = quotaPage.getItems()
                .sorted(Comparator.comparingLong(LongIndexBase::getId))
                .collect(Collectors.toList());

        assertEquals(manualSorting, quotaPage.getItems().collect(Collectors.toList()));

    }

    @Test
    public void entitiesV3ShouldWork() {

        final Stream<DiEntity> ytFiles = generateNirvanaYtFiles(99);

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(ytFiles.collect(Collectors.toList()), LYADZHIN.chooses(INFRA))
                .perform();

        final Stream<DiEntity> workloads = generateWorkloadEntities(77);
        dispenser().quotas()
                .changeInService(CLUSTER_API)
                .createEntities(workloads.collect(Collectors.toList()), LYADZHIN.chooses(INFRA))
                .perform();

        updateHierarchy();

        final DiListPageResponse<?> page = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v3/entities")
                .query("entitySpec", "/nirvana/yt-file")
                .query("pageSize", 10)
                .get(DiListPageResponse.class);

        assertEquals(99L, page.getTotalResultsCount());
        assertEquals(10L, page.size());
        assertEquals(10L, page.getTotalPagesCount());

        assertNotNull(page.getNextPageUrl());

        final DiListPageResponse<?> nextPage = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v3/entities")
                .query("entitySpec", "/nirvana/yt-file")
                .query("page", 2)
                .query("pageSize", 10)
                .get(DiListPageResponse.class);

        assertNotNull(nextPage.getPreviousPageUrl());

        final DiListPageResponse<?> workflowPage = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v3/entities")
                .query("entitySpec", "/cluster-api/workload")
                .query("pageSize", 20)
                .query("page", 4)
                .get(DiListPageResponse.class);

        assertEquals(77L, workflowPage.getTotalResultsCount());
        assertEquals(4, workflowPage.getTotalPagesCount());
        assertNull(workflowPage.getNextPageUrl());
        assertEquals(17L, workflowPage.size());
    }

    @Test
    public void quotaChangeRequestPagination() {
        bigOrderManager.clear();
        BigOrder bigOrder = bigOrderManager.create(BigOrder.builder(LocalDate.of(2020, 1, 1)));

        campaignDao.create(defaultCampaignBuilder(bigOrder).build());

        createProject("Test", YANDEX, BINARY_CAT.getLogin());

        prepareCampaignResources();
        long num = 0;
        while (num < 25) {
            dispenser().quotaChangeRequests()
                    .create(requestBodyBuilderWithDefaultFields()
                            .projectKey("Test")
                            .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                            .changes(NIRVANA, STORAGE, bigOrder.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                            .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                            .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                            .build(), null)
                    .performBy(BINARY_CAT);
            num++;
        }

        updateHierarchy();

        final DiListPageResponse<?> firstPage = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests")
                .query("pagination", true)
                .query("pageSize", 10)
                .get(DiListPageResponse.class);

        assertEquals(10, firstPage.size());
        assertEquals(25, firstPage.getTotalResultsCount());
        assertEquals(3, firstPage.getTotalPagesCount());

        final DiListPageResponse<?> lastPage = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests")
                .query("pagination", true)
                .query("pageSize", 10)
                .query("page", 3)
                .get(DiListPageResponse.class);

        assertEquals(5, lastPage.size());
        assertEquals(25, lastPage.getTotalResultsCount());
        assertEquals(3, lastPage.getTotalPagesCount());

        final DiListPageResponse<?> noPage = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-requests")
                .query("pagination", true)
                .query("pageSize", 10)
                .query("page", 4)
                .get(DiListPageResponse.class);

        assertEquals(0, noPage.size());
        assertEquals(25, noPage.getTotalResultsCount());
        assertEquals(3, noPage.getTotalPagesCount());
    }

    @NotNull
    public static Body.BodyBuilder requestBodyBuilderWithDefaultFields() {
        return new Body.BodyBuilder()
                .summary(TEST_SUMMARY)
                .description(TEST_DESCRIPTION)
                .calculations(TEST_CALCULATIONS);
    }

    @Test
    public void multiRequestPaginationTest() throws InterruptedException {
        final int concurrentThreads = 3;
        final int requests = 100;

        final ExecutorService requestPerformers = Executors.newFixedThreadPool(concurrentThreads);
        final AtomicInteger validRequestCounter = new AtomicInteger();

        for (int i = 0; i < requests; i++) {
            requestPerformers.submit(() -> {
                final int pageNo = new Random().nextInt(3) + 1;
                final DiListPageResponse<?> page = createAuthorizedLocalClient(AMOSOV_F)
                        .path("/v3/quotas")
                        .query("page", pageNo)
                        .get(DiListPageResponse.class);
                if (page.getNextPageUrl().toString().equals("local://api/v3/quotas?page=" + (pageNo + 1))) {
                    validRequestCounter.incrementAndGet();
                }
            });
        }

        requestPerformers.shutdown();
        requestPerformers.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        assertEquals(requests, validRequestCounter.get());
    }
}
