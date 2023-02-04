package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Ссылки в подхедере")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeaderMainLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;

    @Parameterized.Parameter
    public String text;

    @Parameterized.Parameter(1)
    public String dataTest;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameter(3)
    public String tail;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Купить", "SELL", "/moskva/kupit/kvartira/", ""},
                {"Снять", "RENT", "/moskva/snyat/kvartira/", ""},
                {"Новостройки", "SITES", "/moskva_i_moskovskaya_oblast/kupit/novostrojka/karta/", ""},
                {"Коммерческая", "COMMERCIAL", "/moskva/snyat/kommercheskaya-nedvizhimost/", ""},
                {"Ипотека", "MORTGAGE", "/ipoteka/", "?flatType=NEW_FLAT"},
                {"Профессионалам", "FOR_PROFESSIONAL", "/promotion/", ""},
                {"Журнал", "JOURNAL", "/journal/", "?from=main_menu"},
                {"", "SPECIAL_PROJECT", "/samolet/", ""},
        });
    }

    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        urlSteps.setMoscowCookie();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылки хедера")
    public void shouldSeeMainLinks() {
        String expectedUrl = String.format("%s%s", urlSteps.testing().path(expected).toString(), tail);
        user.onBasePage().headerUnder().mainMenuItem(dataTest)
                .should(hasAttribute("href", equalTo(expectedUrl)));
    }
}
