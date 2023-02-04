package ru.auto.tests.mobilecompare;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Сравнение 2-х моделей")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class ModelVsModelBreadcrumbsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(COMPARE_CARS).path("/hyundai-sonata-vs-kia-rio/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по «Авто.ру»")
    public void shouldClickAutoRu() {
        basePageSteps.onCompareCarsPage().breadcrumbs().getItem(0).should(hasText("Авто.ру")).hover().click();
        urlSteps.testing().path(MOSKVA).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по «Каталог»")
    public void shouldClickCatalog() {
        basePageSteps.onCompareCarsPage().breadcrumbs().getItem(1).should(hasText("Каталог")).click();
        urlSteps.testing().path(CATALOG).path(CARS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        basePageSteps.onCompareCarsPage().breadcrumbs().getItem(2).should(hasText("Hyundai")).click();
        urlSteps.testing().path(CATALOG).path(CARS).path("/hyundai/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onCompareCarsPage().breadcrumbs().getItem(3).should(hasText("Sonata")).click();
        urlSteps.testing().path(CATALOG).path(CARS).path("/hyundai/sonata/").shouldNotSeeDiff();
    }
}
