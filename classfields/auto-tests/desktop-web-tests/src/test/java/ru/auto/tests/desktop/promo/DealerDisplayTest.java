package ru.auto.tests.desktop.promo;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.auto.tests.desktop.consts.Pages.DISPLAY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо - дилеры - медийная реклама")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealerDisplayTest {

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

        urlSteps.testing().path(DEALER).path(DISPLAY).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение промо-страницы")
    public void shouldSeePromo() {
        basePageSteps.onPromoDealerPage().content().should(hasText("Медийная реклама\nАвто.ру — один из ведущих " +
                "порталов рунета с объявлениями о продаже всех видов транспорта: от легкового до коммерческого, " +
                "от ретроавтомобилей до суперкаров.\nУсловия размещения\nТысячи автолюбителей ежедневно приходят к нам, " +
                "чтобы купить или продать машины, найти шины и диски, получить информацию о тест-драйвах и узнать " +
                "свежие новости рынка.\nУсловия размещения медийной рекламы на Auto.ru вы можете узнать в отделе продаж " +
                "Яндекса по телефону +7 (495) 755‑55‑77 (доб. 5841), а с актуальными ценами — ознакомиться " +
                "в прайс-листе.\nПрайс-лист\nОставить заявку\nЗаполните эту форму и мы свяжемся с вами, " +
                "чтобы договориться о сотрудничестве.\nИмя\nТелефон\nЭлектронная почта\nНазвание компании\n" +
                "Оставить заявку\nДилерам\nРазмещение и продвижение\nДополнительные сервисы\nКонференции Авто.ру\n" +
                "3D панорамы машин\nПрофессиональным продавцам\nАналитика в Журнале →\nМедийная реклама\nО проекте\n" +
                "Логотип Авто.ру\nДоговор →"));
        basePageSteps.onPromoDealerPage().sidebar().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Прайс-лист»")
    public void shouldClickPriceListButton() {
        basePageSteps.onPromoDealerPage().button("Прайс-лист").click();
        urlSteps.switchToNextTab();
        urlSteps.fromUri("https://yandex.ru/adv/products/display/autoru").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Оставить заявку")
    public void shouldOrder() {
        basePageSteps.onPromoDealerPage().form().input("Имя", "Иван Иванов");
        basePageSteps.onPromoDealerPage().form().input("Телефон", getRandomPhone());
        basePageSteps.onPromoDealerPage().form().input("Электронная почта", getRandomEmail());
        basePageSteps.onPromoDealerPage().form().input("Название компании", getRandomString());
        basePageSteps.onPromoDealerPage().form().button("Оставить заявку").click();
        basePageSteps.onPromoDealerPage().successMessage()
                .waitUntil(hasText("Спасибо, ваша заявка отправлена. Менеджер свяжется с вами в ближайшее время."));
    }
}