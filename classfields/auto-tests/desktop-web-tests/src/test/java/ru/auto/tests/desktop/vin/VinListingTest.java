package ru.auto.tests.desktop.vin;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Бейдж «Отчёт ПроАвто» на сниппете объявления в листинге")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VinListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam("has_history", "true").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поп-ап «Есть отчёт ПроАвто»")
    public void shouldSeePopup() {
        basePageSteps.onListingPage().getSale(0).badge("Отчёт ПроАвто").hover();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed())
                .should(hasText("Есть отчёт ПроАвто\nВы можете купить отчёт с историей этого автомобиля на странице " +
                        "объявления.\nПодробнее"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onListingPage().getSale(0).badge("Отчёт ПроАвто").hover();
        basePageSteps.onListingPage().activePopupLink("Подробнее").waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(HISTORY).addParam("from", "listing_baloon").shouldNotSeeDiff();
    }
}