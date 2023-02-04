package ru.yandex.realty.archive;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OTSENKA_KVARTIRY;

/**
 * @author kantemirov
 */
@DisplayName("Страница архива. Проверяем блок результатов поиска")
@Feature(RealtyFeatures.ARCHIVE)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SearchResultCompareTest {

    private static final String PATH_ADDRESS = "Россия, Санкт-Петербург, Тимуровская улица, 16";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String dealType;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<String> dealTypes() {
        return asList(KUPIT, SNYAT);
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем блок результатов поиска")
    public void shouldSeeSearchResultBlock() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PATH_ADDRESS).path(dealType).path(KVARTIRA).open();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(
                basePageSteps.onArchivePage().searchResultBlock().waitUntil(isDisplayed()));
        basePageSteps.removePrestableCookie();
        basePageSteps.clearCookie("yandexuid");

        urlSteps.production().path(OTSENKA_KVARTIRY).path(PATH_ADDRESS).path(dealType).path(KVARTIRA).open();
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(
                basePageSteps.onArchivePage().searchResultBlock().waitUntil(isDisplayed()));
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
