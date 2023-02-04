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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное новое объявление под зарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActiveSaleNewRegTrucksTest {

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

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferTrucksNew",
                "desktop/ReferenceCatalogTrucksDictionariesV1Equipment").post();

        urlSteps.testing().path(TRUCK).path(NEW).path(SALE).path(SALE_ID).open();
        basePageSteps.setWindowMaxHeight();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение цены")
    public void shouldSeePrice() {
        basePageSteps.onCardPage().price().should(hasText("от 2 710 000 ₽\nс НДС · 2 750 000 ₽ без скидок"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа с ценами")
    public void shouldSeePricePopup() {
        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Цена с НДС\nот 2 710 000 ₽\n" +
                "Продавец указал, что предоставит счёт-фактуру на основании которой покупатель-юрлицо может заявить НДС " +
                "к вычету.\nЦена без скидки\n2 750 000 ₽\n · \n37 176 $\n · \n34 349 €\nСкидки\nВ кредит\nдо 30 000 ₽\n" +
                "С каско\nдо 20 000 ₽\nВ лизинг\nдо 50 000 ₽\nВ трейд-ин\nдо 10 000 ₽\nМаксимальная\n40 000 ₽\n" +
                "Максимальная скидка, которую может предоставить дилер. Подробности узнавайте по телефону.\nПозвонить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Отображение галереи")
    public void shouldSeeGallery() {
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(isDisplayed());
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCardPage().gallery());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(isDisplayed());
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCardPage().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    @Owner(DSVICHIHIN)
    public void shouldSeeCardFeatures() {
        basePageSteps.onCardPage().features().should(hasText("Характеристики\nгод выпуска\n2019\n" +
                "Кузов\nизотермический кузов\nЦвет\nбелый\nДвигатель\n3.9 л / 170 л.с. / Дизель\nГ/подъёмность\n5.0 т\n" +
                "Коробка\nмеханическая\nТип кабины\n2-х местная с 1 спальным\nПодвеска кабины\nПневматическая\n" +
                "Подвеска шасси\nПневмо-пневмо\nКолёсная формула\n4x2\nКласс выхлопа EURO\n5\nVIN\nXWE**************"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение комментария продавца")
    public void shouldSeeSellerComment() {
        basePageSteps.onCardPage().sellerComment().should(hasText("Комментарий продавца\nHYUNDAI HD78 (Евро-5). " +
                "Изотермический фургон\nЦена указана за ГОТОВЫЙ автомобиль с надстройкой: новый в наличии, с ПТС!\n" +
                "Антикоррозионная обработка в подарок!\nНадстройка: Фургон изотермический С/П 50 мм (возможно " +
                "установка ХОУ)., габаритные размеры 4,4х2,2х2,2 м., плакированный металл, пол - транспортная " +
                "ламинированная сетчатая фанера, фурнитура оцинкованная. До 8 европаллет !!!\nКомплектация: " +
                "система курсовой устойчивости (VDC), антиблокировочная система (ABS), электростеклоподъемники, " +
                "обогрев зеркал заднего вида, передние регулируемые противотуманные фары с линзой, магнитола AM/FM, " +
                "AUX\nПоказать полностью"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(DSVICHIHIN)
    public void shouldSeeComplectation() {
        basePageSteps.onCardPage().complectation().should(hasText("Комплектация\nБезопасность\n3\nУправление\n1\n" +
                "Комфорт\n3\nОбзор\n3\nМультимедиа\n1"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок «Контакты»")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().should(hasText(matchesPattern("Авто-М Hyundai Подольск\n" +
                "Официальный дилер\n• На Авто.ру \\d+ (год|года|лет)\nПодольск, Домодедовское шоссе, 5. На карте\n " +
                "станция Подольск\nДоехать с Яндекс.Такси\nЗаказать обратный звонок")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Карта с адресом")
    public void shouldSeeMap() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().contacts().button("На\u00a0карте"), 0, 100);
        basePageSteps.onCardPage().contacts().button("На\u00a0карте").should(isDisplayed()).click();
        basePageSteps.onCardPage().yandexMap().waitUntil("Не подгрузились Яндекс.Карты", isDisplayed(), 5);
    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов")
    public void shouldSeePhones() {
        mockRule.with("desktop/OfferTrucksPhones").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-27\n" +
                "с 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }
}
