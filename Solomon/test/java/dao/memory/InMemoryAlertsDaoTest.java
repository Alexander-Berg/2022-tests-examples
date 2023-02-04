package ru.yandex.solomon.alert.dao.memory;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.AlertsDaoTest;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.idempotency.dao.IdempotentOperationDao;

/**
 * @author Vladimir Gordiychuk
 */
public class InMemoryAlertsDaoTest extends AlertsDaoTest {

    private InMemoryAlertsDao dao;

    @Before
    public void setUp() {
        dao = new InMemoryAlertsDao();
    }

    @After
    public void tearDown() {
        dao = null;
    }

    @Override
    protected EntitiesDao<Alert> getDao() {
        return dao;
    }

    @Override
    protected IdempotentOperationDao getOperationsDao() {
        return dao;
    }
}
