package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Каталог - карточка кузова - характеристики")
@Feature(AutoruFeatures.CATALOG)
public class BodySpecificationsElectroTest {

    private static final String MARK = "tesla";
    private static final String MODEL = "model_x";
    private static final String GENERATION = "20697853";
    private static final String BODY = "20697906";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(BODY)
                .path(SPECIFICATIONS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Характеристики")
    @Category({Regression.class})
    public void shouldSeeSpecifications() {
        basePageSteps.onCatalogBodyPage().complectationDescription().block("1").should(hasText("Запас хода489 " +
                "км\nМощность518 л.с.\nКоробкаавтомат\nТип двигателяэлектро\nПриводполный\nРазгон5 с"));
        basePageSteps.onCatalogBodyPage().complectationDescription().block("2").should(hasText("ОБЩАЯ " +
                "ИНФОРМАЦИЯ\nСтрана маркиСША\nКласс автомобиляJ\nКоличество дверей5\nКоличество мест7"));
        basePageSteps.onCatalogBodyPage().complectationDescription().block("3").should(hasText("РАЗМЕРЫ, ММ\n" +
                "Длина5004\nШирина2083\nВысота1626\nКолёсная база3061\nКлиренс180"));
    }
}
