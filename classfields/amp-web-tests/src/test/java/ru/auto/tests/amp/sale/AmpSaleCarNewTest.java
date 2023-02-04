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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное новое объявление")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class AmpSaleCarNewTest {

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
                "desktop/OfferCarsNewDealer",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment").post();

        urlSteps.testing().path(AMP).path(CARS).path(NEW).path(SALE).path("/kia/optima/")
                .path(SALE_ID).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Все характеристики»")
    public void shouldClickAllFeaturesUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().allFeaturesUrl().should(isDisplayed())
                .should(hasText("Все характеристики")));
        urlSteps.testing().path(CATALOG).path(CARS).path("/kia/optima/21342050/21342121/specifications/21342121_21342344_21342125/")
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комментария полностью / частично")
    @Owner(NATAGOLOVKINA)
    public void shouldExpandAndCollapseSellerComment() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sellerComment().showAllButton("Показать полностью"));
        basePageSteps.onCardPage().sellerComment().waitUntil(hasText("Комментарий продавца\nПри покупке автомобиля " +
                "семейный тур на море в подарок!\n\nАвтоСпецЦентр Север официальный дилер KIA предоставляет полный " +
                "спектр услуг по продаже и сервисному обслуживанию автомобилей.\n\nВ нашем автосалоне Вы можете " +
                "ознакомиться со всей линейкой модельного ряда KIA.\n\nИ приобрести интересующий автомобиль по " +
                "специальной цене.\n\n\nПредложение обновлено 16 ноября 2019г. в 04 ч 03 мин.\n\nТотальная ликвидация " +
                "склада в АвтоСпецЦентр!\nСкрыть подробности"));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sellerComment().showAllButton("Скрыть подробности"));
        basePageSteps.onCardPage().sellerComment().waitUntil(hasText("Комментарий продавца\nПри покупке автомобиля " +
                "семейный тур на море в подарок!\n\nАвтоСпецЦентр Север официальный дилер KIA предоставляет полный " +
                "спектр услуг по продаже и сервисному обслуживанию автомобилей.\n\nВ нашем автосалоне Вы можете " +
                "ознакомиться со всей линейкой модельного ряда KIA.\n\nИ приобрести интересующий автомобиль по " +
                "специальной цене.\n\n\nПредложение обновлено 16 ноября 2019г. в 04 ч 03 мин.\n\nТотальная ликвидация " +
                "склада в АвтоСпецЦентр!\nПоказать полностью"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(NATAGOLOVKINA)
    public void shouldClickOption() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().complectation().option("Элементы экстерьера"));
        basePageSteps.onCardPage().complectation().waitUntil(hasText("Комплектация Comfort\nОбзор\n6\n" +
                "Элементы экстерьера\n1\nДиски 16\nЗащита от угона\n3\nМультимедиа\n4\nСалон\n8\nКомфорт\n12\n" +
                "Безопасность\n12\nПрочее\n1"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Такси»")
    public void shouldClickTaxiUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().contacts().buttonContains("Яндекс.Такси")
                .should(isDisplayed()));
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(containsString("redirect.appmetrica.yandex.com/route?end-lat"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «N авто в наличии»")
    public void shouldClickInStockButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().button("33 авто в наличии"));
        urlSteps.switchToNextTab();
        urlSteps.testing().path(DILER).path(CARS).path(NEW).path("/avtospeccentr_sever_moskva_kia/")
                .ignoreParam("_gl").shouldNotSeeDiff();
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
