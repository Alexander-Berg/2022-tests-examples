package ru.auto.tests.mobile.savedsearches;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сохранение поиска под незарегом")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SavedSearchesListingAddUnauthTest {

    private static final String MARK = "mercedes";
    private static final String MODEL = "e_klasse";
    private static final String POPUP_TEXT = "Укажите свою почту для получения уведомлений" +
            " о новых объявлениях\nУкажите почту\nОтправить";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(ALL).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сохранение поиска")
    public void shouldSaveSearch() {
        basePageSteps.onListingPage().fabSubscriptions().waitUntil(isDisplayed()).click();
        basePageSteps.onBasePage().savedSearchesPopup().input("Укажите почту", getRandomEmail());
        basePageSteps.onBasePage().savedSearchesPopup().sendButton().click();
        basePageSteps.onBasePage().notifier().waitUntil(hasText("Поиск сохранён"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поп-ап сохранения поиска")
    public void shouldSeePopupSavedSearch() {
        basePageSteps.onListingPage().fabSubscriptions().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onListingPage().savedSearchesPopup().waitUntil(isDisplayed()).should(hasText(POPUP_TEXT));
        basePageSteps.onListingPage().savedSearchesPopup().input("Укажите почту").should(isDisplayed());
        basePageSteps.onListingPage().savedSearchesPopup().sendButton().should(isDisplayed());
    }
}
