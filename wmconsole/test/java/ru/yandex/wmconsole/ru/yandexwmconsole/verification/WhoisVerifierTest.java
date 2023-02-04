package ru.yandex.wmconsole.ru.yandexwmconsole.verification;


import org.junit.Assert;
import org.junit.Test;
import ru.yandex.wmconsole.verification.whois.handlers.ContactInfoWhoisHandler;

/**
 * User: azakharov
 * Date: 07.12.12
 * Time: 18:18
 */
public class WhoisVerifierTest {
    final String[] testStrings = {
        "Please note: the registrant of the domain name is specified\n",
        "in the \"registrant\" field.  In most cases, GoDaddy.com, LLC \n",
        "is not the registrant of domain names listed in this database.\n",
        "\n" ,
        "\n" ,
        "   Registered through: GoDaddy.com, LLC (http://www.godaddy.com)\n" ,
        "   Domain Name: TVOYGID.COM\n" ,
        "      Created on: 08-Feb-12\n" ,
        "      Expires on: 08-Feb-15\n" ,
        "      Last Updated on: 09-Sep-12\n" ,
        "\n" ,
        "   Registrant:\n" ,
        "   Tvoy Gid\n" ,
        "   Besiktas\n" ,
        "   Istanbul,  34034\n" ,
        "   Turkey\n" ,
        "\n" ,
        "   Administrative Contactor:\n" ,
        "      Gid, Tvoy\ttvoygidrk@gmail.com\n" ,
        "      Besiktas\n" ,
        "      Istanbul,  34034\n" ,
        "      Turkey\n" ,
        "      +90.905556027303\n" ,
        "\n" ,
        "   Technical Contactor:\n" ,
        "      Gid, Tvoy\ttvoygidrk@gmail.com\n" ,
        "      Besiktas\n" ,
        "      Istanbul,  34034\n" ,
        "      Turkey\n" ,
        "      +90.905556027303\n" ,
        "\n" ,
        "   Domain servers in listed order:\n" ,
        "      NS1.WORDPRESS.COM\n" ,
        "      NS2.WORDPRESS.COM\n" ,
        "      NS3.WORDPRESS.COM\n" ,
        ""
    };
    
    @Test
    public void testContactInfoWhoisRecordHandler() {
        ContactInfoWhoisHandler handler = new ContactInfoWhoisHandler();
        String foundEmail = null;
        for (String line : testStrings) {
            foundEmail = handler.nextLine(line);
            if (foundEmail != null) {
                break;
            }
        }
        Assert.assertTrue("email not found", foundEmail != null);
    }

    @Test
    public void testContactDetails() {
        ContactInfoWhoisHandler handler = new ContactInfoWhoisHandler();
        String [] lines = new String[] {
                "  Registrant Contact Details:\n",
                "    Nikolaj Cupko\t(poltava2003@gmail.com)\n",
        };

        String foundEmail = null;
        for (String line : lines) {
            foundEmail = handler.nextLine(line);
            if (foundEmail != null) {
                break;
            }
        }
        Assert.assertEquals("email not found", "poltava2003@gmail.com", foundEmail);
    }

    @Test
    public void testContactor() {
        ContactInfoWhoisHandler handler = new ContactInfoWhoisHandler();
        String [] lines = new String[] {
                "  Technical Contactor:\n",
                "    Igor Shakhbazyan domain.reg@sweb.ru \n",
        };
        String foundEmail = null;
        for (String line : lines) {
            foundEmail = handler.nextLine(line);
            if (foundEmail != null) {
                break;
            }
        }
        Assert.assertEquals("email not found", "domain.reg@sweb.ru", foundEmail);
    }
}
