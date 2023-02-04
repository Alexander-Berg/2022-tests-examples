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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Неактивное объявление мото")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class InactiveSaleMotoTest {

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
        mockRule.newMock().with("desktop/OfferMotoUsedUserInactive").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Плашка «уже продан»")
    public void shouldSeeSoldMessage() {
        basePageSteps.onCardPage().soldMessage().should(hasText("Этот мотоцикл уже продан\nОбъявление доступно " +
                "только по прямой ссылке"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Контакты")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().cardHeader().should(hasText("Harley-Davidson Dyna Super Glide\n6 ноября 2018\n" +
                "67 (15 сегодня)\n№ 1076842087"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Информация")
    public void shouldSeeInfo() {
        basePageSteps.onCardPage().features().should(hasText(anyOf(
                matchesPattern("Тип\nЧоппер\nгод выпуска\n2010\n" +
                        "Пробег\n20 000 км\nЦвет\nчёрный\nДвигатель\n1 584 см³ / 75 л.с. / Инжектор\nЦилиндров\n2 / V-образное\n" +
                        "Тактов\n4\nКоробка\n6 передач\nПривод\nремень\nСостояние\nНе требует ремонта\nВладельцы\n1 владелец\n" +
                        "ПТС\nОригинал\nВладение\n\\d+ (лет|год|года)( и \\d+ (месяц|месяца|месяцев))?\nТаможня\nРастаможен"),
                matchesPattern("Тип\nЧоппер\nгод выпуска\n2010\n" +
                        "Пробег\n20 000 км\nЦвет\nчёрный\nДвигатель\n1 584 см³ / 75 л.с. / Инжектор\nЦилиндров\n2 / V-образное\n" +
                        "Тактов\n4\nКоробка\n6 передач\nПривод\nремень\nСостояние\nНе требует ремонта\nВладельцы\n1 владелец\n" +
                        "ПТС\nОригинал\nВладение\n\\d+ (лет|год|года)\nТаможня\nРастаможен"))));
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