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
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Неактивное объявление комтранса")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class InactiveSaleCommerceTest {

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
        mockRule.newMock().with("desktop/OfferTrucksUsedUserInactive").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Плашка «уже продан»")
    public void shouldSeeSoldMessage() {
        basePageSteps.onCardPage().soldMessage().should(hasText("Этот грузовик уже продан\n" +
                "Объявление доступно только по прямой ссылке"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Контакты")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().cardHeader().should(hasText("ЗИЛ 5301 \"Бычок\"\n20 января 2019\n147 (18 сегодня)\n" +
                "№ 1076842087"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Информация")
    public void shouldSeeInfo() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("год выпуска\n2000\n" +
                "Пробег\n54 000 км\nКузов\nфургон\nЦвет\nбелый\nДвигатель\n4.5 л\nРуль\nЛевый\n" +
                "Состояние\nНе требует ремонта\nВладельцы\n3 или более\nПТС\nОригинал\n" +
                "Владение\n\\d+ (лет|год|года)( и \\d+ (месяц|месяца|месяцев))?\nТаможня\nРастаможен\n" +
                "VIN\nX5S47410\\*Y0\\*\\*\\*\\*12")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не должно быть блока контактов в галерее")
    public void shouldNotSeeContactsInGallery() {
        basePageSteps.onCardPage().gallery().currentImage().should(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().waitUntil(isDisplayed());
        basePageSteps.onCardPage().fullScreenGallery().contacts().showPhoneButton().should(not(isDisplayed()));
        basePageSteps.onCardPage().fullScreenGallery().contacts().address().should(not(isDisplayed()));
        basePageSteps.onCardPage().fullScreenGallery().contacts().sendMessageButton().should(not(isDisplayed()));
    }
}