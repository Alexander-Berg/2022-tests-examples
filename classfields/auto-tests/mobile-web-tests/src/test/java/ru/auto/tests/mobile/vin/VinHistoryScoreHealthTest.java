package ru.auto.tests.mobile.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_18214;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(VIN)
@DisplayName("Про авто - блок «Оценка автомобиля»")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistoryScoreHealthTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/CarfaxOfferCarsRawPaid").post();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_18214);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение оценки автомобиля")
    public void shouldSeeScoreHealth() {
        urlSteps.testing().path(HISTORY).path(SALE_ID).open();
        basePageSteps.onHistoryPage().vinReport().scoreHealth().should(hasText("87\n" +
                "Рейтинг похожих автомобилей 75-92 из 100\nОценка помогает сравнить состояние похожих автомобилей " +
                "и основана на ключевых пунктах отчёта."));
    }
}
