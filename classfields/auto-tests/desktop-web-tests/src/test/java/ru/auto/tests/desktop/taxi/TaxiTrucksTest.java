package ru.auto.tests.desktop.taxi;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Объявление - блок «Зарабатывайте на своём авто»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class TaxiTrucksTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferTrucksUsedUserLcv").post();

        urlSteps.testing().path(LCV).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок «Зарабатывайте на своём авто»")
    public void shouldSeeTaxi() {
        basePageSteps.onCardPage().taxi().should(hasText("Зарабатывайте на своём авто\nПодключайтесь к Яндекс.Такси, " +
                "тариф «Грузовой»\nЗаполнить заявку\nСкрыть"));
        basePageSteps.onCardPage().taxi().button("Заполнить заявку").should(hasAttribute("href",
                "https://taxi.yandex.ru/rabota/cargo/?utm_source=auto.ru")).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Скрыть»")
    public void shouldClickHideButton() {
        basePageSteps.onCardPage().taxi().button("Скрыть").click();
        basePageSteps.onCardPage().taxi().should(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue("taxiPromoBeenViewed", "closed");
    }
}