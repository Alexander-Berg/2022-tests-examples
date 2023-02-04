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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SEARCHES;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сохранение поиска под зарегом")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SavedSearchesListingAddAuthTest {

    private static final String MARK = "mercedes";
    private static final String MODEL = "e_klasse";
    private static final String PRICE_TO = "1000000";
    private static final String POPUP_TEXT = "Поиск сохранён\n" +
            "На адресsosediuser1@mail.ruбудут отправляться письма со свежими объявлениями.\n" +
            "Посмотреть сохранённые поиски";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsRid213",
                "mobile/UserFavoritesAllSubscriptionsEmpty",
                "mobile/UserFavoritesAllSubscriptionsPost",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogTagsV1",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(ALL)
                .addParam("price_to", PRICE_TO).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке сохранения поиска в шапке")
    public void shouldClickSaveSearchHeaderButton() {
        basePageSteps.onListingPage().fabSubscriptions().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onListingPage().savedSearchesPopup().waitUntil(isDisplayed()).should(hasText(POPUP_TEXT));
        basePageSteps.onListingPage().savedSearchesPopup().input("Укажите почту").should(not(isDisplayed()));
        basePageSteps.onListingPage().savedSearchesPopup().sendButton().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке сохранения поиска в листинге")
    public void shouldClickSaveSearchListingButton() {
        basePageSteps.onListingPage().savedSearch().button("Сохранить поиск").click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onListingPage().savedSearchesPopup().waitUntil(isDisplayed()).should(hasText(POPUP_TEXT));
        basePageSteps.onListingPage().savedSearchesPopup().input("Укажите почту").should(not(isDisplayed()));
        basePageSteps.onListingPage().savedSearchesPopup().sendButton().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Посмотреть сохранённые поиски»")
    public void shouldClickShowSearchesUrl() {
        basePageSteps.onListingPage().savedSearch().button("Сохранить поиск").click();
        basePageSteps.onListingPage().savedSearchesPopup().showSearchesUrl().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(LIKE).path(SEARCHES).shouldNotSeeDiff();
    }
}
