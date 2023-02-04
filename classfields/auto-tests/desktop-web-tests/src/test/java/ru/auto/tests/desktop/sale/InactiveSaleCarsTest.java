package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Неактивное объявление авто")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class InactiveSaleCarsTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUserInactive",
                "desktop/OfferCarsRelated").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Плашка «Этот автомобиль уже продан»")
    public void shouldSeeSoldMessage() {
        basePageSteps.onCardPage().soldMessage().should(hasText("Этот автомобиль уже продан\nОбъявление доступно " +
                "только по прямой ссылке"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Похожие объявления")
    public void shouldSeeRelated() {
        basePageSteps.onCardPage().soldMessage().hover();
        basePageSteps.onCardPage().horizontalRelated().should(hasText("Похожие объявления\nToyota Land Cruiser\n2012, " +
                "38 000 км\n2 400 000 ₽\nDodge Caravan\n2005, 115 000 км\n349 000 ₽\nVolkswagen Caddy\n" +
                "2007, 128 000 км\n449 000 ₽\nGreat Wall Hover\n2008, 134 000 км\n329 000 ₽\nMitsubishi Lancer\n" +
                "2007, 130 000 км\n289 000 ₽"));
        basePageSteps.onCardPage().horizontalRelated().itemsList().forEach(i -> i.image().should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Контакты")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().cardHeader().should(hasText("Land Rover Discovery III\n20 августа 2018\n" +
                "1296 (35 сегодня)\n№ 1076842087"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Информация")
    public void shouldSeeInfo() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("год выпуска\n2008\nПробег\n" +
                "210 000 км\nКузов\nвнедорожник 5 дв.\nЦвет\nсеребристый\nДвигатель\n2.7 л / 190 л.с. / Дизель\n" +
                "Комплектация\n57 опций\nКоробка\nавтоматическая\nПривод\nполный\nРуль\nЛевый\nСостояние\n" +
                "Не требует ремонта\nВладельцы\n3 или более\nПТС\nОригинал\nВладение\n\\d+ (год|года|лет)( и \\d+ (месяц|месяца|месяцев))?\nТаможня\n" +
                "Растаможен\nVIN\nSALLAAA148A485103\nГосномер\nА900ВН777")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не должно быть блока контактов в галерее")
    public void shouldNotSeeContactsInGallery() {
        basePageSteps.onCardPage().gallery().currentImage().should(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().contacts().showPhoneButton().should(not(isDisplayed()));
        basePageSteps.onCardPage().fullScreenGallery().contacts().address().should(not(isDisplayed()));
        basePageSteps.onCardPage().fullScreenGallery().contacts().sendMessageButton().should(not(isDisplayed()));
    }
}