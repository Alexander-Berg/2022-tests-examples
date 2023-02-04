package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;

@DisplayName("Карточка новостройки. Спецпроект")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1948")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SpecProjectCardTest {

    private static final String SPEC_PROJECT_PATH = "/bolshoe-putilkovo-1680614/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path(SPEC_PROJECT_PATH)
                .queryParam("from-special", "samolet").open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот спецпроекта")
    public void shouldSeeSpecProjectScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onNewBuildingSitePage().pageBody());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onNewBuildingSitePage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
