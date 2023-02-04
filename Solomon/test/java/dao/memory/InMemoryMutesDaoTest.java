package ru.yandex.solomon.alert.dao.memory;

import org.junit.After;
import org.junit.Before;

import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.dao.MutesDaoTest;
import ru.yandex.solomon.alert.mute.domain.Mute;

/**
 * @author Ivan Tsybulin
 */
public class InMemoryMutesDaoTest extends MutesDaoTest {

    private EntitiesDao<Mute> dao;

    @Before
    public void setUp() {
        dao = new InMemoryMutesDao();
    }

    @After
    public void tearDown() {
        dao = null;
    }

    @Override
    protected EntitiesDao<Mute> getDao() {
        return dao;
    }
}
