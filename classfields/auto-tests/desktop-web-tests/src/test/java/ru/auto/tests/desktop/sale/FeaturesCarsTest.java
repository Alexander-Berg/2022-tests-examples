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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка объявления - характеристики")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FeaturesCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String MARK = "/land_rover/";
    private static final String MODEL = "/discovery/";

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/SessionUnauth",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    public void shouldSeeFeatures() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("год выпуска\n2008\nПробег\n" +
                "210 000 км\nКузов\nвнедорожник 5 дв.\nЦвет\nсеребристый\nДвигатель\n2.7 л / 190 л.с. / Дизель\n" +
                "Комплектация\n57 опций\nКоробка\nавтоматическая\nПривод\nполный\nРуль\nЛевый\nСостояние\n" +
                "Не требует ремонта\nВладельцы\n3 или более\nПТС\nОригинал\nВладение\n\\d+ (год|года|лет)( и \\d+ (месяц|месяцев|месяца))?\n" +
                "Таможня\nРастаможен\nГарантия\nДо января 2030\nVIN\nSALLAAA148A485103\nГосномер\nА900ВН777")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по году")
    public void shouldClickYear() {
        basePageSteps.onCardPage().features().feature("год выпуска").button().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path("/2008-year/").path(USED)
                .shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по кузову")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickBodyType() {
        basePageSteps.onCardPage().features().feature("Кузов").button().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED)
                .path("/body-allroad_5_doors/").shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по цвету")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickColor() {
        basePageSteps.onCardPage().features().feature("Цвет").button().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED)
                .path("/color-serebristyj/").shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по двигателю")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickEngine() {
        basePageSteps.onCardPage().features().feature("Двигатель").button().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED)
                .path("/engine-dizel/").shouldNotSeeDiff();
    }
}