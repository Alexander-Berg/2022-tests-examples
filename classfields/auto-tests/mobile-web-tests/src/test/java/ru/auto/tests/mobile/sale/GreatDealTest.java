package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - хорошая/отличная цена")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GreatDealTest {

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
        mockRule.newMock().post();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отличная цена")
    public void shouldSeeExcellentPrice() {
        String description = "Цена\n700 000 ₽\nОтличная цена\n10 418 $\n · \n9 083 €\nот 11 450 ₽ / мес.\n" +
                "20 августа 2018\nНачальная цена\n850 000 ₽\n28 августа 2018\n- 20 000 ₽\n830 000 ₽\n" +
                "13 сентября 2018\n- 30 000 ₽\n800 000 ₽\n4 октября 2018\n- 50 000 ₽\n750 000 ₽\n17 октября 2018\n" +
                "- 70 000 ₽\n680 000 ₽\n15 ноября 2018\n+ 20 000 ₽\n700 000 ₽\nО скидках и акциях узнавайте по телефону\n" +
                "Стоимость этого автомобиля ниже средней рыночной относительно похожих автомобилей\n";
        String additionalDescription = "Отличной мы считаем цену на несколько процентов ниже средней для похожих " +
                "автомобилей на Авто.ру.\n\nМы учитываем: марку, модель, поколение и комплектацию, пробег, " +
                "год выпуска, регион, тип и объём двигателя, количество владельцев, тип продавца — дилер или частное " +
                "лицо.\n\nРасчёт строится на основе данных из объявления. Проверяйте реальное состояние автомобиля " +
                "в отчёте по VIN и при осмотре вживую.\nСледить за изменением цены";

        mockRule.with("desktop/OfferCarsUsedUserExcellentPrice").update();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().price().greatDealBadge().should(hasText("Отличная цена")).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText(format("%sПодробней про отличную цену\nСледить за изменением цены", description)));
        basePageSteps.onCardPage().popup().button("Подробней про отличную цену").click();
        basePageSteps.onCardPage().popup().waitUntil(hasText(format("%s%s", description, additionalDescription)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Хорошая цена")
    public void shouldSeeGoodPrice() {
        String description = "Цена\n700 000 ₽\nХорошая цена\n10 418 $\n · \n9 083 €\nот 11 450 ₽ / мес.\n" +
                "20 августа 2018\nНачальная цена\n850 000 ₽\n28 августа 2018\n- 20 000 ₽\n830 000 ₽\n13 сентября 2018\n" +
                "- 30 000 ₽\n800 000 ₽\n4 октября 2018\n- 50 000 ₽\n750 000 ₽\n17 октября 2018\n- 70 000 ₽\n" +
                "680 000 ₽\n15 ноября 2018\n+ 20 000 ₽\n700 000 ₽\nО скидках и акциях узнавайте по телефону\n" +
                "Стоимость этого автомобиля соответствует средней рыночной относительно похожих автомобилей\n";
        String additionalDescription = "Хорошей мы считаем среднюю цену для автомобилей с похожими характеристиками " +
                "на Авто.ру.\n\nМы учитываем: марку, модель, поколение и комплектацию, пробег, год выпуска, регион, " +
                "тип и объём двигателя, количество владельцев, тип продавца — дилер или частное лицо.\n\n" +
                "Расчёт строится на основе данных из объявления. Проверяйте реальное состояние автомобиля " +
                "в отчёте по VIN и при осмотре вживую.\nСледить за изменением цены";

        mockRule.with("desktop/OfferCarsUsedUserGoodPrice").update();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().price().greatDealBadge().should(hasText("Хорошая цена")).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText(format("%sПодробней про хорошую цену\nСледить за изменением цены", description)));
        basePageSteps.onCardPage().popup().button("Подробней про хорошую цену").click();
        basePageSteps.onCardPage().popup().waitUntil(hasText(format("%s%s", description, additionalDescription)));
    }
}
