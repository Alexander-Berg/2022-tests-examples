package ru.yandex.payments.fnsreg;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpHeaders;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import ru.yandex.payments.fnsreg.error.FnsApiUnavailableException;
import ru.yandex.payments.fnsreg.fnsapi.FnsClient;
import ru.yandex.payments.fnsreg.fnsapi.MaintenanceSchedule;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth.Health;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth.ProxyHealth;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth.ProxyStatus;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsMaintenanceScheduleEvent;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsMaintenanceScheduleEvent.AffectedFeatures;
import ru.yandex.payments.fnsreg.manager.Caches;
import ru.yandex.payments.fnsreg.manager.FnsHealthManager;
import ru.yandex.payments.fnsreg.manager.FnsHealthManager.Feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth.Status.AVAILABLE;
import static ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth.Status.NOT_AVAILABLE;
import static ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth.Status.NO_SLA_AVAILABLE;
import static ru.yandex.payments.fnsreg.fnsapi.dto.FnsMaintenanceScheduleEvent.AffectedFeatures.API_UNAVAILABLE;
import static ru.yandex.payments.fnsreg.fnsapi.dto.FnsMaintenanceScheduleEvent.AffectedFeatures.FIAS_CHECK_AND_REGISTRATION_UNAVAILABLE;
import static ru.yandex.payments.fnsreg.fnsapi.dto.FnsMaintenanceScheduleEvent.AffectedFeatures.REGISTRATION_UNAVAILABLE;
import static ru.yandex.payments.fnsreg.fnsapi.dto.FnsMaintenanceScheduleEvent.AffectedFeatures.SLA_EXCEEDED;
import static ru.yandex.payments.fnsreg.manager.FnsHealthManager.Feature.FIAS_CHECK;
import static ru.yandex.payments.fnsreg.manager.FnsHealthManager.Feature.OTHER;
import static ru.yandex.payments.fnsreg.manager.FnsHealthManager.Feature.REGISTRATION;

@MicronautTest(propertySources = "classpath:application-health-test.yml")
class HealthManagerTest {
    static {
        HazelcastTimeTravelClock.install();
    }

    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MAINTENANCE_OFFSET = Duration.ofHours(1);

    private static final Health OK = new Health(AVAILABLE, "OK");
    private static final Health NA = new Health(NOT_AVAILABLE, "N/A");
    private static final Health NO_SLA = new Health(NO_SLA_AVAILABLE, "NO SLA");

    private static final ProxyHealth PROXY_OK = new ProxyHealth(ProxyStatus.AVAILABLE, "OK");
    private static final ProxyHealth PROXY_NA = new ProxyHealth(ProxyStatus.NOT_AVAILABLE, "N/A");
    private static final ProxyHealth PROXY_PART = new ProxyHealth(ProxyStatus.PARTIALLY_AVAILABLE, "PART");

    @Inject
    FnsHealthManager healthManager;

    @Inject
    FnsClient fnsClient;

    @Value(Caches.FRESH_CACHE_TTL_PROPERTY)
    Duration freshCacheTtl;

    @Value(Caches.MAINTENANCE_CACHE_TTL_PROPERTY)
    Duration maintenanceCacheTtl;

    @MockBean(FnsClient.class)
    public FnsClient fnsClientMock() {
        return mock(FnsClient.class);
    }

    @MockBean(Clock.class)
    public Clock clock() {
        return Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
    }

    @AfterEach
    public void resetCache() {
        val offset = StreamEx.of(freshCacheTtl, maintenanceCacheTtl)
                .max(Comparator.comparingLong(Duration::toNanos))
                .orElseThrow();
        HazelcastTimeTravelClock.moveForward(offset);
    }

    public static Stream<Arguments> unhealthyParameters() {
        return Stream.of(
                Arguments.of(REGISTRATION, new FnsHealth(NA, OK, OK, PROXY_OK, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(NA, OK, OK, PROXY_OK, OK)),
                Arguments.of(OTHER, new FnsHealth(NA, OK, OK, PROXY_OK, OK)),

                Arguments.of(REGISTRATION, new FnsHealth(NO_SLA, OK, OK, PROXY_OK, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(NO_SLA, OK, OK, PROXY_OK, OK)),
                Arguments.of(OTHER, new FnsHealth(NO_SLA, OK, OK, PROXY_OK, OK)),

                Arguments.of(REGISTRATION, new FnsHealth(OK, NA, OK, PROXY_OK, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(OK, OK, OK, PROXY_OK, NA))
        );
    }

    @ParameterizedTest(name = "{0} - {1}")
    @DisplayName("Verify that HealthManager throws an error when FNS API is unhealthy")
    @MethodSource("unhealthyParameters")
    void testUnhealthy(Feature feature, FnsHealth health) {
        when(fnsClient.getHealth())
                .thenReturn(Mono.just(health));
        when(fnsClient.getMaintenanceSchedule(any()))
                .thenReturn(Mono.just(MaintenanceSchedule.empty()));

        assertThatThrownBy(() -> healthManager.checkHealth(feature).block(BLOCK_TIMEOUT))
                .isInstanceOfSatisfying(FnsApiUnavailableException.class, e -> {
                    assertThat(e.getExtraHeaders())
                            .containsExactly(
                                    entry(HttpHeaders.RETRY_AFTER, String.valueOf(freshCacheTtl.toSeconds()))
                            );
                });

        verify(fnsClient, atLeastOnce()).getHealth();
        verify(fnsClient, atMostOnce()).getMaintenanceSchedule(any());
        verifyNoMoreInteractions(fnsClient);
    }

    public static Stream<Arguments> healthyParameters() {
        return Stream.of(
                Arguments.of(REGISTRATION, new FnsHealth(OK, OK, OK, PROXY_OK, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(OK, OK, OK, PROXY_OK, OK)),
                Arguments.of(OTHER, new FnsHealth(OK, OK, OK, PROXY_OK, OK)),

                Arguments.of(REGISTRATION, new FnsHealth(OK, OK, NA, PROXY_OK, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(OK, OK, NA, PROXY_OK, OK)),
                Arguments.of(OTHER, new FnsHealth(OK, OK, NA, PROXY_OK, OK)),

                Arguments.of(REGISTRATION, new FnsHealth(OK, OK, NO_SLA, PROXY_OK, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(OK, OK, NO_SLA, PROXY_OK, OK)),
                Arguments.of(OTHER, new FnsHealth(OK, OK, NO_SLA, PROXY_OK, OK)),

                Arguments.of(REGISTRATION, new FnsHealth(OK, OK, OK, PROXY_NA, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(OK, OK, OK, PROXY_NA, OK)),
                Arguments.of(OTHER, new FnsHealth(OK, OK, OK, PROXY_NA, OK)),

                Arguments.of(REGISTRATION, new FnsHealth(OK, OK, OK, PROXY_PART, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(OK, OK, OK, PROXY_PART, OK)),
                Arguments.of(OTHER, new FnsHealth(OK, OK, OK, PROXY_PART, OK)),

                Arguments.of(REGISTRATION, new FnsHealth(OK, OK, OK, PROXY_OK, NA)),
                Arguments.of(REGISTRATION, new FnsHealth(OK, OK, OK, PROXY_OK, NO_SLA)),

                Arguments.of(FIAS_CHECK, new FnsHealth(OK, NA, OK, PROXY_OK, OK)),
                Arguments.of(FIAS_CHECK, new FnsHealth(OK, NO_SLA, OK, PROXY_OK, OK)),

                Arguments.of(OTHER, new FnsHealth(OK, NA, OK, PROXY_OK, OK)),
                Arguments.of(OTHER, new FnsHealth(OK, NO_SLA, OK, PROXY_OK, OK)),
                Arguments.of(OTHER, new FnsHealth(OK, OK, OK, PROXY_OK, NA)),
                Arguments.of(OTHER, new FnsHealth(OK, OK, OK, PROXY_OK, NO_SLA)),
                Arguments.of(OTHER, new FnsHealth(OK, NA, OK, PROXY_OK, NA)),
                Arguments.of(OTHER, new FnsHealth(OK, NA, OK, PROXY_OK, NO_SLA)),
                Arguments.of(OTHER, new FnsHealth(OK, NO_SLA, OK, PROXY_OK, NA))
        );
    }

    @ParameterizedTest(name = "{0} - {1}")
    @DisplayName("Verify that HealthManager throws nothing when FNS API is healthy enough")
    @MethodSource("healthyParameters")
    void testHealthy(Feature feature, FnsHealth health) {
        when(fnsClient.getHealth())
                .thenReturn(Mono.just(health));
        when(fnsClient.getMaintenanceSchedule(any()))
                .thenReturn(Mono.just(MaintenanceSchedule.empty()));

        assertThatCode(() -> healthManager.checkHealth(feature).block(BLOCK_TIMEOUT))
                .doesNotThrowAnyException();

        verify(fnsClient, atLeastOnce()).getHealth();
        verify(fnsClient, atLeastOnce()).getMaintenanceSchedule(any());
        verifyNoMoreInteractions(fnsClient);
    }

    private static MaintenanceSchedule onAirEvent(AffectedFeatures features) {
        val start = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                .minus(MAINTENANCE_OFFSET.toNanos(), ChronoUnit.NANOS);
        val end = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                .plus(MAINTENANCE_OFFSET.toNanos(), ChronoUnit.NANOS);
        val events = List.of(new FnsMaintenanceScheduleEvent("0", start, end, "", features, ""));
        return new MaintenanceSchedule(events);
    }

    public static Stream<Arguments> underMaintenanceParameters() {
        return Stream.of(
                Arguments.of(REGISTRATION, onAirEvent(API_UNAVAILABLE)),
                Arguments.of(FIAS_CHECK, onAirEvent(API_UNAVAILABLE)),
                Arguments.of(OTHER, onAirEvent(API_UNAVAILABLE)),

                Arguments.of(REGISTRATION, onAirEvent(SLA_EXCEEDED)),
                Arguments.of(FIAS_CHECK, onAirEvent(SLA_EXCEEDED)),
                Arguments.of(OTHER, onAirEvent(SLA_EXCEEDED)),

                Arguments.of(REGISTRATION, onAirEvent(REGISTRATION_UNAVAILABLE)),

                Arguments.of(REGISTRATION, onAirEvent(FIAS_CHECK_AND_REGISTRATION_UNAVAILABLE)),
                Arguments.of(FIAS_CHECK, onAirEvent(FIAS_CHECK_AND_REGISTRATION_UNAVAILABLE))
        );
    }

    @ParameterizedTest(name = "{0} - {1}")
    @DisplayName("Verify that HealthManager throws an error when FNS API is under maintenance")
    @MethodSource("underMaintenanceParameters")
    void testUnderMaintenance(Feature feature, MaintenanceSchedule maintenanceSchedule) {
        when(fnsClient.getHealth())
                .thenReturn(Mono.just(new FnsHealth(OK, OK, OK, PROXY_OK, OK)));
        when(fnsClient.getMaintenanceSchedule(any()))
                .thenReturn(Mono.just(maintenanceSchedule));

        assertThatThrownBy(() -> healthManager.checkHealth(feature).block(BLOCK_TIMEOUT))
                .isInstanceOfSatisfying(FnsApiUnavailableException.class, e -> {
                    assertThat(e.getExtraHeaders())
                            .containsExactly(
                                    entry(HttpHeaders.RETRY_AFTER, String.valueOf(MAINTENANCE_OFFSET.toSeconds()))
                            );
                });

        verify(fnsClient, atLeastOnce()).getHealth();
        verify(fnsClient, atLeastOnce()).getMaintenanceSchedule(any());
        verifyNoMoreInteractions(fnsClient);
    }

    private static MaintenanceSchedule futureEvent(AffectedFeatures features) {
        val start = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                .plus(MAINTENANCE_OFFSET.toNanos(), ChronoUnit.NANOS)
                .plus(Duration.ofSeconds(1));
        val end = start.plus(Duration.ofHours(1));
        val events = List.of(new FnsMaintenanceScheduleEvent("0", start, end, "", features, ""));
        return new MaintenanceSchedule(events);
    }

    public static Stream<Arguments> noMaintenanceParameters() {
        return Stream.of(
                Arguments.of(REGISTRATION, futureEvent(API_UNAVAILABLE)),
                Arguments.of(FIAS_CHECK, futureEvent(API_UNAVAILABLE)),
                Arguments.of(OTHER, futureEvent(API_UNAVAILABLE)),

                Arguments.of(REGISTRATION, futureEvent(SLA_EXCEEDED)),
                Arguments.of(FIAS_CHECK, futureEvent(SLA_EXCEEDED)),
                Arguments.of(OTHER, futureEvent(SLA_EXCEEDED)),

                Arguments.of(REGISTRATION, futureEvent(REGISTRATION_UNAVAILABLE)),
                Arguments.of(FIAS_CHECK, onAirEvent(REGISTRATION_UNAVAILABLE)),
                Arguments.of(OTHER, onAirEvent(REGISTRATION_UNAVAILABLE)),

                Arguments.of(REGISTRATION, futureEvent(FIAS_CHECK_AND_REGISTRATION_UNAVAILABLE)),
                Arguments.of(FIAS_CHECK, futureEvent(FIAS_CHECK_AND_REGISTRATION_UNAVAILABLE)),
                Arguments.of(OTHER, onAirEvent(FIAS_CHECK_AND_REGISTRATION_UNAVAILABLE))
        );
    }

    @ParameterizedTest(name = "{0} - {1}")
    @DisplayName("Verify that HealthManager throws nothing when FNS API is not under maintenance")
    @MethodSource("noMaintenanceParameters")
    void testNoMaintenance(Feature feature, MaintenanceSchedule maintenanceSchedule) {
        when(fnsClient.getHealth())
                .thenReturn(Mono.just(new FnsHealth(OK, OK, OK, PROXY_OK, OK)));
        when(fnsClient.getMaintenanceSchedule(any()))
                .thenReturn(Mono.just(maintenanceSchedule));

        assertThatCode(() -> healthManager.checkHealth(feature).block(BLOCK_TIMEOUT))
                .doesNotThrowAnyException();

        verify(fnsClient, atLeastOnce()).getHealth();
        verify(fnsClient, atLeastOnce()).getMaintenanceSchedule(any());
        verifyNoMoreInteractions(fnsClient);
    }
}
