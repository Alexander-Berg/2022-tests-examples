package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSetAmountResult;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignUpdate;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.delivery.DeliveryDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceMappingDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.resources_model.ResourcesModelMappingDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.jns.JnsRequest;
import ru.yandex.qe.dispenser.domain.resources_model.ExternalResource;
import ru.yandex.qe.dispenser.domain.resources_model.InternalResource;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDelivery;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDeliveryResolveStatus;
import ru.yandex.qe.dispenser.domain.resources_model.ResourceModelMappingTarget;
import ru.yandex.qe.dispenser.domain.resources_model.ResourcesModelMapping;
import ru.yandex.qe.dispenser.standalone.MockAbcApi;
import ru.yandex.qe.dispenser.standalone.MockDApi;
import ru.yandex.qe.dispenser.standalone.MockJnsApi;
import ru.yandex.qe.dispenser.ws.quota.request.SetResourceAmountBody;
import ru.yandex.qe.dispenser.ws.reqbody.RequestQuotaAllocationBody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class ProvideToAccountTest  extends BaseResourcePreorderTest {
    @Autowired
    @Qualifier("errorMessageSource")
    private MessageSource errorMessageSource;

    @Autowired
    private ResourceMappingDao resourceMappingDao;
    @Autowired
    private SegmentationDao segmentationDao;
    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Autowired
    private MockAbcApi mockAbcApi;
    @Value("${abc.resource.yp.resourceType}")
    private Long resourceType;
    @Autowired
    private ResourcesModelMappingDao resourcesModelMappingDao;
    @Autowired
    private MockDApi mockDApi;
    @Autowired
    private DeliveryDao deliveryDao;
    @Autowired
    private MockJnsApi mockJnsApi;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentDao segmentDao;
    private Campaign aggregatedCampaign;

    @BeforeEach
    public void prepare() {
        aggregatedCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("test-aggregated")
                .setName("test-aggregated")
                .setType(Campaign.Type.AGGREGATED).build());
    }

    @AfterEach
    public void reset() {
        mockDApi.reset();
        mockJnsApi.reset();
    }

    @Test
    public void provideToAccountEmptyRequestValidationTest() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();
        Resource resource = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource)
                                .amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiSetAmountResult setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                        resource.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null)),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .build();

        String errorMessage = errorMessageSource.getMessage(
                "request.allocation.service.account.body.changes.empty", new Object[]{}, Locale.ENGLISH);
        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F, body), errorMessage);
        JnsRequest lastRequest = mockJnsApi.lastRequest();
        assertNotNull(lastRequest);
        assertEquals(errorMessage, lastRequest.getParams().get("details"));
        assertEquals(Long.toString(requestId), lastRequest.getParams().get("quotaRequestId"));
        assertEquals("Failed to allocate resource for quota request", lastRequest.getParams().get("title"));
    }

    @Test
    public void provideToAccountDifferentFoldersBySameAccountTest() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();
        Resource resource = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource resource2 = resourceDao.create(new Resource.Builder("cpu_test_2", saas)
                .description("cpu_2")
                .name("cpu_2")
                .noGroup()
                .priority(43)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource resource3 = resourceDao.create(new Resource.Builder("cpu_test_3", saas)
                .description("cpu_3")
                .name("cpu_3")
                .noGroup()
                .priority(44)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource)
                                .amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiSetAmountResult setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                        resource.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null)),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        String accountId = UUID.randomUUID().toString();
        String folderIdOne = UUID.randomUUID().toString();
        String folderIdTwo = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();

        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource2.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdTwo)
                        .providerId(providerId)
                        .build()
                )
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource3.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .build();

        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F, body), errorMessageSource.getMessage(
                "request.allocation.service.account.body.change.i.account.different.folder", new Object[]{1}, Locale.ENGLISH));
    }

    @Test
    public void provideToAccountDBodyChangeDuplicateTest() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();
        Resource resource = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource resource2 = resourceDao.create(new Resource.Builder("cpu_test_2", saas)
                .description("cpu_2")
                .name("cpu_2")
                .noGroup()
                .priority(43)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource)
                                .amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiSetAmountResult setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                        resource.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null)),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        String accountId = UUID.randomUUID().toString();
        String folderIdOne = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();

        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource2.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .build();

        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F, body), errorMessageSource.getMessage(
                "request.allocation.service.account.body.change.i.duplicate", new Object[]{1}, Locale.ENGLISH));
    }

    @Test
    public void provideToAccountDBodyValidAccountTest() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();
        Resource resource = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource resource2 = resourceDao.create(new Resource.Builder("cpu_test_2", saas)
                .description("cpu_2")
                .name("cpu_2")
                .noGroup()
                .priority(43)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource).amount(1_000_000).order(bigOrder).segments(Set.of()).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource2).amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiSetAmountResult setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(
                                        new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                                resource.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null),
                                        new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                                resource2.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null)
                                ),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        String accountId = UUID.randomUUID().toString();
        String folderIdOne = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();

        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource2.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId("accountId")
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .build();

        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F, body), errorMessageSource.getMessage(
                "request.allocation.service.account.body.change.i.account.not.fount", new Object[]{1}, Locale.ENGLISH));
    }

    @Test
    public void provideToAccountDBodyValidFolderTest() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();
        Resource resource = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource resource2 = resourceDao.create(new Resource.Builder("cpu_test_2", saas)
                .description("cpu_2")
                .name("cpu_2")
                .noGroup()
                .priority(43)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource).amount(1_000_000).order(bigOrder).segments(Set.of()).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource2).amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiSetAmountResult setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(
                                        new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                                resource.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null),
                                        new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                                resource2.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null)
                                ),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        String accountId = UUID.randomUUID().toString();
        String folderIdOne = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();

        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource2.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId("folderIdOne")
                        .providerId(providerId)
                        .build()
                )
                .build();

        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F, body), errorMessageSource.getMessage(
                "request.allocation.service.account.body.change.i.folder.not.found", new Object[]{1}, Locale.ENGLISH));
    }

    @Test
    public void provideToAccountDBodyValidProviderTest() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();
        Resource resource = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource resource2 = resourceDao.create(new Resource.Builder("cpu_test_2", saas)
                .description("cpu_2")
                .name("cpu_2")
                .noGroup()
                .priority(43)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource).amount(1_000_000).order(bigOrder).segments(Set.of()).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource2).amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiSetAmountResult setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(
                                        new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                                resource.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null),
                                        new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                                resource2.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null)
                                ),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        String accountId = UUID.randomUUID().toString();
        String folderIdOne = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();

        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource2.getPublicKey())
                        .orderId(bigOrder.getId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId("providerId")
                        .build()
                )
                .build();

        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F, body), errorMessageSource.getMessage(
                "request.allocation.service.account.body.change.i.provider.not.found", new Object[]{1}, Locale.ENGLISH));
    }

    @Test
    public void provideToAccountDBodyMissingChangeTest() {
        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .build())
                .build());

        updateHierarchy();
        Resource resource = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource resource2 = resourceDao.create(new Resource.Builder("cpu_test_2", saas)
                .description("cpu_2")
                .name("cpu_2")
                .noGroup()
                .priority(43)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        updateHierarchy();

        final Campaign.BigOrder bigOrder = aggregatedCampaign.getBigOrders().iterator().next();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource).amount(1_000_000).order(bigOrder).segments(Set.of()).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(resource2).amount(1_000_000).order(bigOrder).segments(Set.of()).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);

        DiSetAmountResult setAmountResult = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke("PATCH", new SetResourceAmountBody(
                        List.of(new SetResourceAmountBody.Item(requestId, null,
                                List.of(
                                        new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                                resource.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null),
                                        new SetResourceAmountBody.ChangeBody(saas.getKey(), bigOrder.getBigOrderId(),
                                                resource2.getPublicKey(), Set.of(), DiAmount.of(500, DiUnit.CORES), null)
                                ),
                                null)
                        )), DiSetAmountResult.class);
        assertEquals(DiSetAmountResult.SUCCESS.getStatus(), setAmountResult.getStatus());

        String accountId = UUID.randomUUID().toString();
        String folderIdOne = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();

        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource.getPublicKey())
                        .orderId(bigOrder.getBigOrderId())
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(resource.getPublicKey())
                        .orderId(bigOrder.getBigOrderId() + 1L)
                        .segmentKeys(Set.of())
                        .accountId(accountId)
                        .folderId(folderIdOne)
                        .providerId(providerId)
                        .build()
                )
                .build();

        assertThrowsWithMessage(() -> allocateProvider(requestId, "saas", AMOSOV_F, body), errorMessageSource.getMessage(
                "request.allocation.service.account.body.change.i.not.found", new Object[]{1}, Locale.ENGLISH));
    }

    @Test
    public void requestCanBeProvidedTest() {
        campaignDao.partialUpdate(aggregatedCampaign, CampaignUpdate.builder()
                .setBigOrders(List.of(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2)
                ))
                .build());
        aggregatedCampaign = campaignDao.read(aggregatedCampaign.getId());

        final Campaign.BigOrder bigOrder1 = aggregatedCampaign.getBigOrders().get(0);
        final Campaign.BigOrder bigOrder2 = aggregatedCampaign.getBigOrders().get(1);

        Service saas = serviceDao.create(Service.withKey("saas")
                .withName("saas")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", saas)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder2).segments(Set.of(sas)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);
        UUID externalResourceId = UUID.randomUUID();
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpu)
                .campaignId(aggregatedCampaign.getId())
                .addSegments(Set.of(sas))
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(externalResourceId)
                        .externalResourceBaseUnitKey("cores")
                        .numerator(1L)
                        .denominator(1000L)
                        .build()
                )
        );

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("saas")), fetchedRequest.getProvidersToAllocate());
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        String accountId = UUID.randomUUID().toString();
        String folderId = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();
        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(cpu.getPublicKey())
                        .orderId(bigOrder1.getBigOrderId())
                        .segmentKeys(Set.of(sas.getPublicKey()))
                        .accountId(accountId)
                        .folderId(folderId)
                        .providerId(providerId)
                        .build())
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(cpu.getPublicKey())
                        .orderId(bigOrder2.getBigOrderId())
                        .segmentKeys(Set.of(sas.getPublicKey()))
                        .accountId(accountId)
                        .folderId(folderId)
                        .providerId(providerId)
                        .build())
                .build();

        fetchedRequest = allocateProvider(requestId, "saas", AMOSOV_F, body);
        assertEquals(Set.of(), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final QuotaChangeRequest request = requestDao.read(requestId);
        request.getChanges()
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(500_000, c.getAmountAllocated());
                });

        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertTrue(fetchedRequest.isShowAllocationNote());
        assertEquals(Set.of(), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE));

        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/reset-allocation-note")
                .invoke("POST", null, Response.class);

        assertEquals(200, response.getStatus());
        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertFalse(fetchedRequest.isShowAllocationNote());

        final List<QuotaRequestDelivery> deliveries = deliveryDao.readAll();
        assertEquals(1, deliveries.size());
        QuotaRequestDelivery quotaRequestDelivery = deliveries.get(0);
        assertEquals(fetchedRequest.getProject().getAbcServiceId(), Long.valueOf(quotaRequestDelivery.getAbcServiceId()).intValue());
        assertEquals(fetchedRequest.getId(), quotaRequestDelivery.getQuotaRequestId());
        assertEquals(Objects.requireNonNull(fetchedRequest.getCampaign()).getId(), quotaRequestDelivery.getCampaignId());
        assertEquals(saas.getId(), quotaRequestDelivery.getProviderId());
        assertNotNull(quotaRequestDelivery.getResolvedAt());
        assertEquals(Set.of(
                ExternalResource.builder()
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder1.getBigOrderId())
                        .amount(500L)
                        .unitKey("cores")
                        .folderId(UUID.fromString(folderId))
                        .accountId(UUID.fromString(accountId))
                        .providerId(UUID.fromString(providerId))
                        .build(),
                ExternalResource.builder()
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder2.getBigOrderId())
                        .amount(500L)
                        .unitKey("cores")
                        .folderId(UUID.fromString(folderId))
                        .accountId(UUID.fromString(accountId))
                        .providerId(UUID.fromString(providerId))
                        .build()
        ), new HashSet<>(quotaRequestDelivery.getExternalResources()));
        assertEquals(Set.of(
                InternalResource.builder()
                        .resourceId(cpu.getId())
                        .addSegmentId(sas.getId())
                        .bigOrderId(bigOrder1.getBigOrderId())
                        .amount(500000L)
                        .build(),
                InternalResource.builder()
                        .resourceId(cpu.getId())
                        .addSegmentId(sas.getId())
                        .bigOrderId(bigOrder2.getBigOrderId())
                        .amount(500000L)
                        .build()
        ), new HashSet<>(quotaRequestDelivery.getInternalResources()));
        quotaRequestDelivery.getDeliveryResults().forEach(deliveryResult -> {
            assertEquals(UUID.fromString(folderId), deliveryResult.getFolderId());
            assertTrue(deliveryResult.getBigOrderId() == bigOrder1.getBigOrderId() ||
                    deliveryResult.getBigOrderId() == bigOrder2.getBigOrderId());
            assertEquals(externalResourceId, deliveryResult.getResourceId());
            assertNotNull(deliveryResult.getFolderOperationId());
            assertNotNull(deliveryResult.getTimestamp());
            assertEquals(UUID.fromString(accountId), deliveryResult.getAccountId());
            assertNotNull(deliveryResult.getAccountOperationId());
        });
        assertEquals(QuotaRequestDeliveryResolveStatus.RESOLVED, quotaRequestDelivery.getResolveStatus());
    }

    @Test
    public void requestCanBeProvidedToDAndDispenserInSingleRequestTest() {
        campaignDao.partialUpdate(aggregatedCampaign, CampaignUpdate.builder()
                .setBigOrders(List.of(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2)
                ))
                .build());
        aggregatedCampaign = campaignDao.read(aggregatedCampaign.getId());

        final Campaign.BigOrder bigOrder1 = aggregatedCampaign.getBigOrders().get(0);
        final Campaign.BigOrder bigOrder2 = aggregatedCampaign.getBigOrders().get(1);

        Service rtmr = serviceDao.create(Service.withKey("rtmr")
                .withName("rtmr")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", rtmr)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource ram = resourceDao.create(new Resource.Builder("ram_test", rtmr)
                .description("ram")
                .name("ram")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.MEMORY)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        resourceSegmentationDao.setSegmentations(ram, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder2).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(man)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(ram)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);
        UUID externalResourceId = UUID.randomUUID();
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpu)
                .campaignId(aggregatedCampaign.getId())
                .addSegments(Set.of(sas))
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(externalResourceId)
                        .externalResourceBaseUnitKey("cores")
                        .numerator(1L)
                        .denominator(1000L)
                        .build()
                )
        );

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("rtmr")), fetchedRequest.getProvidersToAllocate());
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        String accountId = UUID.randomUUID().toString();
        String folderId = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();
        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(cpu.getPublicKey())
                        .orderId(bigOrder1.getBigOrderId())
                        .segmentKeys(Set.of(sas.getPublicKey()))
                        .accountId(accountId)
                        .folderId(folderId)
                        .providerId(providerId)
                        .build())
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(cpu.getPublicKey())
                        .orderId(bigOrder2.getBigOrderId())
                        .segmentKeys(Set.of(sas.getPublicKey()))
                        .accountId(accountId)
                        .folderId(folderId)
                        .providerId(providerId)
                        .build())
                .build();

        fetchedRequest = allocateProvider(requestId, "rtmr", AMOSOV_F, body);
        assertEquals(Set.of(notAllocatableProviderInfo("rtmr", true, "Quota allocation already requested")), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        final QuotaChangeRequest request = requestDao.read(requestId);
        final Map<Boolean, List<QuotaChangeRequest.Change>> changesByMapping = request.getChanges()
                .stream()
                .collect(Collectors.partitioningBy(c -> c.getResource().equals(cpu) && c.getSegments().equals(Set.of(sas))));
        changesByMapping.get(true)
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(500_000, c.getAmountAllocated());
                });

        changesByMapping.get(false)
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(0, c.getAmountAllocated());
                });

        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertTrue(fetchedRequest.isShowAllocationNote());
        assertEquals(Set.of(notAllocatableProviderInfo("rtmr", true, "Quota allocation already requested")), fetchedRequest.getProvidersToAllocate());
        assertFalse(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_CLOSE_ALLOCATION_NOTE));

        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/" + requestId + "/reset-allocation-note")
                .invoke("POST", null, Response.class);

        assertEquals(200, response.getStatus());
        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertFalse(fetchedRequest.isShowAllocationNote());

        final List<QuotaRequestDelivery> deliveries = deliveryDao.readAll();
        assertEquals(1, deliveries.size());
        QuotaRequestDelivery quotaRequestDelivery = deliveries.get(0);
        assertEquals(fetchedRequest.getProject().getAbcServiceId(), Long.valueOf(quotaRequestDelivery.getAbcServiceId()).intValue());
        assertEquals(fetchedRequest.getId(), quotaRequestDelivery.getQuotaRequestId());
        assertEquals(Objects.requireNonNull(fetchedRequest.getCampaign()).getId(), quotaRequestDelivery.getCampaignId());
        assertEquals(rtmr.getId(), quotaRequestDelivery.getProviderId());
        assertNotNull(quotaRequestDelivery.getResolvedAt());
        assertEquals(Set.of(
                ExternalResource.builder()
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder1.getBigOrderId())
                        .amount(500L)
                        .unitKey("cores")
                        .folderId(UUID.fromString(folderId))
                        .accountId(UUID.fromString(accountId))
                        .providerId(UUID.fromString(providerId))
                        .build(),
                ExternalResource.builder()
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder2.getBigOrderId())
                        .amount(500L)
                        .unitKey("cores")
                        .folderId(UUID.fromString(folderId))
                        .accountId(UUID.fromString(accountId))
                        .providerId(UUID.fromString(providerId))
                        .build()
        ), new HashSet<>(quotaRequestDelivery.getExternalResources()));
        assertEquals(Set.of(
                InternalResource.builder()
                        .resourceId(cpu.getId())
                        .addSegmentId(sas.getId())
                        .bigOrderId(bigOrder1.getBigOrderId())
                        .amount(500000L)
                        .build(),
                InternalResource.builder()
                        .resourceId(cpu.getId())
                        .addSegmentId(sas.getId())
                        .bigOrderId(bigOrder2.getBigOrderId())
                        .amount(500000L)
                        .build()
        ), new HashSet<>(quotaRequestDelivery.getInternalResources()));
        quotaRequestDelivery.getDeliveryResults().forEach(deliveryResult -> {
            assertEquals(UUID.fromString(folderId), deliveryResult.getFolderId());
            assertTrue(deliveryResult.getBigOrderId() == bigOrder1.getBigOrderId() ||
                    deliveryResult.getBigOrderId() == bigOrder2.getBigOrderId());
            assertEquals(externalResourceId, deliveryResult.getResourceId());
            assertNotNull(deliveryResult.getFolderOperationId());
            assertNotNull(deliveryResult.getTimestamp());
            assertEquals(UUID.fromString(accountId), deliveryResult.getAccountId());
            assertNotNull(deliveryResult.getAccountOperationId());
        });
        assertEquals(QuotaRequestDeliveryResolveStatus.RESOLVED, quotaRequestDelivery.getResolveStatus());
    }

    @Test
    public void deliverySavedOnErrorResponseByDTest() {
        campaignDao.partialUpdate(aggregatedCampaign, CampaignUpdate.builder()
                .setBigOrders(List.of(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2)
                ))
                .build());
        aggregatedCampaign = campaignDao.read(aggregatedCampaign.getId());

        final Campaign.BigOrder bigOrder1 = aggregatedCampaign.getBigOrders().get(0);
        final Campaign.BigOrder bigOrder2 = aggregatedCampaign.getBigOrders().get(1);

        Service rtmr = serviceDao.create(Service.withKey("rtmr")
                .withName("rtmr")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", rtmr)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource ram = resourceDao.create(new Resource.Builder("ram_test", rtmr)
                .description("ram")
                .name("ram")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.MEMORY)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        resourceSegmentationDao.setSegmentations(ram, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder2).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(man)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(ram)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);
        UUID externalResourceId = UUID.randomUUID();
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpu)
                .campaignId(aggregatedCampaign.getId())
                .addSegments(Set.of(sas))
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(externalResourceId)
                        .externalResourceBaseUnitKey("cores")
                        .numerator(1L)
                        .denominator(1000L)
                        .build()
                )
        );

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("rtmr")), fetchedRequest.getProvidersToAllocate());
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        String accountId = UUID.randomUUID().toString();
        String folderId = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();
        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(cpu.getPublicKey())
                        .orderId(bigOrder1.getBigOrderId())
                        .segmentKeys(Set.of(sas.getPublicKey()))
                        .accountId(accountId)
                        .folderId(folderId)
                        .providerId(providerId)
                        .build())
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(cpu.getPublicKey())
                        .orderId(bigOrder2.getBigOrderId())
                        .segmentKeys(Set.of(sas.getPublicKey()))
                        .accountId(accountId)
                        .folderId(folderId)
                        .providerId(providerId)
                        .build())
                .build();

        mockDApi.setProvideProcessor(p -> {
            throw new ClientErrorException(400);
        });

        assertThrowsWithMessage(() -> allocateProvider(requestId, "rtmr", AMOSOV_F, body), 500,
                "Unknown error from resources model service. Status - 400: no response");

        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertFalse(fetchedRequest.getProvidersToAllocate().isEmpty());

        final QuotaChangeRequest request = requestDao.read(requestId);
        request.getChanges()
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(0L, c.getAmountAllocated());
                });

        final List<QuotaRequestDelivery> unresolved = deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(requestId,
                rtmr.getId());
        assertEquals(1, unresolved.size());
        QuotaRequestDelivery quotaRequestDelivery = unresolved.get(0);
        assertEquals(fetchedRequest.getProject().getAbcServiceId(), Long.valueOf(quotaRequestDelivery.getAbcServiceId()).intValue());
        assertEquals(fetchedRequest.getId(), quotaRequestDelivery.getQuotaRequestId());
        assertEquals(Objects.requireNonNull(fetchedRequest.getCampaign()).getId(), quotaRequestDelivery.getCampaignId());
        assertEquals(rtmr.getId(), quotaRequestDelivery.getProviderId());
        assertNotNull(quotaRequestDelivery.getResolvedAt());
        assertEquals(Set.of(
                ExternalResource.builder()
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder1.getBigOrderId())
                        .amount(500L)
                        .unitKey("cores")
                        .folderId(UUID.fromString(folderId))
                        .accountId(UUID.fromString(accountId))
                        .providerId(UUID.fromString(providerId))
                        .build(),
                ExternalResource.builder()
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder2.getBigOrderId())
                        .amount(500L)
                        .unitKey("cores")
                        .folderId(UUID.fromString(folderId))
                        .accountId(UUID.fromString(accountId))
                        .providerId(UUID.fromString(providerId))
                        .build()
        ), new HashSet<>(quotaRequestDelivery.getExternalResources()));
        assertEquals(Set.of(
                InternalResource.builder()
                        .resourceId(cpu.getId())
                        .addSegmentId(sas.getId())
                        .bigOrderId(bigOrder1.getBigOrderId())
                        .amount(500000L)
                        .build(),
                InternalResource.builder()
                        .resourceId(cpu.getId())
                        .addSegmentId(sas.getId())
                        .bigOrderId(bigOrder2.getBigOrderId())
                        .amount(500000L)
                        .build()
        ), new HashSet<>(quotaRequestDelivery.getInternalResources()));
        quotaRequestDelivery.getDeliveryResults().forEach(deliveryResult -> {
            assertEquals(UUID.fromString(folderId), deliveryResult.getFolderId());
            assertTrue(deliveryResult.getBigOrderId() == bigOrder1.getBigOrderId() ||
                    deliveryResult.getBigOrderId() == bigOrder2.getBigOrderId());
            assertEquals(externalResourceId, deliveryResult.getResourceId());
            assertNotNull(deliveryResult.getFolderOperationId());
            assertNotNull(deliveryResult.getTimestamp());
            assertEquals(UUID.fromString(accountId), deliveryResult.getAccountId());
            assertNotNull(deliveryResult.getAccountOperationId());
        });
        assertEquals(QuotaRequestDeliveryResolveStatus.IN_PROCESS, quotaRequestDelivery.getResolveStatus());
    }

    @Test
    public void cantRequestProvideTwiceAfterDResponseWithErrorTest() {
        campaignDao.partialUpdate(aggregatedCampaign, CampaignUpdate.builder()
                .setBigOrders(List.of(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2)
                ))
                .build());
        aggregatedCampaign = campaignDao.read(aggregatedCampaign.getId());

        final Campaign.BigOrder bigOrder1 = aggregatedCampaign.getBigOrders().get(0);
        final Campaign.BigOrder bigOrder2 = aggregatedCampaign.getBigOrders().get(1);

        Service rtmr = serviceDao.create(Service.withKey("rtmr")
                .withName("rtmr")
                .withAbcServiceId(42)
                .withPriority(42)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(true)
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build());

        updateHierarchy();
        Resource cpu = resourceDao.create(new Resource.Builder("cpu_test", rtmr)
                .description("cpu")
                .name("cpu")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.PROCESSOR)
                .build());

        Resource ram = resourceDao.create(new Resource.Builder("ram_test", rtmr)
                .description("ram")
                .name("ram")
                .noGroup()
                .priority(42)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .type(DiResourceType.MEMORY)
                .build());

        final DiProject project = createProject("foobar", YANDEX, AMOSOV_F.getLogin());
        updateHierarchy();

        final Segmentation locations = segmentationDao.create(new Segmentation.Builder("locations")
                .name("Locations")
                .description("Locations")
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(cpu, Set.of(locations));
        resourceSegmentationDao.setSegmentations(ram, Set.of(locations));
        final Segment man = segmentDao.create(new Segment.Builder("MAN", locations)
                .name("MAN")
                .description("MAN")
                .priority((short) 1)
                .build());
        final Segment sas = segmentDao.create(new Segment.Builder("SAS", locations)
                .name("SAS")
                .description("SAS")
                .priority((short) 1)
                .build());

        updateHierarchy();

        final long requestId = createRequest(defaultBuilder(aggregatedCampaign)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .project(Hierarchy.get().getProjectReader().read(project.getKey()))
                .changes(List.of(
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder2).segments(Set.of(sas)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(cpu)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(man)).build(),
                        QuotaChangeRequest.Change.newChangeBuilder().resource(ram)
                                .amount(1_000_000).amountReady(500_000).order(bigOrder1).segments(Set.of(sas)).build()
                ))
        );
        createRequestTicket(requestId, AMOSOV_F);
        UUID externalResourceId = UUID.randomUUID();
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpu)
                .campaignId(aggregatedCampaign.getId())
                .addSegments(Set.of(sas))
                .target(ResourceModelMappingTarget.builder()
                        .externalResourceId(externalResourceId)
                        .externalResourceBaseUnitKey("cores")
                        .numerator(1L)
                        .denominator(1000L)
                        .build()
                )
        );

        DiQuotaChangeRequest fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertEquals(Set.of(allocatableProviderInfo("rtmr")), fetchedRequest.getProvidersToAllocate());
        assertTrue(fetchedRequest.getPermissions().contains(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA));

        String accountId = UUID.randomUUID().toString();
        String folderId = UUID.randomUUID().toString();
        String providerId = UUID.randomUUID().toString();
        RequestQuotaAllocationBody body = RequestQuotaAllocationBody.builder()
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(cpu.getPublicKey())
                        .orderId(bigOrder1.getBigOrderId())
                        .segmentKeys(Set.of(sas.getPublicKey()))
                        .accountId(accountId)
                        .folderId(folderId)
                        .providerId(providerId)
                        .build())
                .change(RequestQuotaAllocationBody.Change.build()
                        .resourceKey(cpu.getPublicKey())
                        .orderId(bigOrder2.getBigOrderId())
                        .segmentKeys(Set.of(sas.getPublicKey()))
                        .accountId(accountId)
                        .folderId(folderId)
                        .providerId(providerId)
                        .build())
                .build();

        mockDApi.setProvideProcessor(p -> {
            throw new ClientErrorException(400);
        });

        assertThrowsWithMessage(() -> allocateProvider(requestId, "rtmr", AMOSOV_F, body), 500,
                "Unknown error from resources model service. Status - 400: no response");

        fetchedRequest = getRequest(requestId, AMOSOV_F);
        assertFalse(fetchedRequest.getProvidersToAllocate().isEmpty());

        final QuotaChangeRequest request = requestDao.read(requestId);
        request.getChanges()
                .forEach(c -> {
                    assertEquals(500_000, c.getAmountReady());
                    assertEquals(c.getAmountReady(), c.getAmountAllocating());
                    assertEquals(0L, c.getAmountAllocated());
                });

        final List<QuotaRequestDelivery> unresolved = deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(requestId,
                rtmr.getId());
        assertEquals(1, unresolved.size());
        QuotaRequestDelivery quotaRequestDelivery = unresolved.get(0);
        assertEquals(fetchedRequest.getProject().getAbcServiceId(), Long.valueOf(quotaRequestDelivery.getAbcServiceId()).intValue());
        assertEquals(fetchedRequest.getId(), quotaRequestDelivery.getQuotaRequestId());
        assertEquals(Objects.requireNonNull(fetchedRequest.getCampaign()).getId(), quotaRequestDelivery.getCampaignId());
        assertEquals(rtmr.getId(), quotaRequestDelivery.getProviderId());
        assertNotNull(quotaRequestDelivery.getResolvedAt());
        assertEquals(Set.of(
                ExternalResource.builder()
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder1.getBigOrderId())
                        .amount(500L)
                        .unitKey("cores")
                        .folderId(UUID.fromString(folderId))
                        .accountId(UUID.fromString(accountId))
                        .providerId(UUID.fromString(providerId))
                        .build(),
                ExternalResource.builder()
                        .resourceId(externalResourceId)
                        .bigOrderId(bigOrder2.getBigOrderId())
                        .amount(500L)
                        .unitKey("cores")
                        .folderId(UUID.fromString(folderId))
                        .accountId(UUID.fromString(accountId))
                        .providerId(UUID.fromString(providerId))
                        .build()
        ), new HashSet<>(quotaRequestDelivery.getExternalResources()));
        assertEquals(Set.of(
                InternalResource.builder()
                        .resourceId(cpu.getId())
                        .addSegmentId(sas.getId())
                        .bigOrderId(bigOrder1.getBigOrderId())
                        .amount(500000L)
                        .build(),
                InternalResource.builder()
                        .resourceId(cpu.getId())
                        .addSegmentId(sas.getId())
                        .bigOrderId(bigOrder2.getBigOrderId())
                        .amount(500000L)
                        .build()
        ), new HashSet<>(quotaRequestDelivery.getInternalResources()));
        quotaRequestDelivery.getDeliveryResults().forEach(deliveryResult -> {
            assertEquals(UUID.fromString(folderId), deliveryResult.getFolderId());
            assertTrue(deliveryResult.getBigOrderId() == bigOrder1.getBigOrderId() ||
                    deliveryResult.getBigOrderId() == bigOrder2.getBigOrderId());
            assertEquals(externalResourceId, deliveryResult.getResourceId());
            assertNotNull(deliveryResult.getFolderOperationId());
            assertNotNull(deliveryResult.getTimestamp());
            assertEquals(UUID.fromString(accountId), deliveryResult.getAccountId());
            assertNotNull(deliveryResult.getAccountOperationId());
        });
        assertEquals(QuotaRequestDeliveryResolveStatus.IN_PROCESS, quotaRequestDelivery.getResolveStatus());

        assertThrowsWithMessage(() -> allocateProvider(requestId, "rtmr", AMOSOV_F, body), 400,
                "RequestQuotaAllocationBody.Changes.0.resourceKey allocating already in " +
                        "process\\nRequestQuotaAllocationBody.Changes.1.resourceKey allocating already in process");
    }

    private DiQuotaChangeRequest createRequestTicket(long requestId, DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/quota-requests/" + requestId + "/create-ticket")
                .post(null, DiQuotaChangeRequest.class);
    }

    private DiQuotaChangeRequest getRequest(long requestId, DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/quota-requests/" + requestId)
                .get(DiQuotaChangeRequest.class);
    }

    @NotNull
    private DiQuotaChangeRequest.ProviderAllocationInfo allocatableProviderInfo(String key) {
        return new DiQuotaChangeRequest.ProviderAllocationInfo(key,
                Set.of(DiQuotaChangeRequest.Permission.CAN_ALLOCATE_QUOTA), List.of(), false, false);
    }

    private DiQuotaChangeRequest allocateProvider(long requestId, String providerKey, DiPerson person, RequestQuotaAllocationBody body) {
        return createAuthorizedLocalClient(person)
                .path("/v1/resource-preorder/" + requestId + "/provide-to-account/" + providerKey)
                .post(body, DiQuotaChangeRequest.class);
    }

    @NotNull
    private DiQuotaChangeRequest.ProviderAllocationInfo notAllocatableProviderInfo(String key, boolean allocating, String... notes) {
        return new DiQuotaChangeRequest.ProviderAllocationInfo(key, Set.of(), Arrays.asList(notes), allocating, false);
    }
}
