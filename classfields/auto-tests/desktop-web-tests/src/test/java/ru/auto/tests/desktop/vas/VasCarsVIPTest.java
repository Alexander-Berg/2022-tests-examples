package ru.auto.tests.desktop.vas;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - услуги")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasCarsVIPTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUserOwnerWithVip").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("VIP")
    public void shouldSeeVIP() {
        basePageSteps.onCardPage().cardVas().tab("VIP").hover();
        basePageSteps.onCardPage().cardVas().waitUntil(isDisplayed()).should(hasText("VIP\n4 975 ₽\n" +
                "Турбо-продажа\n1 997 ₽\nПоднятие в поиске\n297 ₽\nВСЕ ОСНОВНЫЕ ОПЦИИ ПРОДВИЖЕНИЯ НА ВЕСЬ СРОК ОБЪЯВЛЕНИЯ\n" +
                "VIP\nМаксимум просмотров\nЭто супер-комбо — все мощности Авто.ру будут использованы для продажи " +
                "вашего автомобиля. Объявление будет размещено в специальном блоке вверху страниц, выделено цветом " +
                "и самое главное — каждый день будет автоматически подниматься на первое место до конца размещения.\n" +
                "Подключить за 4 975 ₽\nВместо 8 291 ₽\nВключены: Выделение цветом, Спецпредложение, Поднятие в ТОП, " +
                "Поднятие в поиске"));
    }
}