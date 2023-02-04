package ru.yandex.qe.dispenser.standalone;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterables;
import org.apache.http.client.ClientProtocolException;
import org.joda.time.LocalDate;

import ru.yandex.inside.goals.GoalsService;
import ru.yandex.inside.goals.model.Goal;
import ru.yandex.inside.goals.model.GoalLog;
import ru.yandex.inside.goals.model.ListResponse;

public class MockGoalsService implements GoalsService {

    private List<Goal> goals = Collections.emptyList();

    public void setGoals(final List<Goal> goals) {
        this.goals = goals;
    }

    @Override
    public GoalsRequestBuilder getGoalsBuilder() {
        return new MockGoalsRequestBuilder();
    }

    @Override
    public ListResponse<Goal> getGoals(final int pageNum, final int perPage) throws ClientProtocolException {
        return null;
    }

    @Override
    public List<GoalLog> getGoalLog(final int id) throws ClientProtocolException {
        return null;
    }

    @Override
    public Goal parseGoal(final String json) throws IOException {
        return null;
    }

    public class MockGoalsRequestBuilder implements GoalsService.GoalsRequestBuilder {
        private int perPage = 50;

        @Override
        public GoalsRequestBuilder isPrivate(final boolean isPrivate) {
            return null;
        }

        @Override
        public GoalsRequestBuilder importances(final Goal.Importance... importances) {
            return null;
        }

        @Override
        public GoalsRequestBuilder statuses(final Goal.Status... statuses) {
            return null;
        }

        @Override
        public GoalsRequestBuilder departmentId(final int departmentId) {
            return null;
        }

        @Override
        public GoalsRequestBuilder userId(final String userId) {
            return null;
        }

        @Override
        public GoalsRequestBuilder deadline(final LocalDate deadline) {
            return null;
        }

        @Override
        public GoalsRequestBuilder tags(final String... tags) {
            return null;
        }

        @Override
        public GoalsRequestBuilder pageNum(final int pageNum) {
            return null;
        }

        @Override
        public GoalsRequestBuilder perPage(final int perPage) {
            this.perPage = perPage;
            return this;
        }

        @Override
        public GoalsRequestBuilder withConfidential() {
            return null;
        }

        @Override
        public ListResponse<Goal> getGoals() throws ClientProtocolException {
            return null;
        }

        @Override
        public Iterator<List<Goal>> getPaginatedGoals() {
            return Iterables.partition(goals, perPage).iterator();
        }
    }
}
