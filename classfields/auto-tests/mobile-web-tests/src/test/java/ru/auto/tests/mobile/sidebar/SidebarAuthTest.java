package ru.auto.tests.mobile.sidebar;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.utils.Utils.getCurrentYear;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар под зарегом")
@Feature(SIDEBAR)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SidebarAuthTest {

    private static final String DEALER_TEXT = "Поиск\n%s\nМои объявления\nИзбранное\nСохранённые поиски\nСообщения\n" +
            "Промокоды\nМои отзывы\nЗаявки на кредит\nПроАвто\nБезопасная сделка\nВыкуп\nДобавить объявление" +
            "\nМоя тачка!\nЛегковые\nКоммерческие\nМото\nДилеры\nКаталог\nПолная версия\n" +
            "Оценить авто\nОтзывы\nЖурнал\nУчебник\nКупить ОСАГО\nСаша Котов\n© 1996–%s " +
            "ООО «Яндекс.Вертикали»";
    private static final String USER_TEXT = "Поиск\n%s\nМои объявления\nИзбранное\nСохранённые поиски\nСообщения\n" +
            "Промокоды\nМои отзывы\nЗаявки на кредит\nГараж\nПроАвто\nБезопасная сделка\nВыкуп\nДобавить объявление" +
            "\nМоя тачка!\nЛегковые\nКоммерческие\nМото\nДилеры\nКаталог\nПолная версия\n" +
            "Оценить авто\nОтзывы\nЖурнал\nУчебник\nКупить ОСАГО\nСаша Котов\n© 1996–%s " +
            "ООО «Яндекс.Вертикали»";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String userMock;

    @Parameterized.Parameter(1)
    public String userName;

    @Parameterized.Parameter(2)
    public String text;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"desktop/SessionAuthUser", "sosediuser1", USER_TEXT},
                {"desktop/SessionAuthDealer", "Major NR", DEALER_TEXT}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                userMock).post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/1076842087-f1e84/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сайдбара под зарегом")
    public void shouldSeeSidebar() {
        basePageSteps.onCardPage().header().sidebarButton().waitUntil(isDisplayed()).click();

        basePageSteps.onCardPage().sidebar().waitUntil(isDisplayed())
                .should(hasText(format(text, userName, getCurrentYear())));
    }
}
