package com.yandex.mail.util;

import android.util.Log;

import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// https://android.googlesource.com/platform/cts/+/master/tests/tests/net/src/android/net/cts/MailToTest.java
@RunWith(IntegrationTestRunner.class)
public class MailToFixedTest {

    private static final String MAILTOURI_1 = "mailto:chris@example.com";

    private static final String MAILTOURI_2 = "mailto:infobot@example.com?subject=current-issue";

    private static final String MAILTOURI_3 =
            "mailto:infobot@example.com?body=send%20current-issue";

    private static final String MAILTOURI_4 = "mailto:infobot@example.com?body=send%20current-" +
                                              "issue%0D%0Asend%20index";

    private static final String MAILTOURI_5 = "mailto:joe@example.com?" +
                                              "cc=bob@example.com&body=hello";

    private static final String MAILTOURI_6 = "mailto:?to=joe@example.com&" +
                                              "cc=bob@example.com&body=hello";

    @Test
    public void testParseMailToURI() {
        assertFalse(MailToFixed.isMailTo(null));
        assertFalse(MailToFixed.isMailTo(""));
        assertFalse(MailToFixed.isMailTo("http://www.google.com"));
        assertTrue(MailToFixed.isMailTo(MAILTOURI_1));
        MailToFixed mailTo_1 = MailToFixed.parse(MAILTOURI_1);
        Log.d("Trace", mailTo_1.toString());
        assertEquals("chris@example.com", mailTo_1.getTo());
        assertEquals(1, mailTo_1.getHeaders().size());
        assertNull(mailTo_1.getBody());
        assertNull(mailTo_1.getCc());
        assertNull(mailTo_1.getSubject());
        assertEquals("mailto:?to=chris%40example.com&", mailTo_1.toString());
        assertTrue(MailToFixed.isMailTo(MAILTOURI_2));
        MailToFixed mailTo_2 = MailToFixed.parse(MAILTOURI_2);
        Log.d("Trace", mailTo_2.toString());
        assertEquals(2, mailTo_2.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_2.getTo());
        assertEquals("current-issue", mailTo_2.getSubject());
        assertNull(mailTo_2.getBody());
        assertNull(mailTo_2.getCc());
        String stringUrl = mailTo_2.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("subject=current-issue&"));
        assertTrue(MailToFixed.isMailTo(MAILTOURI_3));
        MailToFixed mailTo_3 = MailToFixed.parse(MAILTOURI_3);
        Log.d("Trace", mailTo_3.toString());
        assertEquals(2, mailTo_3.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_3.getTo());
        assertEquals("send current-issue", mailTo_3.getBody());
        assertNull(mailTo_3.getCc());
        assertNull(mailTo_3.getSubject());
        stringUrl = mailTo_3.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("body=send%20current-issue&"));
        assertTrue(MailToFixed.isMailTo(MAILTOURI_4));
        MailToFixed mailTo_4 = MailToFixed.parse(MAILTOURI_4);
        Log.d("Trace", mailTo_4.toString() + " " + mailTo_4.getBody());
        assertEquals(2, mailTo_4.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_4.getTo());
        assertEquals("send current-issue\r\nsend index", mailTo_4.getBody());
        assertNull(mailTo_4.getCc());
        assertNull(mailTo_4.getSubject());
        stringUrl = mailTo_4.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("body=send%20current-issue%0D%0Asend%20index&"));
        assertTrue(MailToFixed.isMailTo(MAILTOURI_5));
        MailToFixed mailTo_5 = MailToFixed.parse(MAILTOURI_5);
        Log.d("Trace", mailTo_5.toString() + mailTo_5.getHeaders().toString()
                       + mailTo_5.getHeaders().size());
        assertEquals(3, mailTo_5.getHeaders().size());
        assertEquals("joe@example.com", mailTo_5.getTo());
        assertEquals("bob@example.com", mailTo_5.getCc());
        assertEquals("hello", mailTo_5.getBody());
        assertNull(mailTo_5.getSubject());
        stringUrl = mailTo_5.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("cc=bob%40example.com&"));
        assertTrue(stringUrl.contains("body=hello&"));
        assertTrue(stringUrl.contains("to=joe%40example.com&"));
        assertTrue(MailToFixed.isMailTo(MAILTOURI_6));
        MailToFixed mailTo_6 = MailToFixed.parse(MAILTOURI_6);
        Log.d("Trace", mailTo_6.toString() + mailTo_6.getHeaders().toString()
                       + mailTo_6.getHeaders().size());
        assertEquals(3, mailTo_6.getHeaders().size());
        assertEquals(", joe@example.com", mailTo_6.getTo());
        assertEquals("bob@example.com", mailTo_6.getCc());
        assertEquals("hello", mailTo_6.getBody());
        assertNull(mailTo_6.getSubject());
        stringUrl = mailTo_6.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("cc=bob%40example.com&"));
        assertTrue(stringUrl.contains("body=hello&"));
        assertTrue(stringUrl.contains("to=%2C%20joe%40example.com&"));
    }

    @Test
    public void testParseMailToFixed() {
        // MOBILEMAIL-7293
        String email = "mailto:cats@sgdot.ru?subject=list%3Acategory%3Dhats";
        MailToFixed mailToFixed = MailToFixed.parse(email);
        assertEquals("list:category=hats", mailToFixed.getSubject());
    }

    @Test
    public void testParseMailToFixed2() {
        // MOBILEMAIL-7293
        String email = "mailto:sd@atol.ru?subject=Update_:5Inc.*0000178391&body=Оценка5";
        MailToFixed mailToFixed = MailToFixed.parse(email);
        assertEquals("Update_:5Inc.*0000178391", mailToFixed.getSubject());
        assertEquals("Оценка5", mailToFixed.getBody());
    }

    @Test
    public void testParseLongMailToFixedWithSpaces() {
        // MOBILEMAIL-12004
        String email = "mailto:MyFeedback@mts.ru?subject=%D0%9F%D1%80%D0%B5%D0%B4%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D0%B5%20%D0%BF%D0%BE%20%D1%83%D0%BB%D1%83%D1%87%D1%88%D0%B5%D0%BD%D0%B8%D1%8E%20%D0%9C%D0%BE%D0%B9%20%D0%9C%D0%A2%D0%A1&body=%D0%9F%D0%BE%D0%B6%D0%B0%D0%BB%D1%83%D0%B9%D1%81%D1%82%D0%B0%2C%20%D0%B4%D0%BE%D0%B1%D0%B0%D0%B2%D1%8C%D1%82%D0%B5%20...%0A%0A%D0%AD%D1%82%D0%BE%20%D0%BF%D0%BE%D0%B7%D0%B2%D0%BE%D0%BB%D0%B8%D1%82%20...%0A%0A%0A%0A--------------------------------------------------%0A%D0%9F%D0%BE%D0%B6%D0%B0%D0%BB%D1%83%D0%B9%D1%81%D1%82%D0%B0%2C %D0%BD%D0%B5 %D1%83%D0%B4%D0%B0%D0%BB%D1%8F%D0%B9%D1%82%D0%B5 %D1%8D%D1%82%D1%83 %D0%B8%D0%BD%D1%84%D0%BE%D1%80%D0%BC%D0%B0%D1%86%D0%B8%D1%8E%2C %D0%BE%D0%BD%D0%B0 %D0%BF%D0%BE%D0%BC%D0%BE%D0%B6%D0%B5%D1%82 %D0%BD%D0%B0%D0%BC %D0%BF%D1%80%D0%B8 %D0%B0%D0%BD%D0%B0%D0%BB%D0%B8%D0%B7%D0%B5 %D0%B2%D0%B0%D1%88%D0%B5%D0%B3%D0%BE %D0%BE%D0%B1%D1%80%D0%B0%D1%89%D0%B5%D0%BD%D0%B8%D1%8F%0A--------------------------------------------------%0A%D0%92%D0%B5%D1%80%D1%81%D0%B8%D1%8F %D0%BF%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D1%8F %D0%9C%D0%BE%D0%B9 %D0%9C%D0%A2%D0%A1%3A 4.21%2840210083%29%0A%D0%92%D0%B5%D1%80%D1%81%D0%B8%D1%8F CMS%3A 14.21.0a%0A%D0%A0%D0%B5%D0%B3%D0%B8%D0%BE%D0%BD%3A 1801%0A%D0%A4%D0%98%D0%9E%3A%D0%9F%D0%B0%D0%B2%D0%BB%D0%BE%D0%B2 %D0%A1%D0%B2%D1%8F%D1%82%D0%BE%D1%81%D0%BB%D0%B0%D0%B2 %D0%94%D0%BC%D0%B8%D1%82%D1%80%D0%B8%D0%B5%D0%B2%D0%B8%D1%87%0A%D0%9D%D0%BE%D0%BC%D0%B5%D1%80 %D1%82%D0%B5%D0%BB%D0%B5%D1%84%D0%BE%D0%BD%D0%B0%3A 79111742565%0AForisId%3A 11619%0A%D0%A3%D1%81%D1%82%D1%80%D0%BE%D0%B9%D1%81%D1%82%D0%B2%D0%BE%3A htc HTC 10%0A%D0%92%D0%B5%D1%80%D1%81%D0%B8%D1%8F %D0%9E%D0%A1%3A 7.0%0A%D0%A2%D0%B0%D1%80%D0%B8%D1%84%3A %D0%A1%D0%B0%D0%BD%D0%BA%D1%82-%D0%9F%D0%B5%D1%82%D0%B5%D1%80%D0%B1%D1%83%D1%80%D0%B3 - Smart Nonstop 122015 %28%D0%9C%D0%90%D0%A1%D0%A1%29 %28SCP%29%0A%0A%D0%92%D0%B5%D1%80%D1%81%D0%B8%D1%8F %D1%81%D0%BF%D1%80%D0%B0%D0%B2%D0%BE%D1%87%D0%BD%D0%B8%D0%BA%D0%BE%D0%B2%3A%0Aadvertising%3A 54134%0Abonus%3A 53821%0Aconfiguration%3A 37373%0Afaq%3A 53255%0Amaintenance%3A 53210%0Apopup%3A 53631%0Aregions%3A 53980%0Arest%3A 54160%0Aservice%3A 54199%0Ashops%3A 53980%0Atariff%3A 54074%0Atariff_current%3A 1641%0Atravel%3A 53704%0A%0A%D0%92%D1%80%D0%B5%D0%BC%D1%8F %D0%BF%D0%BE%D1%81%D0%BB%D0%B5%D0%B4%D0%BD%D0%B5%D0%B3%D0%BE %D0%BE%D0%B1%D0%BD%D0%BE%D0%B2%D0%BB%D0%B5%D0%BD%D0%B8%D1%8F%3A%0Abalance%3A 2019-02-05 11%3A00%3A28%0Acounters%3A 2018-08-06 20%3A38%3A06%0Ainternet%3A 2018-10-31 15%3A21%3A25%0Atariff%3A 2019-02-05 11%3A07%3A34%0Aservices%3A null%0Asubscription_list%3A 2019-02-05 11%3A00%3A26%0Abonuses%3A 2019-02-05 11%3A00%3A26%0A%0AFCM Token%3A %0A%0A%D0%94%D0%B0%D1%82%D0%B0%3A 5 %D1%84%D0%B5%D0%B2%D1%80%D0%B0%D0%BB%D1%8F 2019%2C 11%3A07%0A";
        MailToFixed mailToFixed = MailToFixed.parse(email);
        assertEquals("MyFeedback@mts.ru", mailToFixed.getTo());
        String body = "Пожалуйста, добавьте ...\n" +
                "\n" +
                "Это позволит ...\n" +
                "\n" +
                "\n" +
                "\n" +
                "--------------------------------------------------\n" +
                "Пожалуйста, не удаляйте эту информацию, она поможет нам при анализе вашего обращения\n" +
                "--------------------------------------------------\n" +
                "Версия приложения Мой МТС: 4.21(40210083)\n" +
                "Версия CMS: 14.21.0a\n" +
                "Регион: 1801\n" +
                "ФИО:Павлов Святослав Дмитриевич\n" +
                "Номер телефона: 79111742565\n" +
                "ForisId: 11619\n" +
                "Устройство: htc HTC 10\n" +
                "Версия ОС: 7.0\n" +
                "Тариф: Санкт-Петербург - Smart Nonstop 122015 (МАСС) (SCP)\n" +
                "\n" +
                "Версия справочников:\n" +
                "advertising: 54134\n" +
                "bonus: 53821\n" +
                "configuration: 37373\n" +
                "faq: 53255\n" +
                "maintenance: 53210\n" +
                "popup: 53631\n" +
                "regions: 53980\n" +
                "rest: 54160\n" +
                "service: 54199\n" +
                "shops: 53980\n" +
                "tariff: 54074\n" +
                "tariff_current: 1641\n" +
                "travel: 53704\n" +
                "\n" +
                "Время последнего обновления:\n" +
                "balance: 2019-02-05 11:00:28\n" +
                "counters: 2018-08-06 20:38:06\n" +
                "internet: 2018-10-31 15:21:25\n" +
                "tariff: 2019-02-05 11:07:34\n" +
                "services: null\n" +
                "subscription_list: 2019-02-05 11:00:26\n" +
                "bonuses: 2019-02-05 11:00:26\n" +
                "\n" +
                "FCM Token: \n" +
                "\n" +
                "Дата: 5 февраля 2019, 11:07\n";
        assertEquals(body, mailToFixed.getBody());
    }
}
