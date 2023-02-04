package ru.yandex.wmconsole.data.partition;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link WMCPartition} class.
 *
 * @author ailyin
 */
public class WMCPartitionTest {
    private Map<String, Integer> hostToHashes;

    @Before
    public void setUp() {
        hostToHashes = new HashMap<String, Integer>();

        /*
         * These tests should be changed only together with the corresponding c++ tests
         * to ensure we use the same algorithm for choosing a host database everywhere.
         */
        hostToHashes.put("www.mail.ru", 1099);
        hostToHashes.put("www.MAil.ru", 1099);
        hostToHashes.put("WWW.maIl.ru", 1099);
        hostToHashes.put("mail.ru", 696);
        hostToHashes.put("amil.ru", 696);
        hostToHashes.put("ilam.ru", 696);
        hostToHashes.put("yandex.ru", 926);
        hostToHashes.put("webmaster.yandex.ru", 1942);
        hostToHashes.put("ru.wikipedia.org", 1602);
        hostToHashes.put("en.wikipedia.org", 1582);
        hostToHashes.put("vkontakte.ru:8080", 1526);
        hostToHashes.put("pisem.net", 915);
    }

    @Test
    public void test2() {
        testIt(2);
    }

    @Test
    public void test3() {
        testIt(3);
    }

    @Test
    public void test10() {
        testIt(10);
    }

    @Test
    public void test0() {
        final String hostname = "www.lenta.ru";
        final int dbCount = 4;
        System.out.println("DB index for host \"" + hostname + "\": " + WMCPartition.getStringHash(hostname, dbCount) + " (starting from 0, out of " + dbCount + " databases.");
    }

    private void testIt(int baseCount) {
        for (Map.Entry<String, Integer> entry : hostToHashes.entrySet()) {
            assertEquals("wrong database for host " + entry.getKey() + ";",
                    entry.getValue() % baseCount, WMCPartition.getStringHash(entry.getKey(), baseCount));
        }
    }
}
