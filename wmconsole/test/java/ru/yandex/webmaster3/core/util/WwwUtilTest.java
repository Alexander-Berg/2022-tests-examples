package ru.yandex.webmaster3.core.util;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;

import static ru.yandex.webmaster3.core.data.WebmasterHostId.Schema.HTTPS;

/**
 * Created by Oleg Bazdyrev on 12/09/2018.
 */
public class WwwUtilTest {

    @Test
    public void testCutWww() {
        Assert.assertEquals("domain.ru", WwwUtil.cutWww("www.domain.ru"));
        Assert.assertEquals("www.ru", WwwUtil.cutWww("www.ru"));
        Assert.assertEquals("com.tr", WwwUtil.cutWww("www.com.tr")); // хз правильно ли это
        Assert.assertEquals("va", WwwUtil.cutWww("va"));
    }

    @Test
    public void testCutWwwAndM() {
        Assert.assertEquals("domain.ru", WwwUtil.cutWWWAndM("www.domain.ru"));
        Assert.assertEquals("www.ru", WwwUtil.cutWWWAndM("www.ru"));
        Assert.assertEquals("vasya.ru", WwwUtil.cutWWWAndM("m.vasya.ru"));
        Assert.assertEquals("m.vasya.ru", WwwUtil.cutWWWAndM("www.m.vasya.ru")); // TODO правильно ли?
        Assert.assertEquals("delta.ru", WwwUtil.cutWWWAndM(new WebmasterHostId(HTTPS, "www.delta.ru", 80)));
    }

}
