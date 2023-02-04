package ru.auto.tests.mobile.sidebar;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.utils.Utils.getCurrentYear;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар под незарегом")
@Feature(SIDEBAR)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SidebarUnauthProdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение сайдбара под незарегом на проде (не должно быть ссылки «Моя тачка!»)")
    public void shouldSeeSidebarOnProd() {
        urlSteps.autoruProdURI().path(CARS).path(USED).path(SALE).path("/1076842087-f1e84/").open();
        basePageSteps.onCardPage().header().sidebarButton().click();
        basePageSteps.onMainPage().sidebar().waitUntil(isDisplayed()).should(hasText(format("Поиск\nВойти\n" +
                "Мои объявления\nИзбранное\nСохранённые поиски\nСообщения\nПромокоды\nМои отзывы\nЗаявки на кредит\n" +
                "Гараж\nПроАвто\nБезопасная сделка\nВыкуп\nДобавить объявление\nЛегковые\nКоммерческие\nМото\nДилеры" +
                "\nКаталог\nПолная версия\nОценить авто\nОтзывы\nЖурнал\nУчебник\nКупить ОСАГО\nСаша Котов\n" +
                "© 1996–%s ООО «Яндекс.Вертикали»", getCurrentYear())));
    }
}
