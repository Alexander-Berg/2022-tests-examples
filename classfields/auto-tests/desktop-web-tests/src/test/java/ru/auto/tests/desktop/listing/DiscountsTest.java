package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.element.listing.SalesListItem.DISCOUNTS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - скидки")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DiscountsTest {

    private static final String DISCOUNTS_POPUP_TEXT = "1 769 000 ₽\nХорошая цена\nот 1 199 000 ₽ со скидками\n25 466 $\n · \n" +
            "22 662 €\nСкидки\nВ кредит\nдо 250 000 ₽\nС каско\nдо 20 000 ₽\nВ трейд-ин\nдо 300 000 ₽\nМаксимальная\n570 000 ₽\n" +
            "Максимальная скидка, которую может предоставить дилер. Подробности узнавайте по телефону.\nСтоимость этого автомобиля " +
            "соответствует средней рыночной относительно похожих автомобилей\nПодробней про хорошую цену" ;

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsAll")
        ).create();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение скидок")
    public void shouldSeeDiscounts() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();

        basePageSteps.onListingPage().getSale(4).hover();
        basePageSteps.onListingPage().getSale(4).badge(DISCOUNTS).hover();

        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(DISCOUNTS_POPUP_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение скидок, тип листинга «Карусель»")
    public void shouldSeeDiscountsCarousel() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(4).hover();
        basePageSteps.onListingPage().getCarouselSale(4).badge(DISCOUNTS).hover();

        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(DISCOUNTS_POPUP_TEXT));
    }

}
