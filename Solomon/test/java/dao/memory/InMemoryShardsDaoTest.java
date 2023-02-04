package ru.yandex.solomon.alert.dao.memory;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.ShardsDao;
import ru.yandex.solomon.alert.dao.ShardsDaoTest;

/**
 * @author Vladimir Gordiychuk
 */
public class InMemoryShardsDaoTest extends ShardsDaoTest {
    private InMemoryShardsDao dao;

    @Before
    public void setUp() {
        dao = new InMemoryShardsDao();
    }

    @After
    public void tearDown() {
        dao = null;
    }

    @Override
    protected ShardsDao getDao() {
        return dao;
    }

}
