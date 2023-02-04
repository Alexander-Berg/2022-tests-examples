package ru.yandex.realty.archive;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OTSENKA_KVARTIRY;

/**
 * @author kantemirov
 */
@DisplayName("Страница архива. Проверка страниц")
@Feature(RealtyFeatures.ARCHIVE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SearchAddressTest {

    private static final String PATH_ADDRESS = "Россия, Санкт-Петербург, Тимуровская улица, 16";
    private static final String ADDRESS = "тимуровская 16";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.setSpbCookie();
        urlSteps.testing().path(OTSENKA_KVARTIRY).open();
        basePageSteps.onArchivePage().searchForm().input().waitUntil(isDisplayed()).sendKeys(ADDRESS);
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Вводим адрес и жмем Enter, видим адрес в урле")
    public void shouldSeeSearchUrlWithEnter() {
        basePageSteps.onArchivePage().searchForm().input().sendKeys(Keys.ENTER);
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        urlSteps.path(ADDRESS).path(KUPIT).path(KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Вводим адрес, кликаем на первый адрес в садджесте, проверяем урл")
    public void shouldSeeSearchUrlWithSuggestChoice() {
        basePageSteps.onArchivePage().searchForm().addressSuggest().addresses().get(0).waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        urlSteps.path(PATH_ADDRESS).path(KUPIT).path(KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }
}
