package ru.auto.tests.desktop.savedsearches;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.KOPITSA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений - промо подписок в листинге под незарегом")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SavedSearchesListingPromoTest {

    private static final String WRONG_EMAIL = "wrong email";
    private static final String RANDOM_EMAIL = getRandomEmail();
    private static final String SAVED_SUBSCRIPTION_TEXT = "Поиск сохранён";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam("price_from", "100000").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KOPITSA)
    @DisplayName("Сохранение поиска под незарегом")
    public void shouldSaveSearchUnauth() {
        basePageSteps.onListingPage().listingSubscription().input().waitUntil(isDisplayed()).sendKeys(RANDOM_EMAIL);
        basePageSteps.onListingPage().listingSubscription().subscribeButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText(SAVED_SUBSCRIPTION_TEXT));
        basePageSteps.onListingPage().listingSubscription().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(KOPITSA)
    @DisplayName("Видим сообщение о том, что поле почты пустое")
    public void shouldSeeEmptyEmailAlert() {
        basePageSteps.onListingPage().listingSubscription().subscribeButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().listingSubscription().errorText()
                .waitUntil(hasText("Адрес указан неверно"));
    }

    @Test
    @Category({Regression.class})
    @Owner(KOPITSA)
    @DisplayName("Видим сообщение, что почта неправильная")
    public void shouldSeeWrongEmailAlert() {
        basePageSteps.onListingPage().listingSubscription().input().waitUntil(isDisplayed()).sendKeys(WRONG_EMAIL);
        basePageSteps.onListingPage().listingSubscription().subscribeButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().listingSubscription().errorText()
                .waitUntil(hasText("Адрес указан неверно"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение подписки в поп-апе")
    public void shouldSeeSubscription() {
        basePageSteps.onListingPage().filter().saveSearchButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed())
                .should(hasText("Все марки автомобилей\nот 100 000 ₽\nЭлектронная почта\nПолучать на почту\nУдалить"));
    }
}