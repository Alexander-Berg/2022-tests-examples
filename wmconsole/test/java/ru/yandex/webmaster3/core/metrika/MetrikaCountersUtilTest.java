package ru.yandex.webmaster3.core.metrika;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.metrika.counters.MetrikaCountersUtil;
import ru.yandex.webmaster3.core.util.IdUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ifilippov5 on 15.02.18.
 */
public class MetrikaCountersUtilTest {

    @Test
    public void generateHostIdsTest() {
        test("lenta.ru");
        test("гео-спорт.рф");
    }

    public void test(String domain) {
        String wwwDomain = "www." + domain;
        List<WebmasterHostId> actual = MetrikaCountersUtil.generateHostIds(domain).collect(Collectors.toList());
        List<WebmasterHostId> expected = Arrays.asList(
                new WebmasterHostId(WebmasterHostId.Schema.HTTP, IdUtils.IDN.toASCII(domain), WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(WebmasterHostId.Schema.HTTP, IdUtils.IDN.toASCII(wwwDomain), WebmasterHostId.DEFAULT_HTTP_PORT),
                new WebmasterHostId(WebmasterHostId.Schema.HTTPS, IdUtils.IDN.toASCII(domain), WebmasterHostId.DEFAULT_HTTPS_PORT),
                new WebmasterHostId(WebmasterHostId.Schema.HTTPS, IdUtils.IDN.toASCII(wwwDomain), WebmasterHostId.DEFAULT_HTTPS_PORT)
        );
        Assert.assertEquals(expected, actual);

        actual = MetrikaCountersUtil.generateHostIds(wwwDomain).collect(Collectors.toList());
        Assert.assertEquals(expected, actual);
    }
}
