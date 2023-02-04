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
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - услуги")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasMotoTest {

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
                "desktop/OfferMotoUsedUserOwner").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Турбо-продажа")
    public void shouldSeeTurbo() {
        basePageSteps.onCardPage().cardVas().waitUntil(isDisplayed()).should(hasText("Турбо-продажа\n1 497 ₽\n" +
                "Экспресс-продажа\n799 ₽\nПоднятие в поиске\n297 ₽\nПАКЕТ ОПЦИЙ\nТурбо-продажа\nx20 просмотров\n" +
                "Ваше предложение увидит максимум посетителей — это увеличит шансы на быструю и выгодную продажу. " +
                "Объявление будет выделено цветом, поднято в топ, размещено в специальном блоке на главной странице, " +
                "на странице марки и в выдаче объявлений.\nПодключить за 1 497 ₽\nВместо 2 495 ₽\nВключены: " +
                "Выделение цветом, Спецпредложение, Поднятие в ТОП"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Экспресс-продажа")
    public void shouldSeeExpress() {
        basePageSteps.onCardPage().cardVas().tab("Экспресс-продажа").hover();
        basePageSteps.onCardPage().cardVas().waitUntil(isDisplayed()).should(hasText("Турбо-продажа\n1 497 ₽\n" +
                "Экспресс-продажа\n799 ₽\nПоднятие в поиске\n297 ₽\nПАКЕТ ОПЦИЙ\nЭкспресс-продажа\nx5 просмотров\n" +
                "Объявление будет размещено в специальном блоке на карточках похожих автомобилей, в выдаче объявлений " +
                "и на главной странице (для легковых), а также выделено цветом. Это существенно увеличит количество " +
                "просмотров.\nПодключить за 799 ₽\nВместо 1 331 ₽\nВключены: Выделение цветом, Спецпредложение"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поднятие в поиске")
    public void shouldSeeFresh() {
        basePageSteps.onCardPage().cardVas().tab("Поднятие").hover();
        basePageSteps.onCardPage().cardVas().waitUntil(isDisplayed()).should(hasText("Турбо-продажа\n1 497 ₽\n" +
                "Экспресс-продажа\n799 ₽\nПоднятие в поиске\n297 ₽\nПоднятие в поиске\nx3 просмотров\n" +
                "Самый недорогой способ продвижения, который позволит вам в любой момент оказаться наверху списка " +
                "объявлений, отсортированного по актуальности или по дате. Это поможет быстрее найти покупателя — " +
                "ведь предложения в начале списка просматривают гораздо чаще.\nПоднять за 297 ₽"));
    }
}