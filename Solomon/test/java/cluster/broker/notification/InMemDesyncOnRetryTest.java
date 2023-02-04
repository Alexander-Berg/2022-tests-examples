package ru.yandex.solomon.alert.cluster.broker.notification;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.memory.InMemoryNotificationsDao;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class InMemDesyncOnRetryTest extends DesyncOnRetryTest {
    @Before
    public void setUp() {
        super.setUp(new InMemoryNotificationsDao());
    }

    @After
    public void tearDown() {
        super.tearDown();
    }
}
