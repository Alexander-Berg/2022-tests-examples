package ru.yandex.intranet.d.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.datasource.model.SessionPoolDepletedException;
import ru.yandex.intranet.d.datasource.model.YdbSession;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.datasource.utils.ReadinessYdbTableClientHolder;

/**
 * Session pool test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class SessionPoolTest {

    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private ReadinessYdbTableClientHolder readinessYdbTableClientHolder;
    @Value("${ydb.sessionPoolMaxSize}")
    private int ydbPoolMaxSize;
    @Value("${ydb.readiness.sessionPoolMaxSize}")
    private int ydbReadinessPoolMaxSize;

    @Test
    public void testPoolAndQueueDepletion() {
        List<YdbSession> sessions = new ArrayList<>();
        try {
            // Take all available sessions
            for (int i = 0; i < ydbPoolMaxSize; i++) {
                sessions.add(ydbTableClient.getSession().block());
            }
            // Fill wait queue
            for (int i = 0; i < 2 * ydbPoolMaxSize; i++) {
                ydbTableClient.getSession().subscribe(YdbSession::release);
            }
            boolean validException = false;
            // Must fail with "too many outstanding acquire operations"
            try {
                ydbTableClient.getSession().block();
            } catch (Exception e) {
                Assertions.assertTrue(SessionPoolDepletedException.isSessionPoolDepleted(e));
                validException = true;
            }
            Assertions.assertTrue(validException);
        } finally {
            sessions.forEach(YdbSession::release);
        }
    }

    @Test
    public void testPoolDepletion() {
        List<YdbSession> sessions = new ArrayList<>();
        try {
            // Take all available sessions
            for (int i = 0; i < ydbPoolMaxSize; i++) {
                sessions.add(ydbTableClient.getSession().block());
            }
            boolean validException = false;
            // Must fail with "cannot acquire object within 30000ms"
            try {
                ydbTableClient.getSession().block();
            } catch (Exception e) {
                Assertions.assertTrue(SessionPoolDepletedException.isSessionPoolDepleted(e));
                validException = true;
            }
            Assertions.assertTrue(validException);
        } finally {
            sessions.forEach(YdbSession::release);
        }
    }

    @Test
    public void testReadinessPoolAndQueueDepletion() {
        YdbTableClient readinessYdbTableClient = readinessYdbTableClientHolder.getTableClient();
        List<YdbSession> sessions = new ArrayList<>();
        try {
            // Take all available sessions
            for (int i = 0; i < ydbReadinessPoolMaxSize; i++) {
                sessions.add(readinessYdbTableClient.getSession().block());
            }
            // Fill wait queue
            for (int i = 0; i < 2 * ydbReadinessPoolMaxSize; i++) {
                readinessYdbTableClient.getSession().subscribe(YdbSession::release);
            }
            boolean validException = false;
            // Must fail with "too many outstanding acquire operations"
            try {
                readinessYdbTableClient.getSession().block();
            } catch (Exception e) {
                Assertions.assertTrue(SessionPoolDepletedException.isSessionPoolDepleted(e));
                validException = true;
            }
            Assertions.assertTrue(validException);
        } finally {
            sessions.forEach(YdbSession::release);
        }
    }

    @Test
    public void testReadinessPoolDepletion() {
        YdbTableClient readinessYdbTableClient = readinessYdbTableClientHolder.getTableClient();
        List<YdbSession> sessions = new ArrayList<>();
        try {
            // Take all available sessions
            for (int i = 0; i < ydbReadinessPoolMaxSize; i++) {
                sessions.add(readinessYdbTableClient.getSession().block());
            }
            boolean validException = false;
            // Must fail with "cannot acquire object within 30000ms"
            try {
                readinessYdbTableClient.getSession().block();
            } catch (Exception e) {
                Assertions.assertTrue(SessionPoolDepletedException.isSessionPoolDepleted(e));
                validException = true;
            }
            Assertions.assertTrue(validException);
        } finally {
            sessions.forEach(YdbSession::release);
        }
    }

}
