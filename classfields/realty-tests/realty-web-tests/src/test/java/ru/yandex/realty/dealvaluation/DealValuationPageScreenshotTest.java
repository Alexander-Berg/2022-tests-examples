package ru.yandex.realty.dealvaluation;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.DVUHKOMNATNAYA;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.ODNOKOMNATNAYA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CALCULATOR_STOIMOSTI;

@Link("https://st.yandex-team.ru/VERTISTEST-2015")
@DisplayName("Страница оценки рыночной стоимости")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class DealValuationPageScreenshotTest {

    private static final String ADDRESS = "Россия, Санкт-Петербург, проспект%20Энергетиков, 38";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(1600, 4000);
        urlSteps.testing().path(CALCULATOR_STOIMOSTI).path(KVARTIRA);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот страницы")
    public void shouldSeeCalcPage() {
        urlSteps.path(DVUHKOMNATNAYA).queryParam("address", ADDRESS).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().pageBody());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход по ссылка в конце страницы")
    public void shouldSeeDocsRentPage() {
        urlSteps.path(ODNOKOMNATNAYA).open();
        basePageSteps.onMainPage().link("Стоимость продажи 2-комнатной квартиры").click();
        urlSteps.testing().path(CALCULATOR_STOIMOSTI).path(KVARTIRA).path(DVUHKOMNATNAYA)
                .shouldNotDiffWithWebDriverUrl();
    }
}
