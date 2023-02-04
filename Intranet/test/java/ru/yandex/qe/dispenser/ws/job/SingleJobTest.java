package ru.yandex.qe.dispenser.ws.job;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.qe.dispenser.quartz.job.TicketUpdateJob;
import ru.yandex.qe.dispenser.quartz.trigger.QuartzTrackerComment;
import ru.yandex.qe.dispenser.quartz.trigger.TrackerJobRunner;
import ru.yandex.qe.dispenser.standalone.MockTrackerManager;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.LinkCreate;
import ru.yandex.startrek.client.model.Relationship;

import static ru.yandex.qe.dispenser.quartz.trigger.QuartzTrackerCommentTrigger.TRIGGER_GROUP;

public class SingleJobTest extends AbstractJobTest {

    @Inject
    private MockTrackerManager mockTrackerManager;
    @Inject
    QuartzTrackerComment quartzTrackerCommentTrigger;
    @Inject
    private TrackerJobRunner trackerJobRunner;
    @Qualifier("clusteredSingleTaskScheduler")
    @Inject
    private Scheduler clusteredSingleTaskScheduler;

    protected SingleJobTest() {
        super(new String[]{"development", "sqldao"});
    }

    @BeforeEach
    public void beforeMethod() throws SchedulerException {
        clusteredSingleTaskScheduler.standby();
        clusteredSingleTaskScheduler.clear();
    }

    @AfterEach
    public void afterTest() throws SchedulerException {
        clusteredSingleTaskScheduler.standby();
        clusteredSingleTaskScheduler.clear();
    }

    @Test
    public void trackerCommentTriggerTest() throws SchedulerException, InterruptedException, ExecutionException, TimeoutException {
        mockTrackerManager.clearIssues();

        final String issueKey = mockTrackerManager
                .createIssues(IssueCreate.builder()
                        .assignee(AMOSOV_F.getLogin())
                        .build()
                );
        final String comment = "test Comment";

        quartzTrackerCommentTrigger.run(issueKey, comment);

        final Set<TriggerKey> triggerKeys = clusteredSingleTaskScheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(TRIGGER_GROUP));
        final Trigger trigger = clusteredSingleTaskScheduler.getTrigger(triggerKeys.stream().findFirst().get());

        clusteredSingleTaskScheduler.start();
        waitForTrigger(clusteredSingleTaskScheduler, trigger, 1);

        final List<CommentCreate> comments = mockTrackerManager.getIssueComments(issueKey);
        Assertions.assertEquals(1, comments.size());
        Assertions.assertEquals(comment, comments.get(0).getComment().get());
    }

    @Test
    public void issueUpdateTest() throws SchedulerException, InterruptedException, ExecutionException, TimeoutException {
        mockTrackerManager.clearIssues();

        final String issueKey = mockTrackerManager
                .createIssues(IssueCreate.builder()
                        .summary("old")
                        .assignee(AMOSOV_F.getLogin())
                        .build()
                );

        trackerJobRunner.scheduleUpdateIssue(issueKey, null, IssueUpdate
                .summary("new")
                .assignee("starlight")
                .removeDescription()
                .components(2L, 13L, 7L)
                .tags(Cf.list("one"), Cf.list("two"))
                .add("custom1", "val1", "val2")
                .remove("custom2", "val3", "val4")
                .comment(CommentCreate.comment("comment").summonees("sereglond", "keyd").build())
                .link(LinkCreate.local().issue("DISPENSER-2").relationship(Relationship.DUPLICATES).build())
                .build());

        final Set<TriggerKey> triggerKeys = clusteredSingleTaskScheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(TicketUpdateJob.TRIGGER_GROUP));
        SimpleTrigger trigger = (SimpleTrigger) clusteredSingleTaskScheduler.getTrigger(triggerKeys.stream().findFirst().get());
        Assertions.assertEquals(0, trigger.getTimesTriggered());

        clusteredSingleTaskScheduler.start();
        mockTrackerManager.setTrackerAvailable(false);
        try {
            waitForTrigger(clusteredSingleTaskScheduler, trigger, 1);
        } finally {
            mockTrackerManager.setTrackerAvailable(true);
        }
        Assertions.assertEquals("old", mockTrackerManager.getIssue(issueKey).getSummary());


        trigger = (SimpleTrigger) clusteredSingleTaskScheduler.getTrigger(trigger.getKey());
        Assertions.assertEquals(1, trigger.getTimesTriggered());

        clusteredSingleTaskScheduler.triggerJob(trigger.getJobKey(), trigger.getJobDataMap());
        waitForTrigger(clusteredSingleTaskScheduler, trigger, 1);

        Issue issue = mockTrackerManager.getIssue(issueKey);
        Assertions.assertEquals("new", issue.getSummary());
        Map<String, Object> issueFields = mockTrackerManager.getIssueFields(issueKey);
        Assertions.assertEquals("starlight", issueFields.get("assignee"));
        Assertions.assertEquals(Arrays.asList(2, 13, 7), issueFields.get("components"));
        Assertions.assertEquals(Arrays.asList("one"), Arrays.asList(((Object[]) issueFields.get("tags"))));
        Assertions.assertEquals(Arrays.asList("val1", "val2"), Arrays.asList(((Object[]) issueFields.get("custom1"))));
        Assertions.assertEquals(Arrays.asList(), Arrays.asList(((Object[]) issueFields.get("custom2"))));
        final CommentCreate commentCreate = mockTrackerManager.getIssueComments(issueKey).iterator().next();
        Assertions.assertEquals("comment", commentCreate.getComment().get());
        Assertions.assertEquals(Cf.list("sereglond", "keyd"), commentCreate.getSummonees());

        trackerJobRunner.scheduleUpdateIssue(issueKey, "closed", IssueUpdate.builder().build());
        waitForTrigger(clusteredSingleTaskScheduler, trigger, 1);

        issue = mockTrackerManager.getIssue(issueKey);
        issueFields = mockTrackerManager.getIssueFields(issueKey);
        Assertions.assertEquals(issueFields.get("status"), "closed");
    }
}
