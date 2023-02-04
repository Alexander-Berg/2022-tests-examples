package ru.yandex.solomon.alert.tasks;

import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.Nullable;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.alerting.api.task.PublishAlertTemplateParams;
import ru.yandex.alerting.api.task.PublishAlertTemplateProgress;
import ru.yandex.solomon.alert.client.AlertApi;
import ru.yandex.solomon.alert.client.stub.AlertApiStub;
import ru.yandex.solomon.alert.dao.ProjectsHolderStub;
import ru.yandex.solomon.scheduler.ExecutionContext;
import ru.yandex.solomon.scheduler.ExecutionContextStub;
import ru.yandex.solomon.scheduler.Permit;
import ru.yandex.solomon.scheduler.Task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Alexey Trushkin
 */
public class PublishAlertTemplateTaskHandlerTest {

    private static final String TEMPLATE_ID = "id1";
    private static final String TEMPLATE_VERSION_TAG = "version";
    private static final String PROJECT = "id";
    private static final String PROJECT_FAIL = "fail";
    private PublishAlertTemplateTaskHandler handler;
    private AlertApi alertApi;
    private ProjectsHolderStub projectsHolder;

    @Before
    public void setUp() throws Exception {
        alertApi = new AlertApiStub();
        projectsHolder = new ProjectsHolderStub();
        handler = new PublishAlertTemplateTaskHandler(alertApi, projectsHolder, ForkJoinPool.commonPool());
    }

    @Test
    public void absentPermitConfNotLoaded() {
        assertNull(acquire(randomParams()));
    }


    @Test
    public void receivePermit() {
        projectsHolder.addProject(PROJECT);
        assertNotNull(acquire(randomParams()));
    }

    @Test
    public void exceptionRescheduled() {
        projectsHolder.addProject(PROJECT_FAIL);
        var params = randomParams();
        assertNotNull(acquire(params));

        var context = context(params, PublishAlertTemplateProgress.getDefaultInstance());
        execute(context);

        var reschedule = context.takeDoneEvent(ExecutionContextStub.Reschedule.class);
        assertNotNull(reschedule);
    }

    @Test
    public void success() {
        projectsHolder.addProject(PROJECT + 1);
        projectsHolder.addProject(PROJECT + 11);
        projectsHolder.addProject(PROJECT);
        var params = randomParams();
        assertNotNull(acquire(params));
        var context = context(params, PublishAlertTemplateProgress.getDefaultInstance());
        execute(context);
        var progressEvent = context.takeEvent(ExecutionContextStub.Progress.class);
        var progress = PublishAlertTemplateProto.progress(progressEvent.progress());
        assertEquals(PROJECT, progress.getLastProjectId());
        assertEquals(PROJECT.length(), progress.getUpdatedAlerts());
        assertEquals(3, progress.getTotalProjects());
        assertEquals(1, progress.getUpdatedProjects());

        progressEvent = context.takeEvent(ExecutionContextStub.Progress.class);
        progress = PublishAlertTemplateProto.progress(progressEvent.progress());
        assertEquals(PROJECT + 1, progress.getLastProjectId());
        assertEquals((PROJECT + 1).length() + PROJECT.length(), progress.getUpdatedAlerts());
        assertEquals(3, progress.getTotalProjects());
        assertEquals(2, progress.getUpdatedProjects());

        progressEvent = context.takeEvent(ExecutionContextStub.Progress.class);
        progress = PublishAlertTemplateProto.progress(progressEvent.progress());
        assertEquals(PROJECT + 11, progress.getLastProjectId());
        assertEquals((PROJECT + 11).length() + (PROJECT + 1).length() + PROJECT.length(), progress.getUpdatedAlerts());
        assertEquals(3, progress.getTotalProjects());
        assertEquals(3, progress.getUpdatedProjects());

        var completeEvent = context.takeDoneEvent(ExecutionContextStub.Complete.class);
        var result = PublishAlertTemplateProto.result(completeEvent.result());
        assertEquals((PROJECT + 11).length() + (PROJECT + 1).length() + PROJECT.length(), result.getUpdatedAlerts());
    }

    @Test
    public void successResume() {
        projectsHolder.addProject(PROJECT + 1);
        projectsHolder.addProject(PROJECT + 22);
        projectsHolder.addProject(PROJECT);
        var params = randomParams();
        assertNotNull(acquire(params));
        var context = context(params, PublishAlertTemplateProgress.newBuilder()
                .setLastProjectId(PROJECT + 11)
                .setUpdatedProjects(2)
                .setUpdatedAlerts((PROJECT + 1).length() + PROJECT.length())
                .build());
        execute(context);
        var progressEvent = context.takeEvent(ExecutionContextStub.Progress.class);
        var progress = PublishAlertTemplateProto.progress(progressEvent.progress());
        assertEquals(PROJECT + 22, progress.getLastProjectId());
        assertEquals((PROJECT + 22).length() + (PROJECT + 1).length() + PROJECT.length(), progress.getUpdatedAlerts());
        assertEquals(3, progress.getTotalProjects());
        assertEquals(3, progress.getUpdatedProjects());

        var completeEvent = context.takeDoneEvent(ExecutionContextStub.Complete.class);
        var result = PublishAlertTemplateProto.result(completeEvent.result());
        assertEquals((PROJECT + 22).length() + (PROJECT + 1).length() + PROJECT.length(), result.getUpdatedAlerts());
    }

    @Test
    public void successResume_allDone() {
        projectsHolder.addProject(PROJECT + 1);
        projectsHolder.addProject(PROJECT + 22);
        projectsHolder.addProject(PROJECT);
        var params = randomParams();
        assertNotNull(acquire(params));
        var context = context(params, PublishAlertTemplateProgress.newBuilder()
                .setLastProjectId(PROJECT + 22)
                .setUpdatedProjects(3)
                .setUpdatedAlerts((PROJECT + 1).length() + PROJECT.length() + (PROJECT + 22).length())
                .build());
        execute(context);

        var completeEvent = context.takeDoneEvent(ExecutionContextStub.Complete.class);
        var result = PublishAlertTemplateProto.result(completeEvent.result());
        assertEquals((PROJECT + 22).length() + (PROJECT + 1).length() + PROJECT.length(), result.getUpdatedAlerts());
    }

    private PublishAlertTemplateParams randomParams() {
        return PublishAlertTemplateParams.newBuilder()
                .setTemplateId(TEMPLATE_ID)
                .setTemplateVersionTag(TEMPLATE_VERSION_TAG)
                .build();
    }

    @Nullable
    private Permit acquire(PublishAlertTemplateParams params) {
        return handler.acquire(UUID.randomUUID().toString(), Any.pack(params));
    }

    private ExecutionContextStub context(PublishAlertTemplateParams params, PublishAlertTemplateProgress build) {
        var task = Task.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType("random")
                .setExecuteAt(System.currentTimeMillis())
                .setParams(Any.pack(params))
                .setProgress(Any.pack(build))
                .build();

        return new ExecutionContextStub(task);
    }

    private void execute(ExecutionContext context) {
        handler.execute(context);
    }
}
