package ru.yandex.qe.dispenser.ws.api;


import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiBotCampaignGroup;
import ru.yandex.qe.dispenser.api.v1.DiCampaign;
import ru.yandex.qe.dispenser.api.v1.DiCampaignLight;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignForBot;
import ru.yandex.qe.dispenser.domain.CampaignUpdate;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.bot.Configuration;
import ru.yandex.qe.dispenser.domain.bot.PreOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.BotPreOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.MappedPreOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.SimpleBotConfiguration;
import ru.yandex.qe.dispenser.domain.dao.bot.configuration.BotConfigurationDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.BotPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.MappedPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.reqbody.BotCampaignGroupBodyCreate;
import ru.yandex.qe.dispenser.ws.reqbody.BotCampaignGroupBodyUpdate;
import ru.yandex.qe.dispenser.ws.reqbody.CampaignBodyCreate;
import ru.yandex.qe.dispenser.ws.reqbody.CampaignBodyUpdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase.assertThrowsWithMessage;

public class BotCampaignGroupServiceApiTest extends ApiTestBase {

    private static final String BASE_PATH = "/v1/bot/campaign-groups/";

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private MappedPreOrderDao mappedPreOrderDao;
    @Autowired
    private BotPreOrderDao botPreOrderDao;
    @Autowired
    private BotConfigurationDao botConfigurationdao;

    private BigOrder bigOrderOne;
    private BigOrder bigOrderTwo;
    private BigOrder bigOrderThree;

    private final AtomicLong counter = new AtomicLong();

    @BeforeAll
    public void init() {
        bigOrderManager.clear();
        bigOrderOne = bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 6, 1)));
        bigOrderTwo = bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 9, 1)));
        bigOrderThree = bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 12, 1)));

        botConfigurationdao.clear();
        botConfigurationdao.create(new SimpleBotConfiguration(new Configuration(4242L, 4242L, "comp", 1L, "server", "server", true, true, "cool")));
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        botPreOrderDao.clear();
        mappedPreOrderDao.clear();
    }

    private DiBotCampaignGroup create(final BotCampaignGroupBodyCreate bodyCreate, final DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path(BASE_PATH)
                .post(bodyCreate, DiBotCampaignGroup.class);
    }

    private DiBotCampaignGroup get(final long id, final DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path(BASE_PATH + id)
                .get(DiBotCampaignGroup.class);
    }

    private DiBotCampaignGroup update(final long id, final BotCampaignGroupBodyUpdate update, final DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path(BASE_PATH + id)
                .invoke("PATCH", update, DiBotCampaignGroup.class);
    }

    private DiBotCampaignGroup delete(final long id, final DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path(BASE_PATH + id)
                .invoke("DELETE", null, DiBotCampaignGroup.class);
    }


    private void setActiveCampaign(final long campaignId) {
        campaignDao.partialUpdate(campaignDao.readForUpdate(campaignId), CampaignUpdate.builder().setStatus(Campaign.Status.ACTIVE).build());
    }

    private void setClosedCampaign(final long campaignId) {
        campaignDao.partialUpdate(campaignDao.readForUpdate(campaignId), CampaignUpdate.builder().setStatus(Campaign.Status.CLOSED).build());
    }

    private DiCampaign createCampaign(final Set<Long> bigOrders) {
        final long num = counter.incrementAndGet();
        return createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/campaigns/")
                .post(new CampaignBodyCreate("c" + num, "Test campaign " + num, bigOrders, LocalDate.now().plusMonths(num), Campaign.Type.DRAFT), DiCampaign.class);
    }

    @Test
    public void onlyDispenserAdminCanCreateGroup() {
        assertThrowsWithMessage(() -> create(new BotCampaignGroupBodyCreate("foo", "bar", Collections.emptySet(), "quux",
                Collections.emptySet()), SANCHO), HttpStatus.SC_FORBIDDEN, "isn't Dispenser admin!");
    }

    @Test
    public void groupCantBeCreatedWithIncorrectCampaigns() {
        final String key = "cg";
        final String name = "CG";
        final String issueKey = "FOO-42";

        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()));
        assertThrowsWithMessage(() -> create(new BotCampaignGroupBodyCreate(key, name, ImmutableSet.of(bigOrderOne.getId()), issueKey,
                ImmutableSet.of(campaign.getId())), AMOSOV_F), HttpStatus.SC_BAD_REQUEST, "not present in available orders [" + bigOrderOne.getId() + "]");

        assertThrowsWithMessage(() -> create(new BotCampaignGroupBodyCreate(key, name, ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()), issueKey,
                ImmutableSet.of(campaign.getId())), AMOSOV_F), HttpStatus.SC_BAD_REQUEST, "Orders [" + bigOrderThree.getId() + "] not present in available orders [" + bigOrderOne.getId() + ", " + bigOrderTwo.getId() + "]");
    }

    @Test
    public void groupCantBeCreatedWithNotExistingOrders() {
        final DiCampaign c1 = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        assertThrowsWithMessage(() -> create(new BotCampaignGroupBodyCreate("cg2", "cg2", ImmutableSet.of(bigOrderThree.getId(), bigOrderTwo.getId(), 4269L), "FOO-42", ImmutableSet.of(c1.getId())), AMOSOV_F), "Big orders with ids [4269] are missing.");
    }

    @Test
    public void groupCantBeCreatedWithNotExistingCampaigns() {
        assertThrowsWithMessage(() -> create(new BotCampaignGroupBodyCreate("cg2", "cg2", ImmutableSet.of(bigOrderThree.getId(), bigOrderTwo.getId()), "FOO-42", Collections.singleton(42L)), AMOSOV_F), HttpStatus.SC_BAD_REQUEST, "Campaign");
    }

    @Test
    public void groupCanBeCreatedWithOrdersAndCampaigns() {
        final DiCampaign c1 = createCampaign(ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()));
        final DiCampaign c2 = createCampaign(ImmutableSet.of(bigOrderThree.getId()));
        DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("cg1", "CG1", ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()), "FOO-42", ImmutableSet.of(c1.getId(), c2.getId())), AMOSOV_F);
        assertEquals(Set.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()), new HashSet<>(group.getBigOrders()));
        setActiveCampaign(c2.getId());
        final Map<Long, CampaignForBot> campaigns = campaignDao.readForBot(ImmutableSet.of(c1.getId(), c2.getId()));

        final List<DiCampaignLight> expectedCampaigns = campaigns.values().stream()
                .sorted(Comparator.comparing(CampaignForBot::getStartDate))
                .map(CampaignForBot::toView)
                .collect(Collectors.toList());
        final DiCampaignLight expectedActive = expectedCampaigns.stream().filter(c -> c.getStatus().equals("ACTIVE")).findFirst().get();
        group = get(group.getId(), AMOSOV_F);
        assertEquals(new HashSet<>(expectedCampaigns), new HashSet<>(group.getCampaigns()));
        assertFalse(group.getActiveCampaigns().isEmpty());
        assertEquals(expectedActive, group.getActiveCampaigns().get(0));
    }

    @Test
    public void groupCantBeCreatedWithIntersectingOrders() {
        final DiCampaign c1 = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiCampaign c2 = createCampaign(ImmutableSet.of(bigOrderThree.getId()));
        create(new BotCampaignGroupBodyCreate("cg1", "cg1", ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(c1.getId())), AMOSOV_F);
        assertThrowsWithMessage(() -> create(new BotCampaignGroupBodyCreate("cg2", "cg2", ImmutableSet.of(bigOrderTwo.getId(), bigOrderThree.getId()), "FOO-42", ImmutableSet.of(c2.getId())), AMOSOV_F), "Big orders cannot be used in several campaign groups");
    }

    @Test
    public void groupCanBeFetched() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderTwo.getId(), bigOrderOne.getId(), bigOrderThree.getId()), "FOO-42", Collections.singleton(campaign.getId())), AMOSOV_F);
        setActiveCampaign(campaign.getId());
        final DiBotCampaignGroup fetched = get(group.getId(), AMOSOV_F);

        final Optional<CampaignForBot> activeCampaign = campaignDao.getActiveForBotIntegration();
        assertTrue(activeCampaign.isPresent());

        assertTrue(fetched.getId() >= 0);
        assertEquals("foo", fetched.getKey());
        assertEquals("bar", fetched.getName());
        assertEquals("FOO-42", fetched.getBotPreOrderIssueKey());
        assertFalse(fetched.isActive());
        assertEquals(ImmutableList.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()), fetched.getBigOrders());
        assertEquals(ImmutableList.of(activeCampaign.get().toView()), fetched.getCampaigns());
        assertEquals(activeCampaign.get().toView(), fetched.getActiveCampaigns().get(0));
    }

    @Test
    public void groupCanBeFetchedOnlyByAdmin() {
        final DiCampaign c1 = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(c1.getId())), AMOSOV_F);
        assertThrowsWithMessage(() -> get(group.getId(), WHISTLER), HttpStatus.SC_FORBIDDEN, "isn't Dispenser admin!");
    }

    @Test
    public void groupCanBeUpdated() {

        final DiCampaign anotherCampaign = createCampaign(ImmutableSet.of(bigOrderThree.getId()));
        final DiCampaign c1 = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiCampaign c2 = createCampaign(ImmutableSet.of(bigOrderTwo.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(c2.getId())), AMOSOV_F);
        DiBotCampaignGroup anotherGroup = create(new BotCampaignGroupBodyCreate("bar", "qux", ImmutableSet.of(bigOrderThree.getId()), "BAR-42", ImmutableSet.of(anotherCampaign.getId())), AMOSOV_F);
        update(group.getId(), new BotCampaignGroupBodyUpdate("new_key", "new_name", true, ImmutableSet.of(bigOrderOne.getId()), "BAR-24", ImmutableSet.of(c1.getId())), AMOSOV_F);
        setActiveCampaign(c1.getId());
        DiBotCampaignGroup updated = get(group.getId(), AMOSOV_F);

        Collection<CampaignForBot> campaigns = campaignDao.readForBot(ImmutableSet.of(c1.getId())).values();

        assertTrue(updated.isActive());
        assertEquals("new_key", updated.getKey());
        assertEquals("new_name", updated.getName());
        assertEquals("BAR-24", updated.getBotPreOrderIssueKey());
        assertEquals(ImmutableList.of(bigOrderOne.getId()), updated.getBigOrders());
        assertEquals(campaigns.stream().map(CampaignForBot::toView).collect(Collectors.toList()), updated.getCampaigns());
        assertEquals(Iterables.getOnlyElement(campaigns).toView(), updated.getActiveCampaigns().get(0));

        setClosedCampaign(c1.getId());
        update(group.getId(), new BotCampaignGroupBodyUpdate("foo", "bar", false, ImmutableSet.of(bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(c2.getId())), AMOSOV_F);
        updated = get(group.getId(), AMOSOV_F);

        campaigns = campaignDao.readForBot(ImmutableSet.of(c2.getId())).values();

        assertFalse(updated.isActive());
        assertEquals("foo", updated.getKey());
        assertEquals("bar", updated.getName());
        assertEquals("FOO-42", updated.getBotPreOrderIssueKey());
        assertEquals(ImmutableList.of(bigOrderTwo.getId()), updated.getBigOrders());
        assertEquals(campaigns.stream().map(CampaignForBot::toView).collect(Collectors.toList()), updated.getCampaigns());
        assertTrue(updated.getActiveCampaigns().isEmpty());

        campaigns = campaignDao.readForBot(ImmutableSet.of(anotherCampaign.getId())).values();

        anotherGroup = get(anotherGroup.getId(), AMOSOV_F); //check that another group not affected
        assertFalse(anotherGroup.isActive());
        assertEquals("bar", anotherGroup.getKey());
        assertEquals("qux", anotherGroup.getName());
        assertEquals("BAR-42", anotherGroup.getBotPreOrderIssueKey());
        assertEquals(ImmutableList.of(bigOrderThree.getId()), anotherGroup.getBigOrders());
        assertEquals(campaigns.stream().map(CampaignForBot::toView).collect(Collectors.toList()), anotherGroup.getCampaigns());
        assertTrue(anotherGroup.getActiveCampaigns().isEmpty());
    }

    @Test
    public void groupCantBeUpdatedWithIncorrectOrders() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), AMOSOV_F);
        assertThrowsWithMessage(() -> update(group.getId(), new BotCampaignGroupBodyUpdate("new_key", "new_name", true,
                ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId(), 4269L), "BAR-24", Collections.emptySet()), AMOSOV_F), "Big orders with ids [4269] are missing");
    }

    private void createPreOrder(final MappedPreOrder preOrder) {
        botPreOrderDao.create(toSyncedPreOrder(preOrder));
        mappedPreOrderDao.createAll(Collections.singleton(preOrder));
    }

    public static BotPreOrder toSyncedPreOrder(final MappedPreOrder preOrder) {
        return new BotPreOrder.Builder()
                .id(preOrder.getId())
                .bigOrderId(preOrder.getBigOrderId())
                .bigOrderConfigId(preOrder.getBigOrderConfigId())
                .oebsServiceId(1L)
                .responsible("robot-di-bot-test")
                .ticketId("TICKET-1")
                .server(preOrder.getServerId())
                .serverQuantity(preOrder.getServerQuantity())
                .storage(preOrder.getStorageId())
                .storageQuantity(preOrder.getStorageQuantity())
                .categoryCode("new")
                .deleted(false)
                .statusId(PreOrder.Status.DRAFTED.getCode())
                .upgradesCost(0)
                .build();
    }

    @Test
    public void groupCantBeUpdatedWithOrdersIfThereIsAnyPreOrderForDetachedOrders() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), AMOSOV_F);
        final MappedPreOrder preOrder = new MappedPreOrder.Builder()
                .campaignGroupId(group.getId())
                .bigOrderId(bigOrderThree.getId())
                .groupKey("foo")
                .id(4242L)
                .serverId(4242L)
                .serverQuantity(42L)
                .status(MappedPreOrder.Status.DRAFTED)
                .project(Hierarchy.get().getProjectReader().getRoot())
                .service(Hierarchy.get().getServiceReader().read(NIRVANA))
                .name("foo")
                .reserveRate(0)
                .bigOrderConfigId(0L)
                .build();

        createPreOrder(preOrder);

        assertThrowsWithMessage(() -> update(group.getId(), new BotCampaignGroupBodyUpdate(null, null, null, ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()),
                null, null), AMOSOV_F), "Big order unbinding is forbidden when campaign group already has pre-orders for them");
    }

    @Test
    public void groupCantBeUpdatedWithIncorrectCampaigns() {
        final DiCampaign c1 = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiCampaign c2 = createCampaign(ImmutableSet.of(bigOrderTwo.getId(), bigOrderThree.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(c1.getId())), AMOSOV_F);

        assertThrowsWithMessage(() -> update(group.getId(), new BotCampaignGroupBodyUpdate(null, null, null, null,
                null, ImmutableSet.of(c1.getId(), c2.getId())), AMOSOV_F), "Orders [" + bigOrderThree.getId() + "] not present in available orders [" + bigOrderOne.getId() + ", " + bigOrderTwo.getId() + "]");
    }

    @Test
    public void activeUpdateWillNotDisableAllOtherGroups() {
        final DiCampaign c1 = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiCampaign c2 = createCampaign(ImmutableSet.of(bigOrderTwo.getId()));
        final DiCampaign c3 = createCampaign(ImmutableSet.of(bigOrderThree.getId()));
        DiBotCampaignGroup g1 = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(c1.getId())), AMOSOV_F);
        DiBotCampaignGroup g2 = create(new BotCampaignGroupBodyCreate("bar", "foo", ImmutableSet.of(bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(c2.getId())), AMOSOV_F);
        DiBotCampaignGroup g3 = create(new BotCampaignGroupBodyCreate("qux", "qux", ImmutableSet.of(bigOrderThree.getId()), "FOO-42", ImmutableSet.of(c3.getId())), AMOSOV_F);
        assertFalse(g1.isActive());
        assertFalse(g2.isActive());
        assertFalse(g3.isActive());

        g1 = update(g1.getId(), new BotCampaignGroupBodyUpdate(null, null, true, null, null, null), AMOSOV_F);
        g2 = get(g2.getId(), AMOSOV_F);
        g3 = get(g3.getId(), AMOSOV_F);

        assertTrue(g1.isActive());
        assertFalse(g2.isActive());
        assertFalse(g3.isActive());

        g3 = update(g3.getId(), new BotCampaignGroupBodyUpdate(null, null, true, null, null, null), AMOSOV_F);
        g2 = get(g2.getId(), AMOSOV_F);
        g1 = get(g1.getId(), AMOSOV_F);

        assertTrue(g1.isActive());
        assertFalse(g2.isActive());
        assertTrue(g3.isActive());
    }

    @Test
    public void groupCanBeUpdatedOnlyByAdmin() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), AMOSOV_F);
        assertThrowsWithMessage(() -> update(group.getId(), new BotCampaignGroupBodyUpdate("new_key", "new_name", true,
                ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId(), 4269L), "BAR-24", Collections.emptySet()), WELVET), HttpStatus.SC_FORBIDDEN, "isn't Dispenser admin!");
    }

    @Test
    public void groupCanBeDeletedOnlyByAdmin() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), AMOSOV_F);
        assertThrowsWithMessage(() -> delete(group.getId(), WELVET), HttpStatus.SC_FORBIDDEN, "isn't Dispenser admin!");
    }

    @Test
    public void groupCanBeDeleted() {
        final DiCampaign c1 = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiCampaign c2 = createCampaign(ImmutableSet.of(bigOrderTwo.getId()));
        final DiBotCampaignGroup g1 = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(c1.getId())), AMOSOV_F);
        final DiBotCampaignGroup g2 = create(new BotCampaignGroupBodyCreate("bar", "foo", ImmutableSet.of(bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(c2.getId())), AMOSOV_F);

        delete(g2.getId(), AMOSOV_F);
        assertLastResponseStatusEquals(200);
        assertThrowsWithMessage(() -> get(g2.getId(), AMOSOV_F), HttpStatus.SC_BAD_REQUEST, "Invalid object key");

        get(g1.getId(), AMOSOV_F);
        assertLastResponseStatusEquals(200);
    }

    @Test
    public void groupCantBeDeletedIfThereIsAnyPreOrders() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), AMOSOV_F);
        final MappedPreOrder preOrder = new MappedPreOrder.Builder()
                .campaignGroupId(group.getId())
                .bigOrderId(bigOrderThree.getId())
                .groupKey("foo")
                .id(4242L)
                .serverId(4242L)
                .serverQuantity(42L)
                .status(MappedPreOrder.Status.DRAFTED)
                .project(Hierarchy.get().getProjectReader().getRoot())
                .service(Hierarchy.get().getServiceReader().read(NIRVANA))
                .name("foo")
                .reserveRate(0)
                .bigOrderConfigId(0L)
                .build();

        createPreOrder(preOrder);

        assertThrowsWithMessage(() -> delete(group.getId(), AMOSOV_F), "Campaign group removal is forbidden when group already has pre-orders");
    }

    @Test
    public void groupCannotBeDeletedWithActiveCampaign() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), AMOSOV_F);

        CampaignBodyUpdate update = new CampaignBodyUpdate(null, null, Campaign.Status.ACTIVE, null, null, null, null, null, null, null, null, null, null);
        DiCampaign result = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/campaigns/" + campaign.getId())
                .invoke("PATCH", update, DiCampaign.class);

        assertEquals("ACTIVE", result.getStatus());

        assertThrowsWithMessage(() -> delete(group.getId(), AMOSOV_F), 400, "Campaign group cannot be removed with active campaign");
    }

    @Test
    public void activeCampaignCannotBeDetachedFromGroup() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId()), "FOO-42", ImmutableSet.of(campaign.getId())), AMOSOV_F);

        setActiveCampaign(campaign.getId());

        assertThrowsWithMessage(() -> update(group.getId(), new BotCampaignGroupBodyUpdate(null, null, null, null, null, Set.of()), AMOSOV_F),
                400, "Cannot detach active campaign " + campaign.getId() + " from group");
    }

    @Test
    public void activeCampaignCannotBeAttachedGroup() {
        final DiCampaign campaign = createCampaign(ImmutableSet.of(bigOrderOne.getId()));
        final DiCampaign campaign2 = createCampaign(ImmutableSet.of(bigOrderTwo.getId()));
        final DiBotCampaignGroup group = create(new BotCampaignGroupBodyCreate("foo", "bar", ImmutableSet.of(bigOrderOne.getId(), bigOrderTwo.getId()), "FOO-42", ImmutableSet.of(campaign2.getId())), AMOSOV_F);
        setActiveCampaign(campaign.getId());

        assertThrowsWithMessage(() -> update(group.getId(), new BotCampaignGroupBodyUpdate(null, null, null, null, null, Set.of(campaign.getId())), AMOSOV_F),
                400, "Cannot attach active campaign " + campaign.getId() + " to group");
    }
}
