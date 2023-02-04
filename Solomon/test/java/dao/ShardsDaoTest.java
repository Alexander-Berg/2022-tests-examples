package ru.yandex.solomon.alert.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public abstract class ShardsDaoTest {
    protected abstract ShardsDao getDao();

    @Test
    public void findEmpty() {
        var result = getDao().findAll().join();
        assertEquals(List.of(), result);
    }

    @Test
    public void insertFind() {
        var dao = getDao();
        dao.insert("alice").join();

        var result = dao.findAll().join();
        assertEquals(List.of("alice"), result);
    }

    @Test
    public void repeatInsert() {
        var dao = getDao();
        for (int index = 0; index < 3; index++) {
            dao.insert("alice").join();
        }

        var result = dao.findAll().join();
        assertEquals(List.of("alice"), result);
    }

    @Test
    public void insertDifferent() {
        var dao = getDao();
        dao.insert("alice").join();
        assertEquals(List.of("alice"), dao.findAll().join());

        dao.insert("bob").join();
        var result = new ArrayList<>(dao.findAll().join());
        result.sort(String::compareTo);
        assertEquals(List.of("alice", "bob"), result);
    }

    @Test
    public void delete() {
        var dao = getDao();
        dao.insert("alice").join();
        assertEquals(List.of("alice"), dao.findAll().join());

        dao.delete("alice").join();
        assertEquals(List.of(), dao.findAll().join());
    }
}
