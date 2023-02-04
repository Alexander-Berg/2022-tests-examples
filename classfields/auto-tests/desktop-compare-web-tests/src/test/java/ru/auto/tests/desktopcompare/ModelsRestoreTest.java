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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравнение моделей - возвращение удалённой из сравнения модели")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ModelsRestoreTest {

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
        basePageSteps.onComparePage().addModelPopup().markOrModel("Kia").click();
        basePageSteps.onComparePage().addModelPopup().button("Все модели").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("K5").click();
        basePageSteps.onComparePage().addModelPopup().generation("2020–2022").click();
        basePageSteps.onComparePage().addModelPopup().body("Седан").click();

        basePageSteps.onComparePage().addModelButton().click();
        TimeUnit.SECONDS.sleep(1);
        basePageSteps.onComparePage().addModelPopup().button("Все марки").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("LADA (ВАЗ)").hover().click();
        basePageSteps.onComparePage().addModelPopup().button("Все модели").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("XRAY").click();
        basePageSteps.onComparePage().addModelPopup().generation("2015–2022").click();
        basePageSteps.onComparePage().addModelPopup().body("Хэтчбек 5 дв.").click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Возвращение удалённой из сравнения модели")
    @Category({Regression.class, Testing.class})
    public void shouldRestoreModel() {
        basePageSteps.onComparePage().getModel(1).hover();
        basePageSteps.onComparePage().getModel(1).deleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().notifier().waitUntil(isDisplayed());

        basePageSteps.onComparePage().notifier().button(" Вернуть").click();
        basePageSteps.onComparePage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onComparePage().modelsList().waitUntil(hasSize(2));
        basePageSteps.onComparePage().getModel(0).title().should(hasText("Kia K5 III"));
        basePageSteps.onComparePage().getModel(1).title().should(hasText("LADA (ВАЗ) XRAY I"));

        urlSteps.refresh();
        basePageSteps.onComparePage().modelsList().waitUntil(hasSize(2));
        basePageSteps.onComparePage().getModel(0).title().should(hasText("Kia K5 III"));
        basePageSteps.onComparePage().getModel(1).title().should(hasText("LADA (ВАЗ) XRAY I"));
    }
}