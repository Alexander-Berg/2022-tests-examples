package ru.yandex.solomon.alert.cluster.broker.alert;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.memory.InMemoryAlertsDao;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class InMemDesyncOnRetryTest extends DesyncOnRetryTest {
    @Before
    public void setUp() {
        var dao = new InMemoryAlertsDao();
        super.setUp(dao, dao);
    }

    @After
    public void tearDown() {
        super.tearDown();
    }
}
