package ru.auto.tests.desktop.share;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SHARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поделяшки на карточке объявления")
@Feature(SHARE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ShareSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String mock;

    @Parameterized.Parameter(2)
    public String shareText;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser",
                        "%D0%A1%D0%BC%D0%BE%D1%82%D1%80%D0%B8%D1%82%D0%B5%2C%20%D0%BA%D0%B0%D0%BA%D0%B0%D1%8F%20%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D0%B0%3A%20Land%20Rover%20Discovery%20III%202008%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20700%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!"},
                {TRUCK, "desktop/OfferTrucksUsedUser",
                        "%D0%A1%D0%BC%D0%BE%D1%82%D1%80%D0%B8%D1%82%D0%B5%3A%20%D0%97%D0%98%D0%9B%205301%20%22%D0%91%D1%8B%D1%87%D0%BE%D0%BA%22%202000%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20250%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser",
                        "%D0%A1%D0%BC%D0%BE%D1%82%D1%80%D0%B8%D1%82%D0%B5%3A%20Harley-Davidson%20Dyna%20Super%20Glide%202010%20%D0%B3%D0%BE%D0%B4%D0%B0%20%D0%B7%D0%B0%20530%C2%A0000%20%D1%80%D1%83%D0%B1%D0%BB%D0%B5%D0%B9%20%D0%BD%D0%B0%20%D0%90%D0%B2%D1%82%D0%BE.%D1%80%D1%83!"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(mock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().cardHeader().shareButton().click();
    }

    @Test
    @DisplayName("Ссылка кнопки поделиться в вконтакте")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVkButtonLink() {
        basePageSteps.onCardPage().cardHeader().shareDropdown().vk().should(isDisplayed())
                .should(hasAttribute("href", startsWith("https://vk.com")));
    }

    @Test
    @DisplayName("Ссылка кнопки поделиться в одноклассниках")
    @Category({Regression.class, Testing.class})
    public void shouldSeeOkButtonLink() {
        basePageSteps.onCardPage().cardHeader().shareDropdown().ok().should(isDisplayed())
                .should(hasAttribute("href", startsWith("https://connect.ok.ru")));
    }

}
