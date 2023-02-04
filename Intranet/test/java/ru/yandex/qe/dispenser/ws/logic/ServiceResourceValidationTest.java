package ru.yandex.qe.dispenser.ws.logic;

import java.text.DecimalFormat;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResource;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParametersAreNonnullByDefault
public class ServiceResourceValidationTest extends BusinessLogicTestBase {

    @Test
    public void resourcesCanBeFetchedByGroups() {
        final Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/services/nirvana/resources").query("group", "storages").get();
        assertEquals(200, response.getStatus());

        final DiListResponse<DiResource> resources = dispenser().service(NIRVANA).resourcesByGroups("storages").perform();
        assertEquals(1, resources.size());
        assertEquals("storages", resources.getFirst().getGroup());
    }

    @Test
    public void resourceCanBeCreated() {
        final String resourceKey = "new-resource";

        dispenser().service(SCRAPER).resource(resourceKey)
                .create()
                .withName("New Storage")
                .withType(DiResourceType.STORAGE)
                .inMode(DiQuotingMode.ENTITIES_ONLY)
                .withPriority(4)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiResource resource = dispenser().service(SCRAPER).resource(resourceKey).get().perform();
        assertNotNull(resource);

        assertEquals("New Storage", resource.getName());
        assertEquals(DiResourceType.STORAGE, resource.getType());
        assertEquals(DiQuotingMode.ENTITIES_ONLY, resource.getQuotingMode());
        assertEquals(Integer.valueOf(4), resource.getPriority());
    }

    @Test
    public void resourceBaseFieldsCanBeUpdated() {

        final DiResource resource = dispenser().service(SCRAPER).resource(DOWNLOADS).get().perform();

        final String modifiedResourceName = "Modified name";
        final String modifiedResourceDescription = "Modified description";
        final Integer modifiedPriority = 8;

        dispenser().service(SCRAPER).resource(DOWNLOADS)
                .update()
                .withName(modifiedResourceName)
                .withDescription(modifiedResourceDescription)
                .withType(resource.getType())
                .inMode(resource.getQuotingMode())
                .withPriority(modifiedPriority)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiResource modifiedResource = dispenser().service(SCRAPER).resource(DOWNLOADS).get().perform();

        assertEquals(modifiedResourceName, modifiedResource.getName());
        assertEquals(modifiedResourceDescription, modifiedResource.getDescription());
        assertEquals(modifiedPriority, modifiedResource.getPriority());
    }

    @Test
    public void resourceTypeCantBeUpdated() {
        assertThrows(BadRequestException.class, () -> {
            final DiResource resource = dispenser().service(SCRAPER).resource(DOWNLOADS).get().perform();

            dispenser().service(SCRAPER).resource(DOWNLOADS)
                    .update()
                    .withName(resource.getName())
                    .withDescription(resource.getDescription())
                    .withType(DiResourceType.MEMORY)
                    .inMode(resource.getQuotingMode())
                    .performBy(AMOSOV_F);
        });
    }

    @Test
    public void resourceModeCantBeUpdated() {
        assertThrows(BadRequestException.class, () -> {
            final DiResource resource = dispenser().service(SCRAPER).resource(DOWNLOADS).get().perform();

            dispenser().service(SCRAPER).resource(DOWNLOADS)
                    .update()
                    .withName(resource.getName())
                    .withDescription(resource.getDescription())
                    .withType(resource.getType())
                    .inMode(DiQuotingMode.ENTITIES_ONLY)
                    .performBy(AMOSOV_F);
        });
    }

    @Test
    public void resourceCanBeRemoved() {
        dispenser().service(SCRAPER).resource(DOWNLOADS)
                .delete()
                .performBy(AMOSOV_F);

        updateHierarchy();

        assertThrows(Throwable.class, () -> {
            dispenser().service(SCRAPER).resource(DOWNLOADS).get().perform();
        });
    }

    @Test
    public void resourceGroupCanBeUpdated() {
        final DiResource resource = dispenser().service(NIRVANA).resource(STORAGE).get().perform();
        assertNotEquals(RESOURCE_GROUP_GRAPHS, resource.getGroup());

        dispenser().service(NIRVANA).resource(STORAGE)
                .update()
                .withName(resource.getName())
                .withDescription(resource.getDescription())
                .withType(resource.getType())
                .inMode(resource.getQuotingMode())
                .inGroup(RESOURCE_GROUP_GRAPHS)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiResource refreshedResource = dispenser().service(NIRVANA).resource(STORAGE).get().perform();
        assertEquals(RESOURCE_GROUP_GRAPHS, refreshedResource.getGroup());
    }

    @Test
    public void resourceGroupCanBeCleared() {
        final DiResource resource = dispenser().service(NIRVANA).resource(STORAGE).get().perform();
        assertEquals(RESOURCE_GROUP_STORAGE, resource.getGroup());

        dispenser().service(NIRVANA).resource(STORAGE)
                .update()
                .withName(resource.getName())
                .withDescription(resource.getDescription())
                .withType(resource.getType())
                .inMode(resource.getQuotingMode())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiResource refreshedResource = dispenser().service(NIRVANA).resource(STORAGE).get().perform();
        assertNull(refreshedResource.getGroup());
    }

    @Test
    public void resourcesOfServiceCanBeRequested() {
        final DiListResponse<DiResource> resources = dispenser()
                .service(NIRVANA)
                .resources()
                .perform();

        assertFalse(resources.isEmpty());
    }

    @Test
    public void resourceWithCoresTypeCanBeCreated() {
        final String resourceKey = "cpu-cores";

        dispenser()
                .service(YP)
                .resource(resourceKey)
                .create()
                .withName("Cpu cores")
                .withDescription("Cpu cores")
                .withType(DiResourceType.PROCESSOR)
                .performBy(AMOSOV_F);

        updateHierarchy();

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-specifications/" + YP + "/" + resourceKey + "/" + resourceKey)
                .put(ImmutableMap.of(
                        "key", resourceKey,
                        "description", "Cpu cores spec"
                ));

        updateHierarchy();

        final DiQuota quota = getYandexQuotaForResource(resourceKey);

        assertEquals(resourceKey, quota.getSpecification().getKey());
        final DiAmount quotaMax = quota.getMax().humanize();
        assertEquals(DiUnit.CORES, quotaMax.getUnit());
        assertEquals("0 cores", quotaMax.toString());

        dispenser()
                .service(YP)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(resourceKey)
                        .forProject(YANDEX)
                        .withMax(DiAmount.of(10, DiUnit.PERCENT_CORES))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuota quota2 = getYandexQuotaForResource(resourceKey);

        assertEquals(resourceKey, quota2.getSpecification().getKey());
        final DiAmount max = quota2.getMax().humanize();

        assertEquals(DiUnit.CORES, max.getUnit());
        assertEquals(new DecimalFormat("#.##").format(0.1) + " cores", max.toString());
    }

    @NotNull
    private DiQuota getYandexQuotaForResource(final String resourceKey) {
        final DiQuotaGetResponse quotas = dispenser()
                .quotas()
                .get()
                .inService(YP)
                .forResource(resourceKey)
                .ofProject(YANDEX)
                .perform();

        return quotas.getFirst();
    }
}

