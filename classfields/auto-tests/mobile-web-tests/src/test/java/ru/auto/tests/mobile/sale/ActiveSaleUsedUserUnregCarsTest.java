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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное б/у объявление частника под незарегом")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActiveSaleUsedUserUnregCarsTest {

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
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/ReferenceCatalogCarsDictionariesV1Equipment")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.setWindowMaxHeight();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение цены")
    public void shouldSeePrice() {
        basePageSteps.onCardPage().price().should(hasText("700 000 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа с ценами")
    public void shouldSeePricePopup() {
        basePageSteps.onCardPage().price().button().click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Цена\n700 000 ₽\n10 418 $\n · \n" +
                "9 083 €\nот 20 734 ₽ / мес.\n20 августа 2018\nНачальная цена\n850 000 ₽\n28 августа 2018\n" +
                "- 20 000 ₽\n830 000 ₽\n13 сентября 2018\n- 30 000 ₽\n800 000 ₽\n4 октября 2018\n- 50 000 ₽\n" +
                "750 000 ₽\n17 октября 2018\n- 70 000 ₽\n680 000 ₽\n15 ноября 2018\n+ 20 000 ₽\n700 000 ₽\n" +
                "О скидках и акциях узнавайте по телефону\nСледить за изменением цены"));
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
        basePageSteps.onCardPage().dateAndStats().date().should(hasText("20 августа 2018, Москва"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение счётчика просмотров")
    public void shouldSeeCounter() {
        basePageSteps.onCardPage().views().should(hasText("1296 (35 сегодня)"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение характеристик")
    @Owner(DSVICHIHIN)
    public void shouldSeeCardFeatures() {
        basePageSteps.onCardPage().features().should(hasText(matchesPattern("Характеристики\nгод выпуска\n" +
                "2008\nПробег\n210 000 км\nКузов\nвнедорожник 5 дв.\nЦвет\nсеребристый\nДвигатель\n" +
                "2.7 л / 190 л.с. / Дизель\nКомплектация\n57 опций\nКоробка\nавтоматическая\nПривод\nполный\nРуль" +
                "\nЛевый\nСостояние\nНе требует ремонта\nВладельцы\n3 или более\nПТС\nОригинал\nВладение\n" +
                "\\d+ (год|года|лет)( и \\d+ (месяц|месяца|месяцев))?\nТаможня\nРастаможен\nГарантия\nДо января 2030\n" +
                "VIN\nSALLAAA148A485103\nГосномер\nА900ВН777\nВсе характеристики")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение комментария продавца")
    public void shouldSeeSellerComment() {
        basePageSteps.onCardPage().sellerComment().should(hasText("Комментарий продавца\nАвто в отличном состоянии" +
                "Доводчики, камера 360Панорама, вентиляция\n<script>alert(5)</script> Автомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nПоказать полностью"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(DSVICHIHIN)
    public void shouldSeeComplectation() {
        basePageSteps.onCardPage().complectation().should(hasText("Комплектация\nОбзор\n9\nЭлементы экстерьера\n2\n" +
                "Защита от угона\n4\nМультимедиа\n7\nСалон\n12\nКомфорт\n13\nБезопасность\n7\nПрочее\n3"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок «Контакты»")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().should(hasText("Федор\nЧастное лицо\nМосква, метро Марьино. " +
                "На карте\nДоехать с Яндекс.Такси"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (подменники)")
    public void shouldSeeRedirectPhones() {
        mockRule.setStubs(stub("desktop/OfferCarsPhones")).update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-27\n" +
                "с 10:00 до 23:00\n+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (настоящие)")
    public void shouldSeeRealPhones() {
        mockRule.setStubs(stub("desktop/OfferCarsPhonesRedirectFalse")).update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 916 039-84-30\n" +
                "с 09:00 до 21:00\n+7 916 039-84-31\nс 12:00 до 18:00"));
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
