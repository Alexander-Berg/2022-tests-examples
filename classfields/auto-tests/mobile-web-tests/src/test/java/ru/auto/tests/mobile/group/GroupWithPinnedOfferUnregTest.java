package ru.auto.tests.mobile.group;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка c запиненным оффером под незарегом")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GroupWithPinnedOfferUnregTest {

    private static final String PATH = "/kia/optima/21342125/21342344/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsNewDealer",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение цены")
    public void shouldSeePrice() {
        basePageSteps.onCardPage().price().should(hasText("от 1 254 900 ₽\n1 474 900 ₽ без скидок"));
        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Цена\nот 1 254 900 ₽\n" +
                "Цена без скидки\n1 474 900 ₽\n · \n24 016 $\n · \n21 556 €\nСкидки\nМаксимальная\n220 000 ₽\n" +
                "Максимальная скидка, которую может предоставить дилер. Подробности узнавайте по телефону.\nПозвонить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @DisplayName("Отображение галереи")
    public void shouldSeeGallery() {
        basePageSteps.setWindowMaxHeight();

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
        basePageSteps.onCardPage().features().should(hasText("Характеристики\nгод выпуска\n2019\nКузов\nседан\nЦвет\n" +
                "белый\nДвигатель\n2.0 л / 150 л.с. / Бензин\nКомплектация\nComfort\nНалог\n5 250 ₽ / год\nКоробка\n" +
                "автоматическая\nПривод\nпередний\nVIN\nXWE**************\nВсе характеристики"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение комментария продавца")
    public void shouldSeeSellerComment() {
        basePageSteps.onCardPage().sellerComment().should(hasText("Комментарий продавца\n" +
                "При покупке автомобиля семейный тур на море в подарок!\n\n" +
                "АвтоСпецЦентр Север официальный дилер KIA предоставляет полный спектр услуг по продаже и сервисному " +
                "обслуживанию автомобилей.\n\nВ нашем автосалоне Вы можете ознакомиться со всей линейкой модельного " +
                "ряда KIA.\n\nИ приобрести интересующий автомобиль по специальной цене.\n\n\n" +
                "Предложение обновлено 16 ноября 2019г. в 04 ч 03 мин.\n\n" +
                "Тотальная ликвидация склада в АвтоСпецЦентр!\nПоказать полностью"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(DSVICHIHIN)
    public void shouldSeeComplectation() {
        basePageSteps.onCardPage().complectation().should(hasText("Комплектация Comfort\nОбзор\n6\n" +
                "Элементы экстерьера\n1\nЗащита от угона\n3\nМультимедиа\n4\nСалон\n8\nКомфорт\n12\n" +
                "Безопасность\n12\nПрочее\n1"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок «Контакты»")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().should(hasText("АвтоСпецЦентр KIA Север\nОфициальный дилер\n" +
                "Москва, Клязьминская улица, 5. На карте\nПодписаться на объявления\nДоехать с Яндекс.Такси\n" +
                "33 авто в наличии\nЗаказать обратный звонок"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (подменники)")
    public void shouldSeeRedirectPhones() {
        mockRule.with("desktop/OfferCarsPhones").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-27\n" +
                "с 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (настоящие)")
    public void shouldSeeRealPhones() {
        mockRule.with("desktop/OfferCarsPhonesRedirectFalse").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-30\n" +
                "с 09:00 до 21:00\n+7 916 039-84-31\nс 12:00 до 18:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Все характеристики»")
    public void shouldClickAllFeaturesUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().allFeaturesUrl().should(isDisplayed())
                .should(hasText("Все характеристики")));
        urlSteps.testing().path(CATALOG).path(CARS)
                .path("/kia/optima/21342050/21342121/").path(SPECIFICATIONS)
                .path("/21342121_21342344_21342125/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение налога")
    public void shouldSeeTax() {
        basePageSteps.onCardPage().features().feature("Налог").should(hasText("Налог\n5 250 \u20BD / год"));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().features().feature("Налог").tooltip());
        basePageSteps.onCardPage().popup()
                .should(hasText("Транспортный налог\nНалог рассчитан для двигателя мощностью 150 л.с. в Москве по тарифу " +
                        "2019 года. Правительство РФ пока не утвердило налоговую базу 2022 года"));
    }

    @Test
    @DisplayName("Клик по кнопке «N авто в наличии»")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickSendMessageButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().contacts().button("33 авто в наличии"));
        urlSteps.switchToNextTab();
        urlSteps.testing().path(DILER).path(CARS).path(NEW).path("/avtospeccentr_sever_moskva_kia/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить» в полноэкранной галерее")
    public void shouldClickGalleryCallButton() {
        mockRule.with("desktop/OfferCarsPhones").update();

        basePageSteps.onCardPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Телефон\n+7 916 039-84-27\nс 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить» в поп-апе с ценами")
    public void shouldClickPricePopupCallButton() {
        mockRule.with("desktop/OfferCarsPhones").update();

        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().button("Позвонить").click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Телефон\n+7 916 039-84-27\nс 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поп-ап «Официальный дилер»")
    public void shouldSeeOfficialDealerPopup() {
        String url = "https://yandex.ru/legal/autoru_cars_dogovor/";
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().contacts().buttonContains("Официальный дилер"));
        basePageSteps.onCardPage().popup().waitUntil(hasText(format("Официальный дилер\nСтатус «Официальный дилер» " +
                "используется в значении, указанном в Условиях оказания услуг на сервисе Auto.ru, размещенных по ссылке: " +
                "%s", url)));
        basePageSteps.onCardPage().popup().button().hover().click();
        urlSteps.switchToNextTab();
        urlSteps.fromUri(url).shouldNotSeeDiff();
    }
}
