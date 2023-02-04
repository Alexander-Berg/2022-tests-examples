package ru.auto.tests.mobile.sidebar;

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

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CHAT_AUTH;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.element.Sidebar.SIGNIN;
import static ru.auto.tests.desktop.utils.Utils.getCurrentYear;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар под незарегом")
@Feature(SIDEBAR)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SidebarUnauthTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/OfferCarsUsedUser").post();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сайдбара под незарегом")
    public void shouldSeeSidebar() {
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/1076842087-f1e84/").open();
        basePageSteps.onCardPage().header().sidebarButton().click();
        basePageSteps.onMainPage().sidebar().waitUntil(isDisplayed()).should(hasText(format("Поиск\nВойти\n" +
                "Мои объявления\nИзбранное\nСохранённые поиски\nСообщения\nПромокоды\nМои отзывы\nЗаявки на кредит\n" +
                "Гараж\nПроАвто\nБезопасная сделка\nВыкуп\nДобавить объявление\nМоя тачка!\nЛегковые\nКоммерческие\nМото" +
                "\nДилеры\nКаталог\nПолная версия\nОценить авто\nОтзывы\nЖурнал\nУчебник\nКупить ОСАГО\nСаша Котов\n" +
                "© 1996–%s ООО «Яндекс.Вертикали»", getCurrentYear())));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Скрытие сайдбара")
    public void shouldCloseSidebar() {
        urlSteps.testing().open();
        basePageSteps.onMainPage().header().sidebarButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().sidebar().closeButton().click();
        basePageSteps.onMainPage().sidebar().waitUntil(not(isDisplayed()));
        urlSteps.path("/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Войти»")
    public void shouldClickLoginUrl() {
        urlSteps.testing().open();
        basePageSteps.onMainPage().header().sidebarButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().sidebar().button(SIGNIN).click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s/", urlSteps.getConfig().getTestingURI())))
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Сообщения»")
    public void shouldClickMessagesUrl() {
        urlSteps.testing().open();
        basePageSteps.onMainPage().header().sidebarButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().sidebar().button("Сообщения").click();
        urlSteps.testing().path(CHAT_AUTH)
                .addParam("r2", encode(format("%s/", urlSteps.getConfig().getTestingURI())))
                .shouldNotSeeDiff();
    }
}
