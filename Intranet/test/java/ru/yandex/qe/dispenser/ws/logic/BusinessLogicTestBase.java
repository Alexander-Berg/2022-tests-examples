package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPersonGroup;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.utils.DiEntityIdSupplier;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BusinessLogicTestBase extends AcceptanceTestBase {
    @Value("${spring.profiles.active:default}")
    private String profile;
    @Value("${hierarchy.enabled}")
    private boolean hierarchyEnabled;

    protected boolean isStubMode() {
        return "default".equals(profile);
    }

    protected boolean isHierarchyEnabled() {
        return hierarchyEnabled;
    }

    @NotNull
    protected DiEntity randomUnitEntity() {
        return DiEntity.withKey("pool" + new Random().nextInt())
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.anyOf(DiUnit.BYTE))
                .build();
    }

    @NotNull
    protected Stream<DiEntity> generateNirvanaYtFiles(final int count, @NotNull final DiEntityIdSupplier entityIdSupplier) {
        return Stream.generate(
                () -> {
                    return DiEntity.withKey(entityIdSupplier.get())
                            .bySpecification(YT_FILE)
                            .occupies(STORAGE, DiAmount.anyOf(DiUnit.GIBIBYTE))
                            .build();
                })
                .limit(count);
    }

    @NotNull
    protected Stream<DiEntity> generateNirvanaYtFiles(final int count) {
        return generateNirvanaYtFiles(count, new DiEntityIdSupplier(YT_FILE));
    }

    protected Stream<DiEntity> generateWorkloadEntities(final int count) {
        final DiEntityIdSupplier workloadIdSupplier = new DiEntityIdSupplier(WORKLOAD);
        return Stream.generate(
                () -> {
                    return DiEntity.withKey(workloadIdSupplier.get())
                            .bySpecification(WORKLOAD)
                            .occupies(CPU, DiAmount.anyOf(DiUnit.COUNT))
                            .occupies(RAM, DiAmount.anyOf(DiUnit.GIBIBYTE))
                            .build();
                })
                .limit(count);
    }

    protected void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }

    protected void sleep(final long duration, final TimeUnit timeUnit) {
        sleep(timeUnit.toMillis(duration));
    }

    protected void assertBetween(final long min, final long x, final long max) {
        assertTrue(min <= x);
        assertTrue(x <= max);
    }

    protected void assertActualEquals(@NotNull final DiQuotaGetResponse quotas,
                                      @NotNull final String resourceKey,
                                      @NotNull final String projectKey,
                                      final long actual) {
        assertActualEquals(quotas, resourceKey, Collections.singletonMap(projectKey, actual));
    }

    protected void assertActualEquals(@NotNull final DiQuotaGetResponse quotas,
                                      @NotNull final String resourceKey,
                                      @NotNull final String projectKey1,
                                      final long actual1,
                                      @NotNull final String projectKey2,
                                      final long actual2) {
        assertActualEquals(quotas, resourceKey, ImmutableMap.of(projectKey1, actual1, projectKey2, actual2));
    }

    protected void assertActualEquals(@NotNull final DiQuotaGetResponse quotas,
                                      @NotNull final String resourceKey,
                                      @NotNull final Map<String, Long> project2actual) {
        assertActualEquals(quotas, resourceKey, project2actual::get);
    }

    protected void assertActualEquals(@NotNull final DiQuotaGetResponse quotas,
                                      @NotNull final String resourceKey,
                                      @NotNull final Function<String, Long> project2actual) {
        quotas.forEach(q -> {
            final String quotaResourceKey = q.getSpecification().getResource().getKey();
            if (!quotaResourceKey.equals(resourceKey)) {
                return;
            }
            final long expectedActual = Optional.ofNullable(project2actual.apply(q.getProject().getKey())).orElse(0L);
            final String message = String.format("Different quotas for '%s' resource in '%s' project!", quotaResourceKey, q.getProject().getKey());
            assertEquals(expectedActual, q.getActual().getValue(), message);
        });
    }

    public static void assertThrowsWithMessage(final Runnable runnable, final int status, final String... containsMessages) {
        assertThrows(Throwable.class, runnable::run);
        assertEquals(status, SpyWebClient.lastResponseStatus());
        final String lastResponse = SpyWebClient.lastResponse();
        Arrays.stream(containsMessages)
                .forEach(message -> {
                    assertTrue(lastResponse.contains(message), "Exception message should contain message \"" + message + "\". Actual message: \""
                            + lastResponse + "\"");
                });
    }

    public static void assertThrowsWithMessage(final Runnable runnable, final String... containsMessages) {
        assertThrowsWithMessage(runnable, HttpStatus.SC_BAD_REQUEST, containsMessages);
    }

    protected static void assertThrowsForbiddenWithMessage(final Runnable runnable, final String... containsMessages) {
        assertThrowsWithMessage(runnable, HttpStatus.SC_FORBIDDEN, containsMessages);
    }

    protected static void assertMessageContains(final String message, final String part) {
        assertTrue(message.contains(part), String.format("Message '%s' should contain '%s'", message, part));
    }

    protected DiProject createProject(final String key, final String parentKey, final String... responsible) {
        final DiProject diProject = dispenser().projects()
                .create(DiProject.withKey(key)
                        .withName(key)
                        .withDescription(key)
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(parentKey)
                        .withResponsibles(DiPersonGroup.builder().addPersons(responsible).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        return diProject;
    }
}
