package ru.yandex.webmaster3.worker.notifications;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author akhazhoyan 02/2019
 */
public class EuEmailServiceTest {

    @Test
    public void normalize_mapsAllYandexDomainsToYandexRu() {
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@ya.ru"));

        assertEquals("abc@yandex-team.ru", EuEmailService.normalize("abc@yandex-team.ru"));

        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.com"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.com.am"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.com.ge"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.com.ru"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.com.tr"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.com.ua"));

        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.co.il"));

        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.ee"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.az"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.by"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.fr"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.kg"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.kz"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.lt"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.lv"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.md"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.ru"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.tj"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.tm"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.ua"));
        assertEquals("abc@yandex.ru", EuEmailService.normalize("abc@yandex.uz"));
    }

    @Test
    public void normalize_mapsDotToHyphenForYandexDomains() {
        assertEquals("aa-bbcc@yandex.ru", EuEmailService.normalize("aa.bbcc@yandex.ru"));
        assertEquals("aa-bb-cc@yandex.ru", EuEmailService.normalize("aa.bb.cc@yandex.ru"));
        assertEquals("aa-bb-cc@yandex.ru", EuEmailService.normalize("aa-bb.cc@yandex.ru"));
        assertEquals("aa-bb-cc@yandex.ru", EuEmailService.normalize("aa-bb-cc@yandex.ru"));
    }

    @Test
    public void normalize_doesNotMapDotToHyphenForNonYandexDomains() {
        assertEquals("aa.bbcc@example.com", EuEmailService.normalize("aa.bbcc@example.com"));
        assertEquals("aa.bb.cc@example.com", EuEmailService.normalize("aa.bb.cc@example.com"));
        assertEquals("aa-bb.cc@example.com", EuEmailService.normalize("aa-bb.cc@example.com"));
        assertEquals("aa-bb-cc@example.com", EuEmailService.normalize("aa-bb-cc@example.com"));
    }

    @Test
    public void normalize_lowers() {
        assertEquals("aabbcc@example.com", EuEmailService.normalize("aaBBcc@example.com"));
        assertEquals("aabbcc@example.com", EuEmailService.normalize("AABBCC@example.com"));
    }

    @Test
    public void normalize_trims() {
        assertEquals("aabbcc@example.com", EuEmailService.normalize("  aabbcc@example.com "));
        assertEquals("aabbcc@example.com", EuEmailService.normalize("aabbcc@example.com  "));
        assertEquals("aabbcc@example.com", EuEmailService.normalize(" aabbcc@example.com \n\t"));
    }

    @Test
    public void normalize_mapsGooglemailToGmail() {
        assertEquals("abc@gmail.com", EuEmailService.normalize("abc@googlemail.com"));
    }

    @Test
    public void normalize_testAltogether() {
        assertEquals("aa-bb-cc@yandex.ru", EuEmailService.normalize("  AA.bb-Cc@ya.ru \n "));
        assertEquals("aa.bb-cc@gmail.com", EuEmailService.normalize("  AA.bb-Cc@googlemail.com \n "));
    }

    @Test
    public void testNormalize() {
        assertEquals("andrey@yandex.ru",EuEmailService.normalize("andrey@yandex.ru"));
        assertEquals( "andrey@yandex.ru",EuEmailService.normalize("andrey@yandex.com"));
        assertEquals("andrey@01yandex.kz",EuEmailService.normalize("andrey@01yandex.kz"));
        assertEquals(".@a.ru",EuEmailService.normalize(".@a.ru"));
        assertEquals("test@yandex-team.ru",EuEmailService.normalize("test@yandex-team.ru"));
        assertEquals("test@yandex.ri",EuEmailService.normalize("test@yandex.ri"));
        assertEquals("строка-с-кирилицей@яндекс.рф", EuEmailService.normalize( "строка-с-кирилицей@яндекс.рф"));
    }
}
