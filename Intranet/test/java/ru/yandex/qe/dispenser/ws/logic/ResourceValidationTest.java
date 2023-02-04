package ru.yandex.qe.dispenser.ws.logic;


import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiResource;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ParametersAreNonnullByDefault
public class ResourceValidationTest extends BusinessLogicTestBase {

    @Test
    public void testGetServicesResources() {

        // *Тестирование АПИ без параметров -----------------------------------------------------
        Set<DiResource> resourceFromApi = getResourceFromApi();
        Set<DiResource> resourceFromResourceReader = getResourceFromResourceReader(Collections.emptySet());
        assertEquals(resourceFromApi, resourceFromResourceReader);

        // *Тестирование АПИ с одним параметром ---------------------------------------------
        resourceFromApi = getResourceFromApi("nirvana");
        resourceFromResourceReader = getResourceFromResourceReader(ImmutableSet.of("nirvana"));
        assertEquals(resourceFromApi, resourceFromResourceReader);

        // *Тестирование АПИ с несколькими параметрами параметром ---------------------------------------------
        resourceFromApi = getResourceFromApi("nirvana", "scraper");
        resourceFromResourceReader = getResourceFromResourceReader(ImmutableSet.of("nirvana", "scraper"));
        assertEquals(resourceFromApi, resourceFromResourceReader);
    }

    /**
     * метод для получения ресурсов сервисов из НЕ ЧЕРЕЗ АПИ
     *
     * @param services
     * @return
     */
    @NotNull
    private Set<DiResource> getResourceFromResourceReader(@NotNull final Set<String> services) {
        return Hierarchy.get()
                .getResourceReader()
                .getAll()
                .stream()
                .filter(resource -> (services.isEmpty()) || (services.contains(resource.getService().getKey())))
                .map(Resource::toView)
                .collect(Collectors.toSet());
    }

    /**
     * Метод для получения ресурсов сервисов ЧЕРЕЗ АПИ
     *
     * @param serviceKeys
     * @return
     */
    @NotNull
    private Set<DiResource> getResourceFromApi(final String... serviceKeys) {
        return dispenser()
                .resource()
                .get()
                .query("service", serviceKeys)
                .perform()
                .stream()
                .collect(Collectors.toSet());
    }
}
