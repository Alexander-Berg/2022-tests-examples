package ru.yandex.qe.dispenser.ws.logic;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiActualQuotaUpdate;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaChangeResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.EntitySpec;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.PersonAffiliation;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.util.CollectionUtils;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;
import ru.yandex.qe.dispenser.ws.ServiceService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class QuotaSynchronizationTest extends AcceptanceTestBase {

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void serviceAdminCanUpdateMaxValuesOfServiceQuotas() {
        final DiQuotaGetResponse response = dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(90, DiUnit.GIBIBYTE)).build())
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(10, DiUnit.GIBIBYTE)).build())
                .performBy(SANCHO);
        assertEquals(3, response.size());
        updateHierarchy();
        assertEquals(getQuotaMax(NIRVANA, STORAGE, YANDEX), DiAmount.of(100, DiUnit.GIBIBYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, DEFAULT), DiAmount.of(90, DiUnit.GIBIBYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, INFRA), DiAmount.of(10, DiUnit.GIBIBYTE));
    }

    @Test
    public void serviceTrusteeCanRawUpdateMaxValuesOfServiceQuotas() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiResponse response = dispenser().service(NIRVANA).syncState().rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(90, DiUnit.GIBIBYTE)).build())
                .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(10, DiUnit.GIBIBYTE)).build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        updateHierarchy();
        assertEquals(getQuotaMax(NIRVANA, STORAGE, YANDEX), DiAmount.of(100, DiUnit.GIBIBYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, DEFAULT), DiAmount.of(90, DiUnit.GIBIBYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, INFRA), DiAmount.of(10, DiUnit.GIBIBYTE));
    }

    @Test
    public void serviceTrusteeCanRawUpdateOwnMaxValuesOfServiceQuotas() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiResponse response = dispenser().service(NIRVANA).syncState().rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withOwnMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        updateHierarchy();
        assertEquals(getQuotaOwnMax(NIRVANA, STORAGE, YANDEX), DiAmount.of(100, DiUnit.GIBIBYTE));
    }

    @Test
    public void serviceTrusteeCanRawUpdateActualValuesOfServiceQuotas() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final DiResponse response = dispenser().service(NIRVANA).syncState().rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(YT_CPU).forProject(YANDEX).withActual(DiAmount.of(100, DiUnit.COUNT)).build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        updateHierarchy();
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, YANDEX), DiAmount.of(100, DiUnit.COUNT));
    }

    @Test
    public void projectResponsibleCanUpdateMaxValuesOfSubprojectQuotas() {
        final DiQuotaGetResponse response = dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(100, DiUnit.BYTE)).build())
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(50, DiUnit.BYTE)).build())
                .performBy(WHISTLER);
        assertEquals(2, response.size());
        updateHierarchy();
        assertEquals(getQuotaMax(NIRVANA, STORAGE, DEFAULT), DiAmount.of(100, DiUnit.BYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, INFRA), DiAmount.of(50, DiUnit.BYTE));
    }

    @Test
    public void projectResponsibleCantUpdateMaxValuesOfHisProjectQuotas() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(WHISTLER);
        });
    }

    @Test
    public void projectResponsibleCantRawUpdateMaxValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(WHISTLER);
        });
    }

    @Test
    public void projectResponsibleCantRawUpdateOwnMaxValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withOwnMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(WHISTLER);
        });
    }

    @Test
    public void projectResponsibleCantRawUpdateActualValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withActual(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(WHISTLER);
        });
    }

    @Test
    public void projectMemberCantUpdateQuotaMaxValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(100, DiUnit.BYTE)).build())
                    .performBy(LYADZHIN);
        });
    }

    @Test
    public void projectMemberCantRawUpdateMaxValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(LYADZHIN);
        });
    }

    @Test
    public void projectMemberCantRawUpdateOwnMaxValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withOwnMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(LYADZHIN);
        });
    }

    @Test
    public void projectMemberCantRawUpdateActualValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withActual(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(LYADZHIN);
        });
    }

    @Test
    public void serviceAdminCantRawUpdateMaxValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(SANCHO);
        });
    }

    @Test
    public void serviceAdminCantRawUpdateOwnMaxValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withOwnMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(SANCHO);
        });
    }

    @Test
    public void serviceAdminCantRawUpdateActualValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(NIRVANA).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState.forResource(STORAGE).forProject(YANDEX).withActual(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(SANCHO);
        });
    }

    @Test
    public void serviceTrusteeCanUpdateActualValuesOfServiceQuotasForLeafProjects() {
        final DiQuotaGetResponse response = dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(DEFAULT).withActual(DiAmount.of(10, DiUnit.COUNT)).build())
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(INFRA).withActual(DiAmount.of(20, DiUnit.COUNT)).build())
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(INFRA_SPECIAL).withActual(DiAmount.of(5, DiUnit.COUNT)).build())
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(VERTICALI).withActual(DiAmount.of(0, DiUnit.COUNT)).build())
                .perform();
        assertEquals(4, response.size());
        updateHierarchy();
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, DEFAULT), DiAmount.of(10, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, INFRA), DiAmount.of(20, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, INFRA_SPECIAL), DiAmount.of(5, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, VERTICALI), DiAmount.of(0, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, YANDEX), DiAmount.of(35, DiUnit.COUNT));
    }

    @Test
    public void serviceTrusteeCanUpdateActualValuesOfServiceQuotasForLeafProjectsByDedicatedEndpoint() {
        dispenser().service(NIRVANA).syncState().actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(DEFAULT).actual(DiAmount.of(10, DiUnit.COUNT)).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(INFRA).actual(DiAmount.of(20, DiUnit.COUNT)).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(INFRA_SPECIAL).actual(DiAmount.of(5, DiUnit.COUNT)).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(VERTICALI).actual(DiAmount.of(0, DiUnit.COUNT)).build())
                .perform();
        updateHierarchy();
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, DEFAULT), DiAmount.of(10, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, INFRA), DiAmount.of(20, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, INFRA_SPECIAL), DiAmount.of(5, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, VERTICALI), DiAmount.of(0, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, YANDEX), DiAmount.of(35, DiUnit.COUNT));
    }

    public void serviceTrusteeCantUpdateQuotaActualValuesForLeafProjects() {
        dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(SEARCH).withActual(DiAmount.of(10, DiUnit.COUNT)).build())
                .perform();
    }

    public void serviceTrusteeCantUpdateQuotaActualValuesForLeafProjectsByDedicatedEndpoint() {
        dispenser().service(NIRVANA).syncState().actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(SEARCH).actual(DiAmount.of(10, DiUnit.COUNT)).build())
                .perform();
    }

    @Test
    public void serviceTrusteeCantUpdateQuotaActualValuesForResourcesWithIncorrectQuotingMode() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(DEFAULT).withActual(DiAmount.of(10, DiUnit.GIBIBYTE)).build())
                    .perform();
        });
    }

    @Test
    public void serviceTrusteeCantUpdateQuotaActualValuesForResourcesWithIncorrectQuotingModeByDedicatedEndpoint() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().service(NIRVANA).syncState().actualQuotas()
                    .changeActualQuota(DiActualQuotaUpdate.forResource(STORAGE).project(DEFAULT).actual(DiAmount.of(10, DiUnit.GIBIBYTE)).build())
                    .perform();
        });
    }

    @Test
    public void serviceAdminCantUpdateQuotaActualValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(DEFAULT).withActual(DiAmount.of(10, DiUnit.COUNT)).build())
                    .performBy(SANCHO);
        });
    }

    @Test
    public void serviceAdminCantUpdateQuotaActualValuesByDedicatedEndpoint() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().actualQuotas()
                    .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(DEFAULT).actual(DiAmount.of(10, DiUnit.COUNT)).build())
                    .performBy(SANCHO);
        });
    }

    @Test
    public void projectResponsibleCantUpdateQuotaActualValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(DEFAULT).withActual(DiAmount.of(10, DiUnit.COUNT)).build())
                    .performBy(WHISTLER);
        });
    }

    @Test
    public void projectResponsibleCantUpdateQuotaActualValuesByDedicatedEndpoint() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().actualQuotas()
                    .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(DEFAULT).actual(DiAmount.of(10, DiUnit.COUNT)).build())
                    .performBy(WHISTLER);
        });
    }

    @Test
    public void projectMemberCantUpdateQuotaActualValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(DEFAULT).withActual(DiAmount.of(10, DiUnit.COUNT)).build())
                    .performBy(LYADZHIN);
        });
    }

    @Test
    public void projectMemberCantUpdateQuotaActualValuesByDedicatedEndpoint() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().actualQuotas()
                    .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(DEFAULT).actual(DiAmount.of(10, DiUnit.COUNT)).build())
                    .performBy(LYADZHIN);
        });
    }

    @Test
    public void acquireForResourcesInSynchronizationModeIsNotAllowed() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .acquireResource(DiResourceAmount.ofResource(YT_CPU).withAmount(1, DiUnit.COUNT).build(), LYADZHIN.chooses(INFRA))
                    .perform();
        });
    }

    @Test
    public void releaseForResourcesInSynchronizationModeIsNotAllowed() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(INFRA).withActual(DiAmount.of(1, DiUnit.COUNT)).build())
                    .perform();
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .releaseResource(DiResourceAmount.ofResource(YT_CPU).withAmount(1, DiUnit.COUNT).build(), LYADZHIN.chooses(INFRA))
                    .perform();
        });
    }

    @Test
    public void creatingEntitySpecWithResourceInSynchronizationModeIsNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> {
            final Service nirvana = serviceDao.read(NIRVANA);
            final Resource ytCpu = resourceDao.read(new Resource.Key(YT_CPU, nirvana));
            entitySpecDao.create(EntitySpec.builder().withKey("yt_task").withDescription("YT task").overResource(ytCpu).build());
        });
    }

    @Test
    public void canUpdateQuotaActualValuesForProjectsWithPersonalSubprojects() {
        // create entity to cause personal project creation
        final DiQuotaChangeResponse createEntityResponse = dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA))
                .perform();
        assertEquals(1, createEntityResponse.size());
        updateHierarchy();
        // check personal project really exists
        final Project personalProject = projectDao.readOrNull(Project.Key.of(INFRA, new Person(LYADZHIN.getLogin(), 1120000000014351L, false, false, false, PersonAffiliation.YANDEX)));
        Assertions.assertNotNull(personalProject);
        // sync quota actual value for project with just created personal subproject
        final DiQuotaGetResponse syncQuotaResponse = dispenser()
                .service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(INFRA).withActual(DiAmount.of(1, DiUnit.COUNT)).build())
                .perform();
        assertEquals(1, syncQuotaResponse.size());
        updateHierarchy();
        // check project quota value and aggregation
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, INFRA), DiAmount.of(1, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, YANDEX), DiAmount.of(1, DiUnit.COUNT));
    }

    @Test
    public void canUpdateQuotaActualValuesForProjectsWithPersonalSubprojectsByDedicatedEndpoint() {
        // create entity to cause personal project creation
        final DiQuotaChangeResponse createEntityResponse = dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA))
                .perform();
        assertEquals(1, createEntityResponse.size());
        updateHierarchy();
        // check personal project really exists
        final Project personalProject = projectDao.readOrNull(Project.Key.of(INFRA, new Person(LYADZHIN.getLogin(), 1120000000014351L, false, false, false, PersonAffiliation.YANDEX)));
        Assertions.assertNotNull(personalProject);
        // sync quota actual value for project with just created personal subproject
        dispenser()
                .service(NIRVANA).syncState().actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU).project(INFRA).actual(DiAmount.of(1, DiUnit.COUNT)).build())
                .perform();
        updateHierarchy();
        // check project quota value and aggregation
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, INFRA), DiAmount.of(1, DiUnit.COUNT));
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, YANDEX), DiAmount.of(1, DiUnit.COUNT));
    }

    @Test
    public void whenUpdatingMaxValueCorrectQuotaStateShouldBeReturned() {
        dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(DEFAULT).withActual(DiAmount.of(10, DiUnit.COUNT)).build())
                .perform();
        updateHierarchy();
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, DEFAULT), DiAmount.of(10, DiUnit.COUNT));
        final DiQuotaGetResponse response = dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.COUNT)).build())
                .performBy(SANCHO);
        final DiQuota changedQuota = CollectionUtils.first(response);
        assertEquals(100, changedQuota.getMax(DiUnit.COUNT));
        assertEquals(10, changedQuota.getActual(DiUnit.COUNT));
    }

    @NotNull
    private DiQuota getQuota(@NotNull final String serviceKey, @NotNull final String resourceKey, @NotNull final String projectKey) {
        return dispenser().quotas().get()
                .inService(serviceKey)
                .forResource(resourceKey)
                .ofProject(projectKey)
                .perform()
                .getFirst();
    }

    @NotNull
    private DiAmount getQuotaMax(@NotNull final String serviceKey, @NotNull final String resourceKey, @NotNull final String projectKey) {
        return getQuota(serviceKey, resourceKey, projectKey).getMax().humanize();
    }

    @NotNull
    private DiAmount getQuotaOwnMax(@NotNull final String serviceKey, @NotNull final String resourceKey, @NotNull final String projectKey) {
        return getQuota(serviceKey, resourceKey, projectKey).getOwnMax().humanize();
    }

    @NotNull
    private DiAmount getQuotaActual(@NotNull final String serviceKey,
                                    @NotNull final String resourceKey,
                                    @NotNull final String projectKey) {
        return getQuota(serviceKey, resourceKey, projectKey).getActual().humanize();
    }
}
