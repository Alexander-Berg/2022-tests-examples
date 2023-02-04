package ru.yandex.solomon.alert.dao.memory;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.NotificationsDao;
import ru.yandex.solomon.alert.dao.NotificationsDaoTest;

/**
 * @author Vladimir Gordiychuk
 */
public class InMemoryNotificationsDaoTest extends NotificationsDaoTest {

    private NotificationsDao dao;

    @Before
    public void setUp() {
        dao = new InMemoryNotificationsDao();
    }

    @After
    public void tearDown() {
        dao = null;
    }

    @Override
    protected NotificationsDao getDao() {
        return dao;
    }
}
