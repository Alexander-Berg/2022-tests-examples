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
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Карточка дилера - инфо")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardMotoTest {

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
        mockRule.newMock().with("desktop/SearchMotoBreadcrumbs",
                "desktop/SalonMoto",
                "desktop/SalonPhonesMoto",
                "mobile/SearchMotoAllDealerId").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(MOTORCYCLE).path(ALL).path("/motogarazh_moskva/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Информация")
    public void shouldSeeInfo() {
        basePageSteps.onDealerCardPage().info()
                .should(hasText("МОТОГАРАЖ\nежедн. 10:00-22:00\nм. станция Дмитровская (МЦД-2), Дмитровская\n" +
                        "Москва и Московская область, Москва, ул. Костякова, д.3"));
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
        basePageSteps.onDealerCardPage().showPhoneButton().click();
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
