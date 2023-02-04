package ru.yandex.qe.dispenser.ws.api;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiBotServiceReserve;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.response.DiListPageResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignForBot;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.service.reserve.BotServiceReserve;
import ru.yandex.qe.dispenser.domain.dao.bot.service.reserve.BotServiceReserveDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentReader;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.reqbody.BotServiceReserveBody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase.assertThrowsWithMessage;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

@SuppressWarnings("OverlyCoupledClass")
public class BotServiceReserveServiceApiTest extends ApiTestBase {

    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private BotServiceReserveDao botServiceReserveDao;

    @Autowired
    private CampaignDao campaignDao;

    private BigOrder bigOrderOne;
    private BigOrder bigOrderTwo;

    @BeforeAll
    public void init() {
        bigOrderManager.clear();
        bigOrderOne = bigOrderManager.create(BigOrder.builder(LocalDate.of(2019, 1, 1)));
        bigOrderTwo = bigOrderManager.create(BigOrder.builder(LocalDate.of(2019, 2, 1)));
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        botServiceReserveDao.clear();
        createCampaign(Arrays.asList(bigOrderOne.getId(), bigOrderTwo.getId()));
    }

    @Test
    public void onlyServiceAdminOrDispenserAdminOrProcessResponsibleCanCreateReserve() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotServiceReserve requestBody = DiBotServiceReserve.forResource(SEGMENT_CPU)
                .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                .forService(YP)
                .withBigOrder(bigOrderOne.getId())
                .withCampaignId(campaign.getId())
                .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_2)
                .build();

        createReservesFails(requestBody, AQRU, "'aqru' has no access to edit reserves in service 'yp'!"); // random person

        createReservesFails(requestBody, SANCHO, "'sancho' has no access to edit reserves in service 'yp'!"); // nirvana admin

        createReserves(requestBody, AMOSOV_F); // dispenser admin

        createReserves(requestBody, SLONNN); // yp admin

        createReserves(requestBody, KEYD); // PROCESS_RESPONSIBLE
    }

    private void createReserves(final DiBotServiceReserve requestBody, final DiPerson person) {
        final DiBotServiceReserve result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves")
                .post(requestBody, DiBotServiceReserve.class);
        assertLastResponseStatusEquals(200);
        assertNotNull(result.getId());
        assertTrue(botServiceReserveDao.contains(result.getId()));
    }

    private void createReservesFails(final DiBotServiceReserve requestBody, final DiPerson person, final String s) {
        createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves")
                .post(requestBody);
        assertLastResponseStatusEquals(403);
        assertTrue(SpyWebClient.lastResponse().contains(s));
    }

    @Test
    public void onlyServiceAdminOrDispenserAdminOrProcessResponsibleCanDeleteReserve() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotServiceReserve reserve = DiBotServiceReserve.forResource(SEGMENT_CPU)
                .forService(YP)
                .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                .withSegments(DC_SEGMENT_1)
                .withBigOrder(bigOrderOne.getId())
                .withCampaignId(campaign.getId())
                .build();
        BotServiceReserve createdReserve = botServiceReserveDao.create(fromView(reserve));

        deleteReserves(createdReserve, AQRU, 403, true); // random person
        assertTrue(SpyWebClient.lastResponse().contains("'aqru' has no access to edit reserves in service 'yp'!"));

        deleteReserves(createdReserve, SANCHO, 403, true); // nirvana admin
        assertTrue(SpyWebClient.lastResponse().contains("'sancho' has no access to edit reserves in service 'yp'!"));

        deleteReserves(createdReserve, AMOSOV_F, 200, false); // dispenser admin

        createdReserve = botServiceReserveDao.create(fromView(reserve));

        deleteReserves(createdReserve, SLONNN, 200, false); // yp admin

        createdReserve = botServiceReserveDao.create(fromView(reserve));

        deleteReserves(createdReserve, KEYD, 200, false); // PROCESS_RESPONSIBLE
    }

    private void deleteReserves(final BotServiceReserve reserve, final DiPerson person, final int status, final boolean assertFor) {
        createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/" + reserve.getId())
                .delete();
        assertLastResponseStatusEquals(status);
        assertEquals(botServiceReserveDao.contains(reserve.getId()), assertFor);
    }

    @Test
    public void onlyServiceAdminOrDispenserAdminOrProcessResponsibleCanUpdateReserve() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotServiceReserve reserve = DiBotServiceReserve.forResource(SEGMENT_CPU)
                .forService(YP)
                .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                .withSegments(DC_SEGMENT_1)
                .withBigOrder(bigOrderOne.getId())
                .withCampaignId(campaign.getId())
                .build();
        final BotServiceReserve createdReserve = botServiceReserveDao.create(fromView(reserve));

        updateReservesFail(createdReserve, AQRU, "'aqru' has no access to edit reserves in service 'yp'!"); // random person

        updateReservesFail(createdReserve, SANCHO, "'sancho' has no access to edit reserves in service 'yp'!"); // nirvana admin

        updateReserves(createdReserve, AMOSOV_F, bigOrderTwo.getId()); // dispenser admin

        updateReserves(createdReserve, SLONNN, bigOrderOne.getId()); // yp admin

        updateReserves(createdReserve, KEYD, bigOrderTwo.getId()); // PROCESS_RESPONSIBLE
    }

    private void updateReservesFail(final BotServiceReserve createdReserve, final DiPerson person, final String s) {
        createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/" + createdReserve.getId())
                .put(new BotServiceReserveBody(null, bigOrderTwo.getId(), null));
        assertLastResponseStatusEquals(403);
        assertTrue(SpyWebClient.lastResponse().contains(s));
        assertEquals(botServiceReserveDao.read(createdReserve.getId()).getBigOrderId(), Long.valueOf(bigOrderOne.getId()));
    }

    private void updateReserves(final BotServiceReserve createdReserve, final DiPerson person, final long l) {
        createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/" + createdReserve.getId())
                .put(new BotServiceReserveBody(null, l, null));
        assertLastResponseStatusEquals(200);
        assertEquals(botServiceReserveDao.read(createdReserve.getId()).getBigOrderId(), Long.valueOf(l));
    }

    @Test
    public void reserveCanBeUpdated() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotServiceReserve reserve = DiBotServiceReserve.forResource(SEGMENT_CPU)
                .forService(YP)
                .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                .withSegments(DC_SEGMENT_1)
                .withBigOrder(bigOrderOne.getId())
                .withCampaignId(campaign.getId())
                .build();
        final BotServiceReserve createdReserve = botServiceReserveDao.create(fromView(reserve));

        createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/" + createdReserve.getId())
                .put(new BotServiceReserveBody(null, null, null));

        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("Empty update!"));

        final @NotNull DiPerson person = SLONNN;

        DiBotServiceReserve result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/" + createdReserve.getId())
                .put(new BotServiceReserveBody(DiAmount.of(2, DiUnit.CORES), null, null), DiBotServiceReserve.class);

        assertEquals(DiAmount.of(2000, DiUnit.PERMILLE_CORES), result.getAmount());
        assertEquals(2000L, botServiceReserveDao.read(createdReserve.getId()).getAmount().longValue());

        result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/" + createdReserve.getId())
                .put(new BotServiceReserveBody(null, bigOrderTwo.getId(), null), DiBotServiceReserve.class);

        assertEquals(bigOrderTwo.getId(), result.getBigOrderId().longValue());
        assertEquals(bigOrderTwo.getId(), botServiceReserveDao.read(createdReserve.getId()).getBigOrderId().longValue());

        final ImmutableSet<String> segmentKeys = ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1);
        result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/" + createdReserve.getId())
                .put(new BotServiceReserveBody(null, null, segmentKeys), DiBotServiceReserve.class);

        assertEquals(result.getSegmentKeys(), segmentKeys);
        assertEquals(DC_SEGMENT_2, botServiceReserveDao.read(createdReserve.getId()).getSegments().iterator().next().getPublicKey());
    }

    @Test
    public void everyoneCanReadReserves() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final List<DiBotServiceReserve> reserves = Arrays.asList(
                DiBotServiceReserve.forResource(SEGMENT_CPU)
                        .forService(YP)
                        .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                        .withSegments(DC_SEGMENT_1)
                        .withBigOrder(bigOrderOne.getId())
                        .withCampaignId(campaign.getId())
                        .build(),
                DiBotServiceReserve.forResource(SEGMENT_CPU)
                        .forService(YP)
                        .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                        .withSegments(DC_SEGMENT_2)
                        .withBigOrder(bigOrderOne.getId())
                        .withCampaignId(campaign.getId())
                        .build(),
                DiBotServiceReserve.forResource(SEGMENT_CPU)
                        .forService(YP)
                        .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                        .withSegments(DC_SEGMENT_1)
                        .withBigOrder(bigOrderTwo.getId())
                        .withCampaignId(campaign.getId())
                        .build(),
                DiBotServiceReserve.forResource(SEGMENT_CPU)
                        .forService(YP)
                        .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                        .withSegments(DC_SEGMENT_2)
                        .withBigOrder(bigOrderTwo.getId())
                        .withCampaignId(campaign.getId())
                        .build()
        );

        botServiceReserveDao.createAll(reserves.stream().map(BotServiceReserveServiceApiTest::fromView).collect(Collectors.toList()));

        DiListPageResponse<DiBotServiceReserve> response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves")
                .get(new GenericType<DiListPageResponse<DiBotServiceReserve>>() {
                });

        assertEquals(4, response.getTotalResultsCount());

        response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves")
                .query("service", NIRVANA)
                .get(new GenericType<DiListPageResponse<DiBotServiceReserve>>() {
                });

        assertEquals(0, response.getTotalResultsCount());

        response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves")
                .query("bigorder", bigOrderTwo.getId())
                .get(new GenericType<DiListPageResponse<DiBotServiceReserve>>() {
                });

        assertEquals(2, response.getTotalResultsCount());
        response.stream().forEach(reserve -> assertEquals(bigOrderTwo.getId(), reserve.getBigOrderId().longValue()));

        response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves")
                .query("service", YP)
                .query("bigorder", bigOrderOne.getId())
                .get(new GenericType<DiListPageResponse<DiBotServiceReserve>>() {
                });

        assertEquals(2, response.getTotalResultsCount());

        response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves")
                .query("bigorder", bigOrderOne.getId())
                .query("bigorder", bigOrderTwo.getId())
                .get(new GenericType<DiListPageResponse<DiBotServiceReserve>>() {
                });

        assertEquals(4, response.getTotalResultsCount());
    }

    @Test
    public void everyoneCanAccessReserve() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotServiceReserve reserve = DiBotServiceReserve.forResource(SEGMENT_CPU)
                .forService(YP)
                .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                .withSegments(DC_SEGMENT_1)
                .withBigOrder(bigOrderOne.getId())
                .withCampaignId(campaign.getId())
                .build();
        final BotServiceReserve createdReserve = botServiceReserveDao.create(fromView(reserve));

        final DiBotServiceReserve response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/" + createdReserve.getId())
                .get(DiBotServiceReserve.class);
        assertEquals(response, createdReserve.toView());
    }

    @Test
    public void reserveWithMultipleSegmentsCanBeCreated() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final ImmutableSet<String> segments = ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1);

        DiBotServiceReserve reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves")
                .post(DiBotServiceReserve.forResource(SEGMENT_CPU)
                                .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                                .forService(YP)
                                .withBigOrder(bigOrderOne.getId())
                                .withCampaignId(campaign.getId())
                                .withSegments(segments)
                                .build(),
                        DiBotServiceReserve.class);

        assertEquals(reserve.getSegmentKeys(), segments);

        reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/" + reserve.getId())
                .get(DiBotServiceReserve.class);

        assertEquals(reserve.getSegmentKeys(), segments);
    }

    @Test
    public void reserveWithMultipleSegmentsCanBeUpdated() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();

        DiBotServiceReserve reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves")
                .post(DiBotServiceReserve.forResource(SEGMENT_CPU)
                                .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                                .forService(YP)
                                .withBigOrder(bigOrderOne.getId())
                                .withCampaignId(campaign.getId())
                                .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                                .build(),
                        DiBotServiceReserve.class);

        final ImmutableSet<String> segments = ImmutableSet.of(DC_SEGMENT_3, SEGMENT_SEGMENT_1);

        reserve = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/reserves/" + reserve.getId())
                .put(new BotServiceReserveBody(null, null, segments), DiBotServiceReserve.class);

        assertEquals(reserve.getSegmentKeys(), segments);

        reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/" + reserve.getId())
                .get(DiBotServiceReserve.class);

        assertEquals(reserve.getSegmentKeys(), segments);

        final ImmutableSet<String> segments2 = ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_2);

        reserve = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/reserves/" + reserve.getId())
                .put(new BotServiceReserveBody(null, null, segments2), DiBotServiceReserve.class);

        assertEquals(reserve.getSegmentKeys(), segments2);

        reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/" + reserve.getId())
                .get(DiBotServiceReserve.class);

        assertEquals(reserve.getSegmentKeys(), segments2);
    }

    @Test
    public void reserveShouldHaveCorrectSegments() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();

        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(SLONNN)
                    .path("/v1/bot/reserves")
                    .post(DiBotServiceReserve.forResource(SEGMENT_CPU)
                                    .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                                    .forService(YP)
                                    .withBigOrder(bigOrderOne.getId())
                                    .withCampaignId(campaign.getId())
                                    .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1, SEGMENT_SEGMENT_2)
                                    .build(),
                            DiBotServiceReserve.class);
        }, "multiple segments for segmentation");

        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(SLONNN)
                    .path("/v1/bot/reserves")
                    .post(DiBotServiceReserve.forResource(SEGMENT_CPU)
                                    .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                                    .forService(YP)
                                    .withBigOrder(bigOrderOne.getId())
                                    .withCampaignId(campaign.getId())
                                    .withSegments(SEGMENT_SEGMENT_2)
                                    .build(),
                            DiBotServiceReserve.class);
        }, "Invalid segment set");

        final DiBotServiceReserve reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves")
                .post(DiBotServiceReserve.forResource(SEGMENT_CPU)
                                .withAmount(DiAmount.of(1000L, DiUnit.CORES))
                                .forService(YP)
                                .withBigOrder(bigOrderOne.getId())
                                .withCampaignId(campaign.getId())
                                .withSegments(SEGMENT_SEGMENT_2, DC_SEGMENT_3)
                                .build(),
                        DiBotServiceReserve.class);

        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(SLONNN)
                    .path("/v1/bot/reserves/" + reserve.getId())
                    .put(new BotServiceReserveBody(null, null, Collections.singleton(DC_SEGMENT_3)), DiBotServiceReserve.class);
        }, "Invalid segment set");

    }

    @Test
    public void reserveWithoutSegmentsCanBeCreated() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotServiceReserve reserve = createAuthorizedLocalClient(AGODIN)
                .path("/v1/bot/reserves")
                .post(DiBotServiceReserve.forResource(HDD)
                                .withAmount(DiAmount.of(1000L, DiUnit.BYTE))
                                .forService(MDS)
                                .withBigOrder(bigOrderOne.getId())
                                .withCampaignId(campaign.getId())
                                .withSegments()
                                .build(),
                        DiBotServiceReserve.class);
        assertNotNull(reserve);
        assertTrue(reserve.getSegmentKeys().isEmpty());
        assertNotNull(reserve.getId());
        assertEquals(1000L, reserve.getAmount().getValue());
        assertEquals(DiUnit.BYTE, reserve.getAmount().getUnit());
        assertEquals(MDS, reserve.getServiceKey());
        assertEquals(HDD, reserve.getResourceKey());
        assertEquals(bigOrderOne.getId(), reserve.getBigOrderId());
        assertEquals(campaign.getId(), reserve.getCampaignId());
    }

    private static BotServiceReserve fromView(@NotNull final DiBotServiceReserve view) {
        final Service service = Hierarchy.get().getServiceReader().read(view.getServiceKey());
        final Resource resource = Hierarchy.get().getResourceReader().read(new Resource.Key(view.getResourceKey(), service));
        final Long amount = resource.getType().getBaseUnit().convert(view.getAmount());

        final SegmentReader segmentReader = Hierarchy.get().getSegmentReader();
        final Set<Segment> segments = view.getSegmentKeys().stream()
                .map(segmentReader::read)
                .collect(Collectors.toSet());

        final Long bigOrderId = view.getBigOrderId();
        final Long campaignId = view.getCampaignId();
        return new BotServiceReserve(new BotServiceReserve.Key(resource, segments, bigOrderId, campaignId), amount);
    }

    private Campaign createCampaign(final List<Long> bigOrders) {
        return campaignDao.create(defaultCampaignBuilder(Objects.requireNonNull(bigOrderManager.getById(bigOrders.get(0))))
                .setBigOrders(bigOrders.stream().map(i -> new Campaign.BigOrder(i, LocalDate.now())).collect(Collectors.toList()))
                .build());
    }

}
