package ru.yandex.solomon.alert.dao.memory;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.TelegramDao;
import ru.yandex.solomon.alert.dao.TelegramDaoTest;

/**
 * @author alexlovkov
 **/
public class InMemoryTelegramDaoTest extends TelegramDaoTest {

    private TelegramDao dao;

    @Before
    public void setUp() {
        dao = new InMemoryTelegramDao();
    }

    @After
    public void tearDown() {
        dao = null;
    }

    @Override
    protected TelegramDao getDao() {
        return dao;
    }
}
