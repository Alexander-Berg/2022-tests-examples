package ru.auto.tests.amp.sale;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное новое объявление")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class AmpSaleTruckNewTest {

    private static final String SALE_ID = "1076842087-f1e84";

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
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/OfferTrucksNew",
                "desktop/ReferenceCatalogTrucksDictionariesV1Equipment").post();

        urlSteps.testing().path(AMP).path(TRUCK).path(NEW).path(SALE).path("/Hyundai/hd78/")
                .path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комментария полностью / частично")
    @Owner(NATAGOLOVKINA)
    public void shouldExpandAndCollapseSellerComment() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sellerComment().showAllButton("Показать полностью"));
        basePageSteps.onCardPage().sellerComment().waitUntil(hasText("Комментарий продавца\nHYUNDAI HD78 (Евро-5). " +
                "Изотермический фургон\nЦена указана за ГОТОВЫЙ автомобиль с надстройкой: новый в наличии, с ПТС!\n" +
                "Антикоррозионная обработка в подарок!\nНадстройка: Фургон изотермический С/П 50 мм (возможно установка " +
                "ХОУ)., габаритные размеры 4,4х2,2х2,2 м., плакированный металл, пол - транспортная ламинированная " +
                "сетчатая фанера, фурнитура оцинкованная. До 8 европаллет !!!\nКомплектация: система курсовой " +
                "устойчивости (VDC), антиблокировочная система (ABS), электростеклоподъемники, обогрев зеркал заднего " +
                "вида, передние регулируемые противотуманные фары с линзой, магнитола AM/FM, AUX\nСкрыть подробности"));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sellerComment().showAllButton("Скрыть подробности"));
        basePageSteps.onCardPage().sellerComment().waitUntil(hasText("Комментарий продавца\nHYUNDAI HD78 (Евро-5). " +
                "Изотермический фургон\nЦена указана за ГОТОВЫЙ автомобиль с надстройкой: новый в наличии, с ПТС!\n" +
                "Антикоррозионная обработка в подарок!\nНадстройка: Фургон изотермический С/П 50 мм (возможно установка " +
                "ХОУ)., габаритные размеры 4,4х2,2х2,2 м., плакированный металл, пол - транспортная ламинированная " +
                "сетчатая фанера, фурнитура оцинкованная. До 8 европаллет !!!\nКомплектация: система курсовой " +
                "устойчивости (VDC), антиблокировочная система (ABS), электростеклоподъемники, обогрев зеркал заднего " +
                "вида, передние регулируемые противотуманные фары с линзой, магнитола AM/FM, AUX\nПоказать полностью"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(NATAGOLOVKINA)
    public void shouldClickOption() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().complectation().option("Обзор"));
        basePageSteps.onCardPage().complectation().waitUntil(hasText("Комплектация\nБезопасность\n3\nУправление\n1\n" +
                "Комфорт\n3\nОбзор\n3\nКорректор фар\nОбогрев зеркал\nПротивотуманные фары\nМультимедиа\n1"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Такси»")
    public void shouldClickTaxiUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().contacts().buttonContains("Яндекс.Такси").should(isDisplayed()));
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(containsString("redirect.appmetrica.yandex.com/route?end-lat"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (подменники)")
    public void shouldSeeRedirectPhones() {
        mockRule.with("desktop/OfferCarsPhones").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().floatingContacts().button("Позвонить +7 916 039-84-27");
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (настоящие)")
    public void shouldSeeRealPhones() {
        mockRule.with("desktop/OfferCarsPhonesRedirectFalse").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().floatingContacts().button("Позвонить +7 916 039-84-30");
    }
}