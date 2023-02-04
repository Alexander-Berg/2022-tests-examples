package ru.yandex.webmaster3.core.robotstxt;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Oleg Bazdyrev on 30/07/2018.
 */
public class RobotsTxtUtilsTest {

    private static final String ROBOTS_1 = "" +
            "User-agent: Yandex  \n" +
            "Allow: /  \n" +
            "Disallow: /test \n" +
            "Allow: /*/vasya \n" +
            "Allow: /t2*/vasya \n" +
            "Disallow: /test2  \n";


    private static final String ROBOTS_2 = "" +
            "User-agent: Yandex  \n" +
            "Allow: /  \n" +
            "Disallow: /test \n" +
            "Allow: /t2*/vasya \n" +
            "Disallow: /test2  \n";

    private static final String ROBOTS_4 = "" +
            "User-agent: *\n" +
            "Disallow: /private/\n" +
            "";

    private static final String ROBOTS_5 = "" +
            "User-agent: *\n" +
            "Disallow: /\n" +
            "";


    @Test
    public void test1() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/test", ROBOTS_1);
        Assert.assertTrue("Есть паттерн, под который может попасть префикс", result.isAllow());
        Assert.assertEquals("/*/vasya", result.getRule());
    }

    @Test
    public void test2() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/test", ROBOTS_2);
        Assert.assertTrue("Нет паттернов, под которые может попасть префикс", !result.isAllow());
        Assert.assertEquals("/test", result.getRule());
    }

    private static final String ROBOTS_3 = "" +
            "User-agent: *\n" +
            "Allow: /\n" +
            "Disallow: /private\n" +
            "some\n" +
            "trash\n" +
            "in\n" +
            "file\n" +
            "\n" +
            "\n" +
            "User-agent: Yandex  \n" +
            "Allow: /  \n" +
            "Disallow: /vasya/private \n" +
            "Allow: /vasya \n" +
            "Disallow: /private  \n";

    @Test
    public void test3() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/vasya/private", ROBOTS_3);
        Assert.assertTrue("Нет паттернов, под которые может попасть префикс", !result.isAllow());
        Assert.assertEquals("/vasya/private", result.getRule());
    }

    @Test
    public void test4() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/vasya", ROBOTS_4);
        Assert.assertTrue(result.isAllow());
        Assert.assertEquals("", result.getRule());
    }

    @Test
    public void test5() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/", ROBOTS_5);
        Assert.assertTrue(!result.isAllow());
        Assert.assertEquals("/", result.getRule());
    }


    private static final String LENTA_RU = "User-agent: YandexBot\n" +
            "Allow: /rss/yandexfull/turbo\n" +
            "\n" +
            "User-agent: Yandex\n" +
            "Disallow: /search\n" +
            "Disallow: /check_ed\n" +
            "Disallow: /auth\n" +
            "Disallow: /my\n" +
            "Disallow: /core\n" +
            "Host: https://lenta.ru\n" +
            "\n" +
            "User-agent: GoogleBot\n" +
            "Disallow: /search\n" +
            "Disallow: /check_ed\n" +
            "Disallow: /auth\n" +
            "Disallow: /my\n" +
            "Disallow: /core\n" +
            "\n" +
            "User-agent: *\n" +
            "Disallow: /search\n" +
            "Disallow: /check_ed\n" +
            "Disallow: /auth\n" +
            "Disallow: /my\n" +
            "Disallow: /core\n" +
            "\n" +
            "Sitemap: https://lenta.ru/sitemap.xml.gz";

    @Test
    public void testLenta() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/core/", LENTA_RU);
        Assert.assertTrue(!result.isAllow());
        Assert.assertEquals("/core", result.getRule());
    }

    public static final String ROBOTS_6 = "" +
            "User-Agent: *\n" +
            "AlloW: /test/\n" +
            "Disallow: /*extremely-long-path\n";

    @Test
    public void test6() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/test/", ROBOTS_6);
        Assert.assertTrue(result.isAllow());
        Assert.assertEquals("/test/", result.getRule());
    }

    public static final String ROBOTS_7 = "" +
            "User-Agent: *\n" +
            "Disallow: /pda/\n" +
            "Allow: /*?v=*\n";

    /**
     * Если в правиле есть * или $ - то не учитываем их в длине
     * @throws Exception
     */
    @Test
    public void test7() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/pda/", ROBOTS_7);
        Assert.assertFalse(result.isAllow());
        Assert.assertEquals("/pda/", result.getRule());
    }

    public static final String ROBOTS_8 = "" +
            "User-agent: *\n" +
            "Disallow: /voeiq24g/\n" +
            "\n" +
            "User-agent: Yandex\n" +
            "Disallow: /voeiq24g/\n" +
            "\n" +
            "Sitemap: https://queenfurs.ru/sitemap.xml";

    @Test
    public void test8() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/voeiq24g/", ROBOTS_8);
        Assert.assertFalse(result.isAllow());
        Assert.assertEquals("/voeiq24g/", result.getRule());
    }

    public static final String ROBOTS_9 =
            "User-agent: Yandex\n" +
            "Disallow: /search/*\n" +
            "Disallow: /*route=account/\n" +
            "Disallow: /*route=affiliate/\n" +
            "Disallow: /*route=checkout/\n" +
            "Disallow: /*route=product/search\n" +
            "Disallow: /index.php?route=product/product*&manufacturer_id=\n" +
            "Disallow: /admin\n" +
            "Disallow: /catalog\n" +
            "Disallow: /download\n" +
            "Disallow: /cart\n" +
            "Disallow: /login\n" +
            "Clean-param: tracking";

    @Test
    public void test9() throws Exception {
        AllowPrefixInfo result = RobotsTxtUtils.isPrefixAllowed("/search/", ROBOTS_9);
        Assert.assertFalse(result.isAllow());
        Assert.assertEquals("/search/*", result.getRule());
    }
}
