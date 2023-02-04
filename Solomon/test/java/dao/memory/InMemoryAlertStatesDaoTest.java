package ru.yandex.solomon.alert.dao.memory;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.AlertStatesDaoTest;

/**
 * @author Vladimir Gordiychuk
 */
public class InMemoryAlertStatesDaoTest extends AlertStatesDaoTest {
    private InMemoryAlertStatesDao dao;

    @Before
    public void setUp() {
        dao = new InMemoryAlertStatesDao();
    }

    @After
    public void tearDown() {
        dao = null;
    }

    @Override
    protected InMemoryAlertStatesDao getDao() {
        return dao;
    }
}
