package ru.yandex.realty.mappage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;

@DisplayName("Карта. Общее")
@Feature(MAP)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MapSnippetScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String dealType;

    @Parameterized.Parameter(2)
    public String realtyType;

    @Parameterized.Parameters(name = "Клик по сниппету на {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Аренда дома", SNYAT, DOM},
                {"Снять Коммерческая", SNYAT, COMMERCIAL},
                {"Новостройки", KUPIT, NOVOSTROJKA},
                {"Коттеджные поселки", KUPIT, KOTTEDZHNYE_POSELKI}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по сниппету -> скриншот боковой панели")
    public void shouldSeeSidebarScreenshot() {
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MOSKVA_I_MO).path(dealType).path(realtyType).path(KARTA).open();
        clickMapOfferAndShowSnippetOffers();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().sidebar());
        urlSteps.setProductionHost().open();

        clickMapOfferAndShowSnippetOffers();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().sidebar());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    //Бывает что карта неотображает первый оффер
    @Step("Отдаляем карту и жмем на оффер на карте")
    public void clickMapOfferAndShowSnippetOffers() {
        basePageSteps.onMapPage().unzoom().click();
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1, SECONDS).atMost(30, SECONDS).ignoreExceptions()
                .until(() -> {
                    basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(1));
                    basePageSteps.onMapPage().sidebar().waitUntil(isDisplayed());
                    return true;
                });
    }
}
