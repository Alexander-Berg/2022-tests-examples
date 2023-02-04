package ru.yandex.solomon.alert.dao.ydb.entity;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.dao.codec.AlertCodec;
import ru.yandex.solomon.alert.dao.codec.AlertRecord;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.AlertType;
import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class AlertCodecTest {

    private ObjectMapper mapper;
    private AlertCodec alertCodec;

    @Before
    public void setUp() {
        this.mapper = new ObjectMapper();
        this.alertCodec = new AlertCodec(mapper);
    }

    private AlertRecord randomAlertRecord() throws JsonProcessingException {
        return randomAlertRecord(ThreadLocalRandom.current());
    }

    private AlertRecord randomAlertRecord(ThreadLocalRandom random) throws JsonProcessingException {
        AlertRecord record = new AlertRecord();

        record.projectId = "junk";
        record.id = UUID.randomUUID().toString();
        record.version = random.nextInt(1, 100500);

        record.name = "Expression alert with random " + random.nextInt();
        record.state = random.nextInt(0, AlertState.values().length);
        record.createdAt = System.currentTimeMillis() - random.nextLong(0, 1_000_000);
        record.updatedAt = System.currentTimeMillis() + random.nextLong(0, 1_000_000);
        record.groupByLabels = mapper.writeValueAsString(random.nextBoolean() ? Collections.emptyList() : Collections.singleton("host"));
        record.notificationChannels = mapper.writeValueAsString(random.nextBoolean() ? Collections.emptyList() : Collections.singleton("notify-" + UUID.randomUUID().toString()));
        record.type = random.nextInt(0, 2);
        record.config = "invalid";
        record.createdBy = AlertTestSupport.randomUser(random);
        record.updatedBy = AlertTestSupport.randomUser(random);
        record.annotations = mapper.writeValueAsString(AlertTestSupport.randomAnnotations(random));
        record.delaySeconds = random.nextInt(0, 3600);

        record.description = "Description-" + UUID.randomUUID().toString();

        return record;
    }

    private String dropNewFields(String config) throws IOException {
        ObjectNode object = (ObjectNode) mapper.readTree(config);

        return mapper.writeValueAsString(object.without(List.of("transformations", "predicate_rules", "serviceProviderAnnotations", "severity", "escalations")));
    }

    @Test
    public void fromOldStyleThreshold() throws IOException {
        AlertRecord record = randomAlertRecord();

        record.type = AlertType.THRESHOLD.getNumber();
        record.config = "{\"selectors\":\"{project='solomon', cluster='production', service='alerting', " +
                "host='cluster'}\",\"period_millis\":300000,\"threshold_type\":\"AT_LEAST_ONE\",\"threshold\":0.0," +
                "\"comparison\":\"GT\"}";

        Alert alert = alertCodec.decode(record).toBuilder().build();
        AlertRecord recordCopy = alertCodec.encode(alert);
        Alert clone = alertCodec.decode(recordCopy).toBuilder().build();

        assertThat(alert, equalTo(clone));
        assertThat(mapper.readTree(record.config), not(equalTo(mapper.readTree(recordCopy.config))));
        assertThat(mapper.readTree(record.config), equalTo(mapper.readTree(dropNewFields(recordCopy.config))));
    }

    @Test
    public void toOldStyleThreshold() throws IOException {
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlertWithManyRules();

        AlertRecord record = alertCodec.encode(alert);

        record.config = dropNewFields(record.config);

        ThresholdAlert sliced = (ThresholdAlert) alertCodec.decode(record).toBuilder().build();

        assertThat(alert.getPredicateRule().withTargetStatus(TargetStatus.ALARM), equalTo(sliced.getPredicateRule()));
        assertThat(sliced.getTransformations(), isEmptyString());
    }

    @Test
    public void configuredChannelsAlert() {
        Alert alert = AlertTestSupport.randomAlertWithConfiguredChannels();

        AlertRecord record = alertCodec.encode(alert);
        Alert recovered = alertCodec.decode(record);

        assertThat(recovered.getNotificationChannels(), equalTo(alert.getNotificationChannels()));
    }

    @Test
    public void configuredChannelsAlertNewCodeOldDb() {
        Alert alert = AlertTestSupport.randomAlertWithConfiguredChannels();

        AlertRecord record = alertCodec.encode(alert);
        record.notificationConfig = "";
        Alert recovered = alertCodec.decode(record);

        var sliced = alert.getNotificationChannels().keySet().stream()
                .collect(Collectors.toMap(Function.identity(), (ignore) -> ChannelConfig.EMPTY));
        assertThat(recovered.getNotificationChannels(), equalTo(sliced));
    }

    @Test
    public void notificationConfigIsPreferred() {
        Alert alert = AlertTestSupport.randomAlertWithConfiguredChannels();

        AlertRecord record = alertCodec.encode(alert);
        record.notificationConfig = "{\"channels\":{" +
            "\"alice\":{}," +
            "\"bob\":{\"repeat_notification_delay_millis\":600000,\"notify_about_statuses\":[\"OK\",\"WARN\",\"ALARM\"]}" +
            "}}";
        Alert recovered = alertCodec.decode(record);

        assertThat(recovered.getNotificationChannels().get("alice"), sameInstance(ChannelConfig.EMPTY));
        assertThat(recovered.getNotificationChannels().get("bob"), equalTo(new ChannelConfig(
            Set.of(EvaluationStatus.Code.OK, EvaluationStatus.Code.WARN, EvaluationStatus.Code.ALARM),
            Duration.ofMinutes(10))
        ));
    }

}
