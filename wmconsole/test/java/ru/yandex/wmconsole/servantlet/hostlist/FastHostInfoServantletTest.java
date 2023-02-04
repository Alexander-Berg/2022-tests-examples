package ru.yandex.wmconsole.servantlet.hostlist;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.wmconsole.data.info.XmlSearchFastHostInfo;
import ru.yandex.wmtools.common.error.UserException;

/**
 * User: azakharov
 * Date: 05.04.12
 * Time: 12:44
 */
public class FastHostInfoServantletTest {

    private FastHostInfoServantlet fastHostInfoServantlet = new FastHostInfoServantlet();

    @Test
    public void testUnmarshallNotNull() throws UserException {
        List<XmlSearchFastHostInfo> list = fastHostInfoServantlet.unmarshall(0L,
                "[{hostId: 1, indexCount: 2, urls: 3, tcy: 4}, {hostId: 22, indexCount: 23, urls: 24, tcy: 25}]");
        Assert.assertEquals(2, list.size());

        XmlSearchFastHostInfo hi1 = list.get(0);
        Assert.assertEquals(Long.valueOf((long)1), hi1.getHostId());
        Assert.assertEquals(Long.valueOf((long)2), hi1.getIndexCount());
        Assert.assertEquals(Long.valueOf((long)3), hi1.getUrls());
        Assert.assertEquals(Long.valueOf((long)4), hi1.getTcy());

        XmlSearchFastHostInfo hi2 = list.get(1);
        Assert.assertEquals(Long.valueOf((long)22), hi2.getHostId());
        Assert.assertEquals(Long.valueOf((long)23), hi2.getIndexCount());
        Assert.assertEquals(Long.valueOf((long)24), hi2.getUrls());
        Assert.assertEquals(Long.valueOf((long)25), hi2.getTcy());
    }

    @Test
    public void testUnmarshallNull() throws UserException {
        List<XmlSearchFastHostInfo> list = fastHostInfoServantlet.unmarshall(0L,
                "[{hostId: 1, indexCount: null, urls: null, tcy: null}, {hostId: 22, indexCount: null, urls: null, tcy: null}]");
        Assert.assertEquals(2, list.size());

        XmlSearchFastHostInfo hi1 = list.get(0);
        Assert.assertEquals(Long.valueOf((long)1), hi1.getHostId());
        Assert.assertEquals(null, hi1.getIndexCount());
        Assert.assertEquals(null, hi1.getUrls());
        Assert.assertEquals(null, hi1.getTcy());

        XmlSearchFastHostInfo hi2 = list.get(1);
        Assert.assertEquals(Long.valueOf((long)22), hi2.getHostId());
        Assert.assertEquals(null, hi2.getIndexCount());
        Assert.assertEquals(null, hi2.getUrls());
        Assert.assertEquals(null, hi2.getTcy());
    }

    @Test
    public void testUnmarshallNoKey() throws UserException {
        List<XmlSearchFastHostInfo> list = fastHostInfoServantlet.unmarshall(0L,
                "[{hostId: 1}, {hostId: 22}]");
        Assert.assertEquals(2, list.size());

        XmlSearchFastHostInfo hi1 = list.get(0);
        Assert.assertEquals(Long.valueOf((long)1), hi1.getHostId());
        Assert.assertEquals(null, hi1.getIndexCount());
        Assert.assertEquals(null, hi1.getUrls());
        Assert.assertEquals(null, hi1.getTcy());

        XmlSearchFastHostInfo hi2 = list.get(1);
        Assert.assertEquals(Long.valueOf((long)22), hi2.getHostId());
        Assert.assertEquals(null, hi2.getIndexCount());
        Assert.assertEquals(null, hi2.getUrls());
        Assert.assertEquals(null, hi2.getTcy());
    }

    @Test(expected = UserException.class)
    public void testUnmarshallNoHostIdKey() throws UserException {
        List<XmlSearchFastHostInfo> list = fastHostInfoServantlet.unmarshall(0L, "[{}]");
        Assert.fail("Должно быть исключение из-за отсутствия атрибута hostId");
    }

    @Test
    public void testEmptyList() throws UserException {
        List<XmlSearchFastHostInfo> list = fastHostInfoServantlet.unmarshall(0L, "[]");
        Assert.assertEquals(0, list.size());
    }

    @Test(expected = UserException.class)
    public void testEmptyString() throws UserException {
        List<XmlSearchFastHostInfo> list = fastHostInfoServantlet.unmarshall(0L, "");
        Assert.fail("Должно быть исключение из-за отсутствия атрибута hostId");
    }
}
