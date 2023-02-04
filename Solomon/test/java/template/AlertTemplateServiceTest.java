package ru.yandex.solomon.alert.template;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.util.Timestamps;
import io.grpc.Status;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.solomon.alert.api.validators.AlertValidatorTest;
import ru.yandex.solomon.alert.dao.ydb.YdbAlertTemplateDao;
import ru.yandex.solomon.alert.dao.ydb.YdbAlertTemplateLastVersionDao;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.alert.protobuf.AlertTemplate;
import ru.yandex.solomon.alert.protobuf.CreateAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.DeleteAlertTemplatePublicationRequest;
import ru.yandex.solomon.alert.protobuf.DeployAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.ListAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.ListAlertTemplateVersionsRequest;
import ru.yandex.solomon.alert.protobuf.PublishAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.ReadAlertTemplateRequest;
import ru.yandex.solomon.alert.protobuf.ReadAlertTemplateResponse;
import ru.yandex.solomon.alert.protobuf.TemplateDeployPolicy;
import ru.yandex.solomon.alert.template.domain.AlertTemplateLastVersion;
import ru.yandex.solomon.alert.template.domain.expression.ExpressionAlertTemplate;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;
import ru.yandex.solomon.scheduler.ProgressOperator;
import ru.yandex.solomon.scheduler.Task;
import ru.yandex.solomon.scheduler.TaskScheduler;
import ru.yandex.solomon.ut.ManualClock;

import static ru.yandex.solomon.alert.protobuf.AlertTemplateStatus.ALERT_TEMPLATE_STATUS_DRAFT;
import static ru.yandex.solomon.alert.protobuf.AlertTemplateStatus.ALERT_TEMPLATE_STATUS_PUBLISHED;

/**
 * @author Alexey Trushkin
 */
public class AlertTemplateServiceTest {

    private AlertTemplateService alertTemplateService;
    private TaskScheduler scheduler;
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private YdbAlertTemplateLastVersionDao versionDao;
    private YdbAlertTemplateDao dao;

    @Before
    public void setUp() throws Exception {
        var mapper = new ObjectMapper();
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        dao = new YdbAlertTemplateDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT, mapper);
        dao.createSchemaForTests().join();

        versionDao = new YdbAlertTemplateLastVersionDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT);
        versionDao.createSchemaForTests().join();
        alertTemplateService = new AlertTemplateServiceImpl(
                dao,
                versionDao,
                scheduler = new Scheduler(),
                new ManualClock()
        );
    }

    @Test
    public void create() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        var response = alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();

        var result = dao.findById(template.getId(), template.getTemplateVersionTag()).join();
        Assert.assertEquals(true, result.isPresent());
        Assert.assertEquals(ALERT_TEMPLATE_STATUS_DRAFT, response.getAlertTemplate().getAlertTemplateStatus());
    }

    @Test(expected = CompletionException.class)
    public void create_failed() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        dao.create(ExpressionAlertTemplate.newBuilder()
                .setId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .setServiceProviderId("service provider")
                .build()).join();

        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
    }

    @Test
    public void read() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();

        ReadAlertTemplateResponse join = alertTemplateService.read(ReadAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .build()).join();

        Assert.assertEquals(template.getId(), join.getAlertTemplate().getId());
        Assert.assertEquals(template.getTemplateVersionTag(), join.getAlertTemplate().getTemplateVersionTag());
        Assert.assertEquals(ALERT_TEMPLATE_STATUS_DRAFT, join.getAlertTemplate().getAlertTemplateStatus());
    }

    @Test(expected = CompletionException.class)
    public void read_failed() {
        ReadAlertTemplateResponse join = alertTemplateService.read(ReadAlertTemplateRequest.newBuilder()
                .setTemplateId("id")
                .setTemplateVersionTag("tag")
                .build()).join();
    }

    @Test
    public void read_statuses() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template.toBuilder().setTemplateVersionTag("tag2"))
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), "", "", 0, "")).join();

        ReadAlertTemplateResponse response = alertTemplateService.read(ReadAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .build()).join();
        Assert.assertEquals(ALERT_TEMPLATE_STATUS_PUBLISHED, response.getAlertTemplate().getAlertTemplateStatus());
        response = alertTemplateService.read(ReadAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag("tag2")
                .build()).join();
        Assert.assertEquals(ALERT_TEMPLATE_STATUS_DRAFT, response.getAlertTemplate().getAlertTemplateStatus());
        response = alertTemplateService.read(ReadAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .build()).join();
        Assert.assertEquals(ALERT_TEMPLATE_STATUS_PUBLISHED, response.getAlertTemplate().getAlertTemplateStatus());
    }

    @Test
    public void read_last() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template.toBuilder().setTemplateVersionTag("tag2"))
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), "", "", 0, "")).join();

        ReadAlertTemplateResponse join = alertTemplateService.read(ReadAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .build()).join();

        Assert.assertEquals(template.getId(), join.getAlertTemplate().getId());
        Assert.assertEquals(template.getTemplateVersionTag(), join.getAlertTemplate().getTemplateVersionTag());
        Assert.assertEquals(ALERT_TEMPLATE_STATUS_PUBLISHED, join.getAlertTemplate().getAlertTemplateStatus());
    }

    @Test(expected = CompletionException.class)
    public void read_last_failed() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        ReadAlertTemplateResponse join = alertTemplateService.read(ReadAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .build()).join();
    }

    @Test(expected = CompletionException.class)
    public void publish_failed() {
        var join = alertTemplateService.publish(PublishAlertTemplateRequest.newBuilder()
                .setTemplateId("id")
                .build()).join();
    }

    @Test
    public void publish_new() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        var join = alertTemplateService.publish(PublishAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .build()).join();

        var version = versionDao.findById(template.getId()).join();

        Assert.assertEquals(template.getId(), join.getAlertTemplate().getId());
        Assert.assertEquals(template.getTemplateVersionTag(), join.getAlertTemplate().getTemplateVersionTag());
        Assert.assertEquals(version.get(), new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, ""));
        Assert.assertEquals(ALERT_TEMPLATE_STATUS_PUBLISHED, join.getAlertTemplate().getAlertTemplateStatus());
    }

    @Test
    public void publish_existed() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, "task")).join();

        var join = alertTemplateService.publish(PublishAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .build()).join();

        var version = versionDao.findById(template.getId()).join();

        Assert.assertEquals(template.getId(), join.getAlertTemplate().getId());
        Assert.assertEquals(template.getTemplateVersionTag(), join.getAlertTemplate().getTemplateVersionTag());
        Assert.assertEquals(version.get(), new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 1, ""));
        Assert.assertEquals(ALERT_TEMPLATE_STATUS_PUBLISHED, join.getAlertTemplate().getAlertTemplateStatus());
    }

    @Test(expected = CompletionException.class)
    public void publish_existed_failed_already_publishing() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, "fail")).join();

        var join = alertTemplateService.publish(PublishAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .build()).join();
    }

    @Test
    public void list() {
        long start = System.currentTimeMillis();
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION).toBuilder()
                .setCreatedAt(Timestamps.fromMillis(start))
                .clearLabels()
                .putAllLabels(Map.of("type", "1"))
                .build();
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template.toBuilder()
                        .putLabels("type2", "2")
                        .setTemplateVersionTag("tag2").setCreatedAt(Timestamps.fromMillis(start + 100))
                        .build())
                .build()).join();
        var template2 = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.THRESHOLD).toBuilder()
                .setServiceProviderId(template.getServiceProviderId())
                .clearLabels()
                .putLabels("type2", "2")
                .putLabels("type", "1")
                .build();
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template2)
                .build()).join();

        var list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .build()).join();
        Assert.assertEquals(0, list.getAlertTemplatesList().size());

        versionDao.create(new AlertTemplateLastVersion(template2.getId(), template2.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, "")).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, "")).join();

        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .build()).join();
        Assert.assertEquals(2, list.getAlertTemplatesList().size());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template2.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template2.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());

        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .setLabelsSelector("type='1'")
                .build()).join();
        Assert.assertEquals(2, list.getAlertTemplatesList().size());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template2.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template2.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());

        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .addAlertTemplateStatusesFilter(ALERT_TEMPLATE_STATUS_PUBLISHED)
                .build()).join();
        Assert.assertEquals(2, list.getAlertTemplatesList().size());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template2.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template2.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());

        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .setServiceProviderId(template.getServiceProviderId())
                .addAlertTemplateStatusesFilter(ALERT_TEMPLATE_STATUS_DRAFT)
                .build()).join();
        Assert.assertEquals(1, list.getAlertTemplatesList().size());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template.getId()) &&
                alertTemplate.getTemplateVersionTag().equals("tag2") && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_DRAFT)).count());

        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .setServiceProviderId(template.getServiceProviderId())
                .addAlertTemplateStatusesFilter(ALERT_TEMPLATE_STATUS_DRAFT)
                .addAlertTemplateStatusesFilter(ALERT_TEMPLATE_STATUS_PUBLISHED)
                .build()).join();
        Assert.assertEquals(2, list.getAlertTemplatesList().size());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template.getId()) &&
                alertTemplate.getTemplateVersionTag().equals("tag2") && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_DRAFT)).count());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template2.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template2.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());

        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .addAlertTemplateStatusesFilter(ALERT_TEMPLATE_STATUS_PUBLISHED)
                .setLabelsSelector("type2='2'")
                .build()).join();
        Assert.assertEquals(1, list.getAlertTemplatesList().size());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template2.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template2.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());


        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .addAlertTemplateStatusesFilter(ALERT_TEMPLATE_STATUS_PUBLISHED)
                .setLabelsSelector("type2='3'")
                .build()).join();
        Assert.assertEquals(0, list.getAlertTemplatesList().size());

        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .addAlertTemplateStatusesFilter(ALERT_TEMPLATE_STATUS_PUBLISHED)
                .setLabelsSelector("type2='2', type='11'")
                .build()).join();
        Assert.assertEquals(0, list.getAlertTemplatesList().size());

        list = alertTemplateService.list(ListAlertTemplateRequest.newBuilder()
                .addAlertTemplateStatusesFilter(ALERT_TEMPLATE_STATUS_PUBLISHED)
                .setLabelsSelector("type2='2', type='1'")
                .build()).join();
        Assert.assertEquals(1, list.getAlertTemplatesList().size());
        Assert.assertEquals(1, list.getAlertTemplatesList().stream().filter(alertTemplate -> alertTemplate.getId().equals(template2.getId()) &&
                alertTemplate.getTemplateVersionTag().equals(template2.getTemplateVersionTag()) && alertTemplate.getAlertTemplateStatus().equals(ALERT_TEMPLATE_STATUS_PUBLISHED)).count());
    }

    @Test
    public void deploy() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, "task")).join();

        var join = alertTemplateService.deploy(DeployAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .setTemplateDeployPolicy(TemplateDeployPolicy.TEMPLATE_DEPLOY_POLICY_MANUAL)
                .build()).join();

        join = alertTemplateService.deploy(DeployAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .setTemplateDeployPolicy(TemplateDeployPolicy.TEMPLATE_DEPLOY_POLICY_AUTO)
                .build()).join();

        var version = versionDao.findById(template.getId()).join();
        Optional<Task> taskOptional = scheduler.getTask(version.get().publishingTaskId()).join();
        Assert.assertEquals(true, taskOptional.isPresent());
        Assert.assertNotEquals("task", version.get().publishingTaskId());
    }

    @Test(expected = CompletionException.class)
    public void deploy_existed_failed() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), template.getServiceProviderId(), template.getName(), 0, "fail")).join();

        alertTemplateService.deploy(DeployAlertTemplateRequest.newBuilder()
                .setTemplateId(template.getId())
                .setTemplateVersionTag(template.getTemplateVersionTag())
                .setTemplateDeployPolicy(TemplateDeployPolicy.TEMPLATE_DEPLOY_POLICY_AUTO)
                .build()).join();
    }

    @Test(expected = CompletionException.class)
    public void unpublish_failed_noTemplate() {
        var join = alertTemplateService.unpublish(DeleteAlertTemplatePublicationRequest.newBuilder()
                .setTemplateId("id")
                .build()).join();
    }

    @Test(expected = CompletionException.class)
    public void unpublish_failed_noPublishedTemplate() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        var join = alertTemplateService.unpublish(DeleteAlertTemplatePublicationRequest.newBuilder()
                .setTemplateId(template.getId())
                .build()).join();
    }

    @Test(expected = CompletionException.class)
    public void unpublish_failed_DeployTask() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), "", "", 0, "fail")).join();

        var join = alertTemplateService.unpublish(DeleteAlertTemplatePublicationRequest.newBuilder()
                .setTemplateId(template.getId())
                .build()).join();
    }

    @Test
    public void unpublish() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), "", "", 0, "task old id")).join();

        alertTemplateService.unpublish(DeleteAlertTemplatePublicationRequest.newBuilder()
                .setTemplateId(template.getId())
                .build()).join();

        Optional<AlertTemplateLastVersion> join = versionDao.findById(template.getId()).join();
        Assert.assertTrue(join.isEmpty());
    }

    @Test
    public void listTemplateVersions() {
        var template = AlertValidatorTest.randomAlertTemplate(AlertTemplate.TypeCase.EXPRESSION);
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template)
                .build()).join();
        alertTemplateService.create(CreateAlertTemplateRequest.newBuilder()
                .setAlertTemplate(template.toBuilder().setTemplateVersionTag("tag2"))
                .build()).join();
        versionDao.create(new AlertTemplateLastVersion(template.getId(), template.getTemplateVersionTag(), "", "", 0, "")).join();

        var response = alertTemplateService.listTemplateVersions(ListAlertTemplateVersionsRequest.newBuilder()
                .setTemplateId(template.getId())
                .setPageSize(10000)
                .build()).join();
        Assert.assertEquals(2, response.getAlertTemplatesList().size());
    }

    private class Scheduler implements TaskScheduler {
        private ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Optional<Task>> getTask(String taskId) {
            if (taskId.equals("fail")) {
                return CompletableFuture.completedFuture(
                        Optional.of(new Task(taskId, "", Any.newBuilder().build(), 1, Task.State.RUNNING, Any.newBuilder().build(), Status.OK, Any.newBuilder().build(), 0))
                );
            }
            return CompletableFuture.completedFuture(Optional.ofNullable(tasks.get(taskId)));
        }

        @Override
        public CompletableFuture<Void> schedule(Task task) {
            tasks.put(task.id(), task);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> reschedule(String taskId, long executeAt, ProgressOperator progressOperator) {
            throw new UnsupportedOperationException();
        }
    }
}
