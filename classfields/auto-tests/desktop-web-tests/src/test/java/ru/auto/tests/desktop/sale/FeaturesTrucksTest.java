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
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка объявления - характеристики")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FeaturesTrucksTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String MARK = "/zil/";
    private static final String MODEL = "/5301/";

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
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/OfferTrucksUsedUser",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    public void shouldSeeFeatures() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("год выпуска\n2000\n" +
                "Пробег\n54 000 км\nКузов\nфургон\nЦвет\nбелый\nДвигатель\n4.5 л\nРуль\nЛевый\n" +
                "Состояние\nНе требует ремонта\nВладельцы\n3 или более\nПТС\nОригинал\n" +
                "Владение\n\\d+ (лет|год|года)( и \\d+ (месяц|месяца|месяцев))?\n" +
                "Таможня\nРастаможен\nVIN\nX5S47410\\*Y0\\*\\*\\*\\*12")));
    }

    @Test
    @DisplayName("Клик по году")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickYear() {
        basePageSteps.onCardPage().features().feature("год выпуска").button().click();
        urlSteps.testing().path(MOSKVA).path(TRUCK).path(MARK).path(MODEL).path("/2000-year/").path(USED)
                .shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по кузову")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickBodyType() {
        basePageSteps.onCardPage().features().feature("Кузов").button().click();
        urlSteps.testing().path(MOSKVA).path(TRUCK).path(MARK).path(MODEL).path(USED)
                .addParam("truck_type", "VAN").shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по цвету")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickColor() {
        basePageSteps.onCardPage().features().feature("Цвет").button().click();
        urlSteps.testing().path(MOSKVA).path(TRUCK).path(MARK).path(MODEL).path(USED)
                .addParam("color", "FAFBFB").shouldNotSeeDiff();
    }
}
