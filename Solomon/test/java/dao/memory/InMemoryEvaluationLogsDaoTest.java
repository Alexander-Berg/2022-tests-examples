package ru.yandex.solomon.alert.dao.memory;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.EvaluationLogsDaoTest;

/**
 * @author Vladimir Gordiychuk
 */
public class InMemoryEvaluationLogsDaoTest extends EvaluationLogsDaoTest {
    private InMemoryEvaluationLogsDao dao;

    @Before
    public void setUp() {
        dao = new InMemoryEvaluationLogsDao();
    }

    @After
    public void tearDown() {
        dao = null;
    }

    @Override
    protected InMemoryEvaluationLogsDao getDao() {
        return dao;
    }
}
