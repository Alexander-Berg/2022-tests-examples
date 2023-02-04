package ru.yandex.solomon.alert.api.validators;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import ru.yandex.solomon.alert.protobuf.AlertFromTemplate;
import ru.yandex.solomon.alert.protobuf.AlertParameter;
import ru.yandex.solomon.alert.protobuf.AlertTemplate;
import ru.yandex.solomon.alert.protobuf.AlertTemplateParameter;
import ru.yandex.solomon.alert.protobuf.CreateAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.CreateAlertsFromTemplateRequest;
import ru.yandex.solomon.alert.protobuf.DeployAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.DoubleTemplateParameter;
import ru.yandex.solomon.alert.protobuf.ECompare;
import ru.yandex.solomon.alert.protobuf.EThresholdType;
import ru.yandex.solomon.alert.protobuf.Expression;
import ru.yandex.solomon.alert.protobuf.IntegerTemplateParameter;
import ru.yandex.solomon.alert.protobuf.LabelListTemplateParameter;
import ru.yandex.solomon.alert.protobuf.NoPointsPolicy;
import ru.yandex.solomon.alert.protobuf.PredicateRule;
import ru.yandex.solomon.alert.protobuf.PublishAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.ReadAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.ResolvedEmptyPolicy;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.alert.protobuf.TCreateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertRequest;
import ru.yandex.solomon.alert.protobuf.TExpression;
import ru.yandex.solomon.alert.protobuf.TNotificationChannelOptions;
import ru.yandex.solomon.alert.protobuf.TPredicateRule;
import ru.yandex.solomon.alert.protobuf.TReadAlertInterpolatedRequest;
import ru.yandex.solomon.alert.protobuf.TReadAlertRequest;
import ru.yandex.solomon.alert.protobuf.TThreshold;
import ru.yandex.solomon.alert.protobuf.TUpdateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TemplateDeployPolicy;
import ru.yandex.solomon.alert.protobuf.TextListTemplateParameter;
import ru.yandex.solomon.alert.protobuf.TextTemplateParameter;
import ru.yandex.solomon.alert.protobuf.Threshold;
import ru.yandex.solomon.alert.protobuf.UpdateAlertTemplateVersionRequest;
import ru.yandex.solomon.model.protobuf.MatchType;
import ru.yandex.solomon.model.protobuf.Selector;
import ru.yandex.solomon.model.protobuf.Selectors;

import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertValidatorTest {

    @Test(expected = ValidationException.class)
    public void createRequestDefaultNotValid() throws Exception {
        ensureNotValid(TCreateAlertRequest.getDefaultInstance());
    }

    @Test(expected = ValidationException.class)
    public void thresholdCreateDefaultNotValid() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setThreshold(TThreshold.getDefaultInstance())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void expressionCreateDefaultNotValid() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setExpression(TExpression.getDefaultInstance())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void thresholdCreateEmptySelectorNotValid() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("My second alert")
                        .setProjectId("test")
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(5))
                        .setThreshold(TThreshold.newBuilder()
                                .setComparison(ECompare.GT)
                                .setThreshold(10)
                                .setThresholdType(EThresholdType.AVG)
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void expressionNotValidProgram() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setName("My first expression alert")
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(5))
                        .setExpression(TExpression.newBuilder()
                                .setProgram(" it's not valid program that failed during parse ast ")
                                .setCheckExpression("avg({project=a, service=b, cluster=c, sensor=d, host=c}) > 10")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void expressionNotValidCheckExpressionResult() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setProjectId("test")
                        .setName("My first expression alert")
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(5))
                        .setExpression(TExpression.newBuilder()
                                .setCheckExpression("let a")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void expressionNotValidCheckExpression() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setProjectId("test")
                        .setName("My expression alert")
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(5))
                        .setExpression(TExpression.newBuilder()
                                .setCheckExpression("it's not valid check expression")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForCreateAlert() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("TestAlert")
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(2))
                        .setExpression(TExpression.newBuilder()
                                .setCheckExpression("avg({sensor='jvm.memory.used', memory='heap'}) >= 1024 * 2")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void expressionRequiredStatusIf() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("TestAlert")
                        .setProjectId("junk")
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(2))
                        .setExpression(TExpression.newBuilder()
                                .setProgram("let data = avg({sensor='jvm.memory.used', memory='heap'});")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void expressionSoleOkIf() throws Exception {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("TestAlert")
                        .setProjectId("junk")
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(2))
                        .setExpression(TExpression.newBuilder()
                                .setProgram("ok_if(avg({sensor='jvm.memory.used', memory='heap'}) < 10);")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForUpdateAlert() throws Exception {
        ensureNotValid(AlertValidator::ensureValid, TUpdateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("TestAlert")
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(2))
                        .setVersion(2)
                        .setExpression(TExpression.newBuilder()
                                .setCheckExpression("avg({sensor='jvm.memory.used', memory=heap}) >= 1024 * 2")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForDeleteAlert() throws Exception {
        ensureNotValid(AlertValidator::ensureValid, TDeleteAlertRequest.newBuilder()
                .setAlertId("TESt")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForReadAlert() throws Exception {
        ensureNotValid(AlertValidator::ensureValid, TReadAlertRequest.newBuilder()
                .setAlertId("TESt")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForReadInterpolatedAlert() throws Exception {
        ensureNotValid(AlertValidator::ensureValid, TReadAlertInterpolatedRequest.newBuilder()
                .setAlertId("TESt")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void idRequiredForDeleteAlert() throws Exception {
        ensureNotValid(AlertValidator::ensureValid, TDeleteAlertRequest.newBuilder()
                .setProjectId("Hello")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void notValidSelectors() {
        ensureNotValid(AlertValidator::ensureValid, TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setProjectId("modadvert")
                        .setId("modadvert_sandbox_scheduler_stat2")
                        .setName("modadvert_sandbox_scheduler_stat")
                        .setPeriodMillis(300000)
                        .addGroupByLabels("sensor")
                        .setThreshold(TThreshold.newBuilder()
                                .setThresholdType(EThresholdType.MIN)
                                .setThreshold(1.0)
                                .setComparison(ECompare.LT)
                                .setNewSelectors(Selectors.newBuilder()
                                        .addLabelSelectors(Selector.newBuilder()
                                                .setKey("project")
                                                .setPattern("modadvert")
                                                .setMatchType(MatchType.GLOB)
                                                .build())
                                        .addLabelSelectors(Selector.newBuilder()
                                                .setKey("cluster")
                                                .setPattern("circuit")
                                                .setMatchType(MatchType.GLOB)
                                                .build())
                                        .addLabelSelectors(Selector.newBuilder()
                                                .setKey("service")
                                                .setPattern("modadvert")
                                                .setMatchType(MatchType.GLOB)
                                                .build())
                                        .addLabelSelectors(Selector.newBuilder()
                                                .setKey("expression")
                                                .setPattern("group_lines\n('min', selector)") // not valid
                                                .setMatchType(MatchType.GLOB)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void noSelectors() {
        ensureNotValid(AlertValidator::ensureValid, TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setProjectId("modadvert")
                        .setId("modadvert_sandbox_scheduler_stat2")
                        .setName("modadvert_sandbox_scheduler_stat")
                        .setPeriodMillis(300000)
                        .setThreshold(TThreshold.newBuilder()
                                .setThresholdType(EThresholdType.MIN)
                                .setThreshold(1.0)
                                .setComparison(ECompare.LT)
                                .build())
                        .build())
                .build());
    }

    @Test
    public void newSelectors() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setProjectId("modadvert")
                        .setId("modadvert_sandbox_scheduler_stat2")
                        .setName("modadvert_sandbox_scheduler_stat")
                        .setPeriodMillis(300000)
                        .setThreshold(TThreshold.newBuilder()
                                .setNewSelectors(Selectors.newBuilder()
                                        .setNameSelector("metric")
                                        .addLabelSelectors(Selector.newBuilder()
                                                .setKey("host")
                                                .setPattern("solomon-01")
                                                .setMatchType(MatchType.EXACT)))
                                .setThresholdType(EThresholdType.MIN)
                                .setThreshold(1.0)
                                .setComparison(ECompare.LT)))
                        .build());
    }

    @Test
    public void newSelectorsNoLabels() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setProjectId("modadvert")
                        .setId("modadvert_sandbox_scheduler_stat2")
                        .setName("modadvert_sandbox_scheduler_stat")
                        .setPeriodMillis(300000)
                        .setThreshold(TThreshold.newBuilder()
                                .setNewSelectors(Selectors.newBuilder().setNameSelector("metric"))
                                .setThresholdType(EThresholdType.MIN)
                                .setThreshold(1.0)
                                .setComparison(ECompare.LT)))
                .build());
    }


    @Test(expected = ValidationException.class)
    public void predicateRuleWithoutTargetStatus() {
        ensureNotValid(AlertValidator::ensureValid, TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setProjectId("modadvert")
                        .setId("modadvert_sandbox_scheduler_stat2")
                        .setName("modadvert_sandbox_scheduler_stat")
                        .setPeriodMillis(300000)
                        .setThreshold(TThreshold.newBuilder()
                                .setNewSelectors(Selectors.newBuilder().setNameSelector("metric"))
                                .addPredicateRules(TPredicateRule.newBuilder()
                                        .setComparison(ECompare.LTE)
                                        .setThresholdType(EThresholdType.MAX)
                                        .setThreshold(42d))))
                .build());
    }

    @Test
    public void onlyPredicateRules() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setProjectId("modadvert")
                        .setId("modadvert_sandbox_scheduler_stat2")
                        .setName("modadvert_sandbox_scheduler_stat")
                        .setPeriodMillis(300000)
                        .setThreshold(TThreshold.newBuilder()
                                .setNewSelectors(Selectors.newBuilder()
                                        .setNameSelector("metric"))
                                .addPredicateRules(TPredicateRule.newBuilder()
                                        .setComparison(ECompare.LTE)
                                        .setThresholdType(EThresholdType.MAX)
                                        .setThreshold(42d)
                                        .setTargetStatus(TPredicateRule.ETargetStatus.WARN))))
                .build());
    }

    @Test
    public void thresholdWithTransform() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
            .setAlert(TAlert.newBuilder()
                .setProjectId("modadvert")
                .setId("modadvert_sandbox_scheduler_stat2")
                .setName("modadvert_sandbox_scheduler_stat")
                .setPeriodMillis(300000)
                .setThreshold(TThreshold.newBuilder()
                    .setNewSelectors(Selectors.newBuilder()
                        .setNameSelector("metric"))
                    .setTransformations("derivative(input{})")
                    .addPredicateRules(TPredicateRule.newBuilder()
                        .setComparison(ECompare.LTE)
                        .setThresholdType(EThresholdType.MAX)
                        .setThreshold(42d)
                        .setTargetStatus(TPredicateRule.ETargetStatus.WARN))))
            .build());
    }

    @Test(expected = ValidationException.class)
    public void thresholdWithBadTransform() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
            .setAlert(TAlert.newBuilder()
                .setProjectId("modadvert")
                .setId("modadvert_sandbox_scheduler_stat2")
                .setName("modadvert_sandbox_scheduler_stat")
                .setPeriodMillis(300000)
                .setThreshold(TThreshold.newBuilder()
                    .setNewSelectors(Selectors.newBuilder()
                        .setNameSelector("metric"))
                    .setTransformations("let result = derivative(input{});")
                    .addPredicateRules(TPredicateRule.newBuilder()
                        .setComparison(ECompare.LTE)
                        .setThresholdType(EThresholdType.MAX)
                        .setThreshold(42d)
                        .setTargetStatus(TPredicateRule.ETargetStatus.WARN))))
            .build());
    }

    @Test(expected = ValidationException.class)
    public void thresholdTransformationsWithSeveralLoads() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
            .setAlert(TAlert.newBuilder()
                .setProjectId("modadvert")
                .setId("modadvert_sandbox_scheduler_stat2")
                .setName("modadvert_sandbox_scheduler_stat")
                .setPeriodMillis(300000)
                .setThreshold(TThreshold.newBuilder()
                    .setNewSelectors(Selectors.newBuilder()
                        .setNameSelector("metric"))
                    .setTransformations("errors{project='foo', cluster='bar', service='baz'} / " +
                                        "requests{project='foo', cluster='bar', service='baz'}")
                    .addPredicateRules(TPredicateRule.newBuilder()
                        .setComparison(ECompare.LTE)
                        .setThresholdType(EThresholdType.MAX)
                        .setThreshold(42d)
                        .setTargetStatus(TPredicateRule.ETargetStatus.WARN))))
            .build());
    }

    @Test(expected = ValidationException.class)
    public void thresholdTransformationsWithoutLoad() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
            .setAlert(TAlert.newBuilder()
                .setProjectId("modadvert")
                .setId("modadvert_sandbox_scheduler_stat2")
                .setName("modadvert_sandbox_scheduler_stat")
                .setPeriodMillis(300000)
                .setThreshold(TThreshold.newBuilder()
                    .setNewSelectors(Selectors.newBuilder()
                        .setNameSelector("metric"))
                    .setTransformations("42")
                    .addPredicateRules(TPredicateRule.newBuilder()
                        .setComparison(ECompare.LTE)
                        .setThresholdType(EThresholdType.MAX)
                        .setThreshold(42d)
                        .setTargetStatus(TPredicateRule.ETargetStatus.WARN))))
            .build());
    }

    @Test(expected = ValidationException.class)
    public void alertDelaySecondsShouldBePositive() {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("TestAlert")
                        .setProjectId("test")
                        .setExpression(TExpression.newBuilder()
                                .setCheckExpression("true")
                                .build())
                        .setPeriodMillis(TimeUnit.MINUTES.toMillis(1))
                        .setDelaySeconds(-42)
                        .build())
                .build());
    }

    @Test
    public void ensureValidExpression() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setExpression(TExpression.newBuilder()
                                .setProgram("let p75 = last(histogram_percentile(75, 'update_lag', {sensor=test}));")
                                .setCheckExpression("p75 > 1000")
                                .build())
                        .setPeriodMillis(10_000)
                        .build())
                .build());
    }

    @Test
    public void ensureEmptyCheckIsOk() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setExpression(TExpression.newBuilder()
                                .setProgram("let p75 = last(histogram_percentile(75, 'update_lag', {sensor=test})); alarm_if(p75 > 1000);")
                                .setCheckExpression("")
                                .build())
                        .setPeriodMillis(10_000)
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void ensureBlankGroupByLabelIsForbidden() {
        AlertValidator.ensureValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setExpression(TExpression.newBuilder()
                                .setProgram("let p75 = last(histogram_percentile(75, 'update_lag', {sensor=test})); alarm_if(p75 > 1000);")
                                .setCheckExpression("")
                                .build())
                        .addGroupByLabels("host")
                        .addGroupByLabels(" ")
                        .setPeriodMillis(10_000)
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void templateCreateDefaultNotValid() {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setAlertFromTemplate(AlertFromTemplate.getDefaultInstance())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void templateCreateNotValid_resolvedEmptyPolicy() {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_MANUAL)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current()))
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void templateCreateNotValid_NoPointsPolicy() {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_OK)
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current()))
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void templateCreateNotValid_GroupByLabels() {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .addAllGroupByLabels(List.of("host"))
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current()))
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void templateCreateNotValid_DelaySeconds() {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .setDelaySeconds(10)
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current()))
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void templateCreateNotValid_PeriodMillis() {
        ensureNotValid(TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .setPeriodMillis(10)
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current()))
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void templateUpdateNotValid_PeriodMillis() {
        ensureNotValid(AlertValidator::ensureValid, TUpdateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .setPeriodMillis(10)
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current()))
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void templateUpdateDefaultNotValid() {
        ensureNotValid(AlertValidator::ensureValid, TUpdateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setAlertFromTemplate(AlertFromTemplate.getDefaultInstance())
                        .build())
                .build());
    }

    @Test
    public void templateCreateValid() {
        AlertValidator.ensureValid((TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .putAllAnnotations(Map.of("a1", "a2"))
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current()))
                        .build())
                .build()));
        AlertValidator.ensureValid((TCreateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current())
                                .toBuilder()
                                .clearAlertParameters()
                                .clearAlertThresholds()
                                .build())
                        .build())
                .build()));
    }

    @Test
    public void templateCreateUpdate() {
        AlertValidator.ensureValid(TUpdateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .putAllAnnotations(Map.of("a1", "a2"))
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current()))
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .build())
                .build());
        AlertValidator.ensureValid(TUpdateAlertRequest.newBuilder()
                .setAlert(TAlert.newBuilder()
                        .setName("test")
                        .setProjectId("myProject")
                        .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                        .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_DEFAULT)
                        .setAlertFromTemplate(randomAlertFromTemplate(ThreadLocalRandom.current())
                                .toBuilder()
                                .clearAlertParameters()
                                .clearAlertThresholds()
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void readTemplateNotValid() {
        ensureNotValid(AlertValidator::ensureValid, ReadAlertTemplateRequest.newBuilder()
                .setTemplateVersionTag("")
                .build());
    }

    @Test
    public void readTemplateValid() {
        AlertValidator.ensureValid(ReadAlertTemplateRequest.newBuilder()
                .setTemplateVersionTag("tag")
                .setTemplateId("id")
                .build());
    }


    @Test(expected = ValidationException.class)
    public void publishTemplateNotValid_Id() {
        ensureNotValid(AlertValidator::ensureValid, PublishAlertTemplateRequest.newBuilder()
                .build());
    }

    @Test(expected = ValidationException.class)
    public void publishTemplateNotValid_Tag() {
        ensureNotValid(AlertValidator::ensureValid, PublishAlertTemplateRequest.newBuilder()
                .setTemplateId("id")
                .build());
    }

    @Test
    public void publishTemplateValid() {
        AlertValidator.ensureValid(PublishAlertTemplateRequest.newBuilder()
                .setTemplateVersionTag("tag")
                .setTemplateId("id")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void deployTemplateNotValid_Id() {
        ensureNotValid(AlertValidator::ensureValid, DeployAlertTemplateRequest.newBuilder()
                .build());
    }

    @Test(expected = ValidationException.class)
    public void deployTemplateNotValid_Tag() {
        ensureNotValid(AlertValidator::ensureValid, DeployAlertTemplateRequest.newBuilder()
                .setTemplateId("id")
                .build());
    }

    @Test
    public void deployTemplateValid() {
        AlertValidator.ensureValid(DeployAlertTemplateRequest.newBuilder()
                .setTemplateVersionTag("tag")
                .setTemplateId("id")
                .setTemplateDeployPolicy(TemplateDeployPolicy.TEMPLATE_DEPLOY_POLICY_AUTO)
                .build());
    }

    @Test(expected = ValidationException.class)
    public void updateTemplateVersionNotValid_Id() {
        ensureNotValid(AlertValidator::ensureValid, UpdateAlertTemplateVersionRequest.newBuilder()
                .build());
    }

    @Test(expected = ValidationException.class)
    public void updateTemplateVersionNotValid_Tag() {
        ensureNotValid(AlertValidator::ensureValid, UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("id")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void updateTemplateVersionNotValid_Project() {
        ensureNotValid(AlertValidator::ensureValid, UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("id")
                .setTemplateVersionTag("id")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void updateTemplateVersionNotValid_Count() {
        ensureNotValid(AlertValidator::ensureValid, UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("id")
                .setTemplateVersionTag("id")
                .setProjectId("project")
                .setUpdateCount(0)
                .build());
    }

    @Test(expected = ValidationException.class)
    public void updateTemplateVersionNotValid_AlertId() {
        ensureNotValid(AlertValidator::ensureValid, UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("id")
                .setTemplateVersionTag("id")
                .setProjectId("project")
                .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder().build())
                .build());
    }

    @Test
    public void updateTemplateVersion() {
        AlertValidator.ensureValid(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("id")
                .setTemplateVersionTag("id")
                .setProjectId("project")
                .setAlertData(UpdateAlertTemplateVersionRequest.AlertData.newBuilder()
                        .setAlertId("alertId")
                        .build())
                .build());
    }

    @Test
    public void createExpressionTemplate() {
        var template = randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        AlertValidator.ensureValid(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template.toBuilder().setId(template.getServiceProviderId() + "-id").build())
                .build());
    }

    @Test
    public void createThresholdTemplate() {
        var template = randomAlertTemplate(AlertTemplate.TypeCase.THRESHOLD);
        AlertValidator.ensureValid(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template.toBuilder().setId(template.getServiceProviderId() + "-id").build())
                .build());
    }

    @Test
    public void updateTemplateVersion_count() {
        AlertValidator.ensureValid(UpdateAlertTemplateVersionRequest.newBuilder()
                .setTemplateId("id")
                .setTemplateVersionTag("id")
                .setProjectId("project")
                .setUpdateCount(10)
                .build());
    }

    @Test(expected = ValidationException.class)
    public void createAlertTemplateRequest_projectId() {
        ensureNotValid(AlertValidator::ensureValid, CreateAlertsFromTemplateRequest.newBuilder()
                .build());
    }

    @Test(expected = ValidationException.class)
    public void createAlertTemplateRequest_spId() {
        ensureNotValid(AlertValidator::ensureValid, CreateAlertsFromTemplateRequest.newBuilder()
                .setProjectId("test")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void createAlertTemplateRequest_templates() {
        ensureNotValid(AlertValidator::ensureValid, CreateAlertsFromTemplateRequest.newBuilder()
                .setProjectId("test")
                .setServiceProviderId("spId")
                .build());
    }

    @Test(expected = ValidationException.class)
    public void createAlertTemplateRequest_channels() {
        ensureNotValid(AlertValidator::ensureValid, CreateAlertsFromTemplateRequest.newBuilder()
                .setProjectId("test")
                .setServiceProviderId("spId")
                .addAllTemplateIds(List.of("1"))
                .build());
    }

    @Test(expected = ValidationException.class)
    public void createAlertTemplateRequest_resources() {
        ensureNotValid(AlertValidator::ensureValid, CreateAlertsFromTemplateRequest.newBuilder()
                .setProjectId("test")
                .setServiceProviderId("spId")
                .addAllTemplateIds(List.of("1"))
                .putAllChannels(Map.of("1", TNotificationChannelOptions.newBuilder().build()))
                .build());
    }

    @Test
    public void createAlertTemplateRequest() {
        AlertValidator.ensureValid(CreateAlertsFromTemplateRequest.newBuilder()
                .setProjectId("test")
                .setServiceProviderId("spId")
                .addAllTemplateIds(List.of("1"))
                .putAllChannels(Map.of("1", TNotificationChannelOptions.newBuilder().build()))
                .addAllResources(List.of(CreateAlertsFromTemplateRequest.Resource.newBuilder()
                        .putAllResourceParameters(Map.of("1", "2"))
                        .build()))
                .build());
    }

    private void ensureNotValid(TCreateAlertRequest request) {
        ensureNotValid(AlertValidator::ensureValid, request);
    }

    private <Req> void ensureNotValid(Consumer<Req> fn, Req request) {
        try {
            fn.accept(request);
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
            throw e;
        }
        fail("Expected that request will be invalid: " + request);
    }

    private AlertFromTemplate randomAlertFromTemplate(ThreadLocalRandom random) {
        return AlertFromTemplate.newBuilder()
                .setTemplateId("id" + random.nextInt())
                .setTemplateVersionTag("idTag" + random.nextInt())
                .addAllAlertThresholds(getThresholds())
                .addAllAlertParameters(getParameters())
                .build();
    }

    private List<AlertParameter> getParameters() {
        return List.of(
                AlertParameter.newBuilder()
                        .setDoubleParameterValue(AlertParameter.DoubleParameterValue.newBuilder()
                                .setName("DoubleParameterValue param")
                                .setValue(2.1)
                                .build())
                        .build(),
                AlertParameter.newBuilder()
                        .setIntegerParameterValue(AlertParameter.IntegerParameterValue.newBuilder()
                                .setName("IntegerParameterValue param")
                                .setValue(22)
                                .build())
                        .build(),
                AlertParameter.newBuilder()
                        .setTextParameterValue(AlertParameter.TextParameterValue.newBuilder()
                                .setName("TextParameterValue param")
                                .setValue("text param")
                                .build())
                        .build(),
                AlertParameter.newBuilder()
                        .setTextListParameterValue(AlertParameter.TextListParameterValue.newBuilder()
                                .setName("TextParameterValues param")
                                .addAllValues(List.of("1", "2", "3"))
                                .build())
                        .build(),
                AlertParameter.newBuilder()
                        .setLabelListParameterValue(AlertParameter.LabelListParameterValue.newBuilder()
                                .setName("LabelParameterValues param")
                                .addAllValues(List.of("1l", "2l", "3L"))
                                .build())
                        .build()
        );
    }

    private List<AlertParameter> getThresholds() {
        return List.of(
                AlertParameter.newBuilder()
                        .setDoubleParameterValue(AlertParameter.DoubleParameterValue.newBuilder()
                                .setName("DoubleParameterValue")
                                .setValue(1.1)
                                .build())
                        .build(),
                AlertParameter.newBuilder()
                        .setIntegerParameterValue(AlertParameter.IntegerParameterValue.newBuilder()
                                .setName("IntegerParameterValue")
                                .setValue(2)
                                .build())
                        .build(),
                AlertParameter.newBuilder()
                        .setTextParameterValue(AlertParameter.TextParameterValue.newBuilder()
                                .setName("TextParameterValue")
                                .setValue("text")
                                .build())
                        .build(),
                AlertParameter.newBuilder()
                        .setTextListParameterValue(AlertParameter.TextListParameterValue.newBuilder()
                                .setName("TextParameterValues")
                                .addAllValues(List.of("1", "2"))
                                .build())
                        .build(),
                AlertParameter.newBuilder()
                        .setLabelListParameterValue(AlertParameter.LabelListParameterValue.newBuilder()
                                .setName("LabelParameterValues")
                                .addAllValues(List.of("1l", "2l"))
                                .build())
                        .build()
        );
    }

    public static AlertTemplate randomAlertTemplate(AlertTemplate.TypeCase type) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        AlertTemplate.Builder builder = AlertTemplate.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTemplateVersionTag(UUID.randomUUID().toString())
                .setServiceProviderId(UUID.randomUUID() + "service_provider")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description with random: " + random.nextInt())
                .setCreatedBy("created by")
                .setModifiedBy("modified by")
                .setPeriodMillis(ThreadLocalRandom.current().nextLong(1000, 1_000_00))
                .setDelaySeconds(ThreadLocalRandom.current().nextInt(0, 100))
                .putAllAnnotations(random.nextBoolean() ? Map.of() : Map.of("key", "host"))
                .putAllLabels(random.nextBoolean() ? Map.of() : Map.of("key2", "host2"))
                .addAllGroupByLabels(random.nextBoolean() ? Collections.emptyList() : Collections.singleton("host"))
                .setNoPointsPolicy(NoPointsPolicy.NO_POINTS_NO_DATA)
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.RESOLVED_EMPTY_DEFAULT)
                .addAllAlertTemplateParameters(getTemplateParameters())
                .addAllAlertTemplateThresholds(getTemplateThresholds());

        switch (type) {
            case EXPRESSION:
                builder.setExpression(randomExpression(random));
                break;
            case THRESHOLD:
                builder.setThreshold(randomThreshold(random));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported alert type: " + type);
        }

        return builder.build();
    }

    public static Threshold randomThreshold(ThreadLocalRandom random) {
        var selectors = "project=solomon, cluster=local, service=test, sensor=idleTime, host=solomon-"
                + random.nextInt(1, 100);

        return Threshold.newBuilder()
                .setSelectors(selectors)
                .setTransformations(random.nextBoolean() ? "" : "derivative")
                .addAllPredicateRules(IntStream.range(0, random.nextInt(2, 5))
                        .mapToObj((ignore) -> randomPredicateRule(random))
                        .collect(Collectors.toList()))
                .build();
    }

    private static PredicateRule randomPredicateRule(ThreadLocalRandom random) {
        return PredicateRule.newBuilder()
                .setThresholdParameterTemplate(random.nextInt() + "")
                .setThreshold(random.nextDouble())
                .setComparison(ECompare.values()[random.nextInt(0, ECompare.values().length - 1)])
                .setThresholdType(EThresholdType.values()[random.nextInt(0, EThresholdType.values().length - 1)])
                .setTargetStatus(TPredicateRule.ETargetStatus.values()[random.nextInt(1, TPredicateRule.ETargetStatus.values().length - 1)])
                .build();
    }

    private static Expression randomExpression(ThreadLocalRandom random) {
        return Expression.newBuilder()
                .setProgram("let rr = random01() < " + random.nextDouble(1, 10) + ";")
                .build();
    }

    private static List<AlertTemplateParameter> getTemplateParameters() {
        return List.of(
                AlertTemplateParameter.newBuilder()
                        .setDoubleParameter(DoubleTemplateParameter.newBuilder()
                                .setName("DoubleParameterValue param")
                                .setTitle("DoubleParameterValue param title")
                                .setDefaultValue(2.1)
                                .build())
                        .build(),
                AlertTemplateParameter.newBuilder()
                        .setDoubleParameter(DoubleTemplateParameter.newBuilder()
                                .setName("DoubleParameterValue param2")
                                .setTitle("DoubleParameterValue param title2")
                                .setDefaultValue(2.12)
                                .build())
                        .build(),
                AlertTemplateParameter.newBuilder()
                        .setIntegerParameter(IntegerTemplateParameter.newBuilder()
                                .setName("IntegerParameterValue param")
                                .setTitle("IntegerParameterValue param title")
                                .setDefaultValue(22)
                                .build())
                        .build(),
                AlertTemplateParameter.newBuilder()
                        .setTextParameter(TextTemplateParameter.newBuilder()
                                .setName("TextParameterValue param")
                                .setTitle("TextParameterValue param title")
                                .setDefaultValue("text param")
                                .build())
                        .build(),
                AlertTemplateParameter.newBuilder()
                        .setTextListParameter(TextListTemplateParameter.newBuilder()
                                .setName("TextParameterValues param")
                                .setTitle("TextParameterValues param title")
                                .addAllDefaultValues(List.of("1", "2", "3"))
                                .build())
                        .build(),
                AlertTemplateParameter.newBuilder()
                        .setLabelListParameter(LabelListTemplateParameter.newBuilder()
                                .setName("LabelParameterValues param")
                                .setTitle("LabelParameterValues param title")
                                .setSelectors("selector")
                                .setLabelKey("key")
                                .addAllDefaultValues(List.of("1l", "2l", "3L"))
                                .setProjectId("prj")
                                .setMultiselectable(true)
                                .build())
                        .build()
        );
    }

    private static List<AlertTemplateParameter> getTemplateThresholds() {
        return List.of(
                AlertTemplateParameter.newBuilder()
                        .setDoubleParameter(DoubleTemplateParameter.newBuilder()
                                .setName("DoubleParameterValue threshold")
                                .setTitle("DoubleParameterValue threshold title")
                                .setDefaultValue(12.1)
                                .build())
                        .build(),
                AlertTemplateParameter.newBuilder()
                        .setDoubleParameter(DoubleTemplateParameter.newBuilder()
                                .setName("DoubleParameterValue threshold2")
                                .setTitle("DoubleParameterValue threshold title2")
                                .setDefaultValue(12.12)
                                .build())
                        .build(),
                AlertTemplateParameter.newBuilder()
                        .setIntegerParameter(IntegerTemplateParameter.newBuilder()
                                .setName("IntegerParameterValue threshold")
                                .setTitle("IntegerParameterValue threshold title")
                                .setDefaultValue(122)
                                .build())
                        .build()
        );
    }
}
