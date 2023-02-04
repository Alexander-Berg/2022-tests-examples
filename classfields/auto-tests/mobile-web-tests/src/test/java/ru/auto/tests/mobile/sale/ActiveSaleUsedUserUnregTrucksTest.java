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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное б/у объявление частника под незарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class ActiveSaleUsedUserUnregTrucksTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferTrucksUsedUser"),
                stub("desktop/ReferenceCatalogTrucksDictionariesV1Equipment")
        ).create();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение цены")
    public void shouldSeePrice() {
        basePageSteps.onCardPage().price().should(hasText("250 000 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа с ценами")
    public void shouldSeePricePopup() {
        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Цена\n250 000 ₽\n3 767 $\n · \n" +
                "3 309 €\nСледить за изменением цены"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение галереи")
    public void shouldSeeGallery() {
        basePageSteps.onCardPage().gallery().currentImage().waitUntil(isDisplayed())
                .should(hasAttribute("src", format("https://images.mds-proxy.%s/get-autoru-vos/2068493/" +
                        "2a6d0468e113dd54842e7a01e5d571e7/456x342", urlSteps.getConfig().getBaseDomain())));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение даты")
    public void shouldSeeDate() {
        basePageSteps.onCardPage().dateAndStats().date().should(hasText("20 января 2019, Москва"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение счётчика просмотров")
    public void shouldSeeCounter() {
        basePageSteps.onCardPage().views().should(hasText("147 (18 сегодня)"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    @Owner(DSVICHIHIN)
    public void shouldSeeCardFeatures() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("Характеристики\nгод выпуска\n2000\nПробег\n54 000 км\n" +
                "Кузов\nфургон\nЦвет\nбелый\nДвигатель\n4.5 л\nРуль\nЛевый\nСостояние\nНе требует ремонта\nВладельцы\n" +
                "3 или более\nПТС\nОригинал\nВладение\n\\d+ (лет|год|года)( и \\d+ (месяц|месяца|месяцев))?\n" +
                "Таможня\nРастаможен\nVIN\nX5S47410\\*Y0\\*\\*\\*\\*12")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение комментария продавца")
    public void shouldSeeSellerComment() {
        basePageSteps.onCardPage().sellerComment().should(hasText("Комментарий продавца\nКомментарий продавца\n" +
                "<script>alert(5)</script> Грузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\n" +
                "Грузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\n" +
                "Грузовик\nГрузовик\nГрузовик\nГрузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик\nПоказать полностью"));
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
        basePageSteps.onCardPage().contacts().should(hasText("Юрий\nЧастное лицо\nМосква, район Южное Бутово. " +
                "На карте\n Улица ГорчаковаБульвар адмирала Ушакова\nДоехать с Яндекс.Такси"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Доехать с Яндекс.Такси»")
    public void shouldClickTaxiButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().contacts()
                .button("Доехать\u00a0с\u00a0Яндекс.Такси"));
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(containsString("redirect.appmetrica.yandex.com/route?end-lat"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (подменники)")
    public void shouldSeeRedirectPhones() {
        mockRule.setStubs(stub("desktop/OfferTrucksPhones")).update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-27\n" +
                "с 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (настоящие)")
    public void shouldSeeRealPhones() {
        mockRule.setStubs(stub("desktop/OfferTrucksPhonesRedirectFalse")).update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-50-85\n" +
                "+7 916 039-50-90"));
    }

    @Test
    @DisplayName("Клик по кнопке «Написать»")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickSendMessageButton() {
        String saleUrl = urlSteps.getCurrentUrl();

        basePageSteps.onCardPage().floatingContacts().sendMessageButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().authPopup().should(isDisplayed());
        basePageSteps.onCardPage().authPopup().iframe()
                .should(hasAttribute("src", containsString(
                        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                                .addParam("r", encode(saleUrl))
                                .addParam("inModal", "true")
                                .addParam("autoLogin", "true")
                                .addParam("welcomeTitle", "")
                                .toString()
                )));
    }
}
