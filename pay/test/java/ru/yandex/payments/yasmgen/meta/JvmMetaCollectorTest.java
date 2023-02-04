package ru.yandex.payments.yasmgen.meta;

import javax.inject.Inject;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.executor.ExecutorType;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class JvmMetaCollectorTest {
    @Inject
    private JvmMetaCollector collector;

    @Test
    @DisplayName("Verify that JvmMetaCollector correctly collects GC name")
    void gcInfoCollectTest() {
        assertThat(collector.getGc())
                .isEqualTo(Gc.G1);
    }

    @Test
    @DisplayName("Verify that JvmMetaCollector correctly collects all of the executors info")
    void executorsInfoCollectTest() {
        assertThat(collector.getExecutors())
                .containsExactlyInAnyOrder(
                        new ExecutorInfo(ExecutorType.CACHED, TaskExecutors.IO),
                        new ExecutorInfo(ExecutorType.SCHEDULED, TaskExecutors.SCHEDULED),
                        new ExecutorInfo(ExecutorType.WORK_STEALING, "custom")
                );
    }
}
