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

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка объявления - характеристики")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FeaturesMotoTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String MARK = "/harley_davidson/";
    private static final String MODEL = "/dyna_super_glide/";

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
        mockRule.newMock().with("desktop/SearchMotoBreadcrumbs",
                "desktop/SessionUnauth",
                "desktop/OfferMotoUsedUser",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    public void shouldSeeFeatures() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("Тип\nЧоппер\nгод выпуска\n2010\n" +
                "Пробег\n20 000 км\nЦвет\nчёрный\nДвигатель\n1 584 см³ / 75 л.с. / Инжектор\nЦилиндров\n" +
                "2 / V-образное\nТактов\n4\nКоробка\n6 передач\nПривод\nремень\nСостояние\nНе требует ремонта\n" +
                "Владельцы\n1 владелец\nПТС\nОригинал\nВладение\n\\d+ (лет|год|года)( и \\d+ (месяц|месяца|месяцев))?\nТаможня\nРастаможен")));
    }

    @Test
    @DisplayName("Клик по году")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickYear() {
        basePageSteps.onCardPage().features().feature("год выпуска").button().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(MODEL).path("/2010-year/").path(USED)
                .shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по цвету")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickColor() {
        basePageSteps.onCardPage().features().feature("Цвет").button().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(MODEL).path(USED)
                .addParam("color", "040001").shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по двигателю")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickEngine() {
        basePageSteps.onCardPage().features().feature("Двигатель").button().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(MODEL).path(USED)
                .addParam("engine_type", "INJECTOR").shouldNotSeeDiff();
    }
}
