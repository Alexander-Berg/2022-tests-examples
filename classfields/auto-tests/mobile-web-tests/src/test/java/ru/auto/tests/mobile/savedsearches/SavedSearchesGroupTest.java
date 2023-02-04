package ru.auto.tests.mobile.savedsearches;

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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сохранение поиска на группе")
@Feature(SAVE_SEARCHES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SavedSearchesGroupTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with(//"mobile/SearchCarsBreadcrumbsMarkModelGroup",
                        //"mobile/SearchCarsGroupContextGroup",
                        //"mobile/SearchCarsGroupContextListing",
                        //"mobile/UserFavoritesAllSubscriptionsEmpty",
                        //"mobile/UserFavoritesCarsSubscriptionsPostGroup",
                        //"desktop/ReferenceCatalogTagsV1",
                        //"desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                        "desktop/UserFavoritesAllSubscriptionsEmpty",
                        "desktop/ProxyPublicApi")
                .post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Сохранить поиск»")
    public void shouldClickSaveSearchButton() {
        basePageSteps.onGroupPage().savedSearch().button("Сохранить поиск").click();
        basePageSteps.onGroupPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onGroupPage().savedSearch().button("Сохранён").should(isDisplayed());
        basePageSteps.onGroupPage().savedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("Укажите свою почту для получения уведомлений о новых объявлениях\nУкажите почту\n" +
                        "Отправить"));
        basePageSteps.onGroupPage().savedSearchesPopup().input("Укажите почту", "test@test.com");
        basePageSteps.onGroupPage().savedSearchesPopup().sendButton().click();
        basePageSteps.onGroupPage().savedSearchesPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().header().deleteSearchButton().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Сохранить поиск» в шапке")
    public void shouldClickSaveSearchHeaderButton() {
        basePageSteps.onGroupPage().header().saveSearchButton().click();
        basePageSteps.onGroupPage().notifier().waitUntil(isDisplayed()).should(hasText("Поиск сохранён"));
        basePageSteps.onGroupPage().header().deleteSearchButton().should(isDisplayed());
        basePageSteps.onGroupPage().savedSearchesPopup().waitUntil(isDisplayed())
                .should(hasText("Укажите свою почту для получения уведомлений о новых объявлениях\nУкажите почту\n" +
                        "Отправить"));
        basePageSteps.onGroupPage().savedSearchesPopup().input("Укажите почту", "test@test.com");
        basePageSteps.onGroupPage().savedSearchesPopup().sendButton().click();
        basePageSteps.onGroupPage().savedSearchesPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().savedSearch().button("Сохранён").should(isDisplayed());
    }
}
