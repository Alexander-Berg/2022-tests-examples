package ru.yandex.realty.archive;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OTSENKA_KVARTIRY;

/**
 * @author kantemirov
 */
@DisplayName("Страница архива. Проверка поисковой строки")
@Feature(RealtyFeatures.ARCHIVE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ClearInputText {

    private static final String PATH_ADDRESS_1 = "Россия%2C%20Санкт-Петербург%2C%20Тимуровская%20улица%2%20C16";
    private static final String PATH_ADDRESS_2 = "Россия%2C%20Санкт-Петербург%2C%20Светлановский%20проспект%2C%20115к1";
    private static final String ADDRESS_2 = "светлановский 115к1";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Нажимаем на крестик в строке поискаб проверяем новый адрес в урле")
    public void shouldSeeNewQueryUrl() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PATH_ADDRESS_1).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onArchivePage().searchForm().input().waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().searchForm().clearButton().waitUntil(isDisplayed()).click();
        basePageSteps.onArchivePage().searchForm().input().sendKeys(ADDRESS_2);
        basePageSteps.onArchivePage().searchForm().addressSuggest().waitUntil(isDisplayed());
        basePageSteps.onArchivePage().searchForm().input().sendKeys(Keys.ENTER);
        basePageSteps.onArchivePage().paranja().waitUntil(not(isDisplayed()));
        String actual =  urlSteps.fromUri(urlSteps.getCurrentUrl()).toString();
        String expected = urlSteps.testing().path(OTSENKA_KVARTIRY).path(PATH_ADDRESS_2).path(KUPIT).path(KVARTIRA)
                .toString();
        assertThat(actual).isEqualTo(expected);
    }
}
