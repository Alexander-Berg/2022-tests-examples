package ru.yandex.solomon.alert.notification.channel.cloud.email;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.notification.channel.cloud.dto.NotifyDto;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class NotifyDtoTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
    }

    @Test
    public void serialize() throws JsonProcessingException {
        var email = AccessBlocked.create();
        email.receiver = "gordiychuk@yandex-team.ru";
        email.data.reason = "hi";
        var str = mapper.writeValueAsString(email);
        assertEquals("{\"type\":\"access-blocked\",\"receiver\":\"gordiychuk@yandex-team.ru\",\"data\":{\"reason\":\"hi\"}}", str);
    }

    private static class AccessBlocked {
        public String reason;

        public static NotifyDto<AccessBlocked> create() {
            return new NotifyDto<>("access-blocked", new AccessBlocked());
        }
    }

    @Test
    public void omitNulls() throws JsonProcessingException {
        var email = new NotifyDto<>("monitoring-alert-triggered", new CloudEmailNotificationChannel.Payload());
        email.iamUserId = "bfbijd6293co2t5mh6r9";
        email.data.alertStatus = "ALARM";
        email.data.alertName = "vlgo";
        email.data.alertDescription = "";
        email.data.alertId = "vlgo";
        email.data.cloudId = "junk";
        email.data.folderId = "";
        email.data.evaluationDate = 1577714406237L;
        email.data.since = 1577714406237L;
        email.data.latestEval = 1577714406237L;
        email.data.annotations = Map.of();
        email.data.serviceProviderAnnotations = Map.of();
        email.data.thresholdAlertGraph = null;

        var str = mapper.writeValueAsString(email);
        assertThat(str, containsString("\"annotations\""));
        assertThat(str, containsString("\"serviceProviderAnnotations\""));
        assertThat(str, not(containsString("\"thresholdAlertGraph\"")));
    }
}
