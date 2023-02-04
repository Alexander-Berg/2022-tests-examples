package ru.yandex.qe.dispenser.ws.dao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.delivery.DeliveryDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.resources_model.DeliveryResult;
import ru.yandex.qe.dispenser.domain.resources_model.ExternalResource;
import ru.yandex.qe.dispenser.domain.resources_model.InternalResource;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDelivery;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDeliveryResolveStatus;
import ru.yandex.qe.dispenser.ws.logic.BaseResourcePreorderTest;

public class DeliveryDaoTest extends BaseResourcePreorderTest {
    @Autowired
    DeliveryDao deliveryDao;
    @Autowired
    QuotaChangeRequestDao quotaChangeRequestDao;
    @Autowired
    PersonDao personDao;

    private Service nirvana;
    private Person aqru;
    private Project cloud;
    private QuotaChangeRequest request;

    private QuotaRequestDelivery.Builder getQuotaRequestDeliveryBuilder() {
        Integer abcServiceId = cloud.getAbcServiceId();
        assert abcServiceId != null;
        return QuotaRequestDelivery.builder()
                .providerId(nirvana.getId())
                .quotaRequestId(request.getId())
                .id(UUID.randomUUID())
                .authorId(aqru.getId())
                .authorUid(aqru.getUid())
                .abcServiceId(abcServiceId)
                .campaignId(campaign.getId())
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .addExternalResource(ExternalResource.builder()
                        .resourceId(UUID.randomUUID())
                        .amount(1L)
                        .bigOrderId(1L)
                        .unitKey("SSD")
                        .build())
                .addInternalResource(InternalResource.builder()
                        .resourceId(1L)
                        .addSegmentId(1L)
                        .amount(1L)
                        .bigOrderId(1L)
                        .build())
                .resolved(false)
                .resolveStatus(QuotaRequestDeliveryResolveStatus.IN_ALLOCATING_PROCESS);
    }

    @SuppressWarnings("deprecation")
    @BeforeEach
    public void setUp() {
        super.setUp();
        deliveryDao.clear();
        this.nirvana = hierarchy.get().getServiceReader().read(NIRVANA);
        this.aqru = personDao.readPersonByLogin("aqru");
        Project yandex = projectDao.read("yandex");
        this.cloud = this.projectDao.create(Project.withKey("cloud")
                .name("Cloud")
                .description("Cloud")
                .abcServiceId(777)
                .parent(yandex)
                .build());

        this.request = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                .status(QuotaChangeRequest.Status.NEW)
                .updated(0)
                .created(0)
                .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                .summary("WOW")
                .description("need")
                .calculations("2*2")
                .project(cloud)
                .author(aqru)
                .chartLinks(Collections.emptyList())
                .campaign(QuotaChangeRequest.Campaign.from(campaign))
                .campaignType(campaign.getType())
                .cost(0)
                .requestOwningCost(0L)
                .changes(List.of())
                .build());
    }

    @Test
    public void deliveryCanBeCreatedTest() {

        QuotaRequestDelivery quotaRequestDelivery = getQuotaRequestDeliveryBuilder().build();
        deliveryDao.create(quotaRequestDelivery);
        List<QuotaRequestDelivery> unresolvedByQuotaRequestIdAndProviderId =
                deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(quotaRequestDelivery.getQuotaRequestId(),
                        quotaRequestDelivery.getProviderId());

        Assertions.assertEquals(1, unresolvedByQuotaRequestIdAndProviderId.size());
        Assertions.assertEquals(quotaRequestDelivery, unresolvedByQuotaRequestIdAndProviderId.get(0));
    }

    @Test
    public void deliveryCanBeCreatedWithEmptyJsonFieldsTest() {

        QuotaRequestDelivery quotaRequestDelivery = getQuotaRequestDeliveryBuilder()
                .setDeliveryResults(List.of())
                .setInternalResources(List.of())
                .setExternalResources(List.of())
                .build();

        deliveryDao.create(quotaRequestDelivery);
        List<QuotaRequestDelivery> unresolvedByQuotaRequestIdAndProviderId =
                deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(quotaRequestDelivery.getQuotaRequestId(),
                        quotaRequestDelivery.getProviderId());

        Assertions.assertEquals(1, unresolvedByQuotaRequestIdAndProviderId.size());
        Assertions.assertEquals(quotaRequestDelivery, unresolvedByQuotaRequestIdAndProviderId.get(0));
    }

    @Test
    public void deliveryCanBeUpdatedTest() {
        QuotaRequestDelivery quotaRequestDelivery = getQuotaRequestDeliveryBuilder().build();
        deliveryDao.create(quotaRequestDelivery);
        List<QuotaRequestDelivery> unresolvedByQuotaRequestIdAndProviderId =
                deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(quotaRequestDelivery.getQuotaRequestId(),
                        quotaRequestDelivery.getProviderId());

        Assertions.assertEquals(1, unresolvedByQuotaRequestIdAndProviderId.size());
        Assertions.assertEquals(quotaRequestDelivery, unresolvedByQuotaRequestIdAndProviderId.get(0));

        QuotaRequestDelivery build = quotaRequestDelivery.toBuilder()
                .resolvedAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .addDeliveryResult(DeliveryResult.builder()
                        .folderId(UUID.randomUUID())
                        .bigOrderId(1L)
                        .folderOperationId(UUID.randomUUID())
                        .resourceId(UUID.randomUUID())
                        .timestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                        .build())
                .build();
        deliveryDao.update(build);

        List<QuotaRequestDelivery> unresolvedByQuotaRequestIdAndProviderId1 =
                deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(quotaRequestDelivery.getQuotaRequestId(),
                quotaRequestDelivery.getProviderId());

        Assertions.assertEquals(1, unresolvedByQuotaRequestIdAndProviderId1.size());
        Assertions.assertEquals(build, unresolvedByQuotaRequestIdAndProviderId1.get(0));
    }

    @Test
    public void deliveryCanBeSelectedTest() {
        Service yp = hierarchy.get().getServiceReader().read(YP);
        QuotaRequestDelivery quotaRequestDelivery = getQuotaRequestDeliveryBuilder().build();
        QuotaRequestDelivery quotaRequestDelivery1 = quotaRequestDelivery.toBuilder()
                .id(UUID.randomUUID())
                .build();
        QuotaRequestDelivery quotaRequestDelivery2 = quotaRequestDelivery.toBuilder()
                .id(UUID.randomUUID())
                .providerId(yp.getId())
                .build();

        deliveryDao.create(quotaRequestDelivery);
        deliveryDao.create(quotaRequestDelivery1);
        deliveryDao.create(quotaRequestDelivery2);

        List<QuotaRequestDelivery> unresolvedByQuotaRequestIdAndProviderId =
                deliveryDao.getUnresolvedByQuotaRequestIdAndProviderId(quotaRequestDelivery.getQuotaRequestId(),
                        quotaRequestDelivery.getProviderId());

        Assertions.assertEquals(2, unresolvedByQuotaRequestIdAndProviderId.size());
        Assertions.assertEquals(Set.of(quotaRequestDelivery, quotaRequestDelivery1),
                new HashSet<>(unresolvedByQuotaRequestIdAndProviderId));
    }
}
