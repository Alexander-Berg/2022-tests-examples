package ru.auto.tests.desktop.sale;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - развертка с повреждениями")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DamageTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока повреждений")
    public void shouldSeeDamageBlock() {
        basePageSteps.onCardPage().damages().waitUntil(hasText("Состояние кузова\n1\nПередний бампер\n" +
                "Царапина/Скол\nсколото крепление спойлера, расколота рамка радиатора\n2\nПереднее правое крыло\n" +
                "Царапина/Скол\nцарапина вдоль всего борта\n3\nПередняя правая дверь\nВмятина\nвмятина, на открывание " +
                "не влияет (продолжение царапины)\n4\nЗадняя правая дверь\nВмятина\nвмятина, на открывание не влияет " +
                "(продолжение царапины)\n5\nЗаднее правое крыло\nЦарапина/Скол\nцарапина вдоль всего борта\n6\n" +
                "Задний бампер\nЦарапина/Скол\nсветорассеиватели задних фонарей расколоты (держатся на скотче)" +
                "\n1\n6\n2\n5\n3\n4"
        ));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поп-ап повреждения")
    public void shouldSeePinsPopup() {
        basePageSteps.onCardPage().damages().getPin(0).should(isDisplayed()).hover();
        basePageSteps.onCardPage().activePopup().waitUntil(isDisplayed())
                .waitUntil(hasText("Передний бампер\nЦарапина/Скол\nсколото крепление спойлера, расколота рамка " +
                        "радиатора"));
    }
}