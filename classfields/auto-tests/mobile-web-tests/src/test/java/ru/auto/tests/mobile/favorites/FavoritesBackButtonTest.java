package ru.auto.tests.mobile.favorites;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное - возврат на предыдущую страницу")
@Feature(AutoruFeatures.FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FavoritesBackButtonTest {

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/UserFavoritesAll",
                "desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(LIKE).open();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Возврат в избранное с карточки объявления")
    public void shouldReturnFromSale() {
        basePageSteps.onFavoritesPage().getSale(0).header().click();
        basePageSteps.onCardPage().header().backButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
    }
}
