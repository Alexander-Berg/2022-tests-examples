package ru.auto.tests.desktop.rule;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.WebDriverException;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.DesktopStatic;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.utils.AntiAdBlockUtil;

import static java.lang.String.format;

/**
 * @author kurau (Yuri Kalinin)
 */
public class SetCookiesRule extends ExternalResource {

    private static final Logger LOG = Logger.getLogger(SetCookiesRule.class);

    @Inject
    @Getter
    private WebDriverManager driverManager;

    @Inject
    private WebDriverSteps webDriverSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private DesktopConfig conf;

    @Override
    protected void before() {
        setupBrowser();
    }

    @Step("Настраиваем браузер")
    private void setupBrowser() {
        // First of all, you need to be on the domain that the cookie will be valid for.
        // If you are trying to preset cookies before you start interacting with a site and your homepage is large /
        // takes a while to load an alternative is to find a smaller page on the site
        // (typically the 404 page is small, e.g. http://example.com/some404page).
        //urlSteps.fromUri(format("https://auth.%s/login/", conf.getBaseDomain())).open();

        urlSteps.fromUri(format("https://%s/home/", conf.getBaseDomain())).open();

        try {
            setCookies();
        } catch (WebDriverException e) {
            urlSteps.refresh();
            setCookies();
        }
    }

    @Step("Проставляем куки")
    private void setCookies() {
        if (conf.isDiscountDays()) {
            webDriverSteps.setCookie(DesktopStatic.COOKIE_NAME_VAS_DISCOUNT_HIDDEN, "all",
                    format(".%s", conf.getBaseDomain()));
            webDriverSteps.setCookie(conf.getDiscountCookieName(), conf.getDiscountCookieValue(),
                    format(".%s", conf.getBaseDomain()));
        }

        if (!conf.getAdsCookieName().isEmpty()) {
            webDriverSteps.setCookie(conf.getAdsCookieName(), conf.getAdsCookieValue(),
                    format(".%s", conf.getBaseDomain()));
        }

        if (!conf.getLosCookieName().isEmpty()) {
            webDriverSteps.setCookie(conf.getLosCookieName(), conf.getLosCookieValue(),
                    format(".%s", conf.getBaseDomain()));
        }

        if (!conf.getGdprCookieName().isEmpty()) {
            webDriverSteps.setCookie(conf.getGdprCookieName(), conf.getGdprCookieValue(),
                    format(".%s", conf.getBaseDomain()));
        }

        if (!conf.getAabPartnerKey().isEmpty()) {
            String cycada = AntiAdBlockUtil.generateAntiAdBlockCookie(conf.getAabPartnerKey());
            webDriverSteps.setCookie("cycada", cycada, format(".%s", conf.getBaseDomain()));
        } else {
            LOG.warn("Don't generate anti ad block cookie without aab_partner_key env parameter. " +
                    "The page will be encrypted!");
        }

        if (!conf.getBranchCookieValue().isEmpty()) {
            webDriverSteps.setCookie(conf.getBranchCookieName(), conf.getBranchCookieValue(),
                    format(".%s", conf.getBaseDomain()));
        }

        if (conf.getBaseCookies() != null) {
            conf.getBaseCookies().forEach(cookieString -> {
                if (cookieString.contains("=")) {
                    String[] cookieNameValue = cookieString.split("=");
                    String cookieName = cookieNameValue[0].trim();
                    String cookieValue = cookieNameValue.length > 1 ? cookieNameValue[1].trim() : "";
                    webDriverSteps.setCookie(cookieName, cookieValue, format(".%s", conf.getBaseDomain()));
                }
            });
        }
    }
}