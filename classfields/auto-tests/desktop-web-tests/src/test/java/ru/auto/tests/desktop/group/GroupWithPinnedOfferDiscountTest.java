package ru.auto.tests.desktop.group;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка c запиненным оффером - скидки")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GroupWithPinnedOfferDiscountTest {

    private static final String PATH = "/kia/optima/21342125/21342344/1076842087-f1e84/";

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/OfferCarsNewDealerDiscount").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение цены cо скидкой")
    public void shouldSeeDiscountPrice() {
        basePageSteps.onCardPage().cardHeader().price().should(hasText("от 2 319 642 ₽\n2 480 900 ₽ без скидок"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа cо скидками")
    public void shouldSeeDiscountPopup() {
        basePageSteps.onCardPage().cardHeader().price().hover();
        basePageSteps.onCardPage().pricePopup().waitUntil(isDisplayed()).should(hasText("от 2 319 642 ₽\n" +
                "2 480 900 ₽ без скидок\nСкидки\nМаксимальная\n161 258 ₽\n38 908 $\n35 284 €"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение скидок под кредитной кнопкой")
    public void shouldSeeDiscounts() {
        basePageSteps.onCardPage().discounts().should(hasText("Максимальная\n161 258 ₽\nМаксимальная скидка, " +
                "которую может предоставить дилер. Подробности узнавайте по телефону."));
    }
}