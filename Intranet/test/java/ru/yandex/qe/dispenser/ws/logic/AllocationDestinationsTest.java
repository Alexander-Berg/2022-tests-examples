package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSetAmountResult;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.request.quota.BodyUpdate;
import ru.yandex.qe.dispenser.api.v1.request.quota.ChangeBody;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceSegmentation;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.d.DeliveryAccountDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryAccountsSpaceDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryDestinationDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryDestinationProvidersDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryDestinationRequestDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryDestinationResourceDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryDestinationResponseDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryProviderDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryResourcesAccountsDto;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.goal.GoalDao;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.resources_model.ResourcesModelMappingDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.resources_model.ResourceModelMappingTarget;
import ru.yandex.qe.dispenser.domain.resources_model.ResourcesModelMapping;
import ru.yandex.qe.dispenser.standalone.MockDApi;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.quota.request.SetResourceAmountBody;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationGroup;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationProvider;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationQuotaRequest;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationSelectionQuotaRequest;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationSelectionRequest;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationSelectionResponse;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationSourceResource;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationSourceResourceSegment;
import ru.yandex.qe.dispenser.ws.reqbody.AllocationDestinationTargetResource;

public class AllocationDestinationsTest extends AcceptanceTestBase {

    @Autowired
    private BigOrderManager bigOrderManager;
    @Autowired
    private GoalDao goalDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentationDao segmentationDao;
    @Autowired
    private SegmentDao segmentDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private ProjectDao projectDao;
    @Autowired
    private ResourcesModelMappingDao resourcesModelMappingDao;
    @Autowired
    private MockDApi mockDApi;

    @BeforeEach
    public void beforeEachTest() {
        bigOrderManager.clear();
        goalDao.clear();
        mockDApi.clearDestinationRequests();
    }

    @Test
    public void testSuccess() {
        List<BigOrder> bigOrders = prepareBigOrders();
        Campaign campaign = prepareCampaign(bigOrders);
        BotCampaignGroup campaignGroup = prepareCampaignGroup(campaign, bigOrders);
        Service providerOne = prepareProvider("logfeller", 1, true, "simpleResourceModelMapper");
        Service providerTwo = prepareProvider("distbuild", 2, true, "simpleResourceModelMapper");
        Service providerThree = prepareProvider("strm", 3, false, null);
        Segmentation providerTwoSegmentationOne = prepareSegmentation("segmentationOne");
        Segmentation providerTwoSegmentationTwo = prepareSegmentation("segmentationTwo");
        Segment providerTwoSegmentationOneSegmentOne = prepareSegment(providerTwoSegmentationOne, "segmentOne");
        Segment providerTwoSegmentationOneSegmentTwo = prepareSegment(providerTwoSegmentationOne, "segmentTwo");
        Segment providerTwoSegmentationTwoSegmentOne = prepareSegment(providerTwoSegmentationTwo, "segmentThree");
        Segment providerTwoSegmentationTwoSegmentTwo = prepareSegment(providerTwoSegmentationTwo, "segmentFour");
        Resource providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR);
        Resource providerOneResourceTwo = prepareResource(providerOne, "resourceTwo", DiResourceType.PROCESSOR);
        Resource providerTwoResourceOne = prepareResource(providerTwo, "resourceThree", DiResourceType.PROCESSOR,
                providerTwoSegmentationOne, providerTwoSegmentationTwo);
        Resource providerTwoResourceTwo = prepareResource(providerTwo, "resourceFour", DiResourceType.PROCESSOR,
                providerTwoSegmentationOne, providerTwoSegmentationTwo);
        Resource providerThreeResourceOne = prepareResource(providerThree, "resourceFive", DiResourceType.PROCESSOR);
        Resource providerThreeResourceTwo = prepareResource(providerThree, "resourceSix", DiResourceType.PROCESSOR);
        Project projectOne = prepareProject("projectOne", 4);
        UUID commonExternalResourceId = UUID.randomUUID();
        ResourcesModelMapping providerOneResourceOneMappingOne = prepareResourcesModelMapping(campaign,
                providerOneResourceOne, 1, 1, null, "core", commonExternalResourceId);
        ResourcesModelMapping providerOneResourceOneMappingTwo = prepareResourcesModelMapping(campaign,
                providerOneResourceOne, 2, 1, null, "core", UUID.randomUUID());
        ResourcesModelMapping providerOneResourceTwoMappingOne = prepareResourcesModelMapping(campaign,
                providerOneResourceTwo, 3, 1, null, "core", commonExternalResourceId);
        ResourcesModelMapping providerOneResourceTwoMappingTwo = prepareResourcesModelMapping(campaign,
                providerOneResourceTwo, 4, 1, null, "core", UUID.randomUUID());
        ResourcesModelMapping providerTwoResourceOneMappingOne = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 5, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceOneMappingTwo = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 6, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceOneMappingThree = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 7, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceOneMappingFour = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 8, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceTwoMappingOne = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 9, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceTwoMappingTwo = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 10, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceTwoMappingThree = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 11, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceTwoMappingFour = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 12, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        updateHierarchy();
        prepareCampaignResources();
        DiQuotaChangeRequest requestOne = prepareRequestOne(bigOrders, providerOne, providerTwo, providerThree,
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
                providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
                providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
                providerThreeResourceTwo, projectOne, campaign);
        DiQuotaChangeRequest requestTwo = prepareRequestTwo(bigOrders, providerOne, providerTwo, providerThree,
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
                providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
                providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
                providerThreeResourceTwo, projectOne, campaign);
        DiQuotaChangeRequest confirmedRequestOne = confirmRequest(requestOne);
        DiQuotaChangeRequest confirmedRequestTwo = confirmRequest(requestTwo);
        setReadyRequestOne(bigOrders, providerOne, providerTwo, providerThree, providerTwoSegmentationOneSegmentOne,
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentOne,
                providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne, providerOneResourceTwo,
                providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne, providerThreeResourceTwo,
                confirmedRequestOne);
        setReadyRequestTwo(bigOrders, providerOne, providerTwo, providerThree, providerTwoSegmentationOneSegmentOne,
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentOne,
                providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne, providerOneResourceTwo,
                providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne, providerThreeResourceTwo,
                confirmedRequestTwo);
        Assertions.assertEquals(Set.of(providerOne.getKey(), providerTwo.getKey()),
                getQuotaRequest(requestOne.getId()).getProvidersToAllocate().stream()
                        .filter(DiQuotaChangeRequest.ProviderAllocationInfo::isDeliverable)
                        .map(DiQuotaChangeRequest.ProviderAllocationInfo::getKey).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(providerOne.getKey(), providerTwo.getKey()),
                getQuotaRequest(requestTwo.getId()).getProvidersToAllocate().stream()
                        .filter(DiQuotaChangeRequest.ProviderAllocationInfo::isDeliverable)
                        .map(DiQuotaChangeRequest.ProviderAllocationInfo::getKey).collect(Collectors.toSet()));
        List<DeliveryDestinationProvidersDto> destinations = new ArrayList<>();
        List<DeliveryProviderDto> requestOneProviders = new ArrayList<>();
        List<DeliveryProviderDto> requestTwoProviders = new ArrayList<>();
        String folderId = UUID.randomUUID().toString();
        String externalProviderIdOne = UUID.randomUUID().toString();
        String externalProviderIdTwo = UUID.randomUUID().toString();
        String externalProviderIdThree = UUID.randomUUID().toString();
        String externalProviderIdFour = UUID.randomUUID().toString();
        DeliveryAccountDto accountOne = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountOne", folderId, "test");
        DeliveryAccountDto accountTwo = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountTwo", folderId, "test");
        DeliveryAccountDto accountThree = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountThree", folderId, "test");
        DeliveryAccountDto accountFour = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountFour", folderId, "test");
        DeliveryAccountDto accountFive = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountFive", folderId, "test");
        DeliveryAccountDto accountSix = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountSix", folderId, "test");
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdOne, "providerOne", "providerOne",
                true, null, null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingOne)), List.of(accountOne))));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdTwo, "providerTwo", "providerTwo",
                true, null, null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingTwo), getResourceId(providerOneResourceTwoMappingTwo)),
                List.of(accountTwo))));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdOne, "providerOne", "providerOne",
                true, null, null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingOne)), List.of(accountOne))));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdTwo, "providerTwo", "providerTwo",
                true, null, null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingTwo), getResourceId(providerOneResourceTwoMappingTwo)),
                List.of(accountTwo))));
        DeliveryAccountsSpaceDto accountSpaceOne = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceOne", "accountsSpaceOne", true, null,
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceOneMappingOne),
                        getResourceId(providerTwoResourceOneMappingTwo)), List.of(accountThree)));
        DeliveryAccountsSpaceDto accountSpaceTwo = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceTwo", "accountsSpaceTwo", true, null,
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceOneMappingThree),
                        getResourceId(providerTwoResourceOneMappingFour)), List.of(accountFour)));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdThree, "providerThree", "providerThree",
                true, null, List.of(accountSpaceOne, accountSpaceTwo), null));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdThree, "providerThree", "providerThree",
                true, null, List.of(accountSpaceOne, accountSpaceTwo), null));
        DeliveryAccountsSpaceDto accountSpaceThree = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceThree", "accountsSpaceThree", true, null,
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceTwoMappingOne),
                        getResourceId(providerTwoResourceTwoMappingTwo)), List.of(accountFive)));
        DeliveryAccountsSpaceDto accountSpaceFour = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceFour", "accountsSpaceFour", true, null,
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceTwoMappingThree),
                        getResourceId(providerTwoResourceTwoMappingFour)), List.of(accountSix)));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdFour, "providerFour", "providerFour",
                true, null, List.of(accountSpaceThree, accountSpaceFour), null));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdFour, "providerFour", "providerFour",
                true, null, List.of(accountSpaceThree, accountSpaceFour), null));
        destinations.add(new DeliveryDestinationProvidersDto(Objects.requireNonNull(projectOne.getAbcServiceId())
                .longValue(), requestOne.getId(), true, null, requestOneProviders));
        destinations.add(new DeliveryDestinationProvidersDto(Objects.requireNonNull(projectOne.getAbcServiceId())
                .longValue(), requestTwo.getId(), true, null, requestTwoProviders));
        List<DeliveryDestinationResourceDto> resources = new ArrayList<>();
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceOneMappingOne),
                "resourceOne", "resourceOne", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceOneMappingTwo),
                "resourceTwo", "resourceTwo", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceTwoMappingTwo),
                "resourceThree", "resourceThree", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingOne),
                "resourceFour", "resourceFour", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingTwo),
                "resourceFive", "resourceFive", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingThree),
                "resourceSix", "resourceSix", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingFour),
                "resourceSeven", "resourceSeven", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingOne),
                "resourceEight", "resourceEight", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingTwo),
                "resourceNine", "resourceNine", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingThree),
                "resourceTen", "resourceTen", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingFour),
                "resourceEleven", "resourceEleven", true, null));
        mockDApi.setDestinations(new DeliveryDestinationResponseDto(destinations, resources));
        AllocationDestinationSelectionResponse result = findDestinationSelection(
                Map.of(confirmedRequestOne.getId(),
                        Set.of(providerOne.getKey(), providerTwo.getKey(), providerThree.getKey()),
                        confirmedRequestTwo.getId(), Set.of()));
        Assertions.assertNotNull(result);
        List<DeliveryDestinationRequestDto> destinationRequests = mockDApi.getDestinationRequests();
        Assertions.assertEquals(1, destinationRequests.size());
        Assertions.assertEquals("1120000000022901", destinationRequests.get(0).getUserUid());
        Assertions.assertEquals(2, destinationRequests.get(0).getDeliverables().size());
        Map<Long, DeliveryDestinationDto> requestDestinationsById = destinationRequests.get(0).getDeliverables()
                .stream().collect(Collectors.toMap(DeliveryDestinationDto::getQuotaRequestId, Function.identity()));
        Assertions.assertEquals(Objects.requireNonNull(projectOne.getAbcServiceId()).longValue(),
                requestDestinationsById.get(requestOne.getId()).getServiceId());
        Assertions.assertEquals(Objects.requireNonNull(projectOne.getAbcServiceId()).longValue(),
                requestDestinationsById.get(requestTwo.getId()).getServiceId());
        Set<String> expectedResourceIds = new HashSet<>();
        expectedResourceIds.add(getResourceId(providerOneResourceOneMappingOne));
        expectedResourceIds.add(getResourceId(providerOneResourceOneMappingTwo));
        expectedResourceIds.add(getResourceId(providerOneResourceTwoMappingOne));
        expectedResourceIds.add(getResourceId(providerOneResourceTwoMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingOne));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingThree));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingFour));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingOne));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingThree));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingFour));
        Assertions.assertEquals(expectedResourceIds,
                new HashSet<>(requestDestinationsById.get(requestOne.getId()).getResourceIds()));
        Assertions.assertEquals(expectedResourceIds,
                new HashSet<>(requestDestinationsById.get(requestTwo.getId()).getResourceIds()));
        Assertions.assertEquals(2, result.getRequests().size());
        Map<Long, AllocationDestinationQuotaRequest> requestsById = result.getRequests().stream()
                .collect(Collectors.toMap(AllocationDestinationQuotaRequest::getQuotaRequestId, Function.identity()));
        Assertions.assertTrue(requestsById.get(requestOne.getId()).isEligible());
        Assertions.assertTrue(requestsById.get(requestTwo.getId()).isEligible());
        Assertions.assertTrue(requestsById.get(requestOne.getId()).getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestsById.get(requestTwo.getId()).getIneligibilityReasons().isEmpty());
        Assertions.assertEquals(3, requestsById.get(requestOne.getId()).getProviders().size());
        Assertions.assertEquals(3, requestsById.get(requestTwo.getId()).getProviders().size());
        Map<String, AllocationDestinationProvider> requestOneProvidersByKey = requestsById.get(requestOne.getId())
                .getProviders().stream().collect(Collectors.toMap(AllocationDestinationProvider::getProviderKey,
                        Function.identity()));
        Map<String, AllocationDestinationProvider> requestTwoProvidersByKey = requestsById.get(requestTwo.getId())
                .getProviders().stream().collect(Collectors.toMap(AllocationDestinationProvider::getProviderKey,
                        Function.identity()));
        Assertions.assertTrue(requestOneProvidersByKey.get(providerOne.getKey()).isEligible());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerTwo.getKey()).isEligible());
        Assertions.assertFalse(requestOneProvidersByKey.get(providerThree.getKey()).isEligible());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerOne.getKey()).isEligible());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerTwo.getKey()).isEligible());
        Assertions.assertFalse(requestTwoProvidersByKey.get(providerThree.getKey()).isEligible());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerOne.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerTwo.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertFalse(requestOneProvidersByKey.get(providerThree.getKey())
                .getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerOne.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerTwo.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertFalse(requestTwoProvidersByKey.get(providerThree.getKey())
                .getIneligibilityReasons().isEmpty());
        Assertions.assertEquals(2, requestOneProvidersByKey.get(providerOne.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(2, requestTwoProvidersByKey.get(providerOne.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(4, requestOneProvidersByKey.get(providerTwo.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(4, requestTwoProvidersByKey.get(providerTwo.getKey())
                .getDestinationGroups().size());
        Map<String, AllocationDestinationGroup> requestOneProviderOneDestinationGroupsByName = requestOneProvidersByKey
                .get(providerOne.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestOneProviderTwoDestinationGroupsByName = requestOneProvidersByKey
                .get(providerTwo.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestTwoProviderOneDestinationGroupsByName = requestTwoProvidersByKey
                .get(providerOne.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestTwoProviderTwoDestinationGroupsByName = requestTwoProvidersByKey
                .get(providerTwo.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Assertions.assertEquals(1, requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().size());
        Assertions.assertEquals(accountOne.getId(),
                requestOneProviderOneDestinationGroupsByName.get("providerOne").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountTwo.getId(),
                requestOneProviderOneDestinationGroupsByName.get("providerTwo").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountOne.getId(),
                requestTwoProviderOneDestinationGroupsByName.get("providerOne").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountTwo.getId(),
                requestTwoProviderOneDestinationGroupsByName.get("providerTwo").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountThree.getId(), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountFour.getId(), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountFive.getId(), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountSix.getId(), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountThree.getId(), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountFour.getId(), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountFive.getId(), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountSix.getId(), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(2, requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().size());
        Assertions.assertEquals(2, requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().size());
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneProviderOneResources
                = requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneProviderTwoResources
                = requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoProviderOneResources
                = requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoProviderTwoResources
                = requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Assertions.assertEquals(2, requestOneProviderOneResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderOneResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderTwoResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderTwoResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderTwoResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderTwoResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceOneResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceTwoResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceThreeResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceThree")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceFourResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceFour")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceOneResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceTwoResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceThreeResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceThree")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceFourResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceFour")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Assertions.assertEquals(2, requestOneSpaceOneResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceTwoResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceThreeResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceFourResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceOneResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceTwoResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceThreeResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceFourResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingOne),
                getResourceId(providerTwoResourceOneMappingTwo)), requestOneSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingThree),
                getResourceId(providerTwoResourceOneMappingFour)), requestOneSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingOne),
                getResourceId(providerTwoResourceTwoMappingTwo)), requestOneSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingThree),
                getResourceId(providerTwoResourceTwoMappingFour)), requestOneSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingOne),
                getResourceId(providerTwoResourceOneMappingTwo)), requestTwoSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingThree),
                getResourceId(providerTwoResourceOneMappingFour)), requestTwoSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingOne),
                getResourceId(providerTwoResourceTwoMappingTwo)), requestTwoSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingThree),
                getResourceId(providerTwoResourceTwoMappingFour)), requestTwoSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertTrue(requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
    }

    @Test
    public void testSuccessAccountsMissing() {
        List<BigOrder> bigOrders = prepareBigOrders();
        Campaign campaign = prepareCampaign(bigOrders);
        BotCampaignGroup campaignGroup = prepareCampaignGroup(campaign, bigOrders);
        Service providerOne = prepareProvider("logfeller", 1, true, "simpleResourceModelMapper");
        Service providerTwo = prepareProvider("distbuild", 2, true, "simpleResourceModelMapper");
        Service providerThree = prepareProvider("strm", 3, false, null);
        Segmentation providerTwoSegmentationOne = prepareSegmentation("segmentationOne");
        Segmentation providerTwoSegmentationTwo = prepareSegmentation("segmentationTwo");
        Segment providerTwoSegmentationOneSegmentOne = prepareSegment(providerTwoSegmentationOne, "segmentOne");
        Segment providerTwoSegmentationOneSegmentTwo = prepareSegment(providerTwoSegmentationOne, "segmentTwo");
        Segment providerTwoSegmentationTwoSegmentOne = prepareSegment(providerTwoSegmentationTwo, "segmentThree");
        Segment providerTwoSegmentationTwoSegmentTwo = prepareSegment(providerTwoSegmentationTwo, "segmentFour");
        Resource providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR);
        Resource providerOneResourceTwo = prepareResource(providerOne, "resourceTwo", DiResourceType.PROCESSOR);
        Resource providerTwoResourceOne = prepareResource(providerTwo, "resourceThree", DiResourceType.PROCESSOR,
                providerTwoSegmentationOne, providerTwoSegmentationTwo);
        Resource providerTwoResourceTwo = prepareResource(providerTwo, "resourceFour", DiResourceType.PROCESSOR,
                providerTwoSegmentationOne, providerTwoSegmentationTwo);
        Resource providerThreeResourceOne = prepareResource(providerThree, "resourceFive", DiResourceType.PROCESSOR);
        Resource providerThreeResourceTwo = prepareResource(providerThree, "resourceSix", DiResourceType.PROCESSOR);
        Project projectOne = prepareProject("projectOne", 4);
        UUID commonExternalResourceId = UUID.randomUUID();
        ResourcesModelMapping providerOneResourceOneMappingOne = prepareResourcesModelMapping(campaign,
                providerOneResourceOne, 1, 1, null, "core", commonExternalResourceId);
        ResourcesModelMapping providerOneResourceOneMappingTwo = prepareResourcesModelMapping(campaign,
                providerOneResourceOne, 2, 1, null, "core", UUID.randomUUID());
        ResourcesModelMapping providerOneResourceTwoMappingOne = prepareResourcesModelMapping(campaign,
                providerOneResourceTwo, 3, 1, null, "core", commonExternalResourceId);
        ResourcesModelMapping providerOneResourceTwoMappingTwo = prepareResourcesModelMapping(campaign,
                providerOneResourceTwo, 4, 1, null, "core", UUID.randomUUID());
        ResourcesModelMapping providerTwoResourceOneMappingOne = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 5, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceOneMappingTwo = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 6, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceOneMappingThree = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 7, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceOneMappingFour = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 8, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceTwoMappingOne = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 9, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceTwoMappingTwo = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 10, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceTwoMappingThree = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 11, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceTwoMappingFour = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 12, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        updateHierarchy();
        prepareCampaignResources();
        DiQuotaChangeRequest requestOne = prepareRequestOne(bigOrders, providerOne, providerTwo, providerThree,
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
                providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
                providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
                providerThreeResourceTwo, projectOne, campaign);
        DiQuotaChangeRequest requestTwo = prepareRequestTwo(bigOrders, providerOne, providerTwo, providerThree,
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
                providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
                providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
                providerThreeResourceTwo, projectOne, campaign);
        DiQuotaChangeRequest confirmedRequestOne = confirmRequest(requestOne);
        DiQuotaChangeRequest confirmedRequestTwo = confirmRequest(requestTwo);
        setReadyRequestOne(bigOrders, providerOne, providerTwo, providerThree, providerTwoSegmentationOneSegmentOne,
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentOne,
                providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne, providerOneResourceTwo,
                providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne, providerThreeResourceTwo,
                confirmedRequestOne);
        setReadyRequestTwo(bigOrders, providerOne, providerTwo, providerThree, providerTwoSegmentationOneSegmentOne,
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentOne,
                providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne, providerOneResourceTwo,
                providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne, providerThreeResourceTwo,
                confirmedRequestTwo);
        Assertions.assertEquals(Set.of(providerOne.getKey(), providerTwo.getKey()),
                getQuotaRequest(requestOne.getId()).getProvidersToAllocate().stream()
                        .filter(DiQuotaChangeRequest.ProviderAllocationInfo::isDeliverable)
                        .map(DiQuotaChangeRequest.ProviderAllocationInfo::getKey).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(providerOne.getKey(), providerTwo.getKey()),
                getQuotaRequest(requestTwo.getId()).getProvidersToAllocate().stream()
                        .filter(DiQuotaChangeRequest.ProviderAllocationInfo::isDeliverable)
                        .map(DiQuotaChangeRequest.ProviderAllocationInfo::getKey).collect(Collectors.toSet()));
        List<DeliveryDestinationProvidersDto> destinations = new ArrayList<>();
        List<DeliveryProviderDto> requestOneProviders = new ArrayList<>();
        List<DeliveryProviderDto> requestTwoProviders = new ArrayList<>();
        String folderId = UUID.randomUUID().toString();
        String externalProviderIdOne = UUID.randomUUID().toString();
        String externalProviderIdTwo = UUID.randomUUID().toString();
        String externalProviderIdThree = UUID.randomUUID().toString();
        String externalProviderIdFour = UUID.randomUUID().toString();
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdOne, "providerOne", "providerOne",
                true, null, null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingOne)), List.of())));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdTwo, "providerTwo", "providerTwo",
                true, null, null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingTwo), getResourceId(providerOneResourceTwoMappingTwo)),
                List.of())));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdOne, "providerOne", "providerOne",
                true, null, null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingOne)), List.of())));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdTwo, "providerTwo", "providerTwo",
                true, null, null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingTwo), getResourceId(providerOneResourceTwoMappingTwo)),
                List.of())));
        DeliveryAccountsSpaceDto accountSpaceOne = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceOne", "accountsSpaceOne", true, null,
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceOneMappingOne),
                        getResourceId(providerTwoResourceOneMappingTwo)), List.of()));
        DeliveryAccountsSpaceDto accountSpaceTwo = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceTwo", "accountsSpaceTwo", true, null,
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceOneMappingThree),
                        getResourceId(providerTwoResourceOneMappingFour)), List.of()));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdThree, "providerThree", "providerThree",
                true, null, List.of(accountSpaceOne, accountSpaceTwo), null));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdThree, "providerThree", "providerThree",
                true, null, List.of(accountSpaceOne, accountSpaceTwo), null));
        DeliveryAccountsSpaceDto accountSpaceThree = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceThree", "accountsSpaceThree", true, null,
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceTwoMappingOne),
                        getResourceId(providerTwoResourceTwoMappingTwo)), List.of()));
        DeliveryAccountsSpaceDto accountSpaceFour = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceFour", "accountsSpaceFour", true, null,
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceTwoMappingThree),
                        getResourceId(providerTwoResourceTwoMappingFour)), List.of()));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdFour, "providerFour", "providerFour",
                true, null, List.of(accountSpaceThree, accountSpaceFour), null));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdFour, "providerFour", "providerFour",
                true, null, List.of(accountSpaceThree, accountSpaceFour), null));
        destinations.add(new DeliveryDestinationProvidersDto(Objects.requireNonNull(projectOne.getAbcServiceId())
                .longValue(), requestOne.getId(), true, null, requestOneProviders));
        destinations.add(new DeliveryDestinationProvidersDto(Objects.requireNonNull(projectOne.getAbcServiceId())
                .longValue(), requestTwo.getId(), true, null, requestTwoProviders));
        List<DeliveryDestinationResourceDto> resources = new ArrayList<>();
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceOneMappingOne),
                "resourceOne", "resourceOne", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceOneMappingTwo),
                "resourceTwo", "resourceTwo", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceTwoMappingTwo),
                "resourceThree", "resourceThree", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingOne),
                "resourceFour", "resourceFour", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingTwo),
                "resourceFive", "resourceFive", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingThree),
                "resourceSix", "resourceSix", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingFour),
                "resourceSeven", "resourceSeven", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingOne),
                "resourceEight", "resourceEight", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingTwo),
                "resourceNine", "resourceNine", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingThree),
                "resourceTen", "resourceTen", true, null));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingFour),
                "resourceEleven", "resourceEleven", true, null));
        mockDApi.setDestinations(new DeliveryDestinationResponseDto(destinations, resources));
        AllocationDestinationSelectionResponse result = findDestinationSelection(
                Map.of(confirmedRequestOne.getId(),
                        Set.of(providerOne.getKey(), providerTwo.getKey(), providerThree.getKey()),
                        confirmedRequestTwo.getId(), Set.of()));
        Assertions.assertNotNull(result);
        List<DeliveryDestinationRequestDto> destinationRequests = mockDApi.getDestinationRequests();
        Assertions.assertEquals(1, destinationRequests.size());
        Assertions.assertEquals("1120000000022901", destinationRequests.get(0).getUserUid());
        Assertions.assertEquals(2, destinationRequests.get(0).getDeliverables().size());
        Map<Long, DeliveryDestinationDto> requestDestinationsById = destinationRequests.get(0).getDeliverables()
                .stream().collect(Collectors.toMap(DeliveryDestinationDto::getQuotaRequestId, Function.identity()));
        Assertions.assertEquals(Objects.requireNonNull(projectOne.getAbcServiceId()).longValue(),
                requestDestinationsById.get(requestOne.getId()).getServiceId());
        Assertions.assertEquals(Objects.requireNonNull(projectOne.getAbcServiceId()).longValue(),
                requestDestinationsById.get(requestTwo.getId()).getServiceId());
        Set<String> expectedResourceIds = new HashSet<>();
        expectedResourceIds.add(getResourceId(providerOneResourceOneMappingOne));
        expectedResourceIds.add(getResourceId(providerOneResourceOneMappingTwo));
        expectedResourceIds.add(getResourceId(providerOneResourceTwoMappingOne));
        expectedResourceIds.add(getResourceId(providerOneResourceTwoMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingOne));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingThree));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingFour));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingOne));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingThree));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingFour));
        Assertions.assertEquals(expectedResourceIds,
                new HashSet<>(requestDestinationsById.get(requestOne.getId()).getResourceIds()));
        Assertions.assertEquals(expectedResourceIds,
                new HashSet<>(requestDestinationsById.get(requestTwo.getId()).getResourceIds()));
        Assertions.assertEquals(2, result.getRequests().size());
        Map<Long, AllocationDestinationQuotaRequest> requestsById = result.getRequests().stream()
                .collect(Collectors.toMap(AllocationDestinationQuotaRequest::getQuotaRequestId, Function.identity()));
        Assertions.assertTrue(requestsById.get(requestOne.getId()).isEligible());
        Assertions.assertTrue(requestsById.get(requestTwo.getId()).isEligible());
        Assertions.assertTrue(requestsById.get(requestOne.getId()).getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestsById.get(requestTwo.getId()).getIneligibilityReasons().isEmpty());
        Assertions.assertEquals(3, requestsById.get(requestOne.getId()).getProviders().size());
        Assertions.assertEquals(3, requestsById.get(requestTwo.getId()).getProviders().size());
        Map<String, AllocationDestinationProvider> requestOneProvidersByKey = requestsById.get(requestOne.getId())
                .getProviders().stream().collect(Collectors.toMap(AllocationDestinationProvider::getProviderKey,
                        Function.identity()));
        Map<String, AllocationDestinationProvider> requestTwoProvidersByKey = requestsById.get(requestTwo.getId())
                .getProviders().stream().collect(Collectors.toMap(AllocationDestinationProvider::getProviderKey,
                        Function.identity()));
        Assertions.assertTrue(requestOneProvidersByKey.get(providerOne.getKey()).isEligible());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerTwo.getKey()).isEligible());
        Assertions.assertFalse(requestOneProvidersByKey.get(providerThree.getKey()).isEligible());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerOne.getKey()).isEligible());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerTwo.getKey()).isEligible());
        Assertions.assertFalse(requestTwoProvidersByKey.get(providerThree.getKey()).isEligible());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerOne.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerTwo.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertFalse(requestOneProvidersByKey.get(providerThree.getKey())
                .getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerOne.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerTwo.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertFalse(requestTwoProvidersByKey.get(providerThree.getKey())
                .getIneligibilityReasons().isEmpty());
        Assertions.assertEquals(2, requestOneProvidersByKey.get(providerOne.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(2, requestTwoProvidersByKey.get(providerOne.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(4, requestOneProvidersByKey.get(providerTwo.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(4, requestTwoProvidersByKey.get(providerTwo.getKey())
                .getDestinationGroups().size());
        Map<String, AllocationDestinationGroup> requestOneProviderOneDestinationGroupsByName = requestOneProvidersByKey
                .get(providerOne.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestOneProviderTwoDestinationGroupsByName = requestOneProvidersByKey
                .get(providerTwo.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestTwoProviderOneDestinationGroupsByName = requestTwoProvidersByKey
                .get(providerOne.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestTwoProviderTwoDestinationGroupsByName = requestTwoProvidersByKey
                .get(providerTwo.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Assertions.assertEquals(0, requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getTargetAccounts().size());
        Assertions.assertEquals(0, requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getTargetAccounts().size());
        Assertions.assertEquals(0, requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getTargetAccounts().size());
        Assertions.assertEquals(0, requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getTargetAccounts().size());
        Assertions.assertEquals(0, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().size());
        Assertions.assertEquals(0, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().size());
        Assertions.assertEquals(0, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().size());
        Assertions.assertEquals(0, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().size());
        Assertions.assertEquals(0, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().size());
        Assertions.assertEquals(0, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().size());
        Assertions.assertEquals(0, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().size());
        Assertions.assertEquals(0, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().size());
        Assertions.assertEquals(2, requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().size());
        Assertions.assertEquals(2, requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().size());
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestOneProviderOneDestinationGroupsByName.get("providerOne")
                        .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                        .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                        .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                        .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneProviderOneResources
                = requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneProviderTwoResources
                = requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoProviderOneResources
                = requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoProviderTwoResources
                = requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Assertions.assertEquals(2, requestOneProviderOneResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderOneResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderTwoResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderTwoResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderTwoResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderTwoResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceOneResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceTwoResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceThreeResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceThree")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceFourResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceFour")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceOneResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceTwoResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceThreeResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceThree")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceFourResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceFour")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Assertions.assertEquals(2, requestOneSpaceOneResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceTwoResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceThreeResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceFourResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceOneResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceTwoResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceThreeResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceFourResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingOne),
                getResourceId(providerTwoResourceOneMappingTwo)), requestOneSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingThree),
                getResourceId(providerTwoResourceOneMappingFour)), requestOneSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingOne),
                getResourceId(providerTwoResourceTwoMappingTwo)), requestOneSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingThree),
                getResourceId(providerTwoResourceTwoMappingFour)), requestOneSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingOne),
                getResourceId(providerTwoResourceOneMappingTwo)), requestTwoSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingThree),
                getResourceId(providerTwoResourceOneMappingFour)), requestTwoSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingOne),
                getResourceId(providerTwoResourceTwoMappingTwo)), requestTwoSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingThree),
                getResourceId(providerTwoResourceTwoMappingFour)), requestTwoSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertTrue(requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .allMatch(AllocationDestinationTargetResource::isEligible));
    }

    @Test
    public void testSuccessIneligible() {
        List<BigOrder> bigOrders = prepareBigOrders();
        Campaign campaign = prepareCampaign(bigOrders);
        BotCampaignGroup campaignGroup = prepareCampaignGroup(campaign, bigOrders);
        Service providerOne = prepareProvider("logfeller", 1, true, "simpleResourceModelMapper");
        Service providerTwo = prepareProvider("distbuild", 2, true, "simpleResourceModelMapper");
        Service providerThree = prepareProvider("strm", 3, false, null);
        Segmentation providerTwoSegmentationOne = prepareSegmentation("segmentationOne");
        Segmentation providerTwoSegmentationTwo = prepareSegmentation("segmentationTwo");
        Segment providerTwoSegmentationOneSegmentOne = prepareSegment(providerTwoSegmentationOne, "segmentOne");
        Segment providerTwoSegmentationOneSegmentTwo = prepareSegment(providerTwoSegmentationOne, "segmentTwo");
        Segment providerTwoSegmentationTwoSegmentOne = prepareSegment(providerTwoSegmentationTwo, "segmentThree");
        Segment providerTwoSegmentationTwoSegmentTwo = prepareSegment(providerTwoSegmentationTwo, "segmentFour");
        Resource providerOneResourceOne = prepareResource(providerOne, "resourceOne", DiResourceType.PROCESSOR);
        Resource providerOneResourceTwo = prepareResource(providerOne, "resourceTwo", DiResourceType.PROCESSOR);
        Resource providerTwoResourceOne = prepareResource(providerTwo, "resourceThree", DiResourceType.PROCESSOR,
                providerTwoSegmentationOne, providerTwoSegmentationTwo);
        Resource providerTwoResourceTwo = prepareResource(providerTwo, "resourceFour", DiResourceType.PROCESSOR,
                providerTwoSegmentationOne, providerTwoSegmentationTwo);
        Resource providerThreeResourceOne = prepareResource(providerThree, "resourceFive", DiResourceType.PROCESSOR);
        Resource providerThreeResourceTwo = prepareResource(providerThree, "resourceSix", DiResourceType.PROCESSOR);
        Project projectOne = prepareProject("projectOne", 4);
        UUID commonExternalResourceId = UUID.randomUUID();
        ResourcesModelMapping providerOneResourceOneMappingOne = prepareResourcesModelMapping(campaign,
                providerOneResourceOne, 1, 1, null, "core", commonExternalResourceId);
        ResourcesModelMapping providerOneResourceOneMappingTwo = prepareResourcesModelMapping(campaign,
                providerOneResourceOne, 2, 1, null, "core", UUID.randomUUID());
        ResourcesModelMapping providerOneResourceTwoMappingOne = prepareResourcesModelMapping(campaign,
                providerOneResourceTwo, 3, 1, null, "core", commonExternalResourceId);
        ResourcesModelMapping providerOneResourceTwoMappingTwo = prepareResourcesModelMapping(campaign,
                providerOneResourceTwo, 4, 1, null, "core", UUID.randomUUID());
        ResourcesModelMapping providerTwoResourceOneMappingOne = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 5, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceOneMappingTwo = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 6, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceOneMappingThree = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 7, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceOneMappingFour = prepareResourcesModelMapping(campaign,
                providerTwoResourceOne, 8, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceTwoMappingOne = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 9, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceTwoMappingTwo = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 10, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationTwoSegmentOne);
        ResourcesModelMapping providerTwoResourceTwoMappingThree = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 11, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        ResourcesModelMapping providerTwoResourceTwoMappingFour = prepareResourcesModelMapping(campaign,
                providerTwoResourceTwo, 12, 1, null, "core", UUID.randomUUID(),
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentTwo);
        updateHierarchy();
        prepareCampaignResources();
        DiQuotaChangeRequest requestOne = prepareRequestOne(bigOrders, providerOne, providerTwo, providerThree,
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
                providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
                providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
                providerThreeResourceTwo, projectOne, campaign);
        DiQuotaChangeRequest requestTwo = prepareRequestTwo(bigOrders, providerOne, providerTwo, providerThree,
                providerTwoSegmentationOneSegmentOne, providerTwoSegmentationOneSegmentTwo,
                providerTwoSegmentationTwoSegmentOne, providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne,
                providerOneResourceTwo, providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne,
                providerThreeResourceTwo, projectOne, campaign);
        DiQuotaChangeRequest confirmedRequestOne = confirmRequest(requestOne);
        DiQuotaChangeRequest confirmedRequestTwo = confirmRequest(requestTwo);
        setReadyRequestOne(bigOrders, providerOne, providerTwo, providerThree, providerTwoSegmentationOneSegmentOne,
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentOne,
                providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne, providerOneResourceTwo,
                providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne, providerThreeResourceTwo,
                confirmedRequestOne);
        setReadyRequestTwo(bigOrders, providerOne, providerTwo, providerThree, providerTwoSegmentationOneSegmentOne,
                providerTwoSegmentationOneSegmentTwo, providerTwoSegmentationTwoSegmentOne,
                providerTwoSegmentationTwoSegmentTwo, providerOneResourceOne, providerOneResourceTwo,
                providerTwoResourceOne, providerTwoResourceTwo, providerThreeResourceOne, providerThreeResourceTwo,
                confirmedRequestTwo);
        Assertions.assertEquals(Set.of(providerOne.getKey(), providerTwo.getKey()),
                getQuotaRequest(requestOne.getId()).getProvidersToAllocate().stream()
                        .filter(DiQuotaChangeRequest.ProviderAllocationInfo::isDeliverable)
                        .map(DiQuotaChangeRequest.ProviderAllocationInfo::getKey).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(providerOne.getKey(), providerTwo.getKey()),
                getQuotaRequest(requestTwo.getId()).getProvidersToAllocate().stream()
                        .filter(DiQuotaChangeRequest.ProviderAllocationInfo::isDeliverable)
                        .map(DiQuotaChangeRequest.ProviderAllocationInfo::getKey).collect(Collectors.toSet()));
        List<DeliveryDestinationProvidersDto> destinations = new ArrayList<>();
        List<DeliveryProviderDto> requestOneProviders = new ArrayList<>();
        List<DeliveryProviderDto> requestTwoProviders = new ArrayList<>();
        String folderId = UUID.randomUUID().toString();
        String externalProviderIdOne = UUID.randomUUID().toString();
        String externalProviderIdTwo = UUID.randomUUID().toString();
        String externalProviderIdThree = UUID.randomUUID().toString();
        String externalProviderIdFour = UUID.randomUUID().toString();
        DeliveryAccountDto accountOne = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountOne", folderId, "test");
        DeliveryAccountDto accountTwo = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountTwo", folderId, "test");
        DeliveryAccountDto accountThree = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountThree", folderId, "test");
        DeliveryAccountDto accountFour = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountFour", folderId, "test");
        DeliveryAccountDto accountFive = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountFive", folderId, "test");
        DeliveryAccountDto accountSix = new DeliveryAccountDto(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), null, "accountSix", folderId, "test");
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdOne, "providerOne", "providerOne",
                false, List.of("Test provider"), null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingOne)), List.of(accountOne))));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdTwo, "providerTwo", "providerTwo",
                false, List.of("Test provider"), null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingTwo), getResourceId(providerOneResourceTwoMappingTwo)),
                List.of(accountTwo))));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdOne, "providerOne", "providerOne",
                false, List.of("Test provider"), null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingOne)), List.of(accountOne))));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdTwo, "providerTwo", "providerTwo",
                false, List.of("Test provider"), null, new DeliveryResourcesAccountsDto(List
                .of(getResourceId(providerOneResourceOneMappingTwo), getResourceId(providerOneResourceTwoMappingTwo)),
                List.of(accountTwo))));
        DeliveryAccountsSpaceDto accountSpaceOne = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceOne", "accountsSpaceOne", false, List.of("Test accounts space"),
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceOneMappingOne),
                        getResourceId(providerTwoResourceOneMappingTwo)), List.of(accountThree)));
        DeliveryAccountsSpaceDto accountSpaceTwo = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceTwo", "accountsSpaceTwo", false, List.of("Test accounts space"),
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceOneMappingThree),
                        getResourceId(providerTwoResourceOneMappingFour)), List.of(accountFour)));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdThree, "providerThree", "providerThree",
                false, List.of("Test provider"), List.of(accountSpaceOne, accountSpaceTwo), null));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdThree, "providerThree", "providerThree",
                false, List.of("Test provider"), List.of(accountSpaceOne, accountSpaceTwo), null));
        DeliveryAccountsSpaceDto accountSpaceThree = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceThree", "accountsSpaceThree", false, List.of("Test accounts space"),
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceTwoMappingOne),
                        getResourceId(providerTwoResourceTwoMappingTwo)), List.of(accountFive)));
        DeliveryAccountsSpaceDto accountSpaceFour = new DeliveryAccountsSpaceDto(UUID.randomUUID().toString(),
                "accountsSpaceFour", "accountsSpaceFour", false, List.of("Test accounts space"),
                new DeliveryResourcesAccountsDto(List.of(getResourceId(providerTwoResourceTwoMappingThree),
                        getResourceId(providerTwoResourceTwoMappingFour)), List.of(accountSix)));
        requestOneProviders.add(new DeliveryProviderDto(externalProviderIdFour, "providerFour", "providerFour",
                false, List.of("Test provider"), List.of(accountSpaceThree, accountSpaceFour), null));
        requestTwoProviders.add(new DeliveryProviderDto(externalProviderIdFour, "providerFour", "providerFour",
                false, List.of("Test provider"), List.of(accountSpaceThree, accountSpaceFour), null));
        destinations.add(new DeliveryDestinationProvidersDto(Objects.requireNonNull(projectOne.getAbcServiceId())
                .longValue(), requestOne.getId(), false, List.of("Test provider"), requestOneProviders));
        destinations.add(new DeliveryDestinationProvidersDto(Objects.requireNonNull(projectOne.getAbcServiceId())
                .longValue(), requestTwo.getId(), false, List.of("Test provider"), requestTwoProviders));
        List<DeliveryDestinationResourceDto> resources = new ArrayList<>();
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceOneMappingOne),
                "resourceOne", "resourceOne", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceOneMappingTwo),
                "resourceTwo", "resourceTwo", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerOneResourceTwoMappingTwo),
                "resourceThree", "resourceThree", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingOne),
                "resourceFour", "resourceFour", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingTwo),
                "resourceFive", "resourceFive", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingThree),
                "resourceSix", "resourceSix", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceOneMappingFour),
                "resourceSeven", "resourceSeven", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingOne),
                "resourceEight", "resourceEight", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingTwo),
                "resourceNine", "resourceNine", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingThree),
                "resourceTen", "resourceTen", false, List.of("Test resource")));
        resources.add(new DeliveryDestinationResourceDto(getResourceId(providerTwoResourceTwoMappingFour),
                "resourceEleven", "resourceEleven", false, List.of("Test resource")));
        mockDApi.setDestinations(new DeliveryDestinationResponseDto(destinations, resources));
        AllocationDestinationSelectionResponse result = findDestinationSelection(
                Map.of(confirmedRequestOne.getId(),
                        Set.of(providerOne.getKey(), providerTwo.getKey(), providerThree.getKey()),
                        confirmedRequestTwo.getId(), Set.of()));
        Assertions.assertNotNull(result);
        List<DeliveryDestinationRequestDto> destinationRequests = mockDApi.getDestinationRequests();
        Assertions.assertEquals(1, destinationRequests.size());
        Assertions.assertEquals("1120000000022901", destinationRequests.get(0).getUserUid());
        Assertions.assertEquals(2, destinationRequests.get(0).getDeliverables().size());
        Map<Long, DeliveryDestinationDto> requestDestinationsById = destinationRequests.get(0).getDeliverables()
                .stream().collect(Collectors.toMap(DeliveryDestinationDto::getQuotaRequestId, Function.identity()));
        Assertions.assertEquals(Objects.requireNonNull(projectOne.getAbcServiceId()).longValue(),
                requestDestinationsById.get(requestOne.getId()).getServiceId());
        Assertions.assertEquals(Objects.requireNonNull(projectOne.getAbcServiceId()).longValue(),
                requestDestinationsById.get(requestTwo.getId()).getServiceId());
        Set<String> expectedResourceIds = new HashSet<>();
        expectedResourceIds.add(getResourceId(providerOneResourceOneMappingOne));
        expectedResourceIds.add(getResourceId(providerOneResourceOneMappingTwo));
        expectedResourceIds.add(getResourceId(providerOneResourceTwoMappingOne));
        expectedResourceIds.add(getResourceId(providerOneResourceTwoMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingOne));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingThree));
        expectedResourceIds.add(getResourceId(providerTwoResourceOneMappingFour));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingOne));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingTwo));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingThree));
        expectedResourceIds.add(getResourceId(providerTwoResourceTwoMappingFour));
        Assertions.assertEquals(expectedResourceIds,
                new HashSet<>(requestDestinationsById.get(requestOne.getId()).getResourceIds()));
        Assertions.assertEquals(expectedResourceIds,
                new HashSet<>(requestDestinationsById.get(requestTwo.getId()).getResourceIds()));
        Assertions.assertEquals(2, result.getRequests().size());
        Map<Long, AllocationDestinationQuotaRequest> requestsById = result.getRequests().stream()
                .collect(Collectors.toMap(AllocationDestinationQuotaRequest::getQuotaRequestId, Function.identity()));
        Assertions.assertFalse(requestsById.get(requestOne.getId()).isEligible());
        Assertions.assertFalse(requestsById.get(requestTwo.getId()).isEligible());
        Assertions.assertFalse(requestsById.get(requestOne.getId()).getIneligibilityReasons().isEmpty());
        Assertions.assertFalse(requestsById.get(requestTwo.getId()).getIneligibilityReasons().isEmpty());
        Assertions.assertEquals(3, requestsById.get(requestOne.getId()).getProviders().size());
        Assertions.assertEquals(3, requestsById.get(requestTwo.getId()).getProviders().size());
        Map<String, AllocationDestinationProvider> requestOneProvidersByKey = requestsById.get(requestOne.getId())
                .getProviders().stream().collect(Collectors.toMap(AllocationDestinationProvider::getProviderKey,
                        Function.identity()));
        Map<String, AllocationDestinationProvider> requestTwoProvidersByKey = requestsById.get(requestTwo.getId())
                .getProviders().stream().collect(Collectors.toMap(AllocationDestinationProvider::getProviderKey,
                        Function.identity()));
        Assertions.assertTrue(requestOneProvidersByKey.get(providerOne.getKey()).isEligible());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerTwo.getKey()).isEligible());
        Assertions.assertFalse(requestOneProvidersByKey.get(providerThree.getKey()).isEligible());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerOne.getKey()).isEligible());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerTwo.getKey()).isEligible());
        Assertions.assertFalse(requestTwoProvidersByKey.get(providerThree.getKey()).isEligible());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerOne.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestOneProvidersByKey.get(providerTwo.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertFalse(requestOneProvidersByKey.get(providerThree.getKey())
                .getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerOne.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertTrue(requestTwoProvidersByKey.get(providerTwo.getKey()).getIneligibilityReasons().isEmpty());
        Assertions.assertFalse(requestTwoProvidersByKey.get(providerThree.getKey())
                .getIneligibilityReasons().isEmpty());
        Assertions.assertEquals(2, requestOneProvidersByKey.get(providerOne.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(2, requestTwoProvidersByKey.get(providerOne.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(4, requestOneProvidersByKey.get(providerTwo.getKey())
                .getDestinationGroups().size());
        Assertions.assertEquals(4, requestTwoProvidersByKey.get(providerTwo.getKey())
                .getDestinationGroups().size());
        Map<String, AllocationDestinationGroup> requestOneProviderOneDestinationGroupsByName = requestOneProvidersByKey
                .get(providerOne.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestOneProviderTwoDestinationGroupsByName = requestOneProvidersByKey
                .get(providerTwo.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestTwoProviderOneDestinationGroupsByName = requestTwoProvidersByKey
                .get(providerOne.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Map<String, AllocationDestinationGroup> requestTwoProviderTwoDestinationGroupsByName = requestTwoProvidersByKey
                .get(providerTwo.getKey()).getDestinationGroups().stream()
                .collect(Collectors.toMap(AllocationDestinationGroup::getName, Function.identity()));
        Assertions.assertEquals(1, requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().size());
        Assertions.assertEquals(accountOne.getId(),
                requestOneProviderOneDestinationGroupsByName.get("providerOne").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountTwo.getId(),
                requestOneProviderOneDestinationGroupsByName.get("providerTwo").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountOne.getId(),
                requestTwoProviderOneDestinationGroupsByName.get("providerOne").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountTwo.getId(),
                requestTwoProviderOneDestinationGroupsByName.get("providerTwo").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountThree.getId(), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountFour.getId(), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountFive.getId(), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountSix.getId(), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountThree.getId(), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountFour.getId(), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountFive.getId(), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(accountSix.getId(), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getTargetAccounts().get(0).getId());
        Assertions.assertEquals(2, requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().size());
        Assertions.assertEquals(2, requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().size());
        Assertions.assertEquals(1, requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().size());
        Assertions.assertEquals(1, requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().size());
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestOneProviderOneDestinationGroupsByName.get("providerOne")
                        .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                        .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                        .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()),
                        new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of())),
                requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                        .getResources().stream().map(DeliverableResource::new).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestOneProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceOne").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerThree - accountsSpaceTwo").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(), providerTwoSegmentationTwoSegmentOne
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceThree").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(), providerTwoSegmentationTwoSegmentTwo
                        .getPublicKey()))), requestTwoProviderTwoDestinationGroupsByName
                .get("providerFour - accountsSpaceFour").getResources().stream().map(DeliverableResource::new)
                .collect(Collectors.toSet()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneProviderOneResources
                = requestOneProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneProviderTwoResources
                = requestOneProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoProviderOneResources
                = requestTwoProviderOneDestinationGroupsByName.get("providerOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoProviderTwoResources
                = requestTwoProviderOneDestinationGroupsByName.get("providerTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Assertions.assertEquals(2, requestOneProviderOneResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderOneResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderTwoResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestOneProviderTwoResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderOneResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderTwoResources.get(new DeliverableResource(
                providerOneResourceOne.getPublicKey(), Set.of())).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoProviderTwoResources.get(new DeliverableResource(
                providerOneResourceTwo.getPublicKey(), Set.of())).getTargetResources().size());
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceOneResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceTwoResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceThreeResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceThree")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestOneSpaceFourResources
                = requestOneProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceFour")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceOneResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceOne")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceTwoResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerThree - accountsSpaceTwo")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceThreeResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceThree")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Map<DeliverableResource, AllocationDestinationSourceResource> requestTwoSpaceFourResources
                = requestTwoProviderTwoDestinationGroupsByName.get("providerFour - accountsSpaceFour")
                .getResources().stream().collect(Collectors.toMap(DeliverableResource::new, Function.identity()));
        Assertions.assertEquals(2, requestOneSpaceOneResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceTwoResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceThreeResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestOneSpaceFourResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceOneResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceTwoResources.get(new DeliverableResource(
                providerTwoResourceOne.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceThreeResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(2, requestTwoSpaceFourResources.get(new DeliverableResource(
                providerTwoResourceTwo.getPublicKey(), Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().size());
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceOneMappingOne),
                getResourceId(providerOneResourceOneMappingTwo)), requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerOneResourceTwoMappingOne),
                getResourceId(providerOneResourceTwoMappingTwo)), requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().map(AllocationDestinationTargetResource::getResourceId)
                .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingOne),
                getResourceId(providerTwoResourceOneMappingTwo)), requestOneSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingThree),
                getResourceId(providerTwoResourceOneMappingFour)), requestOneSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingOne),
                getResourceId(providerTwoResourceTwoMappingTwo)), requestOneSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingThree),
                getResourceId(providerTwoResourceTwoMappingFour)), requestOneSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingOne),
                getResourceId(providerTwoResourceOneMappingTwo)), requestTwoSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceOneMappingThree),
                getResourceId(providerTwoResourceOneMappingFour)), requestTwoSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingOne),
                getResourceId(providerTwoResourceTwoMappingTwo)), requestTwoSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(getResourceId(providerTwoResourceTwoMappingThree),
                getResourceId(providerTwoResourceTwoMappingFour)), requestTwoSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .map(AllocationDestinationTargetResource::getResourceId).collect(Collectors.toSet()));
        Assertions.assertTrue(requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderOneResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceOne.getPublicKey(), Set.of()))
                .getTargetResources().stream().noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoProviderTwoResources
                .get(new DeliverableResource(providerOneResourceTwo.getPublicKey(), Set.of()))
                .getTargetResources().stream().noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestOneSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceOneResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceTwoResources
                .get(new DeliverableResource(providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceThreeResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()))).getTargetResources().stream()
                .noneMatch(AllocationDestinationTargetResource::isEligible));
        Assertions.assertTrue(requestTwoSpaceFourResources
                .get(new DeliverableResource(providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()))).getTargetResources().stream()
                .noneMatch(AllocationDestinationTargetResource::isEligible));
    }

    private String getResourceId(ResourcesModelMapping mapping) {
        return mapping.getTarget().orElseThrow().getExternalResourceId().toString();
    }

    private void setReadyRequestTwo(List<BigOrder> bigOrders,
                                    Service providerOne,
                                    Service providerTwo,
                                    Service providerThree,
                                    Segment providerTwoSegmentationOneSegmentOne,
                                    Segment providerTwoSegmentationOneSegmentTwo,
                                    Segment providerTwoSegmentationTwoSegmentOne,
                                    Segment providerTwoSegmentationTwoSegmentTwo,
                                    Resource providerOneResourceOne,
                                    Resource providerOneResourceTwo,
                                    Resource providerTwoResourceOne,
                                    Resource providerTwoResourceTwo,
                                    Resource providerThreeResourceOne,
                                    Resource providerThreeResourceTwo,
                                    DiQuotaChangeRequest confirmedRequestTwo) {
        Assertions.assertEquals(DiSetAmountResult.SUCCESS, setQuotaRequestReady(confirmedRequestTwo,
                new SetResourceAmountBody.ChangeBody(providerOne.getKey(), bigOrders.get(0).getId(),
                        providerOneResourceOne.getPublicKey(), Set.of(), DiAmount.of(420, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerOne.getKey(), bigOrders.get(1).getId(),
                        providerOneResourceOne.getPublicKey(), Set.of(), DiAmount.of(690, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerOne.getKey(), bigOrders.get(0).getId(),
                        providerOneResourceTwo.getPublicKey(), Set.of(), DiAmount.of(430, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerOne.getKey(), bigOrders.get(1).getId(),
                        providerOneResourceTwo.getPublicKey(), Set.of(), DiAmount.of(700, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(0).getId(),
                        providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(440, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(1).getId(),
                        providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(710, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(0).getId(),
                        providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(450, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(1).getId(),
                        providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(720, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(0).getId(),
                        providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(460, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(1).getId(),
                        providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(730, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(0).getId(),
                        providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(470, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(1).getId(),
                        providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(740, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerThree.getKey(), bigOrders.get(0).getId(),
                        providerThreeResourceOne.getPublicKey(), Set.of(), DiAmount.of(480, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerThree.getKey(), bigOrders.get(1).getId(),
                        providerThreeResourceOne.getPublicKey(), Set.of(), DiAmount.of(750, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerThree.getKey(), bigOrders.get(0).getId(),
                        providerThreeResourceTwo.getPublicKey(), Set.of(), DiAmount.of(490, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerThree.getKey(), bigOrders.get(1).getId(),
                        providerThreeResourceTwo.getPublicKey(), Set.of(), DiAmount.of(760, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES))));
    }

    private void setReadyRequestOne(List<BigOrder> bigOrders,
                                    Service providerOne,
                                    Service providerTwo,
                                    Service providerThree,
                                    Segment providerTwoSegmentationOneSegmentOne,
                                    Segment providerTwoSegmentationOneSegmentTwo,
                                    Segment providerTwoSegmentationTwoSegmentOne,
                                    Segment providerTwoSegmentationTwoSegmentTwo,
                                    Resource providerOneResourceOne,
                                    Resource providerOneResourceTwo,
                                    Resource providerTwoResourceOne,
                                    Resource providerTwoResourceTwo,
                                    Resource providerThreeResourceOne,
                                    Resource providerThreeResourceTwo,
                                    DiQuotaChangeRequest confirmedRequestOne) {
        Assertions.assertEquals(DiSetAmountResult.SUCCESS, setQuotaRequestReady(confirmedRequestOne,
                new SetResourceAmountBody.ChangeBody(providerOne.getKey(), bigOrders.get(0).getId(),
                        providerOneResourceOne.getPublicKey(), Set.of(), DiAmount.of(42, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerOne.getKey(), bigOrders.get(1).getId(),
                        providerOneResourceOne.getPublicKey(), Set.of(), DiAmount.of(69, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerOne.getKey(), bigOrders.get(0).getId(),
                        providerOneResourceTwo.getPublicKey(), Set.of(), DiAmount.of(43, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerOne.getKey(), bigOrders.get(1).getId(),
                        providerOneResourceTwo.getPublicKey(), Set.of(), DiAmount.of(70, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(0).getId(),
                        providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(44, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(1).getId(),
                        providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(71, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(0).getId(),
                        providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(45, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(1).getId(),
                        providerTwoResourceOne.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(72, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(0).getId(),
                        providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(46, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(1).getId(),
                        providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(73, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(0).getId(),
                        providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(47, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerTwo.getKey(), bigOrders.get(1).getId(),
                        providerTwoResourceTwo.getPublicKey(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(74, DiUnit.CORES), DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerThree.getKey(), bigOrders.get(0).getId(),
                        providerThreeResourceOne.getPublicKey(), Set.of(), DiAmount.of(48, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerThree.getKey(), bigOrders.get(1).getId(),
                        providerThreeResourceOne.getPublicKey(), Set.of(), DiAmount.of(75, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerThree.getKey(), bigOrders.get(0).getId(),
                        providerThreeResourceTwo.getPublicKey(), Set.of(), DiAmount.of(49, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES)),
                new SetResourceAmountBody.ChangeBody(providerThree.getKey(), bigOrders.get(1).getId(),
                        providerThreeResourceTwo.getPublicKey(), Set.of(), DiAmount.of(76, DiUnit.CORES),
                        DiAmount.of(0, DiUnit.CORES))));
    }

    private AllocationDestinationSelectionResponse findDestinationSelection(
            Map<Long, Set<String>> providerFiltersByRequestId) {
        List<AllocationDestinationSelectionQuotaRequest> requests = new ArrayList<>();
        providerFiltersByRequestId.forEach((requestId, providerKeys) -> {
            requests.add(new AllocationDestinationSelectionQuotaRequest(requestId, new ArrayList<>(providerKeys)));
        });
        AllocationDestinationSelectionRequest body = new AllocationDestinationSelectionRequest(requests);
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/find-destination-selection")
                .invoke(HttpMethod.POST, body);
        return response.readEntity(AllocationDestinationSelectionResponse.class);
    }

    private DiSetAmountResult setQuotaRequestReady(DiQuotaChangeRequest request,
                                                   SetResourceAmountBody.ChangeBody... changes) {
        SetResourceAmountBody body = new SetResourceAmountBody(ImmutableList.of(new SetResourceAmountBody
                .Item(request.getId(), null, List.of(changes), "Test")));
        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/resource-preorder/quotaState")
                .invoke(HttpMethod.PATCH, body);
        return response.readEntity(DiSetAmountResult.class);
    }

    private DiQuotaChangeRequest getQuotaRequest(long requestId) {
        return dispenser().quotaChangeRequests().byId(requestId).get().perform();
    }

    private DiQuotaChangeRequest confirmRequest(DiQuotaChangeRequest request) {
        DiQuotaChangeRequest answered = dispenser()
                .quotaChangeRequests()
                .byId(request.getId())
                .update(new BodyUpdate.BodyUpdateBuilder()
                        .chartLinksAbsenceExplanation("test")
                        .requestGoalAnswers(QuotaChangeRequestValidationTest.GROWTH_ANSWER)
                        .build())
                .performBy(AMOSOV_F);
        DiQuotaChangeRequest readyForReview = dispenser()
                .quotaChangeRequests()
                .byId(answered.getId())
                .setStatus(DiQuotaChangeRequest.Status.READY_FOR_REVIEW)
                .performBy(AMOSOV_F);
        DiQuotaChangeRequest approved = dispenser()
                .quotaChangeRequests()
                .byId(readyForReview.getId())
                .setStatus(DiQuotaChangeRequest.Status.APPROVED)
                .performBy(AMOSOV_F);
        return dispenser()
                .quotaChangeRequests()
                .byId(approved.getId())
                .setStatus(DiQuotaChangeRequest.Status.CONFIRMED)
                .performBy(AMOSOV_F);
    }

    private DiQuotaChangeRequest prepareRequestTwo(List<BigOrder> bigOrders,
                                                   Service providerOne,
                                                   Service providerTwo,
                                                   Service providerThree,
                                                   Segment providerTwoSegmentationOneSegmentOne,
                                                   Segment providerTwoSegmentationOneSegmentTwo,
                                                   Segment providerTwoSegmentationTwoSegmentOne,
                                                   Segment providerTwoSegmentationTwoSegmentTwo,
                                                   Resource providerOneResourceOne,
                                                   Resource providerOneResourceTwo,
                                                   Resource providerTwoResourceOne,
                                                   Resource providerTwoResourceTwo,
                                                   Resource providerThreeResourceOne,
                                                   Resource providerThreeResourceTwo,
                                                   Project projectOne,
                                                   Campaign campaign) {
        return prepareQuotaRequest(projectOne.getPublicKey(), campaign.getId(),
                new ChangeBody(providerOne.getKey(), providerOneResourceOne.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(), DiAmount.of(420, DiUnit.CORES)),
                new ChangeBody(providerOne.getKey(), providerOneResourceOne.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(), DiAmount.of(690, DiUnit.CORES)),
                new ChangeBody(providerOne.getKey(), providerOneResourceTwo.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(), DiAmount.of(430, DiUnit.CORES)),
                new ChangeBody(providerOne.getKey(), providerOneResourceTwo.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(), DiAmount.of(700, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceOne.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(440, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceOne.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(710, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceOne.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(450, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceOne.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(720, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceTwo.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(460, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceTwo.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(730, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceTwo.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(470, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceTwo.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(740, DiUnit.CORES)),
                new ChangeBody(providerThree.getKey(), providerThreeResourceOne.getPublicKey(),
                        bigOrders.get(0).getId(), Set.of(), DiAmount.of(480, DiUnit.CORES)),
                new ChangeBody(providerThree.getKey(), providerThreeResourceOne.getPublicKey(),
                        bigOrders.get(1).getId(), Set.of(), DiAmount.of(750, DiUnit.CORES)),
                new ChangeBody(providerThree.getKey(), providerThreeResourceTwo.getPublicKey(),
                        bigOrders.get(0).getId(), Set.of(), DiAmount.of(490, DiUnit.CORES)),
                new ChangeBody(providerThree.getKey(), providerThreeResourceTwo.getPublicKey(),
                        bigOrders.get(1).getId(), Set.of(), DiAmount.of(760, DiUnit.CORES)));
    }

    private DiQuotaChangeRequest prepareRequestOne(List<BigOrder> bigOrders,
                                                   Service providerOne,
                                                   Service providerTwo,
                                                   Service providerThree,
                                                   Segment providerTwoSegmentationOneSegmentOne,
                                                   Segment providerTwoSegmentationOneSegmentTwo,
                                                   Segment providerTwoSegmentationTwoSegmentOne,
                                                   Segment providerTwoSegmentationTwoSegmentTwo,
                                                   Resource providerOneResourceOne,
                                                   Resource providerOneResourceTwo,
                                                   Resource providerTwoResourceOne,
                                                   Resource providerTwoResourceTwo,
                                                   Resource providerThreeResourceOne,
                                                   Resource providerThreeResourceTwo,
                                                   Project projectOne,
                                                   Campaign campaign) {
        return prepareQuotaRequest(projectOne.getPublicKey(), campaign.getId(),
                new ChangeBody(providerOne.getKey(), providerOneResourceOne.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(), DiAmount.of(42, DiUnit.CORES)),
                new ChangeBody(providerOne.getKey(), providerOneResourceOne.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(), DiAmount.of(69, DiUnit.CORES)),
                new ChangeBody(providerOne.getKey(), providerOneResourceTwo.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(), DiAmount.of(43, DiUnit.CORES)),
                new ChangeBody(providerOne.getKey(), providerOneResourceTwo.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(), DiAmount.of(70, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceOne.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(44, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceOne.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(71, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceOne.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(45, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceOne.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(72, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceTwo.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(46, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceTwo.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(providerTwoSegmentationOneSegmentOne.getPublicKey(),
                                providerTwoSegmentationTwoSegmentOne.getPublicKey()),
                        DiAmount.of(73, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceTwo.getPublicKey(), bigOrders.get(0).getId(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(47, DiUnit.CORES)),
                new ChangeBody(providerTwo.getKey(), providerTwoResourceTwo.getPublicKey(), bigOrders.get(1).getId(),
                        Set.of(providerTwoSegmentationOneSegmentTwo.getPublicKey(),
                                providerTwoSegmentationTwoSegmentTwo.getPublicKey()),
                        DiAmount.of(74, DiUnit.CORES)),
                new ChangeBody(providerThree.getKey(), providerThreeResourceOne.getPublicKey(),
                        bigOrders.get(0).getId(), Set.of(), DiAmount.of(48, DiUnit.CORES)),
                new ChangeBody(providerThree.getKey(), providerThreeResourceOne.getPublicKey(),
                        bigOrders.get(1).getId(), Set.of(), DiAmount.of(75, DiUnit.CORES)),
                new ChangeBody(providerThree.getKey(), providerThreeResourceTwo.getPublicKey(),
                        bigOrders.get(0).getId(), Set.of(), DiAmount.of(49, DiUnit.CORES)),
                new ChangeBody(providerThree.getKey(), providerThreeResourceTwo.getPublicKey(),
                        bigOrders.get(1).getId(), Set.of(), DiAmount.of(76, DiUnit.CORES)));
    }

    private ResourcesModelMapping prepareResourcesModelMapping(Campaign campaign, Resource resource, long numerator,
                                                               long denominator, String key, String unitKey,
                                                               UUID externalResourceId, Segment... segments) {
        ResourcesModelMapping result = resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping
                .builder()
                .resource(resource)
                .campaignId(campaign.getId())
                .addSegments(List.of(segments))
                .target(ResourceModelMappingTarget.builder()
                        .numerator(numerator)
                        .denominator(denominator)
                        .key(key)
                        .externalResourceBaseUnitKey(unitKey)
                        .externalResourceId(externalResourceId)
                        .build()));
        updateHierarchy();
        return result;
    }

    private Project prepareProject(String key, int abcServiceId) {
        Project parent = projectDao.read(YANDEX);
        Project result = projectDao.create(Project
                .withKey(key)
                .name(key)
                .parent(parent)
                .abcServiceId(abcServiceId)
                .build());
        updateHierarchy();
        return result;
    }

    private DiQuotaChangeRequest prepareQuotaRequest(String abcServiceKey, Long campaignId, ChangeBody... changes) {
        Body.BodyBuilder builder = new Body.BodyBuilder()
                .summary("test")
                .description("test")
                .calculations("test")
                .projectKey(abcServiceKey)
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH);
        for (ChangeBody change : changes) {
            builder.changes(change);
        }
        DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(builder.build(), campaignId)
                .performBy(AMOSOV_F);
        return quotaRequests.getFirst();
    }

    private Resource prepareResource(Service provider, String key, DiResourceType type, Segmentation... segmentations) {
        Resource resource = resourceDao.create(new Resource.Builder(key, provider)
                .name(key)
                .type(type)
                .mode(DiQuotingMode.DEFAULT)
                .build());
        updateHierarchy();
        for (Segmentation segmentation : segmentations) {
            resourceSegmentationDao.create(new ResourceSegmentation.Builder(resource, segmentation).build());
        }
        updateHierarchy();
        return resource;
    }

    private Segment prepareSegment(Segmentation segmentation, String key) {
        Segment segment = segmentDao.create(new Segment.Builder(key, segmentation)
                .name(key)
                .description(key)
                .priority((short) 0)
                .build());
        updateHierarchy();
        return segment;
    }

    private Segmentation prepareSegmentation(String key) {
        Segmentation result = segmentationDao.create(new Segmentation.Builder(key)
                .name(key)
                .description(key)
                .build());
        updateHierarchy();
        return result;
    }

    private Service prepareProvider(String key, int abcServiceId, boolean allocationEnabled,
                                    String mapperBeanName) {
        Service result = serviceDao.create(Service
                .withKey(key)
                .withName(key)
                .withAbcServiceId(abcServiceId)
                .withSettings(Service.Settings.builder()
                        .manualQuotaAllocation(allocationEnabled)
                        .resourcesMappingBeanName(mapperBeanName)
                        .build())
                .build());
        updateHierarchy();
        return result;
    }

    private BotCampaignGroup prepareCampaignGroup(Campaign campaign, List<BigOrder> bigOrders) {
        BotCampaignGroup.Builder builder = BotCampaignGroup.builder()
                .setKey("test_campaign_group")
                .setName("Test Campaign Group")
                .setActive(true)
                .setBotPreOrderIssueKey("DISPENSERREQ-1")
                .addCampaign(campaignDao.readForBot(Set.of(campaign.getId())).get(campaign.getId()));
        bigOrders.forEach(builder::addBigOrder);
        BotCampaignGroup result = botCampaignGroupDao.create(builder.build());
        updateHierarchy();
        return result;
    }

    private Campaign prepareCampaign(List<BigOrder> bigOrders) {
        Campaign result = campaignDao.create(Campaign.builder()
                .setKey("test")
                .setName("Test")
                .setStatus(Campaign.Status.ACTIVE)
                .setStartDate(LocalDate.of(2021, Month.AUGUST, 1))
                .setBigOrders(bigOrders.stream().map(o -> new Campaign.BigOrder(o.getId(), o.getDate()))
                        .collect(Collectors.toList()))
                .setRequestCreationDisabled(false)
                .setRequestModificationDisabledForNonManagers(false)
                .setAllowedRequestModificationWhenClosed(false)
                .setAllowedModificationOnMissingAdditionalFields(false)
                .setForcedAllowingRequestStatusChange(false)
                .setAllowedRequestCreationForProviderAdmin(false)
                .setSingleProviderRequestModeEnabled(false)
                .setAllowedRequestCreationForCapacityPlanner(false)
                .setType(Campaign.Type.AGGREGATED)
                .setRequestModificationDisabled(false)
                .build());
        updateHierarchy();
        return result;
    }

    private List<BigOrder> prepareBigOrders() {
        List<BigOrder> result = List.of(
                bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 6, 1))),
                bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 9, 1))),
                bigOrderManager.create(BigOrder.builder(LocalDate.of(2022, 12, 1))));
        updateHierarchy();
        return result;
    }

    private static final class DeliverableResource {

        private final String resourceKey;
        private final Set<String> segmentKeys;

        private DeliverableResource(String resourceKey, Set<String> segmentKeys) {
            this.resourceKey = resourceKey;
            this.segmentKeys = segmentKeys;
        }

        private DeliverableResource(AllocationDestinationSourceResource value) {
            this.resourceKey = value.getResourceKey();
            this.segmentKeys = value.getSegments().stream()
                    .map(AllocationDestinationSourceResourceSegment::getSegmentKey).collect(Collectors.toSet());
        }

        public String getResourceKey() {
            return resourceKey;
        }

        public Set<String> getSegmentKeys() {
            return segmentKeys;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DeliverableResource that = (DeliverableResource) o;
            return Objects.equals(resourceKey, that.resourceKey) &&
                    Objects.equals(segmentKeys, that.segmentKeys);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceKey, segmentKeys);
        }

        @Override
        public String toString() {
            return "DeliverableResource{" +
                    "resourceKey='" + resourceKey + '\'' +
                    ", segmentKeys=" + segmentKeys +
                    '}';
        }

    }

}
