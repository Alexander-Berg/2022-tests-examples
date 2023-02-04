package ru.auto.tests.desktop.vas;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - объявления с услугой «Поднятие в поиске»")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FreshSalesListingCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsAll"),
                stub("desktop/SearchCarsCount"),
                stub("desktop/SearchCarsBreadcrumbs")
        ).create();

        screenshotSteps.setWindowSizeForScreenshot();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Поп-ап с описанием услуги")
    public void shouldSeePopup() {
        basePageSteps.onListingPage().getFreshSale(0).freshIcon().should(isDisplayed()).hover();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed()).should(hasText("Поднятие в поиске\n" +
                "Самый недорогой способ продвижения, который позволит вам в любой момент оказаться наверху списка " +
                "объявлений, отсортированного по актуальности или по дате. Это поможет быстрее найти покупателя — " +
                "ведь предложения в начале списка просматривают гораздо чаще.\n3\n" +
                "Увеличивает количество просмотров в 3 раза\nПодключить у себя"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Ссылка в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onListingPage().getFreshSale(0).freshIcon().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().activePopupLink().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(CARS).addParam("from", "listing")
                .addParam("vas_service", "fresh").shouldNotSeeDiff();
    }

}
