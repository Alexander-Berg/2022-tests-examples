package ru.yandex.solomon.alert.dao;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.junit.Test;

import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.domain.NoPointsPolicy;
import ru.yandex.solomon.alert.domain.ResolvedEmptyPolicy;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.template.domain.AlertTemplate;
import ru.yandex.solomon.alert.template.domain.AlertTemplateId;
import ru.yandex.solomon.alert.template.domain.AlertTemplateLastVersion;
import ru.yandex.solomon.alert.template.domain.AlertTemplateParameter;
import ru.yandex.solomon.alert.template.domain.expression.ExpressionAlertTemplate;
import ru.yandex.solomon.alert.template.domain.threshold.TemplatePredicateRule;
import ru.yandex.solomon.alert.template.domain.threshold.ThresholdAlertTemplate;
import ru.yandex.solomon.ydb.page.TokenBasePage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.misc.concurrent.CompletableFutures.join;

/**
 * @author Alexey Trushkin
 */
public abstract class AlertTemplateDaoTest {

    private static final String ALERT_ID = "alert_id";
    private static final String EXPRESSION = "expression";
    private static final String THRESHOLD = "threshold";
    private static final String SERVICE_PROVIDER = "service provider id";

    protected abstract AlertTemplateDao getDao();

    protected abstract AlertTemplateLastVersionDao getVersionsDao();

    @Test
    public void create_expression() {
        AlertTemplate template = expressionTemplate();
        assertTrue(createSync(template));
    }

    @Test
    public void find_expression() {
        AlertTemplate template = expressionTemplate();
        assertTrue(createSync(template));

        Optional<AlertTemplate> sync = findSync(ALERT_ID, EXPRESSION + "TAG");
        assertTrue(sync.isPresent());
        assertFalse(findSync(ALERT_ID, EXPRESSION).isPresent());

        assertTrue(template.equalContent(sync.get()));
    }

    @Test
    public void create_threshold() {
        AlertTemplate template = thresholdTemplate();
        assertTrue(createSync(template));
    }

    @Test
    public void create_publishedNew() {
        AlertTemplate template = expressionTemplate();
        assertTrue(createPublishedSync(template, -1));

        var version = getVersionsDao().findById(template.getId()).join();
        assertTrue(version.isPresent());
        assertEquals(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""), version.get());

        Optional<AlertTemplate> sync = findSync(ALERT_ID, EXPRESSION + "TAG");
        assertTrue(sync.isPresent());
        assertTrue(template.equalContent(sync.get()));
    }

    @Test
    public void create_publishedUpdate() {
        AlertTemplate template = expressionTemplate();
        assertTrue(createPublishedSync(template, -1));
        var version = getVersionsDao().findById(template.getId()).join();
        assertEquals(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""), version.get());

        assertTrue(createPublishedSync(template.toBuilder().setTemplateVersionTag("another1").build(), 0));
        version = getVersionsDao().findById(template.getId()).join();
        assertEquals(new AlertTemplateLastVersion(template.getId(), "another1", template.getServiceProviderId(), template.getName(), 1, ""), version.get());
        Optional<AlertTemplate> sync = findSync(ALERT_ID, EXPRESSION + "TAG");
        assertTrue(sync.isPresent());
        sync = findSync(ALERT_ID, "another1");
        assertTrue(sync.isPresent());

        assertTrue(createPublishedSync(template.toBuilder().setTemplateVersionTag("another2").build(), 1));
        version = getVersionsDao().findById(template.getId()).join();
        assertEquals(new AlertTemplateLastVersion(template.getId(), "another2", template.getServiceProviderId(), template.getName(), 2, ""), version.get());
        sync = findSync(ALERT_ID, EXPRESSION + "TAG");
        assertTrue(sync.isPresent());
        sync = findSync(ALERT_ID, "another1");
        assertTrue(sync.isPresent());
        sync = findSync(ALERT_ID, "another2");
        assertTrue(sync.isPresent());
        assertTrue(template.toBuilder().setTemplateVersionTag("another2").build().equalContent(sync.get()));
    }

    @Test
    public void publishedUpdate() {
        AlertTemplate template = expressionTemplate();
        createSync(template);
        getVersionsDao().create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, "123")).join();

        assertTrue(createPublishedSync(template.toBuilder().setTemplateVersionTag("another1").build(), 0));
        var version = getVersionsDao().findById(template.getId()).join();
        assertEquals(new AlertTemplateLastVersion(template.getId(), "another1", template.getServiceProviderId(), template.getName(), 1, ""), version.get());
    }

    @Test
    public void create_publishedUpdate_failed() {
        AlertTemplate template = expressionTemplate();
        assertTrue(createPublishedSync(template, -1));
        var version = getVersionsDao().findById(template.getId()).join();
        assertEquals(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""), version.get());
        try {
            assertTrue(createPublishedSync(template.toBuilder().setTemplateVersionTag("another1").build(), 1));
        } catch (CompletionException ignore) {
        }
        version = getVersionsDao().findById(template.getId()).join();
        assertEquals(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""), version.get());
        Optional<AlertTemplate> sync = findSync(ALERT_ID, EXPRESSION + "TAG");
        assertTrue(sync.isPresent());
        sync = findSync(ALERT_ID, "another1");
        assertTrue(sync.isEmpty());
    }

    @Test
    public void create_publishedNew_failed() {
        AlertTemplate template = expressionTemplate();
        getVersionsDao().create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 11, "")).join();

        try {
            assertTrue(createPublishedSync(template, -1));
        } catch (CompletionException ignore) {
        }

        var version = getVersionsDao().findById(template.getId()).join();
        assertTrue(version.isPresent());
        assertEquals(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 11, ""), version.get());

        Optional<AlertTemplate> sync = findSync(ALERT_ID, EXPRESSION + "TAG");
        assertTrue(sync.isEmpty());
    }

    @Test
    public void find_threshold() {
        AlertTemplate template = thresholdTemplate();
        assertTrue(createSync(template));

        Optional<AlertTemplate> sync = findSync(ALERT_ID, THRESHOLD + "TAG");
        assertTrue(sync.isPresent());
        assertFalse(findSync(ALERT_ID, THRESHOLD).isPresent());

        assertTrue(template.equalContent(sync.get()));
    }

    @Test
    public void findLatest() {
        var start = System.currentTimeMillis();
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setTemplateVersionTag(THRESHOLD + "TAG1")
                .setCreatedAt(Instant.ofEpochMilli(start))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setTemplateVersionTag(THRESHOLD + "TAG2")
                .setCreatedAt(Instant.ofEpochMilli(start + 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setTemplateVersionTag(THRESHOLD + "TAG3")
                .setCreatedAt(Instant.ofEpochMilli(start - 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id1")
                .setTemplateVersionTag(THRESHOLD + "TAG2")
                .setCreatedAt(Instant.ofEpochMilli(start - 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id1")
                .setTemplateVersionTag(THRESHOLD + "TAG1")
                .setCreatedAt(Instant.ofEpochMilli(start + 100000))
                .build()));

        var result = findSync(List.of(
                new AlertTemplateLastVersion(ALERT_ID, THRESHOLD + "TAG2", "", "", 0, "taskId"),
                new AlertTemplateLastVersion("id1", THRESHOLD + "TAG1", "", "", 0, "taskId2")
        ));
        assertEquals(2, result.size());
        assertNotNull(result.stream().filter(alertTemplate -> alertTemplate.getId().equals(ALERT_ID) && alertTemplate.getTemplateVersionTag().equals(THRESHOLD + "TAG2")).findFirst().get());
        assertNotNull(result.stream().filter(alertTemplate -> alertTemplate.getId().equals("id1") && alertTemplate.getTemplateVersionTag().equals(THRESHOLD + "TAG1")).findFirst().get());
    }

    @Test
    public void listTemplateVersions() {
        var start = System.currentTimeMillis();
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setTemplateVersionTag(THRESHOLD + "TAG1")
                .setCreatedAt(Instant.ofEpochMilli(start))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setTemplateVersionTag(THRESHOLD + "TAG2")
                .setCreatedAt(Instant.ofEpochMilli(start + 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setTemplateVersionTag(THRESHOLD + "TAG3")
                .setCreatedAt(Instant.ofEpochMilli(start + 200000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id1")
                .setTemplateVersionTag(THRESHOLD + "TAG2")
                .setCreatedAt(Instant.ofEpochMilli(start - 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id1")
                .setTemplateVersionTag(THRESHOLD + "TAG1")
                .setCreatedAt(Instant.ofEpochMilli(start + 100000))
                .build()));

        var result = listTemplateVersionsSync(ALERT_ID, 2, "");
        assertEquals(2, result.getItems().size());
        assertEquals("2", result.getNextPageToken());
        assertNotNull(result.getItems().stream().filter(alertTemplate -> alertTemplate.getId().equals(ALERT_ID) && alertTemplate.getTemplateVersionTag().equals(THRESHOLD + "TAG1")).findFirst().get());
        assertNotNull(result.getItems().stream().filter(alertTemplate -> alertTemplate.getId().equals(ALERT_ID) && alertTemplate.getTemplateVersionTag().equals(THRESHOLD + "TAG2")).findFirst().get());

        result = listTemplateVersionsSync(ALERT_ID, 2, result.getNextPageToken());
        assertEquals(1, result.getItems().size());
        assertEquals("", result.getNextPageToken());
        assertNotNull(result.getItems().stream().filter(alertTemplate -> alertTemplate.getId().equals(ALERT_ID) && alertTemplate.getTemplateVersionTag().equals(THRESHOLD + "TAG3")).findFirst().get());

    }

    @Test
    public void listTemplates() {
        var start = System.currentTimeMillis();
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setTemplateVersionTag(THRESHOLD + "TAG1")
                .setCreatedAt(Instant.ofEpochMilli(start + 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setTemplateVersionTag(THRESHOLD + "TAG2")
                .setCreatedAt(Instant.ofEpochMilli(start))
                .build()));

        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id")
                .setTemplateVersionTag(THRESHOLD + "TAG1")
                .setName("a")
                .setCreatedAt(Instant.ofEpochMilli(start + 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id")
                .setTemplateVersionTag(THRESHOLD + "TAG2")
                .setName("a")
                .setCreatedAt(Instant.ofEpochMilli(start))
                .build()));

        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id2")
                .setTemplateVersionTag(THRESHOLD + "TAG1")
                .setName("b")
                .setCreatedAt(Instant.ofEpochMilli(start))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id2")
                .setTemplateVersionTag(THRESHOLD + "TAG2")
                .setName("b")
                .setCreatedAt(Instant.ofEpochMilli(start + 100000))
                .build()));

        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id1")
                .setTemplateVersionTag(THRESHOLD + "TAG2")
                .setServiceProviderId("abcd")
                .setCreatedAt(Instant.ofEpochMilli(start - 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id1")
                .setServiceProviderId("abc")
                .setName("ab")
                .setTemplateVersionTag(THRESHOLD + "TAG1")
                .setCreatedAt(Instant.ofEpochMilli(start + 100000))
                .build()));
        assertTrue(createSync(thresholdTemplate().toBuilder()
                .setId("id1")
                .setServiceProviderId("abc")
                .setName("abc")
                .setTemplateVersionTag(THRESHOLD + "TAG3")
                .setCreatedAt(Instant.ofEpochMilli(start))
                .addLabel("label2", "value2")
                .build()));

        // select single
        var result = join(getDao().listLastTemplates("abc", "", "", 10, "", alertTemplate -> false));
        assertEquals(1, result.getItems().size());
        assertEquals("id1", result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG1", result.getItems().get(0).getTemplateVersionTag());
        assertEquals("", result.getNextPageToken());

        result = join(getDao().listLastTemplates("abc", "", "label1='value1'", 10, "", alertTemplate -> false));
        assertEquals(1, result.getItems().size());
        assertEquals("id1", result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG1", result.getItems().get(0).getTemplateVersionTag());
        assertEquals("", result.getNextPageToken());

        // select by name
        result = join(getDao().listLastTemplates("abc", "abc", "",10, "", alertTemplate -> false));
        assertEquals(1, result.getItems().size());
        assertEquals("id1", result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG3", result.getItems().get(0).getTemplateVersionTag());
        assertEquals("", result.getNextPageToken());

        // select multiple
        result = join(getDao().listLastTemplates(SERVICE_PROVIDER, "", "", 10, "", alertTemplate -> false));
        assertEquals(3, result.getItems().size());
        assertEquals("id", result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG1", result.getItems().get(0).getTemplateVersionTag());
        assertEquals("id2", result.getItems().get(1).getId());
        assertEquals(THRESHOLD + "TAG2", result.getItems().get(1).getTemplateVersionTag());
        assertEquals(ALERT_ID, result.getItems().get(2).getId());
        assertEquals(THRESHOLD + "TAG1", result.getItems().get(2).getTemplateVersionTag());
        assertEquals("", result.getNextPageToken());

        // select by labels
        result = join(getDao().listLastTemplates("abc", "", "label1='value1', label2='value2'", 10, "", alertTemplate -> false));
        assertEquals(1, result.getItems().size());
        assertEquals("id1", result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG3", result.getItems().get(0).getTemplateVersionTag());
        assertEquals("", result.getNextPageToken());

        result = join(getDao().listLastTemplates("abc", "", "label1='value1', label3='value2'", 10, "", alertTemplate -> false));
        assertEquals(0, result.getItems().size());

        result = join(getDao().listLastTemplates("abc", "", "label1='value1', label2='value3'", 10, "", alertTemplate -> false));
        assertEquals(0, result.getItems().size());

        // select with skip published
        var ids = Set.of(
                new AlertTemplateId("id", THRESHOLD + "TAG1"),
                new AlertTemplateId("id", THRESHOLD + "TAG2"),
                new AlertTemplateId("id2", THRESHOLD + "TAG2"),
                new AlertTemplateId(ALERT_ID, THRESHOLD + "TAG1"));
        result = join(getDao().listLastTemplates(SERVICE_PROVIDER, "", "",10, "", alertTemplate -> ids.contains(alertTemplate.getCompositeId())));
        assertEquals(2, result.getItems().size());
        assertEquals("id2", result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG1", result.getItems().get(0).getTemplateVersionTag());
        assertEquals(ALERT_ID, result.getItems().get(1).getId());
        assertEquals(THRESHOLD + "TAG2", result.getItems().get(1).getTemplateVersionTag());
        assertEquals("", result.getNextPageToken());

        result = join(getDao().listLastTemplates(SERVICE_PROVIDER, "", "label1='value1'",10, "", alertTemplate -> ids.contains(alertTemplate.getCompositeId())));
        assertEquals(2, result.getItems().size());
        assertEquals("id2", result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG1", result.getItems().get(0).getTemplateVersionTag());
        assertEquals(ALERT_ID, result.getItems().get(1).getId());
        assertEquals(THRESHOLD + "TAG2", result.getItems().get(1).getTemplateVersionTag());
        assertEquals("", result.getNextPageToken());

        // select pages
        result = join(getDao().listLastTemplates(SERVICE_PROVIDER, "", "",2, "", alertTemplate -> false));
        assertEquals(2, result.getItems().size());
        assertEquals("id", result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG1", result.getItems().get(0).getTemplateVersionTag());
        assertEquals("id2", result.getItems().get(1).getId());
        assertEquals(THRESHOLD + "TAG2", result.getItems().get(1).getTemplateVersionTag());
        assertEquals("2", result.getNextPageToken());

        result = join(getDao().listLastTemplates(SERVICE_PROVIDER, "", "",2, "2", alertTemplate -> false));
        assertEquals(1, result.getItems().size());
        assertEquals(ALERT_ID, result.getItems().get(0).getId());
        assertEquals(THRESHOLD + "TAG1", result.getItems().get(0).getTemplateVersionTag());
    }

    private boolean createSync(AlertTemplate alertTemplate) {
        return join(getDao().create(alertTemplate));
    }

    private boolean createPublishedSync(AlertTemplate alertTemplate, int version) {
        return join(getDao().publish(alertTemplate, version));
    }

    private Optional<AlertTemplate> findSync(String id, String templateVersionTag) {
        return join(getDao().findById(id, templateVersionTag));
    }

    private List<AlertTemplate> findSync(List<AlertTemplateLastVersion> versions) {
        return join(getDao().findVersions(versions));
    }

    private TokenBasePage<AlertTemplate> listTemplateVersionsSync(String id, int pageSize, String pageToken) {
        return join(getDao().listTemplateVersions(id, pageSize, pageToken));
    }

    public static AlertTemplate expressionTemplate() {
        return ExpressionAlertTemplate.newBuilder()
                .setId(ALERT_ID)
                .setTemplateVersionTag(EXPRESSION + "TAG")
                .setServiceProviderId(SERVICE_PROVIDER)
                .setName(EXPRESSION + "name")
                .setDescription(EXPRESSION + "descr")
                .setUpdatedBy("user")
                .setUpdatedAt(Instant.now())
                .setCreatedBy("user2")
                .setCreatedAt(Instant.now())
                .setPeriodMillis(1000)
                .setDelaySeconds(1)
                .setProgram("let rr = random01() < 1;")
                .setGroupByLabels(Collections.singleton("host"))
                .setAnnotations(Map.of("a1", "v1"))
                .setLabels(Map.of("label1", "value2"))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.MANUAL)
                .setNoPointsPolicy(NoPointsPolicy.ALARM)
                .setAlertSeverity(AlertSeverity.CRITICAL)
                .setParameters(getParameters())
                .setThresholds(getThresholds())
                .build();
    }

    public static ThresholdAlertTemplate thresholdTemplate() {
        return ThresholdAlertTemplate.newBuilder()
                .setId(ALERT_ID)
                .setTemplateVersionTag(THRESHOLD + "TAG")
                .setServiceProviderId(SERVICE_PROVIDER)
                .setName(THRESHOLD + "name")
                .setDescription(THRESHOLD + "descr")
                .setUpdatedBy("user")
                .setUpdatedAt(Instant.now())
                .setCreatedBy("user2")
                .setCreatedAt(Instant.now())
                .setPeriodMillis(1000)
                .setDelaySeconds(1)
                .setPredicateRule(randomAlarmRule())
                .setPeriodMillis(1000)
                .setDelaySeconds(1)
                .setTransformations("AVG")
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=solomon-1")
                .setUpdatedBy("user")
                .setUpdatedAt(Instant.now())
                .setCreatedBy("user2")
                .setCreatedAt(Instant.now())
                .setGroupByLabels(Collections.singleton("host"))
                .setAnnotations(Map.of("a1", "v1"))
                .setLabels(Map.of("label1", "value1"))
                .setParameters(getParameters())
                .setThresholds(getThresholds())
                .setAlertSeverity(AlertSeverity.CRITICAL)
                .setDefaultTemplate(true)
                .build();
    }


    private static List<AlertTemplateParameter> getThresholds() {
        return List.of(
                new AlertTemplateParameter.TextParameterValue("text", "name", "title", "descr")
        );
    }

    private static List<AlertTemplateParameter> getParameters() {
        return List.of(
                new AlertTemplateParameter.TextListParameterValue(List.of("t1", "t2"), "p3", "t3", "descr"),
                new AlertTemplateParameter.LabelListParameterValue(
                        "selector",
                        "label",
                        List.of("v1"),
                        "p4",
                        "t4",
                        "descr",
                        "",
                        false
                )
        );
    }

    private static TemplatePredicateRule randomAlarmRule() {
        return TemplatePredicateRule.onThreshold(0.9)
                .withThresholdType(ThresholdType.AVG)
                .withComparison(Compare.GT)
                .onThresholdParameter("name");
    }

}
