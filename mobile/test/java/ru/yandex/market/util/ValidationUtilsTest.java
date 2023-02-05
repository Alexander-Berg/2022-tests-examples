package ru.yandex.market.util;

import org.junit.Test;

import java.util.Random;

import ru.yandex.market.BaseTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ValidationUtilsTest extends BaseTest {

    @Test
    public void testIsValidUrl() {
        String[] validUrls = {
                "yandex.ru",
                "yandex.ru:80",
                "www.yandex.ru",
                "yandex.ru/trololo",
                "www.yandex.ru/trololo",
                "http://yandex.ru",
                "http://yandex.ru:80",
                "http://www.yandex.ru",
                "http://yandex.ru/trololo",
                "http://www.yandex.ru/trololo",
                "https://yandex.ru",
                "https://yandex.ru:80",
                "https://www.yandex.ru",
                "https://yandex.ru/trololo",
                "https://www.yandex.ru/trololo",
        };
        String[] invalidUrls = {
                "ftp://yandex.ru",
                "file://yandex.ru",
                "192.168.1.1:8080",
                "hello"
        };

        for (String validUrl : validUrls) {
            assertTrue("The url: " + validUrl + " must be valid", ValidationUtils.isValidUrl(validUrl));
        }
        for (String invalidUrl : invalidUrls) {
            assertFalse("The url: " + invalidUrl + " must be invalid", ValidationUtils.isValidUrl(invalidUrl));
        }
    }

    @Test
    public void testIsValidEmail() {
        String[] validEmails = {
                "example@example.com",
                "example@example.ru",
        };
        String[] invalidEmails = {
                "example@example.",
                "example@example",
                "example@",
                "example",
        };

        for (String email : validEmails) {
            assertTrue("The email: " + email + " must be valid", ValidationUtils.isValidEmail(email));
        }
        for (String email : invalidEmails) {
            assertFalse("The email: " + email + " must be invalid", ValidationUtils.isValidEmail(email));
        }
    }

    @Test
    public void testIsValidPhone() {
        String[] validPhones = {
                "+71234567890",
                "+380123456789",
                "+7 (235) 7685637"
        };
        String[] invalidPhones = {
                "71234567890",
                "380123456789",
                "81234567890",
                "8380123456789",
        };

        for (String phone : validPhones) {
            assertTrue("The phone: " + phone + " must be valid", ValidationUtils.isValidPhone(phone));
        }
        for (String phone : invalidPhones) {
            assertFalse("The phone: " + phone + " must be invalid", ValidationUtils.isValidPhone(phone));
        }
    }

    private String[] getInvalidNames() {
        return new String[]{
                "",
                " ",
                "  ",
                createRandomString(101),
                createRandomString(50) + " " + createRandomString(51),
                createRandomString(10) + " " + createRandomString(51),
                createRandomString(50) + " " + createRandomString(50) + " " + createRandomString(50),
                createRandomString(5) + " " + createRandomString(1),
                createRandomString(1) + " " + createRandomString(5),
                createRandomString(1) + " " + createRandomString(1)
        };
    }

    private String[] getValidNames() {
        return new String[]{
                createRandomString(2) + " " + createRandomString(2),
                createRandomString(10) + " " + createRandomString(10),
                createRandomString(33) + "  " + createRandomString(33) + " " + createRandomString(32),
                "  " + createRandomString(10) + "  " + createRandomString(10) + " " + createRandomString(10) + " ",
                createRandomString(50) + " " + createRandomString(49)
        };
    }


    private String createRandomString(int length) {
        String symbols = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(symbols.charAt(random.nextInt(symbols.length())));
        }
        return sb.toString();
    }

    @Test
    public void testIsValidFullName() {
        for (String fullName : getValidNames()) {
            assertTrue("The fullName: " + fullName + " must be valid"
                    , ValidationUtils.isValidFullName(fullName));
        }
        for (String fullName : getInvalidNames()) {
            assertFalse("The fullName: " + fullName + " must be invalid"
                    , ValidationUtils.isValidFullName(fullName));
        }
    }
}