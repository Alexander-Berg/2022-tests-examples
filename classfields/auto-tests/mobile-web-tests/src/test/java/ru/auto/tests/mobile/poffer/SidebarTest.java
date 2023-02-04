package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.auto.tests.desktop.utils.Utils.getCurrentYear;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар")
@Epic(BETA_POFFER)
@Feature("Сайдбар")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SidebarTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).open();
        basePageSteps.onPofferPage().addOfferNavigateModal().closeIcon().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сайдбара")
    public void shouldSeeSidebar() {
        basePageSteps.onPofferPage().header().sidebarButton().click();

        basePageSteps.onPofferPage().sidebar().waitUntil(isDisplayed()).should(hasText(format("Поиск\nВойти\n" +
                "Мои объявления\nИзбранное\nСохранённые поиски\nСообщения\nПромокоды\nМои отзывы\nЗаявки на кредит\n" +
                "Гараж\nПроАвто\nБезопасная сделка\nВыкуп\nЛегковые\nКоммерческие\nМото\nДилеры\nКаталог\n" +
                "Оценить авто\nОтзывы\nЖурнал\nУчебник\nКупить ОСАГО\nСаша Котов\n" +
                "© 1996–%s ООО «Яндекс.Вертикали»", getCurrentYear())));
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Скрытие сайдбара")
    public void shouldCloseSidebar() {
        basePageSteps.onPofferPage().header().sidebarButton().should(isDisplayed()).click();
        basePageSteps.onPofferPage().sidebar().closeButton().hover().click();

        basePageSteps.onPofferPage().sidebar().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

}
