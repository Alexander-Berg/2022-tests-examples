package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.goals.GoalsClient;
import ru.yandex.inside.goals.model.Goal;
import ru.yandex.qe.dispenser.domain.dao.goal.GoalDao;
import ru.yandex.qe.dispenser.domain.dao.goal.OkrAncestors;
import ru.yandex.qe.dispenser.domain.index.LongIndexBase;
import ru.yandex.qe.dispenser.standalone.MockGoalsService;
import ru.yandex.qe.dispenser.standalone.MockTrackerGoalClient;
import ru.yandex.qe.dispenser.ws.goal.GoalIssue;
import ru.yandex.qe.dispenser.ws.goal.GoalSyncTask;
import ru.yandex.qe.dispenser.ws.goal.TrackerGoalHelper;
import ru.yandex.startrek.client.model.StatusRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoalSyncTest extends BusinessLogicTestBase {

    @Autowired
    private GoalDao goalDao;

    @Autowired
    private TrackerGoalHelper trackerGoalHelper;

    private MockGoalsService goalsService = new MockGoalsService();
    private MockTrackerGoalClient trackerGoalClient = new MockTrackerGoalClient();

    private GoalSyncTask goalSyncTask;
    private Map<Goal.Importance, String> idByImportance;

    @BeforeAll
    public void setUpMock() {
        final GoalsClient goalsClient = mock(GoalsClient.class);

        when(goalsClient.getGoalsService()).thenReturn(goalsService);

        goalSyncTask = new GoalSyncTask(goalDao, goalsClient, true, trackerGoalClient, trackerGoalHelper);

        idByImportance = MapUtils.invertMap(trackerGoalHelper.getImportanceById());
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        goalDao.clear();
    }


    @Test
    public void goalsMustBeCreated() {
        final Goal externalGoal = createGoal(999L, "Test goal", Goal.Importance.COMPANY, Goal.Status.RISK);
        final GoalIssue goalIssue = createGoalIssue(999L, "Test goal", Goal.Importance.COMPANY, Goal.Status.RISK);
        goalsService.setGoals(Collections.singletonList(externalGoal));
        trackerGoalClient.setIssues(Collections.singletonList(goalIssue));

        goalSyncTask.update();

        final Set<ru.yandex.qe.dispenser.domain.dao.goal.Goal> goals = goalDao.getAll();

        assertEquals(1, goals.size());

        final ru.yandex.qe.dispenser.domain.dao.goal.Goal goal = goals.iterator().next();

        assertEquals(goal.getId(), externalGoal.getId().longValue());
        assertEquals(goal.getName(), externalGoal.getTitle().get());
        assertEquals(goal.getImportance(), externalGoal.getImportance());
        assertEquals(goal.getStatus(), externalGoal.getStatus());
    }

    @NotNull
    private static Goal createGoal(final long id, final String title, final Goal.Importance importance, final Goal.Status status) {
        return new Goal(
                null,
                null,
                importance.ordinal(),
                status.ordinal(),
                null,
                null,
                Optional.of(title),
                null,
                null,
                null,
                null,
                id,
                null
        );
    }

    private GoalIssue createGoalIssue(final long id, final String title, final Goal.Importance importance, final Goal.Status status) {
        return new GoalIssue(
                "uuid" + id,
                "GOALS-" + id,
                title,
                toPriority(importance),
                toStatus(status),
                Collections.emptyList()
        );
    }

    private GoalIssue createGoalIssue(final long id, final String title, final Goal.Importance importance, final Goal.Status status, final long parentId) {
        return new GoalIssue(
                "uuid" + id,
                "GOALS-" + id,
                title,
                toPriority(importance),
                toStatus(status),
                Collections.singletonList(new GoalIssue.LocalLinkRef(
                                null,
                                null,
                                "is dependent by",
                                "uuid" + parentId
                        )
                ));
    }

    private GoalIssue.GoalImportance toPriority(final Goal.Importance importance) {
        final String id = idByImportance.get(importance);
        if (id == null) {
            throw new IllegalArgumentException("Unexpected importance " + importance);
        }
        return new GoalIssue.GoalImportance(null, id);
    }

    private StatusRef toStatus(final Goal.Status goalStatus) {
        switch (goalStatus) {
            case PLANNED: {
                final StatusRef status = Mockito.mock(StatusRef.class);
                Mockito.doReturn("asPlanned").when(status).getKey();
                return status;
            }
            case RISK: {
                final StatusRef status = Mockito.mock(StatusRef.class);
                Mockito.doReturn("withRisks").when(status).getKey();
                return status;
            }
            case BLOCKED: {
                final StatusRef status = Mockito.mock(StatusRef.class);
                Mockito.doReturn("blockedGoal").when(status).getKey();
                return status;
            }
            case CANCELLED: {
                final StatusRef status = Mockito.mock(StatusRef.class);
                Mockito.doReturn("cancelled").when(status).getKey();
                return status;
            }
            case REACHED: {
                final StatusRef status = Mockito.mock(StatusRef.class);
                Mockito.doReturn("achieved").when(status).getKey();
                return status;
            }
            case NEW: {
                final StatusRef status = Mockito.mock(StatusRef.class);
                Mockito.doReturn("newGoal").when(status).getKey();
                return status;
            }
            default:
                throw new IllegalArgumentException("Unexpected status " + goalStatus);
        }
    }

    @Test
    public void goalsMustBeUpdated() {
        final Goal oldGoal = createGoal(9, "Old Goal", Goal.Importance.PRIVATE, Goal.Status.NEW);
        final GoalIssue oldGoalIssue = createGoalIssue(9, "Old Goal", Goal.Importance.PRIVATE, Goal.Status.NEW);
        goalsService.setGoals(Collections.singletonList(oldGoal));
        trackerGoalClient.setIssues(Collections.singletonList(oldGoalIssue));

        goalSyncTask.update();

        Set<ru.yandex.qe.dispenser.domain.dao.goal.Goal> goals = goalDao.getAll();

        assertEquals(1, goals.size());

        ru.yandex.qe.dispenser.domain.dao.goal.Goal goal = goals.iterator().next();

        assertEquals(goal.getId(), oldGoal.getId().longValue());
        assertEquals(goal.getName(), oldGoal.getTitle().get());
        assertEquals(goal.getImportance(), oldGoal.getImportance());
        assertEquals(goal.getStatus(), oldGoal.getStatus());

        final Goal newGoal = createGoal(9, "New Goal", Goal.Importance.PRIVATE, Goal.Status.PLANNED);
        final GoalIssue newGoalIssue = createGoalIssue(9, "New Goal", Goal.Importance.PRIVATE, Goal.Status.PLANNED);
        goalsService.setGoals(Collections.singletonList(newGoal));
        trackerGoalClient.setIssues(Collections.singletonList(newGoalIssue));

        goalSyncTask.update();

        goals = goalDao.getAll();

        assertEquals(1, goals.size());

        goal = goals.iterator().next();

        assertEquals(goal.getId(), newGoal.getId().longValue());
        assertEquals(goal.getName(), newGoal.getTitle().get());
        assertEquals(goal.getImportance(), newGoal.getImportance());
        assertEquals(goal.getStatus(), newGoal.getStatus());
    }

    @Test
    public void goalsCanBeUpsertedButch() {
        Set<ru.yandex.qe.dispenser.domain.dao.goal.Goal> goals = goalDao.getAll();
        assertEquals(0, goals.size());

        final Goal firstGoal = createGoal(9, "First Goal", Goal.Importance.PRIVATE, Goal.Status.NEW);
        final Goal secondGoal = createGoal(10, "Second Goal", Goal.Importance.DEPARTMENT, Goal.Status.RISK);
        final Goal thirdGoal = createGoal(11, "Third Goal", Goal.Importance.COMPANY, Goal.Status.REACHED);
        final GoalIssue firstGoalIssue = createGoalIssue(9, "First Goal", Goal.Importance.PRIVATE, Goal.Status.NEW);
        final GoalIssue secondGoalIssue = createGoalIssue(10, "Second Goal", Goal.Importance.DEPARTMENT, Goal.Status.RISK);
        final GoalIssue thirdGoalIssue = createGoalIssue(11, "Third Goal", Goal.Importance.COMPANY, Goal.Status.REACHED);

        goalsService.setGoals(Arrays.asList(firstGoal, secondGoal, thirdGoal));
        trackerGoalClient.setIssues(Arrays.asList(firstGoalIssue, secondGoalIssue, thirdGoalIssue));

        goalSyncTask.update();

        goals = goalDao.getAll();
        assertEquals(3, goals.size());
    }

    @Test
    public void trackerGoalsHierarchyMustBeSynced() {
        trackerGoalClient.setIssues(Arrays.asList(
                createGoalIssue(1, "Goal1", Goal.Importance.OKR, Goal.Status.NEW),
                createGoalIssue(2, "Goal2", Goal.Importance.OKR, Goal.Status.NEW),
                createGoalIssue(3, "Goal3", Goal.Importance.OKR, Goal.Status.NEW),

                createGoalIssue(4, "Goal4", Goal.Importance.OKR, Goal.Status.NEW, 1),
                createGoalIssue(5, "Goal5", Goal.Importance.OKR, Goal.Status.NEW, 2),
                createGoalIssue(6, "Goal6", Goal.Importance.OKR, Goal.Status.NEW, 3),

                createGoalIssue(7, "Goal7", Goal.Importance.OKR, Goal.Status.NEW, 1),
                createGoalIssue(8, "Goal8", Goal.Importance.OKR, Goal.Status.NEW, 2),
                createGoalIssue(9, "Goal9", Goal.Importance.OKR, Goal.Status.NEW, 1),

                createGoalIssue(10, "Goal10", Goal.Importance.COMPANY, Goal.Status.NEW),
                createGoalIssue(11, "Goal11", Goal.Importance.OKR, Goal.Status.NEW),

                createGoalIssue(12, "Goal12", Goal.Importance.OKR, Goal.Status.NEW, 4),
                createGoalIssue(13, "Goal13", Goal.Importance.OKR, Goal.Status.NEW, 5),
                createGoalIssue(14, "Goal14", Goal.Importance.COMPANY, Goal.Status.NEW, 12),
                createGoalIssue(15, "Goal15", Goal.Importance.COMPANY, Goal.Status.NEW, 13),
                createGoalIssue(16, "Goal16", Goal.Importance.COMPANY, Goal.Status.NEW, 4),
                createGoalIssue(17, "Goal17", Goal.Importance.COMPANY, Goal.Status.NEW, 3),
                createGoalIssue(18, "Goal18", Goal.Importance.COMPANY, Goal.Status.NEW, 15)
        ));

        goalSyncTask.update();

        final Set<ru.yandex.qe.dispenser.domain.dao.goal.Goal> all = goalDao.getAll();

        final Map<Long, List<Long>> okrAncestorByGoalId = all.stream()
                .collect(Collectors.toMap(LongIndexBase::getId, g -> Arrays.asList(
                        g.getOkrParents().getGoalId(OkrAncestors.OkrType.VALUE_STREAM),
                        g.getOkrParents().getGoalId(OkrAncestors.OkrType.UMBRELLA),
                        g.getOkrParents().getGoalId(OkrAncestors.OkrType.CONTOUR))));

        assertEquals(okrAncestorByGoalId.get(1L), Arrays.asList(1L, null, null));
        assertEquals(okrAncestorByGoalId.get(2L), Arrays.asList(2L, null, null));
        assertEquals(okrAncestorByGoalId.get(3L), Arrays.asList(3L, null, null));

        assertEquals(okrAncestorByGoalId.get(4L), Arrays.asList(1L, 4L, null));
        assertEquals(okrAncestorByGoalId.get(5L), Arrays.asList(2L, 5L, null));
        assertEquals(okrAncestorByGoalId.get(6L), Arrays.asList(3L, 6L, null));

        assertEquals(okrAncestorByGoalId.get(7L), Arrays.asList(1L, 7L, null));
        assertEquals(okrAncestorByGoalId.get(8L), Arrays.asList(2L, 8L, null));
        assertEquals(okrAncestorByGoalId.get(9L), Arrays.asList(1L, 9L, null));

        assertEquals(okrAncestorByGoalId.get(10L), Arrays.asList(null, null, null));
        assertEquals(okrAncestorByGoalId.get(11L), Arrays.asList(11L, null, null));

        assertEquals(okrAncestorByGoalId.get(12L), Arrays.asList(1L, 4L, 12L));
        assertEquals(okrAncestorByGoalId.get(13L), Arrays.asList(2L, 5L, 13L));
        assertEquals(okrAncestorByGoalId.get(14L), Arrays.asList(1L, 4L, 12L));
        assertEquals(okrAncestorByGoalId.get(15L), Arrays.asList(2L, 5L, 13L));
        assertEquals(okrAncestorByGoalId.get(16L), Arrays.asList(1L, 4L, null));
        assertEquals(okrAncestorByGoalId.get(17L), Arrays.asList(3L, null, null));
        assertEquals(okrAncestorByGoalId.get(18L), Arrays.asList(2L, 5L, 13L));
    }
}
