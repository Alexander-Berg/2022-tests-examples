package ru.yandex.realty.mappage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.interactions.Actions;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;

@DisplayName("Карта. Общее")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DrawAreaTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        basePageSteps.resize(1920,3000);
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        drawAnArea();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим нарисованную область в урле")
    public void shouldSeeDrawArea() {
        basePageSteps.onMapPage().removeDrawArea().should(isDisplayed());
        basePageSteps.onMapPage().mapButton("Нарисованная область").should(isDisplayed());
        urlSteps.shouldUrl(containsString("mapPolygon"));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Очищаем нарисованную область")
    public void shouldClearDrawArea() {
        basePageSteps.onMapPage().mapButton("Нарисованная область").waitUntil(isDisplayed());
        basePageSteps.onMapPage().removeDrawArea().click();
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Step("Рисуем область")
    private void drawAnArea() {
        basePageSteps.onMapPage().mapButton("Нарисовать область").click();
        (new Actions(basePageSteps.getDriver()))
                .moveToElement(basePageSteps.onMapPage().map())
                .clickAndHold()
                .moveByOffset(0, 100)
                .moveByOffset(100, 0)
                .release()
                .build().perform();
    }
}
