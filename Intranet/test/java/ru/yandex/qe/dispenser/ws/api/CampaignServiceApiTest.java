package ru.yandex.qe.dispenser.ws.api;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.GenericType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiBotCampaignGroup;
import ru.yandex.qe.dispenser.api.v1.DiCampaign;
import ru.yandex.qe.dispenser.api.v1.DiCampaignLight;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.reqbody.BotCampaignGroupBodyCreate;
import ru.yandex.qe.dispenser.ws.reqbody.CampaignBodyCreate;
import ru.yandex.qe.dispenser.ws.reqbody.CampaignBodyUpdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase.assertThrowsWithMessage;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class CampaignServiceApiTest extends ApiTestBase {
    private static final String BASE_PATH = "/v1/campaigns";
    private static final String SANCHO_IS_NOT_ADMIN = "'sancho' isn't Dispenser admin!";

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    private @NotNull BotCampaignGroup botCampaignGroup;

    private BigOrder bigOrderOne;
    private BigOrder bigOrderTwo;
    private BigOrder bigOrderThree;

    @BeforeAll
    public void init() {
        bigOrderManager.clear();
        bigOrderOne = bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 6, 1)));
        bigOrderTwo = bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 9, 1)));
        bigOrderThree = bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 12, 1)));
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        campaignDao.clear();

        final Set<BigOrder> bigOrders = bigOrderManager.getAll();
        final List<BigOrder> sortedOrders = bigOrders.stream()
                .sorted(Comparator.comparing(BigOrder::getId))
                .collect(Collectors.toList());
        botCampaignGroup = botCampaignGroupDao.create(new BotCampaignGroup.Builder()
                .setKey("test_order")
                .setName("Test Order")
                .setBigOrders(sortedOrders)
                .setActive(true)
                .setBotPreOrderIssueKey("FOO-42")
                .build()
        );
    }

    @Test
    public void getById() {
        final Campaign created = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        final DiCampaign campaign = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .get(DiCampaign.class);

        assertEquals(created.getName(), campaign.getName());
        assertEquals(created.getStatus().name(), campaign.getStatus());
        assertEquals(created.getStartDate(), campaign.getStartDate());
        assertEquals(1, campaign.getCampaignBigOrders().size());
    }

    @Test
    public void createDraft() {
        final Set<Long> bigOrders = Sets.newHashSet(bigOrderOne.getId(), bigOrderTwo.getId());
        final CampaignBodyCreate body = new CampaignBodyCreate("test", "Test", bigOrders, LocalDate.now(), Campaign.Type.DRAFT);

        createAuthorizedLocalClient(SANCHO)
                .path(BASE_PATH)
                .post(body);

        assertLastResponseStatusEquals(403);
        assertTrue(SpyWebClient.lastResponse().contains(SANCHO_IS_NOT_ADMIN));

        final DiCampaign diCampaign = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH)
                .post(body, DiCampaign.class);

        assertEquals(2, diCampaign.getCampaignBigOrders().size());
        assertTrue(diCampaign.getId() >= 0);
        assertEquals(diCampaign.getName(), body.getName());
        assertEquals(Campaign.Status.CLOSED.name(), diCampaign.getStatus());
        assertEquals(diCampaign.getStartDate(), body.getStartDate());
        assertEquals(1, campaignDao.getAll().size());
        assertEquals(Campaign.Type.DRAFT.name(), diCampaign.getType());
    }

    @Test
    public void createAggregated() {
        final Set<Long> bigOrders = Sets.newHashSet(bigOrderOne.getId(), bigOrderTwo.getId());
        final CampaignBodyCreate body = new CampaignBodyCreate("test", "Test", bigOrders, LocalDate.now(), Campaign.Type.AGGREGATED);

        createAuthorizedLocalClient(SANCHO)
                .path(BASE_PATH)
                .post(body);

        assertLastResponseStatusEquals(403);
        assertTrue(SpyWebClient.lastResponse().contains(SANCHO_IS_NOT_ADMIN));

        final DiCampaign diCampaign = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH)
                .post(body, DiCampaign.class);

        assertEquals(2, diCampaign.getCampaignBigOrders().size());
        assertTrue(diCampaign.getId() >= 0);
        assertEquals(diCampaign.getName(), body.getName());
        assertEquals(Campaign.Status.CLOSED.name(), diCampaign.getStatus());
        assertEquals(diCampaign.getStartDate(), body.getStartDate());
        assertEquals(1, campaignDao.getAll().size());
        assertEquals(Campaign.Type.AGGREGATED.name(), diCampaign.getType());
    }

    @Test
    public void update() {
        final Campaign created = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        botCampaignGroupDao.attachCampaigns(botCampaignGroup.getId(), Set.of(created.getId()));

        CampaignBodyUpdate update = new CampaignBodyUpdate(null, null, null, null, null, null, null, null, null, null, null, null, null);

        createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update);

        assertLastResponseStatusEquals(400);
        assertTrue(SpyWebClient.lastResponse().contains("Update must be non-empty"));

        update = new CampaignBodyUpdate("Tuturu", null, null, null, null, null, null, null, null, null, null, null, null);

        DiCampaign result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(result.getName(), update.getName());
        assertEquals(1, result.getCampaignBigOrders().size());

        update = new CampaignBodyUpdate(null, null, Campaign.Status.CLOSED, null, null, null, null, null, null, null, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(result.getStatus(), update.getStatus().name());

        update = new CampaignBodyUpdate(null, null, null, LocalDate.now().plusDays(1), null, null, null, null, null, null, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(result.getStartDate(), update.getStartDate());

        update = new CampaignBodyUpdate(null, Sets.newHashSet(bigOrderTwo.getId(), bigOrderThree.getId()), null, null, null, null, null, null, null, null, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(2, result.getCampaignBigOrders().size());
        assertEquals(Sets.newHashSet(bigOrderTwo.getId(), bigOrderThree.getId()),
                result.getCampaignBigOrders().stream().map(DiCampaign.BigOrder::getBigOrderId).collect(Collectors.toSet()));

        update = new CampaignBodyUpdate(null, Sets.newHashSet(bigOrderOne.getId(), bigOrderTwo.getId()), null, null, null, null, null, null, null, null, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(2, result.getCampaignBigOrders().size());
        assertEquals(Sets.newHashSet(bigOrderOne.getId(), bigOrderTwo.getId()),
                result.getCampaignBigOrders().stream().map(DiCampaign.BigOrder::getBigOrderId).collect(Collectors.toSet()));

        update = new CampaignBodyUpdate(null, null, Campaign.Status.ACTIVE, null, null, null, null, null, null, null, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(result.getStatus(), update.getStatus().name());

        update = new CampaignBodyUpdate(null, null, null, null, true, null, null, null, null, null, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(result.isRequestCreationDisabled(), update.getRequestCreationDisabled().booleanValue());

        update = new CampaignBodyUpdate(null, null, null, null, null, true, null, null, null, null, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(result.isRequestModificationDisabledForNonManagers(), update.getRequestModificationDisabledForNonManagers().booleanValue());

        createAuthorizedLocalClient(SANCHO)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update);

        assertLastResponseStatusEquals(403);
        assertTrue(SpyWebClient.lastResponse().contains(SANCHO_IS_NOT_ADMIN));


        update = new CampaignBodyUpdate(null, null, null, null, null, null, null, null, true, false, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(true, result.isForcedAllowingRequestStatusChange());
        assertEquals(true, campaignDao.read(created.getId()).isForcedAllowingRequestStatusChange());
        assertEquals(false, result.isSingleProviderRequestModeEnabled());
        assertEquals(false, campaignDao.read(created.getId()).isSingleProviderRequestModeEnabled());

        update = new CampaignBodyUpdate(null, null, null, null, null, null, null, null, false, true, null, null, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(false, result.isForcedAllowingRequestStatusChange());
        assertEquals(false, campaignDao.read(created.getId()).isForcedAllowingRequestStatusChange());
        assertEquals(true, result.isSingleProviderRequestModeEnabled());
        assertEquals(true, campaignDao.read(created.getId()).isSingleProviderRequestModeEnabled());

        update = new CampaignBodyUpdate(null, null, null, null, null, null, null, null, null, null, null, true, null);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(result.isAllowedRequestCreationForCapacityPlanner(), update.getAllowedRequestCreationForCapacityPlanner().booleanValue());

        update = new CampaignBodyUpdate(null, null, null, null, null, null, null, null, null, null, null, true, true);

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals(result.isRequestModificationDisabled(), update.getRequestModificationDisabled().booleanValue());
    }

    @Test
    public void delete() {
        final Campaign created = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        createAuthorizedLocalClient(SANCHO)
                .path(BASE_PATH + "/" + created.getId())
                .delete();

        assertLastResponseStatusEquals(403);
        assertTrue(SpyWebClient.lastResponse().contains(SANCHO_IS_NOT_ADMIN));
        assertEquals(1, campaignDao.getAll().size());

        createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + created.getId())
                .delete();

        assertLastResponseStatusEquals(200);
        assertTrue(campaignDao.getAll().isEmpty());
    }

    @Test
    public void getAll() {
        campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());
        campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key2")
                .setName("Tuturu")
                .setStartDate(LocalDate.now().plusDays(1))
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        final DiListResponse<DiCampaign> response = createAuthorizedLocalClient(SANCHO)
                .path(BASE_PATH)
                .get(new GenericType<DiListResponse<DiCampaign>>() {
                });

        assertEquals(2L, response.size());

        final DiListResponse<DiCampaign> filteredResponse = createAuthorizedLocalClient(SANCHO)
                .path(BASE_PATH)
                .query("status", Campaign.Status.ACTIVE.name(), Campaign.Status.CLOSED.name())
                .get(new GenericType<DiListResponse<DiCampaign>>() {
                });

        assertEquals(2L, filteredResponse.size());
    }

    @Test
    public void cannotDeleteCampaignWithGroup() {
        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setStatus(Campaign.Status.CLOSED)
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        botCampaignGroupDao.clear();
        final DiBotCampaignGroup group = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/campaign-groups/")
                .post(new BotCampaignGroupBodyCreate("cg", "CG", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), DiBotCampaignGroup.class);

        createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + campaign.getId())
                .delete();

        assertTrue(SpyWebClient.lastResponse().contains("Campaign removal is forbidden when campaign already is in campaign group"));
    }

    @Test
    public void cannotUpdateCampaignWithNewOrderIfOrderNotInGroup() {
        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setStatus(Campaign.Status.CLOSED)
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        botCampaignGroupDao.clear();
        final DiBotCampaignGroup group = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/campaign-groups/")
                .post(new BotCampaignGroupBodyCreate("cg", "CG", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), DiBotCampaignGroup.class);

        final CampaignBodyUpdate update = new CampaignBodyUpdate(null, ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()), null, null, null, null, null, null, null, null, null, null, null);

        assertThrowsWithMessage(() -> createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + campaign.getId())
                .invoke("PATCH", update, DiCampaign.class), "Orders [" + bigOrderTwo.getId() + "] not present in campaign group (cg) orders [" + bigOrderOne.getId() + "]");
    }

    @Test
    public void campaignsCanBeActiveInDifferentGroups() {
        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setStatus(Campaign.Status.CLOSED)
                .setStartDate(LocalDate.now())
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        final Campaign campaign2 = campaignDao.create(defaultCampaignBuilder(bigOrderTwo)
                .setKey("key2")
                .setName("Tratata2")
                .setStatus(Campaign.Status.CLOSED)
                .setStartDate(LocalDate.now().plusDays(1))
                .setBigOrders(bigOrders(bigOrderTwo.getId()))
                .build());

        botCampaignGroupDao.clear();
        DiBotCampaignGroup group = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/campaign-groups/")
                .post(new BotCampaignGroupBodyCreate("cg", "CG", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), DiBotCampaignGroup.class);

        DiBotCampaignGroup group2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/campaign-groups/")
                .post(new BotCampaignGroupBodyCreate("cg2", "CG2", ImmutableSet.of(bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(campaign2.getId())), DiBotCampaignGroup.class);

        CampaignBodyUpdate update = new CampaignBodyUpdate(null, null, Campaign.Status.ACTIVE, null, null, null, null, null, null, null, null, null, null);

        DiCampaign result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + campaign.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals("ACTIVE", result.getStatus());

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + campaign2.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals("ACTIVE", result.getStatus());

        group = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/campaign-groups/" + group.getId())
                .get(DiBotCampaignGroup.class);
        group2 = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/campaign-groups/" + group2.getId())
                .get(DiBotCampaignGroup.class);

        assertEquals(campaign.getId(), group.getActiveCampaigns().get(0).getId());
        assertEquals(campaign2.getId(), group2.getActiveCampaigns().get(0).getId());
    }

    @Test
    public void multipleCampaignsCannotBeBothActiveInOneGroup() {
        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setStatus(Campaign.Status.CLOSED)
                .setStartDate(LocalDate.now())
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        final Campaign campaign2 = campaignDao.create(defaultCampaignBuilder(bigOrderTwo)
                .setKey("key2")
                .setName("Tratata2")
                .setStatus(Campaign.Status.CLOSED)
                .setStartDate(LocalDate.now().plusDays(1))
                .setBigOrders(bigOrders(bigOrderTwo.getId()))
                .build());

        botCampaignGroupDao.clear();
        DiBotCampaignGroup group = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/campaign-groups/")
                .post(new BotCampaignGroupBodyCreate("cg", "CG", ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(campaign.getId(), campaign2.getId())), DiBotCampaignGroup.class);

        CampaignBodyUpdate update = new CampaignBodyUpdate(null, null, Campaign.Status.ACTIVE, null, null, null, null, null, null, null, null, null, null);

        DiCampaign result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + campaign.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals("ACTIVE", result.getStatus());

        result = createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + campaign2.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals("ACTIVE", result.getStatus());

        group = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/bot/campaign-groups/" + group.getId())
                .get(DiBotCampaignGroup.class);

        assertEquals(Set.of(campaign.getId(), campaign2.getId()),
                group.getActiveCampaigns().stream().map(DiCampaignLight::getId).collect(Collectors.toSet()));
    }

    @Test
    public void campaignCannotBeActiveWithoutGroup() {
        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("key")
                .setName("Tratata")
                .setBigOrders(bigOrders(bigOrderOne.getId()))
                .build());

        CampaignBodyUpdate update = new CampaignBodyUpdate(null, null, Campaign.Status.ACTIVE, null, null, null, null, null, null, null, null, null, null);

        assertThrowsWithMessage(() -> createAuthorizedLocalClient(AMOSOV_F)
                .path(BASE_PATH + "/" + campaign.getId())
                .invoke("PATCH", update, DiCampaign.class), 400, "Cannot set ACTIVE status for campaign without campaign group");
    }

    private static List<Campaign.BigOrder> bigOrders(final Long... ids) {
        return Stream.of(ids)
                .map(id -> new Campaign.BigOrder(id, LocalDate.now()))
                .collect(Collectors.toList());
    }
}
