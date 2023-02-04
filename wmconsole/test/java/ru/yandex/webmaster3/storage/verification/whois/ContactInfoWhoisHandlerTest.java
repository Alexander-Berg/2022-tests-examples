package ru.yandex.webmaster3.storage.verification.whois;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Oleg Bazdyrev on 2019-10-23.
 */
public class ContactInfoWhoisHandlerTest {

    String RESP = "%\n" +
            "%AM TLD whois server #1\n" +
            "% Please see 'whois -h whois.amnic.net help' for usage.\n" +
            "%\n" +
            "\n" +
            "   Domain name: zaimix.am\n" +
            "   Registrar:   globalar (GlobalAR LLC)\n" +
            "   Status:      active\n" +
            "\n" +
            "   Registrant:\n" +
            "      Zaimix LLC\n" +
            "      Isahakyan 8-16\n" +
            "      Nor Achin,  2223\n" +
            "      AM\n" +
            "\n" +
            "   Administrative contact:\n" +
            "      David Ohanian\n" +
            "      Isahakyan 8-16\n" +
            "      Nor Achin, 2223\n" +
            "      AM\n" +
            "      zaimix.ru@yandex.ru\n" +
            "      +37493113787\n" +
            "\n" +
            "   Technical contact:\n" +
            "      David Ohanian\n" +
            "      Isahakyan 8-16\n" +
            "      Nor Achin, 2223\n" +
            "      AM\n" +
            "      zaimix.ru@yandex.ru\n" +
            "      +37493113787\n" +
            "\n" +
            "   DNS servers:\n" +
            "      dns1.yandex.net\n" +
            "      dns2.yandex.net\n" +
            "\n" +
            "   Registered:    2019-09-13\n" +
            "   Last modified: 2019-09-13\n" +
            "   Expires:       2020-09-13\n" +
            "\n" +
            "% For query help and examples please see\n" +
            "% http://www.iana.org/whois_for_dummies\n" +
            "%";

    @Test
    public void testContactInfoWhoisHandler() {
        BufferedReader bis = new BufferedReader(new StringReader(RESP));
        ContactInfoWhoisHandler handler = new ContactInfoWhoisHandler();
        Set<String> emails = bis.lines().map(handler::nextLine).filter(Objects::nonNull).collect(Collectors.toSet());
        Assert.assertEquals(Collections.singleton("zaimix.ru@yandex.ru"), emails);
    }

}
