package ru.auto.tests.bem.header;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Шапка - избранные")
@Feature(HEADER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FavoritesAuthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/UserFavoritesAll"),
                stub("desktop/OfferCarsPhones")).create();

        urlSteps.testing().path(CATALOG).path(CARS).open();
        basePageSteps.setWideWindowSize();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефона в попапе избранного")
    public void shouldSeePhoneInFavorites() {
        basePageSteps.onCatalogPage().header().favoritesButton().click();
        basePageSteps.onCatalogPage().favoritesPopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().favoritesPopup().getFavorite(0).button("Показать телефон").click();

        basePageSteps.onCatalogPage().popup().waitUntil(isDisplayed(), 8)
                .should(hasText(containsString("Егор\nЧастное лицо\n+7 916 039-84-27")));
    }
}
