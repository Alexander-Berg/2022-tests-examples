package ru.yandex.solomon.alert.cluster.broker.notification;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.protobuf.Severity;
import ru.yandex.solomon.alert.protobuf.notification.TNotification;
import ru.yandex.solomon.name.resolver.client.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexey Trushkin
 */
@ParametersAreNonnullByDefault
public class ChannelValidationServiceTest {

    private ChannelValidationService service;

    @Before
    public void setUp() throws Exception {
        service = new ChannelValidationService();
    }

    @Test
    public void validateSeverities() {
        var result = service.validateSeverities(List.of(
                TNotification.newBuilder()
                        .setId("1")
                        .setName("My notification channel")
                        .addAllDefaultForAlertSeverity(List.of(Severity.SEVERITY_CRITICAL, Severity.SEVERITY_DISASTER, Severity.SEVERITY_INFO))
                        .build(),
                TNotification.newBuilder()
                        .setId("2")
                        .setName("My notification channel")
                        .addAllDefaultForAlertSeverity(List.of(Severity.SEVERITY_CRITICAL, Severity.SEVERITY_DISASTER))
                        .build(),
                TNotification.newBuilder()
                        .setId("3")
                        .setName("My notification channel")
                        .addAllDefaultForAlertSeverity(List.of(Severity.SEVERITY_CRITICAL))
                        .build(),
                TNotification.newBuilder()
                        .setId("4")
                        .setName("My notification channel")
                        .build()
        )).join();
        assertEquals(new ChannelValidationService.Result(Map.of(
                "1", List.of(
                        new ChannelValidationService.ResultRow(AlertSeverity.CRITICAL, "no valid escalation", false),
                        new ChannelValidationService.ResultRow(AlertSeverity.DISASTER, "no valid escalation", false),
                        new ChannelValidationService.ResultRow(AlertSeverity.INFO, "", true)
                ),
                "2", List.of(
                        new ChannelValidationService.ResultRow(AlertSeverity.CRITICAL, "no valid escalation", false),
                        new ChannelValidationService.ResultRow(AlertSeverity.DISASTER, "no valid escalation", false)
                ),
                "3", List.of(
                        new ChannelValidationService.ResultRow(AlertSeverity.CRITICAL, "no valid escalation", false)
                ),
                "4", List.of()
        )), result);
    }

    @Test
    public void validateChannelsForSeverity() {
        assertFalse(service.validateChannelsForSeverity(List.of(), AlertSeverity.CRITICAL, Resource.Severity.CRITICAL).join());
        assertTrue(service.validateChannelsForSeverity(List.of("1"), AlertSeverity.INFO, Resource.Severity.CRITICAL).join());
        assertTrue(service.validateChannelsForSeverity(List.of("1"), AlertSeverity.CRITICAL, Resource.Severity.NON_CRITICAL).join());
        assertFalse(service.validateChannelsForSeverity(List.of("1"), AlertSeverity.CRITICAL, Resource.Severity.CRITICAL).join());
        assertFalse(service.validateChannelsForSeverity(List.of("1"), AlertSeverity.DISASTER, Resource.Severity.CRITICAL).join());
        assertFalse(service.validateChannelsForSeverity(List.of("1"), AlertSeverity.DISASTER, Resource.Severity.UNKNOWN).join());
        assertFalse(service.validateChannelsForSeverity(List.of("1"), AlertSeverity.DISASTER, Resource.Severity.HIGHLY_CRITICAL).join());
        assertTrue(service.validateChannelsForSeverity(List.of("1"), AlertSeverity.INFO, Resource.Severity.HIGHLY_CRITICAL).join());
        assertTrue(service.validateChannelsForSeverity(List.of("1"), AlertSeverity.INFO, Resource.Severity.HIGHLY_CRITICAL).join());
    }

}
