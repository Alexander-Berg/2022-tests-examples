package ru.yandex.solomon.alert.dao.memory;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.TelegramEventsDao;
import ru.yandex.solomon.alert.dao.TelegramEventsDaoTest;

/**
 * @author alexlovkov
 **/
public class InMemoryTelegramEventsDaoTest extends TelegramEventsDaoTest {

    private TelegramEventsDao dao;

    @Before
    public void setUp() {
        dao = new InMemoryTelegramEventsDao();
    }

    @After
    public void tearDown() {
        dao = null;
    }

    @Override
    protected TelegramEventsDao getDao() {
        return dao;
    }
}
