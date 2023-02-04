package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - плавающая панель")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StickyBarScreenshotTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String saleMock;

    @Parameterized.Parameter(3)
    public String sliderText;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, USED, "desktop/OfferCarsUsedUser",
                        "Федор\nМоскваметро Марьино\n700 000 ₽\nот 20 734 ₽ / мес.\nНаписать\nПоказать телефон"},
                {CARS, NEW, "desktop/OfferCarsNewDealer",
                        "АвтоСпецЦентр KIA Север\nМоскваКлязьминская улица, 5 · 33 автомобиля\nот 1 254 900 ₽\n1 474 900 ₽ без скидок\nПоказать телефон"},

                {TRUCK, USED, "desktop/OfferTrucksUsedUser",
                        "Юрий\n Улица ГорчаковаБульвар адмирала Ушакова\n250 000 ₽\nНаписать\nПоказать телефон"},
                {TRUCK, NEW, "desktop/OfferTrucksNew",
                        "Авто-М Hyundai Подольск\n станция ПодольскПодольскДомодедовское шоссе, 5\nот 2 710 000 ₽\n2 750 000 ₽ без скидок\nПоказать телефон"},

                {MOTORCYCLE, USED, "desktop/OfferMotoUsedUser",
                        "Частное лицо\nМоскваПоселок Остров\n530 000 ₽\nНаписать\nПоказать телефон"},
                {MOTORCYCLE, NEW, "desktop/OfferMotoNew",
                        "АВИЛОН BMW ВОЛГОГРАДСКИЙ ПРОСПЕКТ\n ТекстильщикиВолгоградский проспект\n1 500 000 ₽\nПоказать телефон"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock,
                "desktop/SharkBankList",
                "desktop-lk/SharkCreditProductList").post();

        urlSteps.testing().path(category).path(section).path(SALE).path(SALE_ID).open();
        basePageSteps.scrollDown(1000);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение плавающей панели")
    public void shouldSeeStickyBar() {
        basePageSteps.onCardPage().stickyBar().waitUntil(isDisplayed()).should(hasText(sliderText));
    }
}