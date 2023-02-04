package ru.yandex.qe.dispenser.integration;

import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.qe.dispenser.domain.dao.goal.GoalDao;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;
import ru.yandex.qe.dispenser.ws.goal.GoalSync;
import ru.yandex.qe.dispenser.ws.goal.GoalSyncTask;
import ru.yandex.qe.dispenser.ws.goal.TrackerGoalClientImpl;
import ru.yandex.qe.dispenser.ws.goal.TrackerGoalHelper;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class GoalIntegrationTest extends AcceptanceTestBase {

    private GoalSyncTask goalSyncTask;
    @Autowired
    private GoalDao goalDao;
    @Value("${goal.service.url}")
    private String serviceUrl;
    @Value("${goal.oauth.token}")
    private String token;
    @Value("${goal.sync.from.tracker}")
    private boolean syncFromTracker;
    @Value("${goal.tracker.client.max.connections}")
    private int trackerMaxConnections;
    @Value("${goal.tracker.client.connection.timeout.seconds}")
    private long trackerConnectionTimeoutSeconds;
    @Value("${goal.tracker.client.socket.timeout.milliseconds}")
    private long trackerSocketTimeoutMilliseconds;
    @Value("${tracker.oauth.token}")
    private String trackerToken;
    @Value("${tracker.service.url}")
    private String trackerBaseUrl;
    @Value("${goal.tracker.queue}")
    private String trackerQueue;
    private TrackerGoalClientImpl trackerGoalClient;
    @Autowired
    private TrackerGoalHelper trackerGoalHelper;

    @BeforeAll
    private void setUpClass() throws URISyntaxException {
        trackerGoalClient = new TrackerGoalClientImpl(trackerMaxConnections,
                trackerConnectionTimeoutSeconds, trackerSocketTimeoutMilliseconds, trackerToken, trackerBaseUrl, trackerQueue);
        this.goalSyncTask = new GoalSync(goalDao, serviceUrl, token, syncFromTracker, trackerGoalClient, trackerGoalHelper)
                .getGoalSyncTask();
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        goalDao.clear();
    }

    @Test
    public void configurationsCanBeFetched() {
        goalSyncTask.update();

        assertFalse(goalDao.getAll().isEmpty());
    }

    @Test
    public void goalCanBeFetchedFromTracker() throws URISyntaxException {
        final GoalSyncTask goalSyncTask = new GoalSync(goalDao, serviceUrl, token, true, trackerGoalClient, trackerGoalHelper)
                .getGoalSyncTask();

        goalSyncTask.update();
    }
}

