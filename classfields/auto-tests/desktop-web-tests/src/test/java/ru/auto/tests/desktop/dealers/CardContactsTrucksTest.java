package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Контакты")
@Story(DEALER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CardContactsTrucksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchTrucksBreadcrumbs"),
                stub("desktop/SalonTrucks"),
                stub("desktop/SalonPhonesTrucks")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(TRUCK).path(ALL).path("/sollers_finans_moskva/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Информация")
    public void shouldSeeInfo() {
        basePageSteps.onDealerCardPage().info().should(hasText(matchesPattern("СОЛЛЕРС-ФИНАНС Москва\n" +
                "Грузовики в наличии\nМосква, Вавилова, д.1, м. Ленинский проспект, Площадь Гагарина\nежедн. 9:00-21:00" +
                "\nАвтосалон\nНа Авто.ру \\d+ лет")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Телефоны")
    public void shouldSeePhones() {
        basePageSteps.onDealerCardPage().showPhonesButton().should(isDisplayed()).click();
        basePageSteps.onDealerCardPage().showPhonesButton().waitUntil(isDisplayed())
                .waitUntil(hasText("+7 495 266-44-41\n+7 495 266-44-42"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Адрес")
    public void shouldSeeAddress() {
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.moveCursorAndClick(basePageSteps.onDealerCardPage().gallery().map(), 30, 30);
        basePageSteps.onDealerCardPage().mapPoint().waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().addressPopupTitle().waitUntil(isDisplayed());
    }
}