package ru.auto.tests.desktopcompare;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_OFFERS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравнение - заглушка")
@Feature(AutoruFeatures.COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class StubTest {

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

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сравнение объявлений - отображение заглушки")
    public void shouldSeeSalesStub() {
        urlSteps.testing().path(COMPARE_OFFERS).open();

        basePageSteps.onComparePage().stub().should(hasText("Нажмите кнопку «Добавить в сравнение» на странице " +
                "объявления, в списке избранных объявлений или в результатах поиска\nПоиск авто"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сравнение объявлений - кнопка «Поиск авто»")
    public void shouldClickSearchAutoButton() {
        urlSteps.testing().path(COMPARE_OFFERS).open();

        basePageSteps.onComparePage().stub().button("Поиск авто").click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сравнение моделей - отображение заглушки")
    public void shouldSeeModelsStub() {
        urlSteps.testing().path(COMPARE_MODELS).open();

        basePageSteps.onComparePage().stub().should(hasText("Нажмите кнопку «Добавить модель», чтобы выбрать модели " +
                "и сравнить их\nДобавить модель"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сравнение моделей - кнопка «Добавить модель»")
    public void shouldClickAddModelButton() {
        mockRule.newMock().with("desktop/ProxyPublicApi").post();

        urlSteps.testing().path(COMPARE_MODELS).open();

        mockRule.with("desktop-compare/UserCompareCarsModelsPost",
                "desktop-compare/UserCompareCarsModelsLadaXray").update();

        basePageSteps.onComparePage().stub().button("Добавить модель").click();
        basePageSteps.onComparePage().addModelPopup().waitUntil(isDisplayed());
        basePageSteps.onComparePage().addModelPopup().title().should(hasText("Укажите марку автомобиля"));
        basePageSteps.onComparePage().addModelPopup().button("Все марки").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("LADA (ВАЗ)").click();
        basePageSteps.onComparePage().addModelPopup().breadcrumbs().should(hasText("LADA (ВАЗ)"));
        basePageSteps.onComparePage().addModelPopup().title().should(hasText("Укажите модель"));
        basePageSteps.onComparePage().addModelPopup().button("Все модели").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("XRAY").click();
        basePageSteps.onComparePage().addModelPopup().generation("2015–2022").click();
        basePageSteps.onComparePage().addModelPopup().body("Хэтчбек 5 дв.").click();
        basePageSteps.onComparePage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onComparePage().modelsList().waitUntil(hasSize(1));
        basePageSteps.onComparePage().radioButtonSelected("Модели • 1").should(isDisplayed());
        basePageSteps.onComparePage().getModel(0).title().should(hasText("LADA (ВАЗ) XRAY I"));
    }
}