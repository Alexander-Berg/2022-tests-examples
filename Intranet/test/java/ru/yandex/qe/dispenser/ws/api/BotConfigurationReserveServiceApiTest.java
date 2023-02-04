package ru.yandex.qe.dispenser.ws.api;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import ru.yandex.qe.dispenser.api.v1.DiBotConfigurationReserve;
import ru.yandex.qe.dispenser.api.v1.response.DiListPageResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignForBot;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.bot.Configuration;
import ru.yandex.qe.dispenser.domain.dao.bot.SimpleBotConfiguration;
import ru.yandex.qe.dispenser.domain.dao.bot.configuration.BotConfigurationDao;
import ru.yandex.qe.dispenser.domain.dao.bot.service.reserve.configurations.BotConfigurationReserve;
import ru.yandex.qe.dispenser.domain.dao.bot.service.reserve.configurations.BotConfigurationReserveDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentReader;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.reqbody.BotConfigurationReserveBody;
import ru.yandex.qe.dispenser.ws.reqbody.BotServiceReserveBody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase.assertThrowsWithMessage;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

@SuppressWarnings("OverlyCoupledClass")
public class BotConfigurationReserveServiceApiTest extends ApiTestBase {

    public static final GenericType<DiListPageResponse<DiBotConfigurationReserve>> LIST_TYPE = new GenericType<DiListPageResponse<DiBotConfigurationReserve>>() {
    };
    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private BotConfigurationReserveDao botConfigurationReserveDao;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private BotConfigurationDao botConfigurationDao;

    private BigOrder bigOrderOne;
    private BigOrder bigOrderTwo;

    @BeforeAll
    public void init() {
        bigOrderManager.clear();
        bigOrderOne = bigOrderManager.create(BigOrder.builder(LocalDate.of(2019, 1, 1)));
        bigOrderTwo = bigOrderManager.create(BigOrder.builder(LocalDate.of(2019, 2, 1)));
        botConfigurationDao.clear();
        botConfigurationDao.create(new SimpleBotConfiguration(new Configuration(10L, 10L, "CPU, HDD, etc", 1L, "server", "ServerR", true, true, "...")));
        botConfigurationDao.create(new SimpleBotConfiguration(new Configuration(30L, 30L, "CPU, GPU included", 1L, "server", "ServerR", true, true, "...")));
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        botConfigurationReserveDao.clear();
        createCampaign(Arrays.asList(bigOrderOne.getId(), bigOrderTwo.getId()));
    }

    @Test
    public void onlyServiceAdminOrDispenserAdminOrProcessResponsibleCanCreateReserve() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotConfigurationReserve requestBody = DiBotConfigurationReserve.builder()
                .quantity(10L)
                .serviceKey(YP)
                .bigOrderId(bigOrderOne.getId())
                .campaignId(campaign.getId())
                .locationSegmentKey(DC_SEGMENT_1)
                .configurationId(10L)
                .storageQuantity(30L)
                .storageQuantity(42L)
                .build();

        createReservesFails(requestBody, AQRU, "'aqru' has no access to edit reserves in service 'yp'!"); // random person

        createReservesFails(requestBody, SANCHO, "'sancho' has no access to edit reserves in service 'yp'!"); // nirvana admin

        createReserves(requestBody, AMOSOV_F); // dispenser admin
        botConfigurationReserveDao.clear();

        createReserves(requestBody, SLONNN); // yp admin
        botConfigurationReserveDao.clear();

        createReserves(requestBody, KEYD); // PROCESS_RESPONSIBLE

        createAuthorizedLocalClient(KEYD)
                .path("/v1/bot/reserves/configurations/")
                .post(requestBody);

        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("already exists"));
    }

    private void createReserves(final DiBotConfigurationReserve requestBody, final DiPerson person) {
        final DiBotConfigurationReserve result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations")
                .post(requestBody, DiBotConfigurationReserve.class);
        assertLastResponseStatusEquals(200);
        assertNotNull(result.getId());
        assertNotNull(botConfigurationReserveDao.read(result.getId()));
    }

    private void createReservesFails(final DiBotConfigurationReserve requestBody, final DiPerson person, final String s) {
        createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations")
                .post(requestBody);
        assertLastResponseStatusEquals(403);
        assertTrue(SpyWebClient.lastResponse().contains(s));
    }

    @Test
    public void onlyServiceAdminOrDispenserAdminOrProcessResponsibleCanDeleteReserve() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotConfigurationReserve reserve = DiBotConfigurationReserve.builder()
                .serviceKey(YP)
                .quantity(10L)
                .locationSegmentKey(DC_SEGMENT_1)
                .bigOrderId(bigOrderOne.getId())
                .campaignId(campaign.getId())
                .configurationId(30L)
                .build();
        BotConfigurationReserve createdReserve = botConfigurationReserveDao.create(fromView(reserve));

        deleteReserves(createdReserve, AQRU, 403, true); // random person
        assertTrue(SpyWebClient.lastResponse().contains("'aqru' has no access to edit reserves in service 'yp'!"));

        deleteReserves(createdReserve, SANCHO, 403, true); // nirvana admin
        assertTrue(SpyWebClient.lastResponse().contains("'sancho' has no access to edit reserves in service 'yp'!"));

        deleteReserves(createdReserve, AMOSOV_F, 200, false); // dispenser admin

        createdReserve = botConfigurationReserveDao.create(fromView(reserve));

        deleteReserves(createdReserve, SLONNN, 200, false); // yp admin

        createdReserve = botConfigurationReserveDao.create(fromView(reserve));

        deleteReserves(createdReserve, KEYD, 200, false); // PROCESS_RESPONSIBLE

        createAuthorizedLocalClient(KEYD)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .delete();
        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("No reserve with id"));
    }

    private void deleteReserves(final BotConfigurationReserve reserve, final DiPerson person, final int status, final boolean assertFor) {
        createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations/" + reserve.getId())
                .delete();
        assertLastResponseStatusEquals(status);
        if (assertFor) {
            assertNotNull(botConfigurationReserveDao.read(reserve.getId()));
        } else {
            assertThrows(EmptyResultDataAccessException.class, () -> {
                botConfigurationReserveDao.read(reserve.getId());
            });
        }
    }

    @Test
    public void onlyServiceAdminOrDispenserAdminOrProcessResponsibleCanUpdateReserve() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotConfigurationReserve reserve = DiBotConfigurationReserve.builder()
                .serviceKey(YP)
                .quantity(10L)
                .locationSegmentKey(DC_SEGMENT_1)
                .bigOrderId(bigOrderOne.getId())
                .campaignId(campaign.getId())
                .configurationId(10L)
                .build();
        final BotConfigurationReserve createdReserve = botConfigurationReserveDao.create(fromView(reserve));

        updateReservesFail(createdReserve, AQRU, "'aqru' has no access to edit reserves in service 'yp'!"); // random person

        updateReservesFail(createdReserve, SANCHO, "'sancho' has no access to edit reserves in service 'yp'!"); // nirvana admin

        updateReserves(createdReserve, AMOSOV_F, bigOrderTwo.getId()); // dispenser admin

        updateReserves(createdReserve, SLONNN, bigOrderOne.getId()); // yp admin

        updateReserves(createdReserve, KEYD, bigOrderTwo.getId()); // PROCESS_RESPONSIBLE

        final DiBotConfigurationReserve reserve2 = DiBotConfigurationReserve.builder()
                .serviceKey(YP)
                .quantity(10L)
                .locationSegmentKey(DC_SEGMENT_1)
                .bigOrderId(bigOrderTwo.getId())
                .campaignId(campaign.getId())
                .configurationId(30L)
                .build();
        final BotConfigurationReserve createdReserve2 = botConfigurationReserveDao.create(fromView(reserve2));

        createAuthorizedLocalClient(KEYD)
                .path("/v1/bot/reserves/configurations/" + createdReserve2.getId())
                .put(new BotConfigurationReserveBody(null, null, 10L, null, null, null));
        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("already exists"));

        createAuthorizedLocalClient(KEYD)
                .path("/v1/bot/reserves/configurations/101")
                .put(new BotConfigurationReserveBody(null, null, 10L, null, null, null));
        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("No reserve with id"));
    }

    private void updateReservesFail(final BotConfigurationReserve createdReserve, final DiPerson person, final String s) {
        createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .put(new BotServiceReserveBody(null, bigOrderTwo.getId(), null));
        assertLastResponseStatusEquals(403);
        assertTrue(SpyWebClient.lastResponse().contains(s));
        assertEquals(botConfigurationReserveDao.read(createdReserve.getId()).getKey().getBigOrderId(), Long.valueOf(bigOrderOne.getId()));
    }

    private void updateReserves(final BotConfigurationReserve createdReserve, final DiPerson person, final long l) {
        createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .put(new BotConfigurationReserveBody(l, null, null, null, null, null));
        assertLastResponseStatusEquals(200);
        assertEquals(botConfigurationReserveDao.read(createdReserve.getId()).getKey().getBigOrderId(), Long.valueOf(l));
    }

    @Test
    public void reserveCanBeUpdated() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotConfigurationReserve reserve = DiBotConfigurationReserve.builder()
                .serviceKey(YP)
                .quantity(10L)
                .locationSegmentKey(DC_SEGMENT_1)
                .bigOrderId(bigOrderOne.getId())
                .campaignId(campaign.getId())
                .configurationId(10L)
                .build();
        final BotConfigurationReserve createdReserve = botConfigurationReserveDao.create(fromView(reserve));

        createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .put(new BotServiceReserveBody(null, null, null));

        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("Empty update!"));

        final @NotNull DiPerson person = SLONNN;

        DiBotConfigurationReserve result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .put(new BotConfigurationReserveBody(null, null, null, 2L, null, null), DiBotConfigurationReserve.class);

        assertEquals(2L, result.getQuantity());
        assertEquals(2L, botConfigurationReserveDao.read(createdReserve.getId()).getQuantity());

        result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .put(new BotConfigurationReserveBody(bigOrderTwo.getId(), null, null, null, null, null), DiBotConfigurationReserve.class);

        assertEquals(bigOrderTwo.getId(), result.getBigOrderId().longValue());
        assertEquals(bigOrderTwo.getId(), botConfigurationReserveDao.read(createdReserve.getId()).getKey().getBigOrderId());

        result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .put(new BotConfigurationReserveBody(null, DC_SEGMENT_2, null, null, null, null), DiBotConfigurationReserve.class);

        assertEquals(result.getLocationSegmentKey(), DC_SEGMENT_2);
        assertEquals(hierarchy.get().getSegmentReader().read(DC_SEGMENT_2).getId(),
                botConfigurationReserveDao.read(createdReserve.getId()).getKey().getLocationSegmentId());

        result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .put(new BotConfigurationReserveBody(null, null, 30L, null, null, null), DiBotConfigurationReserve.class);

        assertEquals(result.getConfigurationId(), 30L);
        assertEquals(30L, botConfigurationReserveDao.read(createdReserve.getId()).getKey().getConfigurationId());

        result = createAuthorizedLocalClient(person)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .put(new BotConfigurationReserveBody(null, null, 30L, null, 10L, 42L), DiBotConfigurationReserve.class);

        assertEquals(result.getConfigurationId(), 30L);
        final BotConfigurationReserve updated = botConfigurationReserveDao.read(createdReserve.getId());
        assertEquals(30L, updated.getKey().getConfigurationId());
        assertEquals(10L, updated.getKey().getStorageId());
        assertEquals(42L, updated.getStorageQuantity());
    }

    @Test
    public void reserveCannotBeCreatedWithStorageIdAndWithoutStorageQuantity() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotConfigurationReserve.Builder builder = DiBotConfigurationReserve.builder()
                .serviceKey(YP)
                .quantity(10L)
                .locationSegmentKey(DC_SEGMENT_1)
                .bigOrderId(bigOrderOne.getId())
                .campaignId(campaign.getId())
                .configurationId(10L);
        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/reserves/configurations")
                .post(builder.storageId(30L).build());

        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("Reserve can not be created with 'storageId' and empty or negative 'storageQuantity'"));

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/reserves/configurations")
                .post(builder.storageId(30L).storageQuantity(-1L).build());

        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("Reserve can not be created with 'storageId' and empty or negative 'storageQuantity'"));

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/reserves/configurations")
                .post(builder.storageId(30L).storageQuantity(42L).build());

        assertLastResponseStatusEquals(200);
    }

    @Test
    public void everyoneCanReadReserves() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final List<DiBotConfigurationReserve> reserves = Arrays.asList(
                DiBotConfigurationReserve.builder()
                        .serviceKey(YP)
                        .quantity(10L)
                        .locationSegmentKey(DC_SEGMENT_1)
                        .bigOrderId(bigOrderOne.getId())
                        .campaignId(campaign.getId())
                        .configurationId(10L)
                        .build(),
                DiBotConfigurationReserve.builder()
                        .serviceKey(YP)
                        .quantity(10L)
                        .locationSegmentKey(DC_SEGMENT_2)
                        .bigOrderId(bigOrderOne.getId())
                        .campaignId(campaign.getId())
                        .configurationId(10L)
                        .build(),
                DiBotConfigurationReserve.builder()
                        .serviceKey(YP)
                        .quantity(10L)
                        .locationSegmentKey(DC_SEGMENT_1)
                        .bigOrderId(bigOrderTwo.getId())
                        .campaignId(campaign.getId())
                        .configurationId(30L)
                        .build(),
                DiBotConfigurationReserve.builder()
                        .serviceKey(YP)
                        .quantity(1000L)
                        .locationSegmentKey(DC_SEGMENT_2)
                        .bigOrderId(bigOrderTwo.getId())
                        .campaignId(campaign.getId())
                        .configurationId(30L)
                        .build()
        );

        for (final DiBotConfigurationReserve reserve : reserves) {
            botConfigurationReserveDao.create(fromView(reserve));
        }

        DiListPageResponse<DiBotConfigurationReserve> response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations")
                .get(LIST_TYPE);

        assertEquals(4, response.getTotalResultsCount());

        response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations")
                .query("service", NIRVANA)
                .get(LIST_TYPE);

        assertEquals(0, response.getTotalResultsCount());

        response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations")
                .query("bigorder", bigOrderTwo.getId())
                .get(LIST_TYPE);

        assertEquals(2, response.getTotalResultsCount());
        response.stream().forEach(reserve -> assertEquals(bigOrderTwo.getId(), reserve.getBigOrderId().longValue()));

        response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations")
                .query("service", YP)
                .query("bigorder", bigOrderOne.getId())
                .get(LIST_TYPE);


        assertEquals(2, response.getTotalResultsCount());

        response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations")
                .query("bigorder", bigOrderOne.getId())
                .query("bigorder", bigOrderTwo.getId())
                .get(LIST_TYPE);

        assertEquals(4, response.getTotalResultsCount());
    }

    @Test
    public void everyoneCanAccessReserve() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        final DiBotConfigurationReserve reserve = DiBotConfigurationReserve.builder()
                .serviceKey(YP)
                .quantity(1000L)
                .locationSegmentKey(DC_SEGMENT_1)
                .bigOrderId(bigOrderOne.getId())
                .campaignId(campaign.getId())
                .configurationId(10L)
                .build();
        final BotConfigurationReserve createdReserve = botConfigurationReserveDao.create(fromView(reserve));

        final DiBotConfigurationReserve response = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations/" + createdReserve.getId())
                .get(DiBotConfigurationReserve.class);
        assertEquals(response, createdReserve.toView(hierarchy.get()));
    }

    @Test
    public void reserveSegmentMustBeValid() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();

        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(SLONNN)
                    .path("/v1/bot/reserves/configurations")
                    .post(DiBotConfigurationReserve.builder()
                                    .quantity(1000L)
                                    .serviceKey(YP)
                                    .bigOrderId(bigOrderOne.getId())
                                    .campaignId(campaign.getId())
                                    .locationSegmentKey(SEGMENT_SEGMENT_1)
                                    .configurationId(10L)
                                    .build(),
                            DiBotConfigurationReserve.class);
        }, "Segment from invalid segmentation");

        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(SLONNN)
                    .path("/v1/bot/reserves/configurations")
                    .post(DiBotConfigurationReserve.builder()
                                    .quantity(1000L)
                                    .serviceKey(YP)
                                    .bigOrderId(bigOrderOne.getId())
                                    .campaignId(campaign.getId())
                                    .locationSegmentKey("")
                                    .configurationId(10L)
                                    .build(),
                            DiBotConfigurationReserve.class);
        }, "No segment with key");


        DiBotConfigurationReserve reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations")
                .post(DiBotConfigurationReserve.builder()
                                .quantity(1000L)
                                .serviceKey(YP)
                                .bigOrderId(bigOrderOne.getId())
                                .campaignId(campaign.getId())
                                .locationSegmentKey(DC_SEGMENT_1)
                                .configurationId(10L)
                                .build(),
                        DiBotConfigurationReserve.class);

        assertEquals(reserve.getLocationSegmentKey(), DC_SEGMENT_1);

        final Long reserveId = reserve.getId();
        reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations/" + reserveId)
                .get(DiBotConfigurationReserve.class);

        assertEquals(reserve.getLocationSegmentKey(), DC_SEGMENT_1);

        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(SLONNN)
                    .path("/v1/bot/reserves/configurations/" + reserveId)
                    .put(new BotConfigurationReserveBody(null, SEGMENT_SEGMENT_2, null, null, null, null),
                            DiBotConfigurationReserve.class);
        }, "Segment from invalid segmentation");

        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(SLONNN)
                    .path("/v1/bot/reserves/configurations/" + reserveId)
                    .put(new BotConfigurationReserveBody(null, "**", null, null, null, null),
                            DiBotConfigurationReserve.class);
        }, "No segment with key");

        reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations/" + reserveId)
                .put(new BotConfigurationReserveBody(null, DC_SEGMENT_2, null, null, null, null),
                        DiBotConfigurationReserve.class);

        assertEquals(reserve.getLocationSegmentKey(), DC_SEGMENT_2);

        reserve = createAuthorizedLocalClient(SLONNN)
                .path("/v1/bot/reserves/configurations/" + reserveId)
                .get(DiBotConfigurationReserve.class);

        assertEquals(reserve.getLocationSegmentKey(), DC_SEGMENT_2);
    }

    @Test
    public void reserveWithoutSegmentsCantBeCreated() {
        final CampaignForBot campaign = campaignDao.getActiveForBotIntegration().get();
        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(AGODIN)
                    .path("/v1/bot/reserves/configurations")
                    .post(DiBotConfigurationReserve.builder()
                                    .quantity(1000L)
                                    .serviceKey(MDS)
                                    .bigOrderId(bigOrderOne.getId())
                                    .campaignId(campaign.getId())
                                    .locationSegmentKey("INVALID")
                                    .configurationId(10L)
                                    .build(),
                            DiBotConfigurationReserve.class);
        }, "No segment with key 'INVALID'");
    }

    private static BotConfigurationReserve fromView(@NotNull final DiBotConfigurationReserve view) {
        final SegmentReader segmentReader = Hierarchy.get().getSegmentReader();
        final Segment locationSegment = segmentReader.read(view.getLocationSegmentKey());

        return BotConfigurationReserve.builder()
                .service(Hierarchy.get().getServiceReader().read(view.getServiceKey()))
                .campaignId(view.getCampaignId())
                .bigOrderId(view.getBigOrderId())
                .locationSegmentId(locationSegment.getId())
                .configurationId(view.getConfigurationId())
                .quantity(view.getQuantity())
                .build();
    }

    private Campaign createCampaign(final List<Long> bigOrders) {
        return campaignDao.create(defaultCampaignBuilder(Objects.requireNonNull(bigOrderManager.getById(bigOrders.get(0))))
                .setBigOrders(bigOrders.stream().map(i -> new Campaign.BigOrder(i, LocalDate.now())).collect(Collectors.toList()))
                .build());
    }

}
