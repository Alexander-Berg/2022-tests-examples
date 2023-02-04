package ru.yandex.qe.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.support.XmlWebApplicationContext;


/**
 * User: terry
 * Date: 26.09.13
 * Time: 15:51
 */
public class RmiReplicationCacheTest {

    @Test
    public void check_synch_replication() throws InterruptedException {
        XmlWebApplicationContext appContextOne = new XmlWebApplicationContext();
        appContextOne.setConfigLocation("classpath:spring/cache-one.xml");
        appContextOne.refresh();

        CacheManager cacheManagerOne = (CacheManager) appContextOne.getBean("ehCacheManagerOne");

        XmlWebApplicationContext appContextTwo = new XmlWebApplicationContext();
        appContextTwo.setConfigLocation("classpath:spring/cache-two.xml");
        appContextTwo.refresh();

        CacheManager cacheManagerTwo = (CacheManager) appContextTwo.getBean("ehCacheManagerTwo");

        waitForClusterMembership(1, "testCache", cacheManagerOne, cacheManagerTwo);

        final Cache oneCache = cacheManagerOne.getCache("testCache");
        final Cache twoCache = cacheManagerTwo.getCache("testCache");

       // twoCache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter() {});

        oneCache.put(new Element("key", "value"));

        // give some time for replication
        Thread.sleep(100);

        Assertions.assertNotNull(twoCache.get("key"));
        Assertions.assertNotNull(twoCache.get("key").getObjectValue());

        oneCache.put(new Element("key", "value-changed"));

        Thread.sleep(100);

        Assertions.assertNotNull(twoCache.get("key"));
        Assertions.assertNotNull(twoCache.get("key").getObjectValue());
        Assertions.assertEquals("value-changed", twoCache.get("key").getObjectValue());
    }

    protected static void waitForClusterMembership(int expectedPeersCount, final String cacheName, final CacheManager... managers) {
        while (true) {
            boolean isMembership = true;
            for (CacheManager manager : managers) {
                CacheManagerPeerProvider peerProvider = manager.getCacheManagerPeerProvider("RMI");
                    int peers = peerProvider.listRemoteCachePeers(manager.getEhcache(cacheName)).size();
                    if (peers < expectedPeersCount) {
                        isMembership = false;
                }
            }
            if (isMembership) {
                return;
            }
        }
    }

}
