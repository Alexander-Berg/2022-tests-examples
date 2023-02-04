package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное б/у объявление дилера под незарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActiveSaleUsedDealerUnregCarsTest {

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
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedDealer",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.setWindowMaxHeight();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение цены")
    public void shouldSeePrice() {
        basePageSteps.onCardPage().price().should(hasText("375 000 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа с ценами")
    public void shouldSeePricePopup() {
        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Цена\n375 000 ₽\n5 676 $\n · \n" +
                "5 007 €\nПозвонить"));
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
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение даты")
    public void shouldSeeDate() {
        basePageSteps.onCardPage().dateAndStats().date().should(hasText("8 февраля 2019, Москва"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение счетчика просмотров")
    public void shouldSeeCounter() {
        basePageSteps.onCardPage().views().should(hasText("17 (17 сегодня)"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    @Owner(DSVICHIHIN)
    public void shouldSeeCardFeatures() {
        basePageSteps.onCardPage().features().should(hasText("Характеристики\nгод выпуска\n2010\nПробег\n53 270 км\n" +
                "Кузов\nхэтчбек 5 дв.\nЦвет\nкрасный\nДвигатель\n1.4 л / 86 л.с. / Бензин\nКомплектация\n32 опции\n" +
                "Налог\n17 850 ₽ / год\nКоробка\nмеханическая\nПривод\nпередний\nРуль\nЛевый\nСостояние\n" +
                "Не требует ремонта\nВладельцы\n2 владельца\nПТС\nОригинал\nТаможня\nРастаможен\nОбмен\n" +
                "Рассмотрю варианты\nVIN\nXW8EC25J*BK****93\nВсе характеристики"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение комментария продавца")
    public void shouldSeeSellerComment() {
        basePageSteps.onCardPage().sellerComment().should(hasText("Комментарий продавца\nЛОТ: 01112722\nАвтопрага Юг\n\n" +
                "Цену этого автомобиля можно снизить на 40 000 руб. благодаря программам:\nTrade-in -20 000 руб.\n" +
                "Кредитование -10 000 руб.\nСтрахование КАСКО -10 000 руб.\n\n- Автомобиль продается официальным " +
                "дилером FAVORIT MOTORS.\n- Покупка и обслуживание у официального дилера.\n- Проведена предпродажная " +
                "подготовка,\n- Гарантия юридической чистоты.\n- Круглосуточная техническая поддержка с бесплатным " +
                "эвакуатором.\n\n- Автомобиль можно приобрести за наличный расчет, безналичным платежом, а также мы " +
                "примем вашу машину по рыночной стоимости в рамках программы трейд-ин и при необходимости — выплатим " +
                "за нее кредит.\n\n- Возможна покупка в кредит: специальные предложения по кредитованию " +
                "и страхованию.\n- Оформление кредита по двум документам.\n- КАСКО не обязательно.\n\nПриглашаем " +
                "на бесплатный тест-драйв!\n\nГруппа компаний FAVORIT MOTORS — один из крупнейших российских холдингов, " +
                "официальный дилер известных мировых брендов , которые обслуживаются в технических центрах ГК FAVORIT " +
                "MOTORS в рамках официального сервиса.\n\nМы предлагаем полный комплекс услуг по предпродажной " +
                "подготовке и последующему сервису.\n\nГруппа компаний FAVORIT MOTORS занимает лидирующее положение в " +
                "рейтинге продаж автомобилей с пробегом по Москве и области. Это подтверждают специализированные " +
                "исследования рынка за последние пять лет, указывающие на самый крупный объем реализации.\n\n\n\n" +
                "В комплектации автомобиля\n• Тип обивки салона: Ткань\n• Цвет салона: Темный\n• Тип фар: " +
                "Галогеновые\n• Легкосплавные диски: 15\n• Штатная сигнализация\n• Иммобилайзер\n• " +
                "Бортовой компьютер\n• Обогрев зеркал\n• Усилитель руля\n• Центральный замок\n• Противотуманные фары\n• " +
                "Обогрев форсунок омывателя лобового стекла\n• Обогрев передних сидений\n• Климат: Климат-контроль 1- " +
                "зонный\n• Электропривод зеркал\n• Стеклоподъемники: Электро все\n• Сиденье водителя: " +
                "Ручная регулировка\n• Сиденье пассажира: Ручная регулировка\n• Регулировка руля: В двух плоскостях\n• " +
                "Антиблокировочная система (ABS)\n• Подушки безопасности: Передние и боковые\n• Система крепления " +
                "isofix\n• CD\n• USB\n• Тип открытия задней двери: Подъемная\n• Сервисная книжка\n• Тип передних " +
                "тормозов: Дисковые\n• Тип задних тормозов: Барабанные\nПоказать полностью"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(DSVICHIHIN)
    public void shouldSeeComplectation() {
        basePageSteps.onCardPage().complectation().should(hasText("Комплектация\nОбзор\n3\nЭлементы экстерьера\n2\n" +
                "Защита от угона\n3\nМультимедиа\n2\nСалон\n3\nКомфорт\n9\nБезопасность\n5"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок «Контакты»")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().should(hasText(matchesPattern("Проверенный дилер\n" +
                "FAVORIT MOTORS Юг\nАвтосалон • На Авто.ру \\d+ (лет|год|года)\nМосква, 1й Дорожный проезд,д.4. " +
                "На карте\n ПражскаяЮжная\nПодписаться на объявления\nДоехать с Яндекс.Такси\n189 авто в наличии\n" +
                "Заказать обратный звонок")));
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
}
