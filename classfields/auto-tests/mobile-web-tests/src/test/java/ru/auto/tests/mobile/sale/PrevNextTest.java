package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.SellerTypes.USER;
import static ru.auto.tests.desktop.mobile.page.ListingPage.TOP_SALES_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Feature(SALES)
@DisplayName("Переключение на предыдущее/следующее объявление на карточке")
public class PrevNextTest {

    private String firstSaleUrl;
    private String secondSaleUrl;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    //@Parameter("Тип транспорта")
    @Parameterized.Parameter
    public String type;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[]{
                CARS,
                TRUCK,
                MOTORCYCLE
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(type).path(USED).addParam("dealer_org_type", USER).open();
        firstSaleUrl = basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).url().getAttribute("href");
        secondSaleUrl = basePageSteps.onListingPage().getSale(TOP_SALES_COUNT + 1).url()
                .getAttribute("href");
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Переход на предыдущее/следующее объявление")
    public void shouldClickPrevAndNextButtons() {
        cookieSteps.setCookie("card_prevnext_swipe_info", "1",
                format(".%s", urlSteps.getConfig().getBaseDomain()));
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT + 1).url().click();
        waitSomething(1, TimeUnit.SECONDS);
        String title = basePageSteps.onCardPage().title().getText();
        String sellerComment = basePageSteps.onCardPage().sellerComment().getText();
        basePageSteps.hideElement(basePageSteps.onCardPage().header());
        basePageSteps.hideElement(basePageSteps.onCardPage().floatingContacts());
        basePageSteps.onCardPage().prevNext().buttonContains("Предыдущее").click();
        urlSteps.fromUri(firstSaleUrl).shouldNotSeeDiff();
        basePageSteps.onCardPage().title().should(not(hasText(title)));
        basePageSteps.onCardPage().sellerComment().should(not(hasText(sellerComment)));
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.hideElement(basePageSteps.onCardPage().header());
        basePageSteps.hideElement(basePageSteps.onCardPage().floatingContacts());
        basePageSteps.onCardPage().prevNext().buttonContains("Следующее").click();
        urlSteps.fromUri(secondSaleUrl).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пояснение про переход на предыдущее/следующее объявление")
    public void shouldSeePrevAndNextInfo() {
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT + 1).url().click();
        basePageSteps.onCardPage().prevNextinfo().waitUntil(isDisplayed()).should(hasText("Переключайтесь " +
                "между объявлениями, листая их влево-вправо\nХорошо"));
        basePageSteps.onCardPage().prevNextinfo().button("Хорошо").click();
        cookieSteps.shouldSeeCookieWithValue("card_prevnext_swipe_info", "1");
        basePageSteps.refresh();
        basePageSteps.onCardPage().prevNextinfo().should(not(isDisplayed()));
    }
}
