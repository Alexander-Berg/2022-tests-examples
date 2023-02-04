package ru.yandex.realty.step;

import com.google.inject.Inject;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;

/**
 * Created by vicdev on 30.06.17.
 */
public class CookieSteps extends WebDriverSteps {

    public static final String YANDEXUID_COOKIE = "yandexuid";

    @Inject
    private WebDriverManager wm;

    public String getYandexUid() {
        return wm.getDriver().manage().getCookieNamed(YANDEXUID_COOKIE).getValue();
    }
}
