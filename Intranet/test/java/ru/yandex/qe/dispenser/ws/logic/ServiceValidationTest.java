package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiQuotaMaxDeltaUpdate;
import ru.yandex.qe.dispenser.api.v1.DiQuotaMaxUpdate;
import ru.yandex.qe.dispenser.api.v1.DiResource;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiService;
import ru.yandex.qe.dispenser.api.v1.DiSignedAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiActualQuotaUpdate;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;
import ru.yandex.qe.dispenser.ws.ServiceService;
import ru.yandex.qe.dispenser.ws.reqbody.ServiceBody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ServiceValidationTest extends BusinessLogicTestBase {

    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void serviceAdminsCantRegisterService() {
        Assertions.assertThrows(ForbiddenException.class, () -> {
            dispenser().service("myservice").create().withName("My Service").performBy(SANCHO);
        });
    }

    @Test
    public void rootResponsiblesCantRegisterService() {
        Assertions.assertThrows(ForbiddenException.class, () -> {
            dispenser().service("myservice").create().withName("My Service").performBy(WHISTLER);
        });
    }

    @Test
    public void sanchoIsAdminOfNirvana() {
        final DiService service = dispenser().service(NIRVANA).get().perform();
        assertTrue(service.getAdmins().contains(SANCHO.getLogin()));
    }

    @Test
    public void serviceAdminCantAttachAdminsIfLoginIsNotExists() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispenser()
                    .service(NIRVANA)
                    .admins()
                    .attach("new-admin")
                    .performBy(SANCHO);
        });
    }

    @Test
    public void serviceAdminCantDetachAdminsIfLoginIsNotExists() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispenser()
                    .service(NIRVANA)
                    .admins()
                    .detach("new-admin")
                    .performBy(SANCHO);
        });
    }

    @Test
    public void cantAttachNotExistingLogin() {
        updateHierarchy();
        assertThrowsWithMessage(() -> {
            dispenser()
                    .service(NIRVANA)
                    .admins()
                    .attach("alexandro_del_pirs")
                    .performBy(SANCHO);
        }, "No person with login alexandro_del_pirs");
    }

    @Test
    public void serviceAdminCanAttachAndDetachAdmins() {
        final DiService attachAdminResult = dispenser()
                .service(NIRVANA)
                .admins()
                .attach(AMOSOV_F.getLogin())
                .performBy(SANCHO);
        assertTrue(attachAdminResult.getAdmins().contains(AMOSOV_F.getLogin()));
        updateHierarchy();
        assertTrue(dispenser().service(NIRVANA).get().perform().getAdmins().contains(AMOSOV_F.getLogin()));

        final DiService detachAdminResult = dispenser()
                .service(NIRVANA)
                .admins()
                .detach(AMOSOV_F.getLogin())
                .performBy(SANCHO);
        assertFalse(detachAdminResult.getAdmins().contains(AMOSOV_F.getLogin()));
        updateHierarchy();
        assertFalse(dispenser().service(NIRVANA).get().perform().getAdmins().contains(AMOSOV_F.getLogin()));
    }

    @Test
    public void serviceAdminCantAttachTrusteesIfLoginIsNotExists() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispenser()
                    .service(NIRVANA)
                    .trustees()
                    .attach("new-trustee")
                    .performBy(SANCHO);
        });
    }

    @Test
    public void serviceAdminCantDetachTrusteesIfLoginIsNotExists() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispenser()
                    .service(NIRVANA)
                    .trustees()
                    .detach("new-trustee")
                    .performBy(SANCHO);
        });
    }

    @Test
    public void serviceAdminCanAttachAndDetachTrustees() {
        final DiService attachTrusteeResult = dispenser()
                .service(NIRVANA)
                .trustees()
                .attach(BINARY_CAT.getLogin())
                .performBy(SANCHO);
        assertTrue(attachTrusteeResult.getTrustees().contains(BINARY_CAT.getLogin()));
        updateHierarchy();
        assertTrue(dispenser().service(NIRVANA).get().perform().getTrustees().contains(BINARY_CAT.getLogin()));

        final DiService detachTrusteeResult = dispenser()
                .service(NIRVANA)
                .trustees()
                .detach(BINARY_CAT.getLogin())
                .performBy(SANCHO);
        assertFalse(detachTrusteeResult.getTrustees().contains(BINARY_CAT.getLogin()));
        updateHierarchy();
        assertFalse(dispenser().service(NIRVANA).get().perform().getTrustees().contains(BINARY_CAT.getLogin()));
    }

    @Test
    public void serviceCantBeCreatedWithoutAbcServiceId() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispenser().service("new-service").create().withName("New Service").performBy(AMOSOV_F);
        });
    }

    @Test
    public void serviceCantBeCreatedWithoutAdmins() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            final ServiceBody serviceBody = new ServiceBody();
            serviceBody.setName("New Updated Service");
            serviceBody.setAbcServiceId(50);

            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/services/new-service")
                    .put(serviceBody, DiService.class);
        });
    }

    @Test
    public void serviceCantBeCreatedWithoutTrustees() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            final ServiceBody serviceBody = new ServiceBody();
            serviceBody.setName("New Updated Service");
            serviceBody.setAbcServiceId(50);
            serviceBody.setAdmins(ArrayUtils.EMPTY_STRING_ARRAY);

            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/services/new-service")
                    .put(serviceBody, DiService.class);
        });
    }

    private DiService createService(final String key, final String... responsibles) {
        final DiService createdService = dispenser().service(key).create()
                .withName("New Service")
                .withAbcServiceId(AcceptanceTestBase.TEST_ABC_SERVICE_ID)
                .withAdmins(responsibles)
                .withTrustees(responsibles)
                .performBy(AMOSOV_F);
        updateHierarchy();
        return createdService;
    }

    private DiService createServiceWithPriority(final String key, final Integer priority, final String... responsibles) {
        final DiService createdServiceWithPriority = dispenser().service(key).create()
                .withName("New Service")
                .withAbcServiceId(AcceptanceTestBase.TEST_ABC_SERVICE_ID)
                .withAdmins(responsibles)
                .withTrustees(responsibles)
                .withPriority(priority)
                .performBy(AMOSOV_F);
        updateHierarchy();
        return createdServiceWithPriority;
    }

    @Test
    public void serviceCanBeCreatedWithoutPriority() {
        createService("bar", TERRY.getLogin());
        final DiService createdService = dispenser().service("bar").get().perform();
        assertNull(createdService.getPriority());
    }

    @Test
    public void serviceCanBeCreatedWithPriority() {
        createServiceWithPriority("new-service", 1, TERRY.getLogin());
        final DiService createdServiceWithPriority = dispenser().service("new-service").get().perform();
        final Integer createdPriority = createdServiceWithPriority.getPriority();
        assertNotNull(createdPriority);
        assertEquals(1L, (long) createdPriority);
    }

    @Test
    public void servicePriorityCanBeUpdated() {
        final DiService createdService = createServiceWithPriority("bar", 2);

        final String updatedServiceName = "New Service";
        final Integer updatedAbcServiceID = 50;
        final Integer updatedPriority = 3;

        dispenser().service("bar").update()
                .withName(updatedServiceName)
                .withAbcServiceId(updatedAbcServiceID)
                .withTrustees(createdService.getTrustees())
                .withAdmins(createdService.getAdmins())
                .withPriority(updatedPriority)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiService updatedService = dispenser().service("bar").get().perform();
        assertEquals(updatedPriority, updatedService.getPriority());
    }

    @Test
    public void servicePriorityCanBeUpdatedIfWasNull() {
        final DiService createdService = createService("bar", TERRY.getLogin());

        final String updatedServiceName = "New Service";
        final Integer updatedAbcServiceID = 50;
        final Integer updatedPriority = -3;

        dispenser().service("bar").update()
                .withName(updatedServiceName)
                .withAbcServiceId(updatedAbcServiceID)
                .withTrustees(createdService.getTrustees())
                .withAdmins(createdService.getAdmins())
                .withPriority(updatedPriority)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiService updatedService = dispenser().service("bar").get().perform();
        assertEquals(updatedPriority, updatedService.getPriority());
    }

    @Test
    public void trusteesCanBeCreatedInService() {
        createService("new-service", TERRY.getLogin());
        final DiService createdService = dispenser().service("new-service").get().perform();
        assertTrue(createdService.getTrustees().contains(TERRY.getLogin()));
    }

    @Test
    public void adminsCanBeCreatedInService() {
        createService("new-service", TERRY.getLogin());
        final DiService createdService = dispenser().service("new-service").get().perform();
        assertTrue(createdService.getAdmins().contains(TERRY.getLogin()));
    }

    @Test
    public void abcServiceIdCantBeRemovedFromService() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            createService("new-service");

            dispenser().service("new-service").update()
                    .withName("New Service")
                    .withAbcServiceId(null)
                    .performBy(AMOSOV_F);
        });
    }

    @Test
    public void serviceNameAndAbcServiceIdCanBeUpdated() {
        final DiService createdService = createService("new-service");
        final String updatedServiceName = "New Updated Service";
        final Integer updatedAbcServiceID = 50;

        dispenser().service("new-service").update()
                .withName(updatedServiceName)
                .withAbcServiceId(updatedAbcServiceID)
                .withTrustees(createdService.getTrustees())
                .withAdmins(createdService.getAdmins())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiService updatedService = dispenser().service("new-service").get().perform();

        assertEquals(updatedServiceName, updatedService.getName());
        assertEquals(updatedAbcServiceID, updatedService.getAbcServiceId());
    }

    @Test
    public void serviceAdminsCanBeUpdated() {
        final DiService createdService = createService("new-service", TERRY.getLogin());
        final List<String> updatedAdmins = Collections.singletonList(LOTREK.getLogin());

        dispenser().service("new-service").update()
                .withName(createdService.getName())
                .withAbcServiceId(createdService.getAbcServiceId())
                .withAdmins(updatedAdmins)
                .withTrustees(createdService.getTrustees())
                .performBy(AMOSOV_F);

        updateHierarchy();
        final DiService updatedService = dispenser().service("new-service").get().perform();
        assertEquals(updatedAdmins, updatedService.getAdmins());
    }

    @Test
    public void serviceAdminsCantBeUpdatedIfLoginNotExists() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            final DiService createdService = createService("new-service", TERRY.getLogin());
            final List<String> updatedAdmins = Collections.singletonList("new-admin");

            dispenser().service("new-service").update()
                    .withName(createdService.getName())
                    .withAbcServiceId(createdService.getAbcServiceId())
                    .withAdmins(updatedAdmins)
                    .withTrustees(createdService.getTrustees())
                    .performBy(AMOSOV_F);
        });

    }

    @Test
    public void serviceAdminsCanBeUpdatedToEmpty() {
        final DiService createdService = createService("new-service", TERRY.getLogin());

        final ServiceBody serviceBody = new ServiceBody();
        serviceBody.setName(createdService.getName());
        serviceBody.setAbcServiceId(createdService.getAbcServiceId());
        serviceBody.setAdmins(ArrayUtils.EMPTY_STRING_ARRAY);
        serviceBody.setTrustees(createdService.getTrustees().toArray(ArrayUtils.EMPTY_STRING_ARRAY));

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/services/new-service")
                .post(serviceBody, DiService.class);
        updateHierarchy();
        final DiService updatedService = dispenser().service("new-service").get().perform();
        assertEquals(0, updatedService.getAdmins().size());
    }

    @Test
    public void serviceCantBeUpdatedWhenAdminsNull() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            final DiService createdService = createService("new-service", TERRY.getLogin());

            final ServiceBody serviceBody = new ServiceBody();
            serviceBody.setName(createdService.getName());
            serviceBody.setAbcServiceId(createdService.getAbcServiceId());
            serviceBody.setTrustees(ArrayUtils.EMPTY_STRING_ARRAY);

            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/services/new-service")
                    .post(serviceBody, DiService.class);
        });
    }

    @Test
    public void serviceTrusteesCanBeUpdated() {
        final DiService createdService = createService("new-service", TERRY.getLogin());
        final List<String> updatedTrustees = Collections.singletonList(SLONNN.getLogin());

        dispenser().service("new-service").update()
                .withName(createdService.getName())
                .withAbcServiceId(createdService.getAbcServiceId())
                .withAdmins(createdService.getAdmins())
                .withTrustees(updatedTrustees)
                .performBy(AMOSOV_F);

        updateHierarchy();
        final DiService updatedService = dispenser().service("new-service").get().perform();
        assertEquals(updatedTrustees, updatedService.getTrustees());
    }

    @Test
    public void serviceTrusteesCantBeUpdatedIfLoginNotExists() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            final DiService createdService = createService("new-service", TERRY.getLogin());
            final List<String> updatedTrustees = Collections.singletonList("new-trustee");

            dispenser().service("new-service").update()
                    .withName(createdService.getName())
                    .withAbcServiceId(createdService.getAbcServiceId())
                    .withAdmins(updatedTrustees)
                    .withTrustees(createdService.getTrustees())
                    .performBy(AMOSOV_F);
        });
    }

    @Test
    public void serviceTrusteesCanBeUpdatedToEmpty() {
        final DiService createdService = createService("new-service", TERRY.getLogin());

        final ServiceBody serviceBody = new ServiceBody();
        serviceBody.setName(createdService.getName());
        serviceBody.setAbcServiceId(createdService.getAbcServiceId());
        serviceBody.setAdmins(createdService.getAdmins().toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        serviceBody.setTrustees(ArrayUtils.EMPTY_STRING_ARRAY);

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/services/new-service")
                .post(serviceBody, DiService.class);
        updateHierarchy();
        final DiService updatedService = dispenser().service("new-service").get().perform();
        assertEquals(0, updatedService.getTrustees().size());
    }

    @Test
    public void serviceCantBeUpdatedWhenTrusteesNull() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            final DiService createdService = createService("new-service", TERRY.getLogin());

            final ServiceBody serviceBody = new ServiceBody();
            serviceBody.setName(createdService.getName());
            serviceBody.setAbcServiceId(createdService.getAbcServiceId());
            serviceBody.setAdmins(ArrayUtils.EMPTY_STRING_ARRAY);

            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/services/new-service")
                    .post(serviceBody, DiService.class);
        });
    }

    @Test
    public void rootServiceQuotasShouldBeSettable() {
        dispenser().service(CLUSTER_API)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(CPU)
                        .withMax(DiAmount.of(8, DiUnit.COUNT))
                        .forProject(YANDEX).build())
                .performBy(AMOSOV_F);

    }

    @Test
    public void quotasMaxWithSegmentShouldBeSyncable() {
        final HashSet<String> segmentKeys = Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1);
        dispenser().service(YP)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(SEGMENT_CPU)
                        .withMax(DiAmount.of(30000, DiUnit.PERMILLE_CORES))
                        .forProject(YANDEX)
                        .withSegments(segmentKeys)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final long quotaMax = dispenser().quotas().get()
                .inService(YP)
                .ofProject(YANDEX)
                .forResource(SEGMENT_CPU)
                .withSegments(segmentKeys)
                .perform()
                .getFirst()
                .getMax(DiUnit.PERMILLE_CORES);

        assertEquals(30000, quotaMax);
    }

    @Test
    public void quotasMaxWithSegmentShouldBeRawSyncable() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final HashSet<String> segmentKeys = Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1);
        dispenser().service(YP)
                .syncState()
                .rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_CPU)
                        .withMax(DiAmount.of(30000, DiUnit.PERMILLE_CORES))
                        .forProject(YANDEX)
                        .withSegments(segmentKeys)
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final long quotaMax = dispenser().quotas().get()
                .inService(YP)
                .ofProject(YANDEX)
                .forResource(SEGMENT_CPU)
                .withSegments(segmentKeys)
                .perform()
                .getFirst()
                .getMax(DiUnit.PERMILLE_CORES);

        assertEquals(30000, quotaMax);
    }

    @Test
    public void quotasActualWithSegmentShouldBeSyncable() {
        final HashSet<String> segmentKeys = Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1);
        dispenser().service(YP)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE)
                        .withActual(DiAmount.of(100, DiUnit.BYTE))
                        .forProject(VERTICALI)
                        .withSegments(segmentKeys)
                        .build())
                .perform();

        updateHierarchy();

        final long quotaActual = dispenser().quotas()
                .get()
                .inService(YP)
                .ofProject(VERTICALI)
                .forResource(SEGMENT_STORAGE)
                .withSegments(segmentKeys)
                .perform()
                .getFirst()
                .getActual(DiUnit.BYTE);

        assertEquals(100, quotaActual);
    }

    @Test
    public void quotasActualWithSegmentShouldBeSyncableByDedicatedEndpoint() {
        final HashSet<String> segmentKeys = Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1);
        dispenser().service(YP)
                .syncState()
                .actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE)
                        .actual(DiAmount.of(100, DiUnit.BYTE))
                        .project(VERTICALI)
                        .addSegments(segmentKeys)
                        .build())
                .perform();

        updateHierarchy();

        final long quotaActual = dispenser().quotas()
                .get()
                .inService(YP)
                .ofProject(VERTICALI)
                .forResource(SEGMENT_STORAGE)
                .withSegments(segmentKeys)
                .perform()
                .getFirst()
                .getActual(DiUnit.BYTE);

        assertEquals(100, quotaActual);
    }

    @Test
    public void quotasActualWithSegmentShouldBeRawSyncable() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final HashSet<String> segmentKeys = Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1);
        dispenser().service(YP)
                .syncState()
                .rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE)
                        .withActual(DiAmount.of(100, DiUnit.BYTE))
                        .forProject(VERTICALI)
                        .withSegments(segmentKeys)
                        .build())
                .perform();

        updateHierarchy();

        final long quotaActual = dispenser().quotas()
                .get()
                .inService(YP)
                .ofProject(VERTICALI)
                .forResource(SEGMENT_STORAGE)
                .withSegments(segmentKeys)
                .perform()
                .getFirst()
                .getActual(DiUnit.BYTE);

        assertEquals(100, quotaActual);
    }

    @Test
    public void multipleQuotasActualWithoutSegmentShouldBeSyncable() {
        dispenser().service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .withActual(DiAmount.of(100, DiUnit.PERMILLE))
                        .forProject(VERTICALI)
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_GPU)
                        .withActual(DiAmount.of(10, DiUnit.PERMILLE))
                        .forProject(INFRA)
                        .build())
                .perform();

        updateHierarchy();

        final long actualVerticali = dispenser().quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .ofProject(VERTICALI)
                .perform()
                .getFirst()
                .getActual(DiUnit.PERMILLE);
        assertEquals(100, actualVerticali);

        final long actualInfra = dispenser().quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_GPU)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getActual(DiUnit.PERMILLE);
        assertEquals(10, actualInfra);
    }

    @Test
    public void multipleQuotasActualWithoutSegmentShouldBeSyncableByDedicatedEndpoint() {
        dispenser().service(NIRVANA)
                .syncState()
                .actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU)
                        .actual(DiAmount.of(100, DiUnit.PERMILLE))
                        .project(VERTICALI)
                        .build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_GPU)
                        .actual(DiAmount.of(10, DiUnit.PERMILLE))
                        .project(INFRA)
                        .build())
                .perform();

        updateHierarchy();

        final long actualVerticali = dispenser().quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .ofProject(VERTICALI)
                .perform()
                .getFirst()
                .getActual(DiUnit.PERMILLE);
        assertEquals(100, actualVerticali);

        final long actualInfra = dispenser().quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_GPU)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getActual(DiUnit.PERMILLE);
        assertEquals(10, actualInfra);
    }

    @Test
    public void multipleQuotasActualWithoutSegmentShouldBeRawSyncable() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        dispenser().service(NIRVANA)
                .syncState()
                .rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(YT_CPU)
                        .withActual(DiAmount.of(100, DiUnit.PERMILLE))
                        .forProject(VERTICALI)
                        .build())
                .changeRawQuota(DiQuotaState.forResource(YT_GPU)
                        .withActual(DiAmount.of(10, DiUnit.PERMILLE))
                        .forProject(INFRA)
                        .build())
                .perform();

        updateHierarchy();

        final long actualVerticali = dispenser().quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .ofProject(VERTICALI)
                .perform()
                .getFirst()
                .getActual(DiUnit.PERMILLE);
        assertEquals(100, actualVerticali);

        final long actualInfra = dispenser().quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_GPU)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getActual(DiUnit.PERMILLE);
        assertEquals(10, actualInfra);
    }

    @Test
    public void quotasWithEqualResourceKeyInDifferentServicesShouldBeSyncable() {
        final String nonUniqResourceKey = "non-uniq-cpu";

        final Map<String, Long> amountByServiceKey = ImmutableMap.of(
                YP, 100L,
                GENCFG, 200L,
                NIRVANA, 300L
        );


        for (final String key : amountByServiceKey.keySet()) {
            dispenser()
                    .service(key)
                    .resource(nonUniqResourceKey)
                    .create()
                    .withName(key + " " + nonUniqResourceKey)
                    .withDescription(key + " " + nonUniqResourceKey)
                    .withType(DiResourceType.ENUMERABLE)
                    .performBy(AMOSOV_F);
        }

        updateHierarchy();

        for (final String key : amountByServiceKey.keySet()) {
            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/quota-specifications/" + key + "/" + nonUniqResourceKey + "/" + nonUniqResourceKey)
                    .put(ImmutableMap.of(
                            "key", nonUniqResourceKey,
                            "description", nonUniqResourceKey
                    ));
        }

        updateHierarchy();

        for (final String key : amountByServiceKey.keySet()) {
            dispenser()
                    .service(key)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState.forResource(nonUniqResourceKey)
                            .forProject(YANDEX)
                            .withMax(DiAmount.of(amountByServiceKey.get(key), DiUnit.PERMILLE))
                            .build())
                    .performBy(AMOSOV_F);
        }

        updateHierarchy();

        final DiQuotaGetResponse allQuotas = dispenser().quotas()
                .get()
                .ofProject(YANDEX)
                .perform();

        final List<DiQuota> quotas = allQuotas.stream()
                .filter(q -> {
                    final DiResource resource = q.getSpecification().getResource();
                    return resource.getKey().equals(nonUniqResourceKey) && amountByServiceKey.keySet().contains(resource.getService().getKey());
                })
                .collect(Collectors.toList());

        assertEquals(quotas.size(), amountByServiceKey.size());

        assertTrue(quotas.stream()
                .allMatch(q -> q.getMax().getValue() == amountByServiceKey.get(q.getSpecification().getResource().getService().getKey())));
    }

    @Test
    public void quotasWithEqualResourceKeyInDifferentServicesShouldBeRawSyncable() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final String nonUniqResourceKey = "non-uniq-cpu";

        final Map<String, Long> amountByServiceKey = ImmutableMap.of(
                YP, 100L,
                GENCFG, 200L,
                NIRVANA, 300L
        );


        for (final String key : amountByServiceKey.keySet()) {
            dispenser()
                    .service(key)
                    .resource(nonUniqResourceKey)
                    .create()
                    .withName(key + " " + nonUniqResourceKey)
                    .withDescription(key + " " + nonUniqResourceKey)
                    .withType(DiResourceType.ENUMERABLE)
                    .performBy(AMOSOV_F);
        }

        updateHierarchy();

        for (final String key : amountByServiceKey.keySet()) {
            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/quota-specifications/" + key + "/" + nonUniqResourceKey + "/" + nonUniqResourceKey)
                    .put(ImmutableMap.of(
                            "key", nonUniqResourceKey,
                            "description", nonUniqResourceKey
                    ));
        }

        updateHierarchy();

        for (final String key : amountByServiceKey.keySet()) {
            dispenser()
                    .service(key)
                    .syncState()
                    .rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(nonUniqResourceKey)
                            .forProject(YANDEX)
                            .withMax(DiAmount.of(amountByServiceKey.get(key), DiUnit.PERMILLE))
                            .build())
                    .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        }

        updateHierarchy();

        final DiQuotaGetResponse allQuotas = dispenser().quotas()
                .get()
                .ofProject(YANDEX)
                .perform();

        final List<DiQuota> quotas = allQuotas.stream()
                .filter(q -> {
                    final DiResource resource = q.getSpecification().getResource();
                    return resource.getKey().equals(nonUniqResourceKey) && amountByServiceKey.keySet().contains(resource.getService().getKey());
                })
                .collect(Collectors.toList());

        assertEquals(quotas.size(), amountByServiceKey.size());

        assertTrue(quotas.stream()
                .allMatch(q -> q.getMax().getValue() == amountByServiceKey.get(q.getSpecification().getResource().getService().getKey())));
    }

    @Test
    public void projectMaxesCantBeUpdatedWhenServiceDoesntUseHierarchy() {
        assertThrowsWithMessage(() -> {
            dispenser().service(MDS).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(HDD).forProject(SEARCH).withMax(DiAmount.of(0, DiUnit.BYTE)).build())
                    .performBy(WHISTLER);
        }, "Can't change quota max for service that doesn't use project hierarchy");

        assertThrowsWithMessage(() -> {
            dispenser().service(MDS).updateMax().quotas()
                    .updateMax(DiQuotaMaxUpdate.forResource(HDD).forProject(SEARCH).withMax(DiAmount.of(0, DiUnit.BYTE)).build())
                    .performBy(WHISTLER);
        }, "Can't change quota max for service that doesn't use project hierarchy");

        assertThrowsWithMessage(() -> {
            dispenser().service(MDS).updateMax().deltas()
                    .updateMax(DiQuotaMaxDeltaUpdate.forResource(HDD).forProject(SEARCH).withMaxDelta(DiSignedAmount.positive(0, DiUnit.BYTE)).build())
                    .performBy(WHISTLER);
        }, "Can't change quota max for service that doesn't use project hierarchy");

        assertThrowsWithMessage(() -> {
            dispenser().service(MDS).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(HDD).forProject(INFRA_SPECIAL).withMax(DiAmount.of(0, DiUnit.BYTE)).build())
                    .performBy(WHISTLER);
        }, "Can't change quota max for service that doesn't use project hierarchy");

        assertThrowsWithMessage(() -> {
            dispenser().service(MDS).updateMax().quotas()
                    .updateMax(DiQuotaMaxUpdate.forResource(HDD).forProject(INFRA_SPECIAL).withMax(DiAmount.of(0, DiUnit.BYTE)).build())
                    .performBy(WHISTLER);
        }, "Can't change quota max for service that doesn't use project hierarchy");

        assertThrowsWithMessage(() -> {
            dispenser().service(MDS).updateMax().deltas()
                    .updateMax(DiQuotaMaxDeltaUpdate.forResource(HDD).forProject(INFRA_SPECIAL).withMaxDelta(DiSignedAmount.positive(0, DiUnit.BYTE)).build())
                    .performBy(WHISTLER);
        }, "Can't change quota max for service that doesn't use project hierarchy");
    }

    @Test
    public void projectMaxesCantBeRawUpdatedWhenServiceDoesntUseHierarchy() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        assertThrowsWithMessage(() -> {
            dispenser().service(MDS).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(HDD).forProject(SEARCH).withMax(DiAmount.of(0, DiUnit.BYTE)).build())
                    .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        }, "Can not change quota max values for service that does not use project hierarchy");

        assertThrowsWithMessage(() -> {
            dispenser().service(MDS).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(HDD).forProject(INFRA_SPECIAL).withMax(DiAmount.of(0, DiUnit.BYTE)).build())
                    .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        }, "Can not change quota max values for service that does not use project hierarchy");
    }

    @Test
    public void leafProjectOwnMaxesCanBeUpdatedWhenServiceDoesntUseHierarchy() {
        dispenser().service(MDS).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(HDD).forProject(INFRA).withOwnMax(DiAmount.of(1, DiUnit.GIBIBYTE)).build())
                .performBy(WHISTLER);

        dispenser().service(MDS).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(HDD).forProject(INFRA).withOwnMax(DiAmount.of(2, DiUnit.GIBIBYTE)).build())
                .performBy(WHISTLER);

        dispenser().service(MDS).updateMax().deltas()
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(HDD).forProject(INFRA).withOwnMaxDelta(DiSignedAmount.positive(1, DiUnit.GIBIBYTE)).build())
                .performBy(WHISTLER);

        updateHierarchy();

        final DiQuota quota = dispenser().quotas().get().inService(MDS).forResource(HDD).ofProject(INFRA).perform().getFirst();
        assertEquals(3, quota.getOwnMax(DiUnit.GIBIBYTE));
    }

    @Test
    public void leafProjectOwnMaxesCanBeRawUpdatedWhenServiceDoesntUseHierarchy() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        dispenser().service(MDS).syncState().rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(HDD).forProject(INFRA).withOwnMax(DiAmount.of(1, DiUnit.GIBIBYTE)).build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final DiQuota quota = dispenser().quotas().get().inService(MDS).forResource(HDD).ofProject(INFRA).perform().getFirst();
        assertEquals(1, quota.getOwnMax(DiUnit.GIBIBYTE));
    }

    @Test
    public void projectOwnMaxCanBeUpdatedWhenServiceDoesntUseHierarchy() {
        dispenser().service(MDS).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(HDD).forProject(SEARCH).withOwnMax(DiAmount.of(1, DiUnit.GIBIBYTE)).build())
                .performBy(WHISTLER);

        dispenser().service(MDS).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(HDD).forProject(SEARCH).withOwnMax(DiAmount.of(2, DiUnit.GIBIBYTE)).build())
                .performBy(WHISTLER);

        dispenser().service(MDS).updateMax().deltas()
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(HDD).forProject(SEARCH).withOwnMaxDelta(DiSignedAmount.positive(1, DiUnit.GIBIBYTE)).build())
                .performBy(WHISTLER);

        updateHierarchy();

        final DiQuota quota = dispenser().quotas().get().inService(MDS).forResource(HDD).ofProject(SEARCH).perform().getFirst();
        assertEquals(3, quota.getOwnMax(DiUnit.GIBIBYTE));
    }

    @Test
    public void projectOwnMaxCanBeRawUpdatedWhenServiceDoesntUseHierarchy() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        dispenser().service(MDS).syncState().rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(HDD).forProject(SEARCH).withOwnMax(DiAmount.of(1, DiUnit.GIBIBYTE)).build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final DiQuota quota = dispenser().quotas().get().inService(MDS).forResource(HDD).ofProject(SEARCH).perform().getFirst();
        assertEquals(1, quota.getOwnMax(DiUnit.GIBIBYTE));
    }

    @Test
    public void serviceQuotaManualAllocatedModeShouldBeStored() {
        DiService diService = dispenser()
                .service(GENCFG)
                .get()
                .perform();

        assertFalse(diService.getSettings().isManualQuotaAllocation());

        final Service service = serviceDao.read(GENCFG);

        final Service updatedService = Service.withKey(service.getKey())
                .withName(service.getName())
                .withPriority(service.getPriority())
                .withAbcServiceId(service.getAbcServiceId())
                .withId(service.getId())
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(service.getSettings().accountActualValuesInQuotaDistribution())
                        .usesProjectHierarchy(service.getSettings().usesProjectHierarchy())
                        .manualQuotaAllocation(true)
                        .build())
                .build();

        serviceDao.update(updatedService);

        updateHierarchy();

        diService = dispenser()
                .service(GENCFG)
                .get()
                .perform();

        assertTrue(diService.getSettings().isManualQuotaAllocation());
    }

}
