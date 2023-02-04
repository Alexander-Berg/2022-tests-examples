package ru.yandex.webmaster3.viewer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.webmaster3.viewer.http.camelcase.data.BeautyHostName;

/**
 * User: azakharov
 * Date: 15.05.15
 * Time: 13:06
 */
public class W3CheckHostNameServiceTest {
    private static final Map<String, W3CheckHostNameService.DisplayNameCheckResult> data = new HashMap<String, W3CheckHostNameService.DisplayNameCheckResult>() {
        {
            put("www", W3CheckHostNameService.DisplayNameCheckResult.ERROR_TOO_LITTLE_DOMAIN_NAME_PARTS);
            put("www.yandex.Ru", W3CheckHostNameService.DisplayNameCheckResult.ERROR_TLD_CASE_CHANGE);
            put("leha.Hotmail.ru", W3CheckHostNameService.DisplayNameCheckResult.ERROR_WRONG_GEO_NAME_CASE);
            put("www.atALTAVIsta.com", W3CheckHostNameService.DisplayNameCheckResult.ERROR_TOO_MANY_CONSECUTIVE_UPPER_LETTERS);
            put("W3.yandex.ru", W3CheckHostNameService.DisplayNameCheckResult.ERROR_WRONG_WWW_CASE);

            put("born.USSR.ru", W3CheckHostNameService.DisplayNameCheckResult.GOOD);
            put("lariosik.Zhitomir.USSR.ru", W3CheckHostNameService.DisplayNameCheckResult.GOOD);
            put("www.AltaVista.com", W3CheckHostNameService.DisplayNameCheckResult.GOOD);
            put("www.AltaVista.Zhitomir.USSR.ru", W3CheckHostNameService.DisplayNameCheckResult.GOOD);

            put("born.inUSSR.ru", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("Born.USSR.ru", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("www.atALTAVista.com", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("www.AVista.com", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("www.AvIsta.com", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("www.AltavisTa.com", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("www.AltaVistaSite.com", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("www.AltAviStaSiteWithManyUppers.com", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("www.Zenet-Shop.ru", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);
            put("Blogs-People.ru", W3CheckHostNameService.DisplayNameCheckResult.NEEDS_MODERATION);

            put("www.yandex.ru", W3CheckHostNameService.DisplayNameCheckResult.GOOD);
            put("www.altavista.com", W3CheckHostNameService.DisplayNameCheckResult.GOOD);
            put("leha.hotmail.ru", W3CheckHostNameService.DisplayNameCheckResult.GOOD);
        }
    };

    private W3CheckHostNameService checkNameService;

    @Before
    public void setUp() throws Exception {
        checkNameService = new W3CheckHostNameService();
        checkNameService.setGeoListResource(new ClassPathResource("/webmaster-common-checkhostname-geolist.txt"));
        checkNameService.setIbmListResource(new ClassPathResource("/webmaster-common-checkhostname-ibmlist.txt"));
        checkNameService.setTownListResource(new ClassPathResource("/webmaster-common-checkhostname-townlist.txt"));
        checkNameService.init();
    }

    @Test
    public void testCheckHostName() {
        for (String hostName : data.keySet()) {
            Assert.assertEquals("Incorrect host name check status for: " + hostName, data.get(hostName),
                    checkNameService.checkDisplayName(hostName));
        }
    }

    @Test
    public void testGetDisplayNameWwwRu() {
        BeautyHostName result = checkNameService.getDisplayName("www.ru");
        Assert.assertNull(result.getPrefix());
        Assert.assertEquals("www", result.getMain());
        Assert.assertEquals("ru", result.getSuffix());
    }

    @Test
    public void testGetDisplayNameWwwYandexRu() {
        BeautyHostName result = checkNameService.getDisplayName("www.yandex.ru");
        Assert.assertEquals("www", result.getPrefix());
        Assert.assertEquals("yandex", result.getMain());
        Assert.assertEquals("ru", result.getSuffix());
    }

    @Test
    public void testGetDisplayNameYandexRu() {
        BeautyHostName result = checkNameService.getDisplayName("yandex.ru");
        Assert.assertNull(result.getPrefix());
        Assert.assertEquals("yandex", result.getMain());
        Assert.assertEquals("ru", result.getSuffix());
    }

    @Test
    public void testGetDisplayNameWebmasterYandexRu() {
        BeautyHostName result = checkNameService.getDisplayName("webmaster.yandex.ru");
        Assert.assertNull(result.getPrefix());
        Assert.assertEquals("webmaster.yandex", result.getMain());
        Assert.assertEquals("ru", result.getSuffix());
    }

    @Test
    public void testGetDisplayNameGeodomain2Parts() {
        BeautyHostName result = checkNameService.getDisplayName("habrahabr.ru");
        Assert.assertNull(result.getPrefix());
        Assert.assertNull(result.getMain());
        Assert.assertEquals("habrahabr.ru", result.getSuffix());
    }
}
