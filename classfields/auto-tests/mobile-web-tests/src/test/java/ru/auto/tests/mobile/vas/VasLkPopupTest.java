package ru.auto.tests.mobile.vas;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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

import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Личный кабинет. Покупка услуг продвижения. Поп-ап услуги продвижения")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VasLkPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/User",
                "desktop/SessionAuthUser",
                "desktop/UserOffersCarsActive").post();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Турбо-продажа")
    public void shouldSeeTurboPopup() {
        basePageSteps.onLkPage().vas().service("Турбо-продажа").click();
        basePageSteps.onLkPage().vasPopup().waitUntil(isDisplayed()).should(hasText("Турбо-продажа\n20 просмотров\n" +
                "Ваше предложение увидит максимум посетителей — это увеличит шансы на быструю и выгодную продажу. " +
                "Объявление будет выделено цветом, поднято в топ, размещено в специальном блоке на главной странице, " +
                "на странице марки и в выдаче объявлений. Действует 3 дня.\n581 ₽-40%\n349 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Экспресс-продажа")
    public void shouldSeeExpressPopup() {
        basePageSteps.onLkPage().vas().service("Экспресс-продажа").click();
        basePageSteps.onLkPage().vasPopup().waitUntil(isDisplayed()).should(hasText("Экспресс-продажа\n5 просмотров\n" +
                "Объявление будет размещено в специальном блоке на карточках похожих автомобилей, в выдаче объявлений " +
                "и на главной странице (для легковых), а также выделено цветом. Это существенно увеличит количество " +
                "просмотров. Действует 6 дней.\n411 ₽-40%\n247 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поднять в поиске")
    public void shouldSesFreshPopup() {
        basePageSteps.onLkPage().vas().service("Поднять в поиске ").icon().click();
        basePageSteps.onLkPage().vasPopup().waitUntil(isDisplayed()).should(hasText("Поднятие в поиске\n3 просмотров\n" +
                "Самый недорогой способ продвижения, который позволит вам в любой момент оказаться наверху списка " +
                "объявлений, отсортированного по актуальности или по дате. Это поможет быстрее найти покупателя — " +
                "ведь предложения в начале списка просматривают гораздо чаще. Действует 1 день.\nПодключить за 69 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поднятие в ТОП")
    public void shouldSeeTopPopup() {
        basePageSteps.onLkPage().vas().service("Поднятие в ТОП").click();
        basePageSteps.onLkPage().vasPopup().waitUntil(isDisplayed()).should(hasText("Поднятие в ТОП\n15 просмотров\n" +
                "Ваше объявление окажется в специальном блоке на самом верху списка при сортировке по актуальности " +
                "или по дате. Покупатели вас точно не пропустят. Действует 3 дня.\nПодключить за 199 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выделение цветом")
    public void shouldSeeColorPopup() {
        basePageSteps.onLkPage().vas().service("Выделение цветом").click();
        basePageSteps.onLkPage().vasPopup().waitUntil(isDisplayed()).should(hasText("Выделение цветом\n2 просмотров\n" +
                "Отличная возможность выделить своё предложение среди других — в результатах поиска оно будет " +
                "привлекать больше внимания. Действует 3 дня.\nПодключить за 69 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Спецпредложение")
    public void shouldSeeSpecialPopup() {
        basePageSteps.onLkPage().vas().service("Спецпредложение").click();
        basePageSteps.onLkPage().vasPopup().waitUntil(isDisplayed()).should(hasText("Спецпредложение\n5 просмотров\n" +
                "Ваше объявление будет отображаться в специальном блоке в результатах поиска и на карточках объявлений " +
                "о продаже аналогичных авто. А для легковых — также на главной странице и в Каталоге. " +
                "Действует 3 дня.\nПодключить за 197 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Показ в Историях")
    public void shouldSeeStoriesPopup() {
        basePageSteps.onLkPage().vas().service("Показ в Историях").click();
        basePageSteps.onLkPage().vasPopup().waitUntil(isDisplayed()).should(hasText("Показ в Историях\n40 просмотров" +
                "\nОпубликуем ваше объявление на главной странице — в разделе «Истории». Здесь его увидят пользователи " +
                "мобильной версии сайта и мобильного приложения. Это уникальная и самая заметная опция продвижения. " +
                "Действует 3 дня.\nПодключить за 4 194 ₽"));
    }
}
