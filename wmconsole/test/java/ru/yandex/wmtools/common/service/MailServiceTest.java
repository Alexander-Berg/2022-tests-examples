package ru.yandex.wmtools.common.service;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * User: azakharov
 * Date: 30.03.12
 * Time: 12:44
 */
public class MailServiceTest {
    private MailService mailService = new MailService();

    @Test
    public void testGetYandexServiceHeaderValue() {
        String date = "Fri, 27 Mar 2009 01:14:46 +0300";
        String from = "alsun (Я.ру) <devnull@yandex.ru>";
        String subject = "Штука для снятия стрессов";
        String label = "yaru";
        String hdrValue = mailService.getYandexServiceHeaderValue(date, from, subject, label);
        assertEquals("eWFydSBiOWQ2YTc3ZmJmODkyMDk5NGJiNzZmNWZlMmM2ZDZhYg==", hdrValue);
    }
}
