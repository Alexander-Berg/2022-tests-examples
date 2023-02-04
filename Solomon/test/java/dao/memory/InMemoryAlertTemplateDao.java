package ru.yandex.solomon.alert.dao.memory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.solomon.alert.dao.AlertTemplateDao;
import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.domain.NoPointsPolicy;
import ru.yandex.solomon.alert.domain.ResolvedEmptyPolicy;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.template.domain.AlertTemplate;
import ru.yandex.solomon.alert.template.domain.AlertTemplateId;
import ru.yandex.solomon.alert.template.domain.AlertTemplateLastVersion;
import ru.yandex.solomon.alert.template.domain.expression.ExpressionAlertTemplate;
import ru.yandex.solomon.alert.template.domain.threshold.TemplatePredicateRule;
import ru.yandex.solomon.alert.template.domain.threshold.ThresholdAlertTemplate;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.ydb.page.TokenBasePage;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * @author Alexey Trushkin
 */
public class InMemoryAlertTemplateDao implements AlertTemplateDao {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final ConcurrentMap<AlertTemplateId, AlertTemplate> templateById = new ConcurrentHashMap<>();
    private final boolean createIfNotFound;
    private final boolean noGroupLabels;

    public InMemoryAlertTemplateDao(boolean createIfNotFound) {
        this.createIfNotFound = createIfNotFound;
        this.noGroupLabels = false;
    }

    public InMemoryAlertTemplateDao(boolean createIfNotFound, boolean noGroupLabels) {
        this.createIfNotFound = createIfNotFound;
        this.noGroupLabels = noGroupLabels;
    }

    @Override
    public CompletableFuture<Void> createSchemaForTests() {
        return runAsync(() -> {
        });
    }

    @Override
    public CompletableFuture<Void> dropSchemaForTests() {
        return runAsync(templateById::clear);
    }

    @Override
    public CompletableFuture<Boolean> create(AlertTemplate alertTemplate) {
        return supplyAsync(() -> templateById.putIfAbsent(alertTemplate.getCompositeId(), alertTemplate) == null);
    }

    @Override
    public CompletableFuture<Optional<AlertTemplate>> findById(String id, String templateVersionTag) {
        var id1 = new AlertTemplateId(id, templateVersionTag);
        return supplyAsync(() -> {
            if (templateById.containsKey(id1)) {
                return Optional.ofNullable(templateById.get(id1));
            }
            if (!createIfNotFound) {
                return Optional.empty();
            }
            var template = template(id, templateVersionTag);
            templateById.put(template.getCompositeId(), template);
            return Optional.ofNullable(template);
        });
    }

    @Override
    public CompletableFuture<List<AlertTemplate>> getAll() {
        return CompletableFuture.supplyAsync(() -> List.copyOf(templateById.values()));
    }

    @Override
    public CompletableFuture<List<AlertTemplate>> findVersions(List<AlertTemplateLastVersion> items) {
        return supplyAsync(() -> {
            var result = new ArrayList<AlertTemplate>();
            items.forEach(alertTemplateLastVersion -> {
                var id = new AlertTemplateId(alertTemplateLastVersion.id(), alertTemplateLastVersion.templateVersionTag());
                AlertTemplate template = templateById.get(id);
                if (template != null) {
                    result.add(template);
                }
            });
            return result;
        });
    }

    @Override
    public CompletableFuture<TokenBasePage<AlertTemplate>> listTemplateVersions(String templateId, int pageSize, String pageToken) {
        return CompletableFuture.supplyAsync(() -> {
            var list = templateById.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(entry -> (templateId.isEmpty() || entry.getValue().getId().equals(templateId)))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            return toPageResult(list, pageSize, pageToken);
        });
    }

    @Override
    public CompletableFuture<TokenBasePage<AlertTemplate>> listLastTemplates(String serviceProviderId, String name, String labelsSelector, int pageSize, String pageToken, Predicate<AlertTemplate> skip) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> values = new HashSet<>();
            var selectors = labelsSelector.isEmpty() ?  Selectors.of() : Selectors.parse(labelsSelector);
            var list = templateById.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(entry -> labelsSelector.isEmpty() || selectors.match(entry.getValue().getLabels()))
                    .filter(entry -> (serviceProviderId.isEmpty() || entry.getValue().getServiceProviderId().equals(serviceProviderId)) &&
                            (name.isEmpty() || entry.getValue().getName().equals(name)))
                    .filter(entry -> (!skip.test(entry.getValue())) && values.add(entry.getValue().getId()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            return toPageResult(list, pageSize, pageToken);
        });
    }

    @Override
    public CompletableFuture<Boolean> publish(AlertTemplate newAlertTemplate, int version) {
        return create(newAlertTemplate);
    }

    private AlertTemplate template(String id, String templateVersionTag) {
        if (id.endsWith("true")) {
            return expression(id, templateVersionTag);
        } else {
            return threshold(id, templateVersionTag);
        }
    }

    private AlertTemplate threshold(String id, String templateVersionTag) {
        return ThresholdAlertTemplate.newBuilder()
                .setId(id)
                .setTemplateVersionTag(templateVersionTag)
                .setServiceProviderId("SERVICE_PROVIDER")
                .setName("name")
                .setDescription("descr")
                .setUpdatedBy("user")
                .setUpdatedAt(Instant.now())
                .setCreatedBy("user2")
                .setCreatedAt(Instant.now())
                .setTransformations(IntStream.range(0, random.nextInt(2, 5))
                        .mapToObj((ignore) -> randomTransformation(random))
                        .collect(Collectors.joining(".")))
                .setPredicateRules(IntStream.range(0, random.nextInt(1, 5)).mapToObj((ignore) -> randomRule(random)))
                .setPeriodMillis((int) random.nextLong(1000, 1_000_00))
                .setDelaySeconds(random.nextInt(1, 1000))
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=solomon-" + random.nextInt(1, 100))
                .setGroupByLabels(random.nextBoolean() || noGroupLabels ? Collections.emptyList() : Collections.singleton("host"))
                .setAnnotations(randomAnnotations(random))
                .setLabels(Map.of("label1", "value2"))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.MANUAL)
                .setNoPointsPolicy(NoPointsPolicy.ALARM)
                .setAlertSeverity(AlertSeverity.DISASTER)
                .setParameters(Collections.emptyList())
                .setThresholds(Collections.emptyList())
                .build();
    }

    private static TemplatePredicateRule randomRule(ThreadLocalRandom random) {
        return TemplatePredicateRule.onThreshold(random.nextDouble())
                .withThresholdType(ThresholdType.values()[random.nextInt(0, ThresholdType.values().length)])
                .withComparison(Compare.values()[random.nextInt(0, Compare.values().length)])
                .withTargetStatus(TargetStatus.values()[random.nextInt(0, TargetStatus.values().length)]);
    }

    public static String randomTransformation(ThreadLocalRandom random) {
        switch (random.nextInt(5)) {
            case 0:
                return "derivative";
            case 1:
                return "intergrate";
            case 2:
                return "percentille(95)";
            case 3:
                return "group_lines";
            default:
                return RandomStringUtils.randomAlphanumeric(8);
        }
    }

    public static Map<String, String> randomAnnotations(ThreadLocalRandom random) {
        if (random.nextBoolean()) {
            return ImmutableMap.of();
        } else {
            return ImmutableMap.of("summary", "{{#isAlarm}}Everything broken!{{/isAlarm}}{{#isOk}}Good night!{{/isOk}}");
        }
    }

    private AlertTemplate expression(String id, String templateVersionTag) {
        return ExpressionAlertTemplate.newBuilder()
                .setId(id)
                .setTemplateVersionTag(templateVersionTag)
                .setServiceProviderId("SERVICE_PROVIDER")
                .setName("name")
                .setDescription("descr")
                .setUpdatedBy("user")
                .setUpdatedAt(Instant.now())
                .setCreatedBy("user2")
                .setCreatedAt(Instant.now())
                .setPeriodMillis((int) random.nextLong(1000, 1_000_00))
                .setDelaySeconds(random.nextInt(1, 1000))
                .setProgram("let rr = random01() < " + random.nextDouble(1, 10) + ";")
                .setGroupByLabels(random.nextBoolean() || noGroupLabels ? Collections.emptyList() : Collections.singleton("host"))
                .setAnnotations(randomAnnotations(random))
                .setLabels(Map.of("label1", "value2"))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.MANUAL)
                .setNoPointsPolicy(NoPointsPolicy.ALARM)
                .setAlertSeverity(AlertSeverity.DISASTER)
                .setParameters(Collections.emptyList())
                .setThresholds(Collections.emptyList())
                .build();
    }

    private TokenBasePage<AlertTemplate> toPageResult(List<AlertTemplate> matched, int pageSize, String pageToken) {
        if (pageSize <= 0) {
            pageSize = 100;
        } else {
            pageSize = Math.min(pageSize, 1000);
        }

        int offset = pageToken.isEmpty() ? 0 : Integer.parseInt(pageToken);
        int nextOffset = Math.min(offset + pageSize, matched.size());
        var list = matched.subList(offset, nextOffset);
        if (nextOffset >= matched.size()) {
            return new TokenBasePage<>(list, "");
        }
        return new TokenBasePage<>(list, Integer.toString(nextOffset));
    }
}
