package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiRequestPreOrder;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.bot.ConfigurationWithComponents;
import ru.yandex.qe.dispenser.domain.dao.bot.CompleteBotConfiguration;
import ru.yandex.qe.dispenser.domain.dao.bot.MappedPreOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.configuration.BotConfigurationDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.BotPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.MappedPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.request.BotPreOrderRequestDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.request.PreOrderRequest;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.api.BotCampaignGroupServiceApiTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourcePreorderRequestOrdersTest extends BaseResourcePreorderTest {

    private static final GenericType<DiListResponse<DiRequestPreOrder>> LIST_TYPE = new GenericType<DiListResponse<DiRequestPreOrder>>() {
    };

    @Autowired
    private BotPreOrderRequestDao preOrderRequestDao;
    @Autowired
    private MappedPreOrderDao preOrderDao;
    @Autowired
    private BotPreOrderDao botPreOrderDao;
    @Autowired
    private BotConfigurationDao configurationDao;
    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void requestPreOrdersShouldBeReturned() {
        final long requestId = createRequest(defaultBuilder());

        DiListResponse<DiRequestPreOrder> preOrders = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/preorders")
                .get(LIST_TYPE);

        assertTrue(preOrders.isEmpty());

        configurationDao.clear();
        configurationDao.create(new CompleteBotConfiguration(
                new ConfigurationWithComponents(
                        2741L, 5302L,
                        "1U NOC Server / Intel Xeon D-1521 / 16 GB RAM / 256 GB SSD / 10G",
                        1L, "server", "server",
                        true, true, null,
                        Collections.emptyList()
                ),
                500L, 500L
        ));

        preOrderDao.clear();
        botPreOrderDao.clear();
        createPreOrders(ImmutableList.of(
                new MappedPreOrder.Builder()
                        .id(7018L)
                        .campaignGroupId(botCampaignGroup.getId())
                        .bigOrderConfigId(193L)
                        .bigOrderId(campaign.getBigOrders().iterator().next().getBigOrderId())
                        .project(projectDao.read(YANDEX))
                        .service(serviceDao.read(NIRVANA))
                        .serverId(2741L)
                        .serverQuantity(20L)
                        .status(MappedPreOrder.Status.DRAFTED)
                        .groupKey("")
                        .name("test")
                        .build(),
                new MappedPreOrder.Builder()
                        .id(7019L)
                        .campaignGroupId(botCampaignGroup.getId())
                        .bigOrderConfigId(194L)
                        .bigOrderId(campaign.getBigOrders().iterator().next().getBigOrderId())
                        .project(projectDao.read(YANDEX))
                        .service(serviceDao.read(NIRVANA))
                        .serverId(2741L)
                        .serverQuantity(24L)
                        .status(MappedPreOrder.Status.DRAFTED)
                        .groupKey("")
                        .name("test")
                        .build()
        ));


        preOrderRequestDao.createOrderRequests(ImmutableList.of(
                new PreOrderRequest(new PreOrderRequest.Key(7018L, requestId), new PreOrderRequest.Value(12.45, 10_000)),
                new PreOrderRequest(new PreOrderRequest.Key(7019L, requestId), new PreOrderRequest.Value(18.999, 12_000))
        ));

        preOrders = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/preorders")
                .get(LIST_TYPE);

        assertEquals(2, preOrders.size());

        Map<Long, DiRequestPreOrder> preOrderById = preOrders.stream()
                .collect(Collectors.toMap(po -> po.getPreOrder().getId(), Function.identity()));

        final DiRequestPreOrder firstPreOrder = preOrderById.get(7018L);
        assertEquals(firstPreOrder.getServers(), 12.45, 0.001);
        assertEquals(firstPreOrder.getPreOrder().getConfigurationName(), "1U NOC Server / Intel Xeon D-1521 / 16 GB RAM / 256 GB SSD / 10G");
        assertEquals(firstPreOrder.getPreOrder().getCount(), 20);
        assertEquals(firstPreOrder.getPreOrder().getName(), "test");
        assertEquals(firstPreOrder.getPreOrder().getService().getKey(), NIRVANA);
        assertEquals(firstPreOrder.getPreOrder().getService().getName(), "Nirvana");
        assertEquals(firstPreOrder.getCost(), 10000.0, 0.1);

        final DiRequestPreOrder secondPreOrder = preOrderById.get(7019L);
        assertEquals(secondPreOrder.getServers(), 18.999, 0.0001);
        assertEquals(secondPreOrder.getPreOrder().getConfigurationName(), "1U NOC Server / Intel Xeon D-1521 / 16 GB RAM / 256 GB SSD / 10G");
        assertEquals(secondPreOrder.getPreOrder().getCount(), 24);
        assertEquals(secondPreOrder.getPreOrder().getName(), "test");
        assertEquals(firstPreOrder.getPreOrder().getService().getKey(), NIRVANA);
        assertEquals(firstPreOrder.getPreOrder().getService().getName(), "Nirvana");
        assertEquals(secondPreOrder.getCost(), 12000.0, 0.1);

        preOrders = createAuthorizedLocalClient(KEYD)
                .path("/v1/resource-preorder/" + requestId + "/preorders")
                .get(LIST_TYPE);

        assertEquals(2, preOrders.size());

        preOrderById = preOrders.stream()
                .collect(Collectors.toMap(po -> po.getPreOrder().getId(), Function.identity()));

        assertEquals(preOrderById.get(7018L).getCost(), 10_000.0, 0.1);
        assertEquals(preOrderById.get(7019L).getCost(), 12_000.0, 0.1);
    }

    private void createPreOrders(final Collection<MappedPreOrder> preOrders) {
        preOrders.stream()
                .map(BotCampaignGroupServiceApiTest::toSyncedPreOrder)
                .forEach(botPreOrderDao::create);
        preOrderDao.createAll(preOrders);
    }
}
