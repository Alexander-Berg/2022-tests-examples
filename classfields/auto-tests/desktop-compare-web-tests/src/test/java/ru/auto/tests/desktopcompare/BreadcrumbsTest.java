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
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Хлебные крошки")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BreadcrumbsTest {

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
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        basePageSteps.onComparePage().addModelPopup().bredcrumb("Kia").click();
        basePageSteps.onComparePage().addModelPopup().breadcrumbs().waitUntil(hasText(""));
        basePageSteps.onComparePage().addModelPopup().title().waitUntil(hasText("Укажите марку автомобиля"));
        basePageSteps.onComparePage().addModelPopup().marksOrModelsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onComparePage().addModelPopup().bredcrumb("K5").click();
        basePageSteps.onComparePage().addModelPopup().breadcrumbs().waitUntil(hasText("Kia"));
        basePageSteps.onComparePage().addModelPopup().title().waitUntil(hasText("Укажите модель"));
        basePageSteps.onComparePage().addModelPopup().marksOrModelsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по поколению")
    public void shouldClickGeneration() {
        basePageSteps.onComparePage().addModelPopup().bredcrumb("III").click();
        basePageSteps.onComparePage().addModelPopup().breadcrumbs().waitUntil(hasText("Kia\n  K5"));
        basePageSteps.onComparePage().addModelPopup().title().waitUntil(hasText("Поколение"));
        basePageSteps.onComparePage().addModelPopup().generation("2020–2022").waitUntil(isDisplayed());
    }
}