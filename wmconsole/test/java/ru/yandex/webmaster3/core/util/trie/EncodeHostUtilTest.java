package ru.yandex.webmaster3.core.util.trie;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ifilippov5 on 17.10.17.
 */
public class EncodeHostUtilTest {

    @Test
    public void test() {
        WebmasterHostId.Schema http = WebmasterHostId.Schema.HTTP;
        WebmasterHostId.Schema https = WebmasterHostId.Schema.HTTPS;

        List<WebmasterHostId> hosts = new ArrayList<>(Arrays.asList(
                new WebmasterHostId(http, "benkookaishi1984.xn--80aag8algk2b.xn--p1acf", WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(http, "benkookaishi1984.xn--80aag8algk2b.xn--p1acf", WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(https, "benkookaishi1984.xn--80aag8algk2b.xn--p1acf", WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(https, "benkookaishi1984.xn--80aag8algk2b.xn--p1acf", WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(http, "", WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(http, "", WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(https, "", WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(https, "", WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(http, "www.lenta.ru", WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(http, "www.lenta.ru", WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(https, "xn--abcd", WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(https, "xn--abcd", WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(http, "www.", WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(http, "www.", WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(https, "xn--", WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(https, "xn--", WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(http, "benkookaishi1984.xn--80aag8algk2b.xn--p1acf", 123456789),
                new WebmasterHostId(http, "benkookaishi1984.xn--80aag8algk2b.xn--p1acf", 0),
                new WebmasterHostId(https, "benkookaishi1984.xn--80aag8algk2b.xn--p1acf", -1),
                new WebmasterHostId(http, "", 123456789),
                new WebmasterHostId(http, "", 0),
                new WebmasterHostId(https, "", -1),
                new WebmasterHostId(http, "www.lenta.ru", 123456789),
                new WebmasterHostId(http, "www.lenta.ru", 0),
                new WebmasterHostId(https, "xn--abcd", -1),
                new WebmasterHostId(http, "www.", 123456789),
                new WebmasterHostId(http, "www.", 0),
                new WebmasterHostId(https, "xn--", 123356789),
                new WebmasterHostId(https, "xn--", 0)
        ));
        for (WebmasterHostId host : hosts) {
            byte[] encoded = EncodeHostUtil.hostToByteArray(host);
            int len = encoded.length;
            byte[] encoded1 = new byte[len + 10];
            System.arraycopy(encoded, 0, encoded1, 0, len);
            WebmasterHostId decoded = EncodeHostUtil.fromByteArray(encoded1, len);
            Assert.assertEquals(host, decoded);
        }
    }
}
