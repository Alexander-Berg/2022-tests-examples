package ru.yandex.webmaster3.core.domain;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ru.yandex.webmaster3.core.util.IdUtils;

import java.io.IOException;

/**
 * Created by ifilippov5 on 09.11.17.
 */
public class PublicSuffixListCacheServiceTest {
    /**
     * Tests from https://publicsuffix.org/list/
     * @throws IOException
     */
    @Test
    public void testCheck() throws IOException {
        PublicSuffixListCacheService service = new PublicSuffixListCacheService();
        service.setPublicSuffixListData(new ClassPathResource("/public_suffix_list.dat"));
        service.setWebmasterSuffixList(new ClassPathResource("/webmaster_suffix_list.dat"));

        // null input.
        Assert.assertEquals(null, null);
// Mixed case.
        Assert.assertEquals(null, service.getOwner("COM"));
        Assert.assertEquals("example.com", service.getOwner("example.COM"));
        Assert.assertEquals("example.com", service.getOwner("WwW.example.COM"));
// Leading dot.
        Assert.assertEquals(null, service.getOwner(".com"));
        Assert.assertEquals(null, service.getOwner(".example"));
        Assert.assertEquals(null, service.getOwner(".example.com"));
        Assert.assertEquals(null, service.getOwner(".example.example"));
// Unlisted TLD.
        Assert.assertEquals(null, service.getOwner("example"));
        Assert.assertEquals("example.example", service.getOwner("example.example"));
        Assert.assertEquals("example.example", service.getOwner("b.example.example"));
        Assert.assertEquals("example.example", service.getOwner("a.b.example.example"));

// TLD with only 1 rule.
        Assert.assertEquals(null, service.getOwner("biz"));
        Assert.assertEquals("domain.biz", service.getOwner("domain.biz"));
        Assert.assertEquals("domain.biz", service.getOwner("b.domain.biz"));
        Assert.assertEquals("domain.biz", service.getOwner("a.b.domain.biz"));
// TLD with some 2-level rules.
        Assert.assertEquals(null, service.getOwner("com"));
        Assert.assertEquals("example.com", service.getOwner("example.com"));
        Assert.assertEquals("example.com", service.getOwner("b.example.com"));
        Assert.assertEquals("example.com", service.getOwner("a.b.example.com"));
        Assert.assertEquals(null, service.getOwner("uk.com"));
        Assert.assertEquals("example.uk.com", service.getOwner("example.uk.com"));
        Assert.assertEquals("example.uk.com", service.getOwner("b.example.uk.com"));
        Assert.assertEquals("example.uk.com", service.getOwner("a.b.example.uk.com"));
        Assert.assertEquals("test.ac", service.getOwner("test.ac"));
// TLD with only 1 (wildcard)) rule.
        Assert.assertEquals(null, service.getOwner("mm"));
        Assert.assertEquals(null, service.getOwner("c.mm"));
        Assert.assertEquals("b.c.mm", service.getOwner("b.c.mm"));
        Assert.assertEquals("b.c.mm", service.getOwner("a.b.c.mm"));
// More complex TLD.
        Assert.assertEquals(null, service.getOwner("jp"));
        Assert.assertEquals("test.jp", service.getOwner("test.jp"));
        Assert.assertEquals("test.jp", service.getOwner("www.test.jp"));
        Assert.assertEquals(null, service.getOwner("ac.jp"));
        Assert.assertEquals("test.ac.jp", service.getOwner("test.ac.jp"));
        Assert.assertEquals("test.ac.jp", service.getOwner("www.test.ac.jp"));
        Assert.assertEquals(null, service.getOwner("kyoto.jp"));
        Assert.assertEquals("test.kyoto.jp", service.getOwner("test.kyoto.jp"));
        Assert.assertEquals(null, service.getOwner("ide.kyoto.jp"));
        Assert.assertEquals("b.ide.kyoto.jp", service.getOwner("b.ide.kyoto.jp"));
        Assert.assertEquals("b.ide.kyoto.jp", service.getOwner("a.b.ide.kyoto.jp"));
        Assert.assertEquals(null, service.getOwner("c.kobe.jp"));
        Assert.assertEquals("b.c.kobe.jp", service.getOwner("b.c.kobe.jp"));
        Assert.assertEquals("b.c.kobe.jp", service.getOwner("a.b.c.kobe.jp"));
        Assert.assertEquals("city.kobe.jp", service.getOwner("city.kobe.jp"));
        Assert.assertEquals("city.kobe.jp", service.getOwner("www.city.kobe.jp"));
// TLD with a wildcard rule and exceptions.
        Assert.assertEquals(null, service.getOwner("ck"));
        Assert.assertEquals(null, service.getOwner("test.ck"));
        Assert.assertEquals("b.test.ck", service.getOwner("b.test.ck"));
        Assert.assertEquals("b.test.ck", service.getOwner("a.b.test.ck"));
        Assert.assertEquals("www.ck", service.getOwner("www.ck"));
        Assert.assertEquals("www.ck", service.getOwner("www.www.ck"));
// US K12.
        Assert.assertEquals(null, service.getOwner("us"));
        Assert.assertEquals("test.us", service.getOwner("test.us"));
        Assert.assertEquals("test.us", service.getOwner("www.test.us"));
        Assert.assertEquals(null, service.getOwner("ak.us"));
        Assert.assertEquals("test.ak.us", service.getOwner("test.ak.us"));
        Assert.assertEquals("test.ak.us", service.getOwner("www.test.ak.us"));
        Assert.assertEquals(null, service.getOwner("k12.ak.us"));
        Assert.assertEquals("test.k12.ak.us", service.getOwner("test.k12.ak.us"));
        Assert.assertEquals("test.k12.ak.us", service.getOwner("www.test.k12.ak.us"));
// IDN labels.
        Assert.assertEquals(IdUtils.IDN.toASCII("食狮.com.cn"), service.getOwner("食狮.com.cn"));
        Assert.assertEquals(IdUtils.IDN.toASCII("食狮.公司.cn"), service.getOwner("食狮.公司.cn"));
        Assert.assertEquals(IdUtils.IDN.toASCII("食狮.公司.cn"), service.getOwner("www.食狮.公司.cn"));
        Assert.assertEquals(IdUtils.IDN.toASCII("shishi.公司.cn"), service.getOwner("shishi.公司.cn"));
        Assert.assertEquals(null, service.getOwner("公司.cn"));
        Assert.assertEquals(IdUtils.IDN.toASCII("食狮.中国"), service.getOwner("食狮.中国"));
        Assert.assertEquals(IdUtils.IDN.toASCII("食狮.中国"), service.getOwner("www.食狮.中国"));
        Assert.assertEquals(IdUtils.IDN.toASCII("shishi.中国"), service.getOwner("shishi.中国"));
        Assert.assertEquals(null, service.getOwner("中国"));
// Same as above, service.getOwner(but punycoded.
        Assert.assertEquals("xn--85x722f.com.cn", service.getOwner("xn--85x722f.com.cn"));
        Assert.assertEquals("xn--85x722f.xn--55qx5d.cn", service.getOwner("xn--85x722f.xn--55qx5d.cn"));
        Assert.assertEquals("xn--85x722f.xn--55qx5d.cn", service.getOwner("www.xn--85x722f.xn--55qx5d.cn"));
        Assert.assertEquals("shishi.xn--55qx5d.cn", service.getOwner("shishi.xn--55qx5d.cn"));
        Assert.assertEquals(null, service.getOwner("xn--55qx5d.cn"));
        Assert.assertEquals("xn--85x722f.xn--fiqs8s", service.getOwner("xn--85x722f.xn--fiqs8s"));
        Assert.assertEquals("xn--85x722f.xn--fiqs8s", service.getOwner("www.xn--85x722f.xn--fiqs8s"));
        Assert.assertEquals("shishi.xn--fiqs8s", service.getOwner("shishi.xn--fiqs8s"));
        Assert.assertEquals(null, service.getOwner("xn--fiqs8s"));

// With webmaster suffix list
        Assert.assertEquals("example.tam.by", service.getOwner("example.tam.by"));

    }
}
