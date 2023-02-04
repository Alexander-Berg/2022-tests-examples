package ru.yandex.webmaster3.core.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.webmaster3.core.data.HostDomainInfo;

/**
 * @author aherman
 */
public class HostDomainInfoServiceTest {

    @Test
    public void testReadRules() throws Exception {
        String rules =
                "com\n"
                + "\n"
                + "*.jp\n"
                + "// Hosts in .hokkaido.jp can't set cookies below level 4...\n"
                + "*.hokkaido.jp\n"
                + "*.tokyo.jp\n"
                + "// ...except hosts in pref.hokkaido.jp, which can set cookies at level 3.\n"
                + "!pref.hokkaido.jp\n"
                + "!metro.tokyo.jp";
        Map<String, HostDomainInfoService.Node> result = HostDomainInfoService
                .readRules(new BufferedReader(new StringReader(rules)));

        Assert.assertTrue(result.containsKey("com"));
        Assert.assertTrue(result.containsKey("jp"));
        Assert.assertTrue(result.containsKey("hokkaido.jp"));
        Assert.assertTrue(result.containsKey("tokyo.jp"));
    }

    @Test
    public void testSimple() throws Exception {
        String rules =
                "com\n"
                + "\n"
                + "*.jp\n"
                + "// Hosts in .hokkaido.jp can't set cookies below level 4...\n"
                + "*.hokkaido.jp\n"
                + "*.tokyo.jp\n"
                + "// ...except hosts in pref.hokkaido.jp, which can set cookies at level 3.\n"
                + "!pref.hokkaido.jp\n"
                + "!metro.tokyo.jp";
        Map<String, HostDomainInfoService.Node> result = HostDomainInfoService
                .readRules(new BufferedReader(new StringReader(rules)));

        HostDomainInfoService hostDomainInfoService = new HostDomainInfoService();
        hostDomainInfoService.setRulesMap(result);

        Assert.assertEquals("com", hostDomainInfoService.getOwner("foo.com"));
        Assert.assertEquals("bar.jp", hostDomainInfoService.getOwner("foo.bar.jp"));
        Assert.assertEquals("bar.jp", hostDomainInfoService.getOwner("bar.jp"));
        Assert.assertEquals("bar.hokkaido.jp", hostDomainInfoService.getOwner("foo.bar.hokkaido.jp"));
        Assert.assertEquals("bar.hokkaido.jp", hostDomainInfoService.getOwner("bar.hokkaido.jp"));
        Assert.assertEquals("bar.tokyo.jp", hostDomainInfoService.getOwner("foo.bar.tokyo.jp"));
        Assert.assertEquals("bar.tokyo.jp", hostDomainInfoService.getOwner("bar.tokyo.jp"));
    }

    /**
     * Some tests from https://publicsuffix.org/list/
     * @throws IOException
     */
    @Test
    public void testCheck() throws IOException {
        HostDomainInfoService hostDomainInfoService = new HostDomainInfoService();
        hostDomainInfoService.setHostOwnersData(new ClassPathResource("/effective_tld_names.dat"));
        hostDomainInfoService.init();

        // Unlisted TLD.
        Assert.assertEquals(null, hostDomainInfoService.getOwner("example"));
        Assert.assertEquals("example", hostDomainInfoService.getOwner("example.example"));
        Assert.assertEquals("example", hostDomainInfoService.getOwner("b.example.example"));
        Assert.assertEquals("example", hostDomainInfoService.getOwner("a.b.example.example"));
        // TLD with only 1 rule.
        Assert.assertEquals(null, hostDomainInfoService.getOwner("biz"));
        Assert.assertEquals("biz", hostDomainInfoService.getOwner("domain.biz"));
        Assert.assertEquals("biz", hostDomainInfoService.getOwner("b.domain.biz"));
        Assert.assertEquals("biz", hostDomainInfoService.getOwner("a.b.domain.biz"));
        // TLD with some 2-level rules.
        Assert.assertEquals(null, hostDomainInfoService.getOwner("com"));
        Assert.assertEquals("com", hostDomainInfoService.getOwner("example.com"));
        Assert.assertEquals("com", hostDomainInfoService.getOwner("b.example.com"));
        Assert.assertEquals("com", hostDomainInfoService.getOwner("a.b.example.com"));
        Assert.assertEquals("uk.com", hostDomainInfoService.getOwner("uk.com"));
        Assert.assertEquals("uk.com", hostDomainInfoService.getOwner("example.uk.com"));
        Assert.assertEquals("uk.com", hostDomainInfoService.getOwner("b.example.uk.com"));
        Assert.assertEquals("uk.com", hostDomainInfoService.getOwner("a.b.example.uk.com"));
        Assert.assertEquals("ac", hostDomainInfoService.getOwner("test.ac"));
        // TLD with only 1 (wildcard) rule.
        Assert.assertEquals(null, hostDomainInfoService.getOwner("cy"));
        Assert.assertEquals("c.cy", hostDomainInfoService.getOwner("c.cy"));
        Assert.assertEquals("c.cy", hostDomainInfoService.getOwner("b.c.cy"));
        Assert.assertEquals("c.cy", hostDomainInfoService.getOwner("a.b.c.cy"));
        // More complex TLD.
        Assert.assertEquals(null, hostDomainInfoService.getOwner("jp"));
        Assert.assertEquals("jp", hostDomainInfoService.getOwner("test.jp"));
        Assert.assertEquals("jp", hostDomainInfoService.getOwner("www.test.jp"));
        Assert.assertEquals("ac.jp", hostDomainInfoService.getOwner("ac.jp"));
        Assert.assertEquals("ac.jp", hostDomainInfoService.getOwner("test.ac.jp"));
        Assert.assertEquals("ac.jp", hostDomainInfoService.getOwner("www.test.ac.jp"));
        Assert.assertEquals("kyoto.jp", hostDomainInfoService.getOwner("kyoto.jp"));
        Assert.assertEquals("kyoto.jp", hostDomainInfoService.getOwner("test.kyoto.jp"));
        Assert.assertEquals("ide.kyoto.jp", hostDomainInfoService.getOwner("ide.kyoto.jp"));
        Assert.assertEquals("ide.kyoto.jp", hostDomainInfoService.getOwner("b.ide.kyoto.jp"));
        Assert.assertEquals("ide.kyoto.jp", hostDomainInfoService.getOwner("a.b.ide.kyoto.jp"));
        Assert.assertEquals("c.kobe.jp", hostDomainInfoService.getOwner("c.kobe.jp"));
        Assert.assertEquals("c.kobe.jp", hostDomainInfoService.getOwner("b.c.kobe.jp"));
        Assert.assertEquals("c.kobe.jp", hostDomainInfoService.getOwner("a.b.c.kobe.jp"));
        Assert.assertEquals("city.kobe.jp", hostDomainInfoService.getOwner("city.kobe.jp"));
        Assert.assertEquals("city.kobe.jp", hostDomainInfoService.getOwner("www.city.kobe.jp"));
        // TLD with a wildcard rule and exceptions.
        Assert.assertEquals(null, hostDomainInfoService.getOwner("ck"));
        Assert.assertEquals("test.ck", hostDomainInfoService.getOwner("test.ck"));
        Assert.assertEquals("test.ck", hostDomainInfoService.getOwner("b.test.ck"));
        Assert.assertEquals("test.ck", hostDomainInfoService.getOwner("a.b.test.ck"));
        Assert.assertEquals("www.ck", hostDomainInfoService.getOwner("www.ck"));
        Assert.assertEquals("www.ck", hostDomainInfoService.getOwner("www.www.ck"));
        // US K12.
        Assert.assertEquals(null, hostDomainInfoService.getOwner("us"));
        Assert.assertEquals("us", hostDomainInfoService.getOwner("test.us"));
        Assert.assertEquals("us", hostDomainInfoService.getOwner("www.test.us"));
        Assert.assertEquals("ak.us", hostDomainInfoService.getOwner("ak.us"));
        Assert.assertEquals("ak.us", hostDomainInfoService.getOwner("test.ak.us"));
        Assert.assertEquals("ak.us", hostDomainInfoService.getOwner("www.test.ak.us"));
        Assert.assertEquals("k12.ak.us", hostDomainInfoService.getOwner("k12.ak.us"));
        Assert.assertEquals("k12.ak.us", hostDomainInfoService.getOwner("test.k12.ak.us"));
        Assert.assertEquals("k12.ak.us", hostDomainInfoService.getOwner("www.test.k12.ak.us"));
        // Same as above, but punycoded.
        Assert.assertEquals("com.cn", hostDomainInfoService.getOwner("xn--85x722f.com.cn"));
        Assert.assertEquals("xn--55qx5d.cn", hostDomainInfoService.getOwner("xn--55qx5d.cn"));
        Assert.assertEquals("xn--55qx5d.cn", hostDomainInfoService.getOwner("xn--85x722f.xn--55qx5d.cn"));
        Assert.assertEquals("xn--55qx5d.cn", hostDomainInfoService.getOwner("www.xn--85x722f.xn--55qx5d.cn"));
        Assert.assertEquals("xn--55qx5d.cn", hostDomainInfoService.getOwner("shishi.xn--55qx5d.cn"));
        Assert.assertEquals(null, hostDomainInfoService.getOwner("xn--fiqs8s"));
        Assert.assertEquals("xn--fiqs8s", hostDomainInfoService.getOwner("xn--85x722f.xn--fiqs8s"));
        Assert.assertEquals("xn--fiqs8s", hostDomainInfoService.getOwner("www.xn--85x722f.xn--fiqs8s"));
        Assert.assertEquals("xn--fiqs8s", hostDomainInfoService.getOwner("shishi.xn--fiqs8s"));
    }

    @Test
    public void testDomainInfo() throws Exception {
        HostDomainInfoService hostDomainInfoService = new HostDomainInfoService();
        hostDomainInfoService.setHostOwnersData(new ClassPathResource("/effective_tld_names.dat"));
        hostDomainInfoService.init();

        HostDomainInfo spbRu = hostDomainInfoService.getDomainInfo("spb.ru");
        Assert.assertNull(spbRu.getPrefix());
        Assert.assertEquals("spb.ru", spbRu.getOwner());

        HostDomainInfo wwwSpbRu = hostDomainInfoService.getDomainInfo("www.spb.ru");
        Assert.assertEquals("www", wwwSpbRu.getPrefix());
        Assert.assertEquals("spb.ru", wwwSpbRu.getOwner());

        HostDomainInfo govSpbRu = hostDomainInfoService.getDomainInfo("gov.spb.ru");
        Assert.assertNull(govSpbRu.getPrefix());
        Assert.assertEquals("spb.ru", govSpbRu.getOwner());

        HostDomainInfo mskRu = hostDomainInfoService.getDomainInfo("msk.ru");
        Assert.assertNull(mskRu.getPrefix());
        Assert.assertEquals("msk.ru", mskRu.getOwner());

        HostDomainInfo taxiMskRu = hostDomainInfoService.getDomainInfo("taxi.msk.ru");
        Assert.assertNull(taxiMskRu.getPrefix());
        Assert.assertEquals("msk.ru", taxiMskRu.getOwner());
    }

    @Test
    public void testStrangeHostname() throws IOException {
        HostDomainInfoService hostDomainInfoService = new HostDomainInfoService();
        hostDomainInfoService.setHostOwnersData(new ClassPathResource("/effective_tld_names.dat"));
        hostDomainInfoService.init();

        HostDomainInfo domainInfo = hostDomainInfoService.getDomainInfo("ya");
        Assert.assertNotNull(domainInfo);
        Assert.assertNull(domainInfo.getOwner());
        Assert.assertNull(domainInfo.getPrefix());
        Assert.assertNotNull(domainInfo.getDomainMiddlePart());
    }
}
