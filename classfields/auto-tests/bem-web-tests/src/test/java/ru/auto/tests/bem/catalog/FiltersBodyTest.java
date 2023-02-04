package ru.auto.tests.bem.catalog;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - фильтры - тип кузова")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersBodyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    public BasePageSteps user;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String paramTextValue;

    @Parameterized.Parameter(1)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Седан", "SEDAN"},
                {"Хэтчбек", "%1$sHATCHBACK&%1$sHATCHBACK_3_DOORS&%1$sHATCHBACK_5_DOORS&%1$sHATCHBACK_LIFTBACK"},
                {"Хэтчбек 3 дв.", "%sHATCHBACK_3_DOORS"},
                {"Хэтчбек 5 дв.", "%sHATCHBACK_5_DOORS"},
                {"Лифтбек", "%sHATCHBACK_LIFTBACK"},
                {"Внедорожник", "%sALLROAD"},
                {"Внедорожник 3 дв.", "%sALLROAD_3_DOORS"},
                {"Внедорожник 5 дв.", "%sALLROAD_5_DOORS"},
                {"Универсал", "%sWAGON"},
                {"Купе", "%sCOUPE"},
                {"Минивэн", "%sMINIVAN"},
                {"Пикап", "%sPICKUP"},
                {"Лимузин", "%sLIMOUSINE"},
                {"Фургон", "%sVAN"},
                {"Кабриолет", "%sCABRIO"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр «Тип кузова»")
    public void shouldSeeBodyInUrl() {
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).open();
        user.onCatalogPage().filter().body().should(isDisplayed()).click();
        user.onCatalogPage().activePopup().waitUntil(isDisplayed());
        user.onCatalogPage().activeListItemByContains(paramTextValue).click();
        user.onCatalogPage().filter().body().click();
        user.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldUrl(containsString(format(paramValue.toLowerCase(), "autoru_body_type=")));
    }
}