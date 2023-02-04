package ru.auto.tests.desktopcompare;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравнение моделей")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AddModelTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws InterruptedException {
        urlSteps.testing().path(COMPARE_MODELS).open();
        basePageSteps.onComparePage().stub().button("Добавить модель").click();
        TimeUnit.SECONDS.sleep(1);
    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Добавить модель»")
    public void shouldClickAddModelButton() {
        basePageSteps.onComparePage().addModelPopup().markOrModel("Kia").click();
        basePageSteps.onComparePage().addModelPopup().button("Все модели").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("K5").click();
        basePageSteps.onComparePage().addModelPopup().generation("2020–2022").click();
        basePageSteps.onComparePage().addModelPopup().body("Седан").click();
        basePageSteps.onComparePage().modelsList().waitUntil(hasSize(1));
        basePageSteps.onComparePage().radioButtonSelected("Модели • 1").should(isDisplayed());
        basePageSteps.onComparePage().getModel(0).title().should(hasText("Kia K5 III"));

        basePageSteps.onComparePage().addModelButton().click();
        basePageSteps.onComparePage().addModelPopup().waitUntil(isDisplayed());
        basePageSteps.onComparePage().addModelPopup().title().should(hasText("Укажите марку автомобиля"));
        basePageSteps.onComparePage().addModelPopup().button("Все марки").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("LADA (ВАЗ)").hover().click();
        basePageSteps.onComparePage().addModelPopup().breadcrumbs().should(hasText("LADA (ВАЗ)"));
        basePageSteps.onComparePage().addModelPopup().title().should(hasText("Укажите модель"));
        basePageSteps.onComparePage().addModelPopup().button("Все модели").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("XRAY").click();
        basePageSteps.onComparePage().addModelPopup().generation("2015–2022").click();
        basePageSteps.onComparePage().addModelPopup().body("Хэтчбек 5 дв.").click();
        basePageSteps.onComparePage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onComparePage().modelsList().waitUntil(hasSize(2));
        basePageSteps.onComparePage().radioButtonSelected("Модели • 2").should(isDisplayed());
        basePageSteps.onComparePage().getModel(0).title().should(hasText("Kia K5 III"));
        basePageSteps.onComparePage().getModel(1).title().should(hasText("LADA (ВАЗ) XRAY I"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Раскрытие/скрытие полного списка марок/моделей")
    public void shouldExpandAndCollapseMarksOrModelsList() {
        int marksCount = basePageSteps.onComparePage().addModelPopup().marksOrModelsList()
                .waitUntil(hasSize(greaterThan(0))).size();
        basePageSteps.onComparePage().addModelPopup().button("Все марки").click();
        basePageSteps.onComparePage().addModelPopup().marksOrModelsList().waitUntil(hasSize(greaterThan(marksCount)));

        basePageSteps.onComparePage().addModelPopup().markOrModel("Porsche").hover().click();
        int modelsCount = basePageSteps.onComparePage().addModelPopup().marksOrModelsList()
                .waitUntil(hasSize(greaterThan(0))).size();
        basePageSteps.onComparePage().addModelPopup().button("Все модели").click();
        basePageSteps.onComparePage().addModelPopup().marksOrModelsList().waitUntil(hasSize(greaterThan(modelsCount)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск марки/модели")
    public void shouldSearchMarkOrModel() {
        String mark = "Audi";
        String model = "Q3 Sportback";

        basePageSteps.onComparePage().addModelPopup().input("Поиск марки", mark.toLowerCase());
        basePageSteps.onComparePage().addModelPopup().marksOrModelsList().should(hasSize(1));
        basePageSteps.onComparePage().addModelPopup().getListItem(0).should(hasText(mark)).click();

        basePageSteps.onComparePage().addModelPopup().input("Поиск модели", model.toLowerCase());
        basePageSteps.onComparePage().addModelPopup().marksOrModelsList().should(hasSize(1));
        basePageSteps.onComparePage().addModelPopup().getListItem(0).should(hasText(model));
    }
}