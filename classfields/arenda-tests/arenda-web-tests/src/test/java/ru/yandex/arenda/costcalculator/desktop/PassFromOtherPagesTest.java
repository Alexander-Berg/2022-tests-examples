package ru.yandex.arenda.costcalculator.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.MainSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;
import static ru.yandex.arenda.pages.MainLandingPage.CALCULATOR_FOOTER_LINK_TEXT;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class PassFromOtherPagesTest {

    private static final String REALTY_TESTING_URL = "https://realty.test.vertis.yandex.ru/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MainSteps mainSteps;

    @Test
    @DisplayName("Открытие калькулятор с лендоса -> подвал")
    public void shouldSeePassFromFooter() {
        urlSteps.testing().open();
        mainSteps.onMainLandingPage().footer().link(CALCULATOR_FOOTER_LINK_TEXT).click();
        mainSteps.waitUntilSeeTabsCountAndSwitch(2);
        urlSteps.testing().path(KALKULATOR_ARENDY).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Открытие калькулятор с хедера главной недвиги")
    public void shouldSeePassRealtyHeader() {
        urlSteps.fromUri(REALTY_TESTING_URL).open();
        mainSteps.moveCursor(mainSteps.onRealtyPage().nav().link("Снять"));
        mainSteps.onRealtyPage().headerExpanded().link("Оценить квартиру").click();
        mainSteps.waitUntilSeeTabsCountAndSwitch(2);
        urlSteps.testing().path(KALKULATOR_ARENDY).shouldNotDiffWithWebDriverUrl();
    }
}
