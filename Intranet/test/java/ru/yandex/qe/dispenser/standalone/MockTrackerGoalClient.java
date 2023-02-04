package ru.yandex.qe.dispenser.standalone;

import java.util.List;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.qe.dispenser.ws.goal.GoalIssue;
import ru.yandex.qe.dispenser.ws.goal.TrackerGoalClient;
import ru.yandex.startrek.client.model.IssueRef;

public class MockTrackerGoalClient implements TrackerGoalClient {
    private ListF<GoalIssue> issues = Cf.arrayList();

    @Override
    public IteratorF<GoalIssue> findGoalIssues() {
        return issues.iterator();
    }

    public void addIssue(final GoalIssue issue) {
        issues.add(issue);
    }

    public void reset() {
        issues.clear();
    }

    public void setIssues(final List<GoalIssue> issues) {
        this.issues.clear();
        this.issues.addAll(issues);
    }

    public static class MockIssueRef extends IssueRef {

        public MockIssueRef(final String key) {
            super(null, null, key, null, null);
        }
    }
}
