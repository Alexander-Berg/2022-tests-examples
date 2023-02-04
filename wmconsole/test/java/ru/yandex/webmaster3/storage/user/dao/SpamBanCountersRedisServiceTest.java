package ru.yandex.webmaster3.storage.user.dao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import ru.yandex.webmaster3.core.util.TimeUtils;

/**
 * Created by ifilippov5 on 24.01.17.
 */
public class SpamBanCountersRedisServiceTest {
    static private Map<String, String> map = new HashMap<>();

    static {
        map.put(createHit("1.1.1.1", "20170123_0012"), "1");
        map.put(createHit("1.1.1.1", "20170322_2100"), "100");
        map.put(createHit("1.1.1.1", "20170522_2100"), "10");
        map.put(createHit("1.1.1.1", "20170522_2100"), "10");
        map.put(createHit("1.1.1.2", "20170101_1616"), "15");
        map.put(createHit("1.1.1.2", "20170120_1515"), "5");
        map.put(createHit("255.1.1.2", "20160123_0034"), "20");
        map.put(createHit("255.1.1.2", "20170123_0034"), "2");
        map.put(createHit("255.255.255.1", "20170122_2100"), "10");
        map.put(createHit("255.255.255.1", "20170122_2100"), "15");
    }

    @NotNull
    private static String createHit(String ip, String date) {
        return SpamBanCountersRedisService.buildKey(
                SpamBanCountersRedisService.HITS_COUNTER_PREFIX, ip,  date
        );
    }

    @NotNull
    private static String createMasks(String maskId, String date) {
        return SpamBanCountersRedisService.buildKey(
                SpamBanCountersRedisService.MASKS_COUNTER_PREFIX, maskId,  date
        );
    }

    private SpamBanCountersRedisService spamBanCountersRedisService = new SpamBanCountersRedisService();

    @Before
    public void initialize() {
        spamBanCountersRedisService.setJedisCluster(new JedisClusterMock(new HashSet<>()));
    }

    @Test
    public void testBuildUsableCounter() {
        Set<String> keys = new HashSet<>(Arrays.asList(
                createHit("2.2.2.2", "20170123_0012"),
                createMasks("maskId", "20170123_0012")
        ));
        keys.addAll(map.keySet());

        Map<String, NavigableMap<Instant, Long>> expected = new HashMap<>();

        NavigableMap<Instant, Long> timeCounter = new TreeMap<>();
        timeCounter.put(SpamBanCountersRedisService.deserializeInstant("20170123_0012"), 1L);
        timeCounter.put(SpamBanCountersRedisService.deserializeInstant("20170522_2100"), 10L);
        timeCounter.put(SpamBanCountersRedisService.deserializeInstant("20170322_2100"), 100L);
        expected.put("1.1.1.1", timeCounter);

        timeCounter = new TreeMap<>();
        timeCounter.put(SpamBanCountersRedisService.deserializeInstant("20170120_1515"), 5L);
        timeCounter.put(SpamBanCountersRedisService.deserializeInstant("20170101_1616"), 15L);
        expected.put("1.1.1.2", timeCounter);

        timeCounter = new TreeMap<>();
        timeCounter.put(SpamBanCountersRedisService.deserializeInstant("20170123_0034"), 2L);
        timeCounter.put(SpamBanCountersRedisService.deserializeInstant("20160123_0034"), 20L);
        expected.put("255.1.1.2", timeCounter);

        timeCounter = new TreeMap<>();
        timeCounter.put(SpamBanCountersRedisService.deserializeInstant("20170122_2100"),15L);
        expected.put("255.255.255.1", timeCounter);

        Assert.assertEquals(expected, spamBanCountersRedisService.buildUsableCounter(keys));
    }

    @Test
    public void testDateSerialization() throws Exception {
        Instant inst = Instant.parse("2017-02-16T10:00:00Z");
        String ser = SpamBanCountersRedisService.serializeInstant(inst);
        Assert.assertEquals("20170216_1000", ser);
        Assert.assertEquals(inst, SpamBanCountersRedisService.deserializeInstant(ser));
    }

    public static class JedisClusterMock extends JedisCluster {

        public JedisClusterMock(Set<HostAndPort> nodes) {
            super(nodes);
        }

        @Override
        public String get(final String key) {
            return map.get(key);
        }

        @Override
        public Boolean exists(final String key) {
            return map.containsKey(key);
        }
    }
}
