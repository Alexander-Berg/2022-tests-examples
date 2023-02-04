package ru.auto.tests.desktop.promo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
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

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CONFERENCES;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо - Дилеры - Конференции")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealerConferencesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth").post();

        urlSteps.testing().path(DEALER).path(CONFERENCES).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение промо-страницы")
    public void shouldSeePromo() {
        basePageSteps.onPromoDealerConferencesPage().h1().should(hasText("Конференции"));
        basePageSteps.onPromoDealerConferencesPage().description().should(hasText("Авто.ру помогает дилерским центрам " +
                "продавать машины. Для этого мы проводим конференции и встречи в разных городах России. " +
                "На мероприятиях дилеры могут обменяться опытом и узнать о лучших инструментах и практиках. " +
                "Мы сами подбираем докладчиков и темы."));
        basePageSteps.onPromoDealerConferencesPage().conferencesList().should(hasSize(greaterThan(0)));
        basePageSteps.onPromoDealerConferencesPage().orderForm().should(hasText("Заказать мероприятие\nМы можем провести " +
                "отдельное мероприятие для вашей группы компаний. Формат — бизнес-завтрак или воркшоп. На таких встречах " +
                "выступаем мы сами и наши коллеги из Яндекса. Это хорошая возможность узнать о продуктах для дилеров " +
                "от Авто.ру и рекламных технологиях Яндекса.\nИмя\nТелефон\nЭлектронная почта\nНазвание компании\n" +
                "Оставить заявку"));
        basePageSteps.onPromoDealerConferencesPage().sidebar().should(isDisplayed());
        basePageSteps.onPromoDealerConferencesPage().header().should(isDisplayed());
        basePageSteps.onPromoDealerConferencesPage().footer().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Оставить заявку на мероприятие»")
    public void shouldClickOrderButton() {
        long pageOffset = basePageSteps.getPageYOffset();
        basePageSteps.onPromoDealerPage().button("Оставить заявку на мероприятие").click();
        urlSteps.fragment("promo-page-form").shouldNotSeeDiff();
        waitSomething(1, TimeUnit.SECONDS);
        assertThat("Не произошёл скролл к блоку", basePageSteps.getPageYOffset() > pageOffset);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заказать мероприятие")
    public void shouldOrder() {
        basePageSteps.onPromoDealerPage().form().input("Имя", "Иван Иванов");
        basePageSteps.onPromoDealerPage().form().input("Телефон", getRandomPhone());
        basePageSteps.onPromoDealerPage().form().input("Электронная почта", "test@auto.ru");
        basePageSteps.onPromoDealerPage().form().input("Название компании", "Спектр");
        basePageSteps.onPromoDealerPage().form().button("Оставить заявку").click();
        basePageSteps.onPromoDealerPage().successMessage()
                .waitUntil(hasText("Спасибо за заявку. В ближайшее время мы Вам перезвоним."));
    }
}
