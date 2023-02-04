package ru.auto.tests.desktop.utils;

import okhttp3.Headers;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.InvalidCookieDomainException;
import org.openqa.selenium.WebDriver;

import java.net.HttpCookie;
import java.util.Optional;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class WebDriverUtils {

    public static final String SET_COOKIE_HEADER = "set-cookie";

    private WebDriverUtils() {
    }

    public static void processHeaders(WebDriver driver, Headers headers) {
        headers.values(SET_COOKIE_HEADER).stream()
                .map(WebDriverUtils::parseCookie)
                .filter(Optional::isPresent)
                .forEach(cookie -> addCookieSafely(driver, cookie.get()));
    }

    public static Optional<Cookie> parseCookie(String header) {
        return HttpCookie.parse(header).stream()
                .map(item -> new Cookie(item.getName(), item.getValue(), item.getDomain(), item.getPath(), null))
                .findFirst();
    }

    public static void addCookieSafely(WebDriver driver, Cookie cookie) {
        try {
            driver.manage().addCookie(cookie);
        } catch (InvalidCookieDomainException e) {
            //DO NOTHING
        }
    }
}
