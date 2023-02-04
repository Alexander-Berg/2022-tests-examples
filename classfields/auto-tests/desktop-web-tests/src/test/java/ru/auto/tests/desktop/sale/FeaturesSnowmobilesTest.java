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
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка объявления - характеристики")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FeaturesSnowmobilesTest {

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
        mockRule.newMock().with("desktop/OfferMotoUsedUserSnowmobile").post();

        urlSteps.testing().path(SNOWMOBILE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    public void shouldSeeFeatures() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("Тип\nУтилитарный\n" +
                "год выпуска\n2012\nПробег\n2 800 км\nЦвет\nкрасный\nДвигатель\n1 200 см³ / 140 л.с. / Инжектор\n" +
                "Цилиндров\n4\nТактов\n4\nСостояние\nНе требует ремонта\nВладельцы\n1 владелец\nПТС\nОригинал\n" +
                "Владение\n\\d+ (лет|год|года)( и \\d+ (месяц|месяца|месяцев))?\nТаможня\nРастаможен\n" +
                "VIN\nYH2\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*")));
    }
}