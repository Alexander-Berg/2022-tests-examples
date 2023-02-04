package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.KeyBase;
import ru.yandex.qe.dispenser.api.v1.DiResource;
import ru.yandex.qe.dispenser.api.v1.DiResourceGroup;
import ru.yandex.qe.dispenser.api.v1.DiService;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResourceGroupValidationTest extends BusinessLogicTestBase {

    @Test
    public void groupsCanBeViewed() {
        final DiListResponse<DiResourceGroup> groups = dispenser()
                .service(NIRVANA)
                .resourceGroups()
                .get()
                .perform();

        assertFalse(groups.isEmpty());
    }

    @Test
    public void groupCanBeFetched() {
        final DiResourceGroup resourceGroup = dispenser()
                .service(NIRVANA)
                .resourceGroups()
                .byKey(RESOURCE_GROUP_STORAGE)
                .get()
                .perform();

        assertEquals(RESOURCE_GROUP_STORAGE, resourceGroup.getKey());
        assertNull(resourceGroup.getPriority());
    }


    @Test
    public void groupCanBeCreated() {

        final String newGroupKey = "customGroup";
        final String newGroupName = "Custom group";

        dispenser()
                .service(NIRVANA)
                .resourceGroups()
                .create(DiResourceGroup.withKey(newGroupKey)
                        .inService(DiService.withKey(NIRVANA).withName("").build())
                        .withName(newGroupName)
                        .withPriority(84)
                        .build()
                ).performBy(AMOSOV_F);

        updateHierarchy();

        final DiResourceGroup createdGroup = dispenser().service(NIRVANA)
                .resourceGroups()
                .byKey(newGroupKey)
                .get()
                .perform();

        assertEquals(newGroupName, createdGroup.getName());
        assertEquals(newGroupKey, createdGroup.getKey());
        assertEquals(NIRVANA, createdGroup.getService().getKey());
        assertEquals(84, (int) createdGroup.getPriority());
    }

    @Test
    public void groupCanBeUpdated() {

        dispenser()
                .service(NIRVANA)
                .resourceGroups()
                .byKey(RESOURCE_GROUP_STORAGE)
                .update(DiResourceGroup.withKey(RESOURCE_GROUP_STORAGE)
                        .inService(DiService.withKey(NIRVANA).withName("").build())
                        .withName("modified name")
                        .withPriority(21)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiResourceGroup group = dispenser()
                .service(NIRVANA)
                .resourceGroups()
                .byKey(RESOURCE_GROUP_STORAGE)
                .get()
                .perform();

        assertEquals("modified name", group.getName());
        assertEquals(21, (int) group.getPriority());
    }

    @Test
    public void groupCanBeDeleted() {
        dispenser()
                .service(NIRVANA)
                .resourceGroups()
                .byKey(RESOURCE_GROUP_GRAPHS)
                .delete()
                .performBy(AMOSOV_F);

        updateHierarchy();

        assertThrows(Throwable.class, () -> {
            dispenser().service(NIRVANA)
                    .resourceGroups()
                    .byKey(RESOURCE_GROUP_GRAPHS)
                    .get()
                    .perform();
        });
    }

    @Test
    public void onRemoveGroupResourcesInGroupMustClearGroup() {
        final DiResource resource = dispenser().service(NIRVANA).resource(STORAGE).get().perform();
        assertEquals(RESOURCE_GROUP_STORAGE, resource.getGroup());

        dispenser()
                .service(NIRVANA)
                .resourceGroups()
                .byKey(RESOURCE_GROUP_STORAGE)
                .delete()
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiResource refreshedResource = dispenser().service(NIRVANA).resource(STORAGE).get().perform();
        assertNull(refreshedResource.getGroup());
    }

    @Test
    public void resourceGroupsMustBeSortedByPriority() {
        final DiService service = DiService.withKey(MDB).withName("").build();
        dispenser()
                .service(service.getKey())
                .resourceGroups()
                .create(DiResourceGroup.withKey("g1")
                        .inService(service)
                        .withName("g1")
                        .withPriority(20)
                        .build()
                ).performBy(AMOSOV_F);

        dispenser()
                .service(service.getKey())
                .resourceGroups()
                .create(DiResourceGroup.withKey("g2")
                        .inService(service)
                        .withName("g2")
                        .build()
                ).performBy(AMOSOV_F);

        dispenser()
                .service(service.getKey())
                .resourceGroups()
                .create(DiResourceGroup.withKey("g3")
                        .inService(service)
                        .withName("g3")
                        .withPriority(7)
                        .build()
                ).performBy(AMOSOV_F);

        updateHierarchy();

        List<String> groups = dispenser()
                .service(service.getKey())
                .resourceGroups()
                .get()
                .perform().stream()
                .map(KeyBase::getKey)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("g3", "g1", "g2"), groups);

        dispenser()
                .service(service.getKey())
                .resourceGroups()
                .byKey("g3")
                .update(DiResourceGroup.withKey("g3")
                        .inService(service)
                        .withName("modified name")
                        .withPriority(21)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        groups = dispenser()
                .service(service.getKey())
                .resourceGroups()
                .get()
                .perform().stream()
                .map(KeyBase::getKey)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("g1", "g3", "g2"), groups);
    }

}
