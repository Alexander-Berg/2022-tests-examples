package ru.yandex.infra.auth.tasks;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.Role;
import ru.yandex.infra.controller.concurrent.DummyLeaderService;
import ru.yandex.infra.controller.metrics.GaugeRegistry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class RoleUpdaterTests {

    private Watchdog watchdog;
    private RolesUpdater updater;

    @BeforeEach
    void before() {
        watchdog = new Watchdog(Duration.ofDays(1));
        updater = new RolesUpdater(null, null, null, new DummyLeaderService(new MetricRegistry()), Duration.ZERO, Collections.emptyList(),
                Duration.ofDays(1), watchdog, false, false, false, "deploy-test", GaugeRegistry.EMPTY);
    }

    @AfterEach
    void after() {
        watchdog.shutdown();
        updater.shutdown();
    }

    @Test
    void createBatchesEmptyTest() {
        assertThat(RolesUpdater.createBatches(Collections.emptyList()), empty());
    }

    @Test
    void createBatchesSortingTest() {

        var roles = List.of(
                new Role("Project1.Stage2", "DEVELOPER"),
                new Role("Project1.Stage1", "DEVELOPER"),
                new Role("Project1.Stage1", "VIEWER"),
                new Role("Project1", ""),
                new Role("Project1.Stage2", ""),
                new Role("Project1", "OWNER"),
                new Role("Project1.Stage1", ""),
                new Role("Project2", "VIEWER"),
                new Role("Project1.Stage2", "VIEWER"),
                new Role("Project1", "DEVELOPER")
        );

        final List<List<Role>> batches = RolesUpdater.createBatches(roles);

        assertThat(batches, hasSize(4));

        assertThat(batches.get(0), equalTo(List.of(
                new Role("Project1", ""),
                new Role("Project1", "DEVELOPER"),
                new Role("Project1", "OWNER")
        )));

        assertThat(batches.get(1), equalTo(List.of(
                new Role("Project2", "VIEWER")
        )));

        assertThat(batches.get(2), equalTo(List.of(
                new Role("Project1.Stage1", ""),
                new Role("Project1.Stage1", "DEVELOPER"),
                new Role("Project1.Stage1", "VIEWER")
        )));

        assertThat(batches.get(3), equalTo(List.of(
                new Role("Project1.Stage2", ""),
                new Role("Project1.Stage2", "DEVELOPER"),
                new Role("Project1.Stage2", "VIEWER")
        )));

    }

    @Test
    void createBatchesRootRoleTest() {

        var roles = List.of(
                new Role("Project1", "OWNER"),
                Role.superUser(),
                new Role("Project1", "DEVELOPER"),
                new Role("Project1", ""),
                Role.empty()
        );

        final List<List<Role>> batches = RolesUpdater.createBatches(roles);

        assertThat(batches, hasSize(3));

        assertThat(batches.get(0), equalTo(List.of(
                Role.empty()
        )));

        assertThat(batches.get(1), equalTo(List.of(
                Role.superUser()
        )));

        assertThat(batches.get(2), equalTo(List.of(
                new Role("Project1", ""),
                new Role("Project1", "DEVELOPER"),
                new Role("Project1", "OWNER")
        )));

    }

    @Test
    void createBatchesNannyServicesTest() {

        var roles = List.of(
                new Role("Project1.<Nanny>.s2", "owners"),
                new Role("Project1.<Nanny>.s1", "developers"),
                new Role("Project1.<Nanny>.s2", ""),
                new Role("Project1.<Nanny>.s1", "owners"),
                new Role("Project1.<Nanny>", ""),
                new Role("Project1.<Nanny>.s1", ""),
                new Role("Project1.<Nanny>.s2", "evicters")
        );

        final List<List<Role>> batches = RolesUpdater.createBatches(roles);

        assertThat(batches, hasSize(3));

        assertThat(batches.get(0), equalTo(List.of(
                new Role("Project1.<Nanny>", "")
        )));

        assertThat(batches.get(1), equalTo(List.of(
                new Role("Project1.<Nanny>.s1", ""),
                new Role("Project1.<Nanny>.s1", "developers"),
                new Role("Project1.<Nanny>.s1", "owners")
        )));

        assertThat(batches.get(2), equalTo(List.of(
                new Role("Project1.<Nanny>.s2", ""),
                new Role("Project1.<Nanny>.s2", "evicters"),
                new Role("Project1.<Nanny>.s2", "owners")
        )));

    }

}
