package ru.yandex.qe.dispenser.standalone;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ru.yandex.qe.dispenser.domain.tracker.TrackerManager;
import ru.yandex.qe.dispenser.quartz.trigger.QuartzTrackerComment;
import ru.yandex.startrek.client.model.CommentCreate;

@ParametersAreNonnullByDefault
public class MockQuartzTrackerCommentTrigger implements QuartzTrackerComment {

    private final TrackerManager trackerManager;

    public MockQuartzTrackerCommentTrigger(TrackerManager trackerManager) {
        this.trackerManager = trackerManager;
    }

    @Override
    public boolean run(@NotNull String issueKey, @NotNull String comment, @NotNull String summoned) {
        try {
            CommentCreate.Builder commentBuilder = CommentCreate.comment(comment);
            if (!StringUtils.isEmpty(summoned)) {
                commentBuilder = commentBuilder.summonees(summoned);
            }
            trackerManager.createComment(issueKey, commentBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean run(@NotNull String issueKey, @NotNull String comment) {
        try {
            trackerManager.createComment(issueKey, CommentCreate.comment(comment).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
