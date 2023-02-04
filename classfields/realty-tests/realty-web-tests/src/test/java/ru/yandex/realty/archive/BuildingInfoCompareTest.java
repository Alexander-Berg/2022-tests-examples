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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OTSENKA_KVARTIRY;

/**
 * @author kantemirov
 */
@DisplayName("Страница архива. Проверяем блок информации об объекте")
@Feature(RealtyFeatures.ARCHIVE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BuildingInfoCompareTest {

    private static final String PATH_ADDRESS = "Россия, Санкт-Петербург, Светлановский проспект, 115к1";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем блок информации об объекте")
    public void shouldSeeBuildingInfoBlock() {
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(PATH_ADDRESS).path(KUPIT).path(KVARTIRA).open();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(
                basePageSteps.onArchivePage().buildingInfo().waitUntil(isDisplayed()));
        basePageSteps.removePrestableCookie();
        basePageSteps.clearCookie("yandexuid");

        urlSteps.production().path(OTSENKA_KVARTIRY).path(PATH_ADDRESS).path(KUPIT).path(KVARTIRA).open();
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(
                basePageSteps.onArchivePage().buildingInfo().waitUntil(isDisplayed()));
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
