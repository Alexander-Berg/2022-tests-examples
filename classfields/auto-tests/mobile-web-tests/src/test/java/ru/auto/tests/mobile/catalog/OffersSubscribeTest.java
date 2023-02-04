package ru.auto.tests.mobile.catalog;

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

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - «Предложения о продаже» - кнопка «Узнать о поступлении»")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class OffersSubscribeTest {

    private static final String MARK = "jaguar";
    private static final String MODEL = "xe";
    private static final String GENERATION = "/21508706/21508754/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsJaguar",
                "mobile/SearchCarsJaguarGenConfiguration",
                "mobile/SearchCarsJaguarGen",
                "mobile/UserFavoritesCarsSubscriptionsPost").post();

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCatalogGenerationPage().footer(), 0, 0);
        basePageSteps.onCatalogGenerationPage().offers().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Узнать о поступлении»")
    public void shouldClickSubscribeButton() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCatalogGenerationPage().offers().subscribeButton(), 0, -200);
        basePageSteps.onCatalogGenerationPage().offers().subscribeButton().click();
        basePageSteps.onBasePage().savedSearchesPopup().input("Укажите почту").sendKeys("test@test.com");
        basePageSteps.onBasePage().savedSearchesPopup().sendButton().click();
        basePageSteps.onBasePage().notifier().waitUntil(hasText("Поиск сохранён"));
    }
}
