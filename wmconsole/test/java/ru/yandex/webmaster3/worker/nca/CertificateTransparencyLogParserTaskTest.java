package ru.yandex.webmaster3.worker.nca;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;

import static ru.yandex.webmaster3.worker.nca.CertificateTransparencyLogParserTask.suitableDomain;


/**
 * @author kravchenko99
 * @date 5/5/22
 */

public class CertificateTransparencyLogParserTaskTest {

    @Test
    public void sameNotHttpsHostTest() {
        String domain = "abc.ru";
        WebmasterHostId hostId = IdUtils.urlToHostId("http://abc.ru");
        Assert.assertFalse(suitableDomain(hostId, domain));
    }

    @Test
    public void sameHttpsHostTest() {
        String domain = "abc.ru";
        WebmasterHostId hostId = IdUtils.urlToHostId("https://abc.ru");
        Assert.assertTrue(suitableDomain(hostId, domain));
    }

    @Test
    public void diffHttpsHostTest() {
        String domain = "abc.ru";
        WebmasterHostId hostId = IdUtils.urlToHostId("https://xyz.ru");
        Assert.assertFalse(suitableDomain(hostId, domain));
    }

    @Test
    public void overdomainTest() {
        String domain = "qwe2.abc1.ru";
        WebmasterHostId hostId = IdUtils.urlToHostId("https://abc1.ru");
        Assert.assertTrue(suitableDomain(hostId, domain));
    }

    @Test
    public void subdomainWithWildcardTest() {
        String domain = "*.abc1.ru";
        WebmasterHostId hostId = IdUtils.urlToHostId("https://qwe3.rty2.abc1.ru");
        Assert.assertTrue(suitableDomain(hostId, domain));
    }

    @Test
    public void subdomainWithoutWildcardTest() {
        String domain = "abc1.ru";
        WebmasterHostId hostId = IdUtils.urlToHostId("https://qwe3.rty2.abc1.ru");
        Assert.assertFalse(suitableDomain(hostId, domain));
    }
}
