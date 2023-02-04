package ru.auto.tests.mobile.dealers;

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
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Карточка дилера - инфо")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardTrucksTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchTrucksBreadcrumbs",
                "desktop/SalonTrucks",
                "desktop/SalonPhonesTrucks",
                "mobile/SearchTrucksAllDealerId").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(TRUCK).path(ALL).path("/sollers_finans_moskva/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Информация")
    public void shouldSeeInfo() {
        basePageSteps.onDealerCardPage().info().should(hasText("СОЛЛЕРС-ФИНАНС Москва\nежедн. 9:00-21:00\n" +
                "м. Ленинский проспект, Площадь Гагарина\nМосква, Вавилова, д.1"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отсутствие фильтров")
    public void shouldNotSeeFilters() {
        basePageSteps.onDealerCardPage().filters().should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Телефоны")
    public void shouldSeePhones() {
        basePageSteps.onDealerCardPage().showPhoneButton().should(isDisplayed()).click();
        basePageSteps.onDealerCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Телефон\n+7 495 266-44-41\nс 09:00 до 22:00\n+7 495 266-44-42\nс 10:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Карта")
    public void shouldSeeMap() {
        basePageSteps.onDealerCardPage().info().yaMapsControls().waitUntil(isDisplayed());
    }
}
