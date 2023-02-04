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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное б/у объявление дилера под незарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActiveSaleUsedDealerUnregTrucksTest {

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
        mockRule.newMock().with("desktop/OfferTrucksUsedDealer",
                "desktop/ReferenceCatalogTrucksDictionariesV1Equipment").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение цены")
    public void shouldSeePrice() {
        basePageSteps.onCardPage().price().should(hasText("2 700 000 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа с ценами")
    public void shouldSeePricePopup() {
        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Цена\n2 700 000 ₽\n40 354 $\n · \n36 884 €\nПозвонить"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение галереи")
    public void shouldSeeGallery() {
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(isDisplayed())
                .should(hasAttribute("src", format("https://images.mds-proxy.%s/get-autoru-vos/1698998/" +
                        "68705c854655256b48b9ec60a9fe1f6f/456x342", urlSteps.getConfig().getBaseDomain())));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение даты")
    public void shouldSeeDate() {
        basePageSteps.onCardPage().dateAndStats().date().should(hasText("3 сентября 2019, Люберцы"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение счётчика просмотров")
    public void shouldSeeCounter() {
        basePageSteps.onCardPage().views().should(hasText("21 (7 сегодня)"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    @Owner(DSVICHIHIN)
    public void shouldSeeCardFeatures() {
        basePageSteps.onCardPage().features().should(hasText("Характеристики\nгод выпуска\n2008\nПробег\n842 000 км\n" +
                "Кузов\nбортовой грузовик\nЦвет\nжёлтый\nДвигатель\n12.0 л / 462 л.с. / Дизель\nГ/подъёмность\n16.3 т\n" +
                "Коробка\nмеханическая\nТип кабины\n2-х местная с 2 спальными\nПодвеска кабины\nПневматическая\n" +
                "Подвеска шасси\nПневмо-пневмо\nКолёсная формула\n6x2\nКласс выхлопа EURO\n5\nРуль\nЛевый\n" +
                "Состояние\nНе требует ремонта\nВладельцы\n3 или более\nПТС\nОригинал\nТаможня\nРастаможен\n" +
                "VIN\nXLRAS47M*0E*****8"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение комментария продавца")
    public void shouldSeeSellerComment() {
        basePageSteps.onCardPage().sellerComment().should(hasText("Комментарий продавца\nГрузовой DAF 105 460. " +
                "(БДФ) бортовой. Год выпуска 2008. В РФ с 2011 года. Производитель Нидерланды. Пробег 842000 км " +
                "Двигатель 460 л.с. КПП-мех ZF16. Евро класс 5. Комплектация: электро-подогрев и регулировка зеркал, " +
                "электро-стекло подъемники, мультируль, Круиз контроль, кондиционер, ретарда, люк, тахограф, магнитола, " +
                "рация, топливный бак 800 литров, два инструментальных ящика, запаска.\nХарактеристики.\n" +
                "Год выпуска: 2008\nПробег: 842 000 км\nЕвро класс: пятый\nМощность двигателя: 462 л.с.\n" +
                "Коробка передач/ Модель: Мех. ZF16\nТопливная аппаратура: насос форсунка\nРММ: 26 000 кг\n" +
                "МБН: 9700 кг\nТип тормозов (передние): дисковые\nТип тормозов (задние): дисковые\n" +
                "Тип подвески (передняя): пневматическая\nТип подвески (задняя): пневматическая\n" +
                "Резина передняя (размер/марка/остаток): 315/70 R22.5 \\ RECIONAL / - 50% остатка\n" +
                "Резина средняя (размер/марка/остаток): 315/70\\ R22.5\\ POWERTRAC / - 90% остатка\n" +
                "Резина задняя (размер/марка/остаток): 315/70\\ R22.5 \\ GOODYER / - 50% остатка\n" +
                "Тип кузова: бортовой\nРазмеры кузова:\n- Длина 7,7 м.\n- Высота борта 570 мм.\n- Ширина 2.5 м.\n" +
                "Форма оплаты: без НДС.\n\nПо вопросам приобретения звоните в отдел продаж компании «ТРАК-ПЛАТФОРМА».\n" +
                "Предлагаем срочный выкуп Вашего авто, покупку в Trade-in, быстрая реализация Вашего транспорта!\n" +
                "Кредитование/Лизинг! Любая форма оплаты!\nЗВОНИТЕ!!!\nПоказать полностью"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(DSVICHIHIN)
    public void shouldSeeComplectation() {
        basePageSteps.onCardPage().complectation().should(hasText("Комплектация\nБезопасность\n1\nУправление\n1\n" +
                "Защита от угона\n2\nЭкстерьер\n1\nКомфорт\n3\nОбзор\n3"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок «Контакты»")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().should(hasText("ТРАК ПЛАТФОРМА\nАвтосалон\nЛюберцы, " +
                "Транспортная улица, 16. На карте\nДоехать с Яндекс.Такси\nЗаказать обратный звонок"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (подменники)")
    public void shouldSeeRedirectPhones() {
        mockRule.with("desktop/OfferTrucksPhones").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-27\n" +
                "с 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (настоящие)")
    public void shouldSeeRealPhones() {
        mockRule.with("desktop/OfferTrucksPhonesRedirectFalse").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed())
                .should(hasText("Телефон\n+7 916 039-50-85\n+7 916 039-50-90"));
    }
}
