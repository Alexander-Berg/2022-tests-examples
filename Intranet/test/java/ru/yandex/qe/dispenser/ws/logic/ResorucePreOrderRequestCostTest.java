package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.GenericType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreOrderRequest;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.ConfigurationWithComponents;
import ru.yandex.qe.dispenser.domain.dao.bot.CompleteBotConfiguration;
import ru.yandex.qe.dispenser.domain.dao.bot.MappedPreOrder;
import ru.yandex.qe.dispenser.domain.dao.bot.configuration.BotConfigurationDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.BotPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.MappedPreOrderDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.change.BotPreOrderChangeDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.request.BotPreOrderRequestDao;
import ru.yandex.qe.dispenser.domain.dao.bot.preorder.request.PreOrderRequest;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.ws.api.BotCampaignGroupServiceApiTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResorucePreOrderRequestCostTest extends BaseResourcePreorderTest {

    @Autowired
    private BotPreOrderRequestDao preOrderRequestDao;

    @Autowired
    private BotPreOrderChangeDao preOrderChangeDao;

    @Autowired
    private BotPreOrderDao botPreOrderDao;

    @Autowired
    private MappedPreOrderDao mappedPreOrderDao;

    @Autowired
    private BotConfigurationDao botConfigurationDao;

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void providersServiceConstCanBeFetched() {
        preOrderChangeDao.clear();
        botConfigurationDao.clear();
        botConfigurationDao.create(new CompleteBotConfiguration(new ConfigurationWithComponents(1L, 1L, "", 1L, "server", "server", true, true, "", Collections.emptyList()), 100L, 100L));

        final Campaign.BigOrder bigOrder = campaign.getBigOrders().iterator().next();
        prepareCampaignResources();

        final Service yp = serviceDao.read(YP);

        final Resource nirvanaGpu = resourceDao.read(new Resource.Key(YT_GPU, nirvana));
        final Resource ypHdd = resourceDao.read(new Resource.Key(SEGMENT_HDD, yp));
        final Resource ypCpu = resourceDao.read(new Resource.Key(SEGMENT_CPU, yp));

        final Hierarchy hierarchy = this.hierarchy.get();
        projectDao.attach(hierarchy.getPersonReader().read(KEYD.getLogin()), hierarchy.getProjectReader().read(YANDEX), Role.MEMBER);
        updateHierarchy();

        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        final DiListResponse<DiResourcePreOrderRequest> requests = createAuthorizedLocalClient(KEYD)
                .path("/v1/resource-preorder")
                .post(new Body.BodyBuilder()
                        .summary("s1")
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .projectKey(project.getPublicKey())
                        .calculations("1/0")
                        .chartLinksAbsenceExplanation("404")
                        .changes(NIRVANA, YT_CPU, bigOrder.getBigOrderId(), Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT))
                        .changes(SQS, YT_CPU, bigOrder.getBigOrderId(), Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT))
                        .changes(NIRVANA, YT_GPU, bigOrder.getBigOrderId(), Collections.emptySet(), DiAmount.of(10, DiUnit.COUNT))
                        .changes(YP, SEGMENT_HDD, bigOrder.getBigOrderId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_2), DiAmount.of(10_000, DiUnit.BYTE))
                        .changes(YP, SEGMENT_CPU, bigOrder.getBigOrderId(), ImmutableSet.of(SEGMENT_SEGMENT_1, DC_SEGMENT_2), DiAmount.of(10, DiUnit.CORES))
                        .build(), new GenericType<DiListResponse<DiResourcePreOrderRequest>>() {
                });

        assertEquals(1, requests.size());
        final DiResourcePreOrderRequest requestView = requests.iterator().next();
        assertEquals(ImmutableMap.of(
                YP, 0.0,
                NIRVANA, 0.0,
                SQS, 0.0
        ), requestView.getProviderServersCosts().stream()
                .collect(Collectors.toMap(DiResourcePreOrderRequest.ProviderServersCost::getServiceKey, DiResourcePreOrderRequest.ProviderServersCost::getServersCost))
        );

        final QuotaChangeRequest request = requestDao.read(requestView.getRequest().getId());
        requestDao.update(Collections.singleton(request.copyBuilder().cost(19_000L).build()));

        createPreOrders(Collections.singleton(
                new MappedPreOrder.Builder()
                        .id(2L)
                        .service(nirvana)
                        .project(projectDao.read(YANDEX))
                        .bigOrderConfigId(0L)
                        .bigOrderId(bigOrderOne.getId())
                        .campaignGroupId(botCampaignGroup.getId())
                        .name("name")
                        .groupKey("default")
                        .reserveRate(0)
                        .serverId(1L)
                        .serverQuantity(99L)
                        .build()
        ));

        final Map<Resource, Long> changeIdByResource = request.getChanges().stream()
                .collect(Collectors.toMap(QuotaChangeRequest.ChangeAmount::getResource, QuotaChangeRequest.Change::getId));

        preOrderChangeDao.setOrderChanges(2, ImmutableMap.<Long, Double>builder()
                .put(changeIdByResource.get(ytCpu), 0.1)
                .put(changeIdByResource.get(sqsYtCpu), 0.2)
                .put(changeIdByResource.get(nirvanaGpu), 0.3)
                .put(changeIdByResource.get(ypCpu), 0.4)
                .put(changeIdByResource.get(ypHdd), 0.5)
                .build()
        );

        preOrderRequestDao.createOrderRequests(Collections.singleton(
                new PreOrderRequest(new PreOrderRequest.Key(2, request.getId()), new PreOrderRequest.Value(99, 19_000))
        ));

        final List<DiResourcePreOrderRequest.ProviderServersCost> providerServersCosts = createAuthorizedLocalClient(KEYD)
                .path("/v1/resource-preorder/" + request.getId())
                .get(DiResourcePreOrderRequest.class)
                .getProviderServersCosts();

        final Map<String, Double> resultCost = providerServersCosts.stream()
                .collect(Collectors.toMap(DiResourcePreOrderRequest.ProviderServersCost::getServiceKey, DiResourcePreOrderRequest.ProviderServersCost::getServersCost));

        assertEquals((0.1 + 0.3) / 1.5 * 19_000, resultCost.get(NIRVANA), 0.1);
        assertEquals((0.4 + 0.5) / 1.5 * 19_000, resultCost.get(YP), 0.1);
        assertEquals((0.2) / 1.5 * 19_000, resultCost.get(SQS), 0.1);

        final double sum = resultCost.values().stream().mapToDouble(x -> x).sum();
        assertEquals(19_000, sum, 0.1);

        final List<DiResourcePreOrderRequest.ProviderServersCost> nonVisibleMoneyResult = createAuthorizedLocalClient(BINARY_CAT)
                .path("/v1/resource-preorder/" + request.getId())
                .get(DiResourcePreOrderRequest.class)
                .getProviderServersCosts();

        assertTrue(nonVisibleMoneyResult.isEmpty());

        final Map<String, Double> updateCost = createAuthorizedLocalClient(KEYD)
                .path("/v1/resource-preorder/" + request.getId())
                .invoke(HttpMethod.PATCH, new BodyUpdate.BodyUpdateBuilder().summary("new summary").build(), DiResourcePreOrderRequest.class)
                .getProviderServersCosts().stream()
                .collect(Collectors.toMap(DiResourcePreOrderRequest.ProviderServersCost::getServiceKey, DiResourcePreOrderRequest.ProviderServersCost::getServersCost));

        assertEquals(resultCost, updateCost);

        final Map<String, Double> setStatusCost = createAuthorizedLocalClient(KEYD)
                .path("/v1/resource-preorder/" + request.getId() + "/status/" + DiQuotaChangeRequest.Status.CANCELLED.name())
                .put(null, DiResourcePreOrderRequest.class)
                .getProviderServersCosts().stream()
                .collect(Collectors.toMap(DiResourcePreOrderRequest.ProviderServersCost::getServiceKey, DiResourcePreOrderRequest.ProviderServersCost::getServersCost));

        assertEquals(resultCost, setStatusCost);
    }

    private void createPreOrders(final Collection<MappedPreOrder> preOrders) {
        botPreOrderDao.clear();
        mappedPreOrderDao.clear();
        preOrders.stream()
                .map(BotCampaignGroupServiceApiTest::toSyncedPreOrder)
                .forEach(botPreOrderDao::create);
        mappedPreOrderDao.createAll(preOrders);
    }

}
