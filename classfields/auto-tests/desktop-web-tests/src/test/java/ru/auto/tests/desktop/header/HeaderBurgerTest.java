package ru.auto.tests.desktop.header;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Шапка - бургер")
@Feature(HEADER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HeaderBurgerTest {

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение бургера")
    public void shouldSeeBurger() {
        basePageSteps.onMainPage().header().burgerButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().header().burger().waitUntil(hasText("ПроАвто\nКредиты\nСтраховки\nОтзывы\nКаталог\n" +
                "Оценить автомобиль\nГараж\nБезопасная сделка\nВыкуп\nЛегковые\nС пробегом\nНовые\nНовинки автопрома\nДилеры\n" +
                "Видео\nМото\nМотоциклы\nСкутеры\nМотовездеходы\nСнегоходы\nКоммерческие\nЛёгкие коммерческие\nГрузовики\n" +
                "Седельные тягачи\nАвтобусы\nПрицепы и полуприцепы\nСельскохозяйственная\nСтроительная и дорожная\n" +
                "Погрузчики\nАвтокраны\nЭкскаваторы\nБульдозеры\nКоммунальная\nПартнёрам\nДилерам\nРазместить рекламу\n" +
                "Сотрудничество с ПроАвто\nЖурнал\nНовости\nТесты\nВидео\nРазбор\nИгры\nПодборки\nУчебник\nПро бизнес\n" +
                "Разное\nФорумы\nДоговор купли-продажи\nПомощь\nСтань частью команды\nО проекте\nАналитика Авто.ру"));
    }
}