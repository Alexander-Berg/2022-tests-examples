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

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное б/у объявление")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class AmpSaleCarUsedTest {

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/CarfaxOfferCarsRawNotPaid").post();

        urlSteps.testing().path(AMP).path(CARS).path(USED).path(SALE).path("/land_rover/discovery/")
                .path(SALE_ID).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Все характеристики»")
    public void shouldClickAllFeaturesUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().allFeaturesUrl().should(isDisplayed())
                .should(hasText("Все характеристики")));
        urlSteps.testing().path(CATALOG).path(CARS).path("/land_rover/discovery/2307388/2307389/specifications/2307389__2307392/")
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Показать полный VIN и госномер»")
    public void shouldClickVinReportAncor() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().button("Показать полный VIN и госномер"));
        assertThat("Не произошел скролл к отчёту", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комментария полностью / частично")
    @Owner(NATAGOLOVKINA)
    public void shouldExpandAndCollapseSellerComment() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sellerComment().showAllButton("Показать полностью"));
        basePageSteps.onCardPage().sellerComment().waitUntil(hasText("Комментарий продавца\nАвто в отличном состоянии" +
                "Доводчики, камера 360Панорама, вентиляция\n<script>alert(5)</script> Автомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nСкрыть подробности"));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sellerComment().showAllButton("Скрыть подробности"));
        basePageSteps.onCardPage().sellerComment().waitUntil(hasText("Комментарий продавца\nАвто в отличном состоянии" +
                "Доводчики, камера 360Панорама, вентиляция\n<script>alert(5)</script> Автомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\nАвтомобиль\n" +
                "Автомобиль\nАвтомобиль\nПоказать полностью"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комплектации")
    @Owner(NATAGOLOVKINA)
    public void shouldClickOption() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().complectation().option("Элементы экстерьера"));
        basePageSteps.onCardPage().complectation().waitUntil(hasText("Комплектация\nОбзор\n9\nЭлементы экстерьера\n2\n" +
                "Легкосплавные диски\nДиски 20\nЗащита от угона\n4\nМультимедиа\n7\nСалон\n12\nКомфорт\n13\n" +
                "Безопасность\n7\nПрочее\n3"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Бесплатный отчёт»")
    public void shouldClickVinFreeReport() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport()
                .buttonContains("Смотреть бесплатный отчёт"));
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam("r", encode(format("%s", urlSteps.testing()
                .path(CARS).path(USED).path(SALE).path("/land_rover/discovery/").path(SALE_ID))));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Купить в приложении»")
    public void shouldClickBuyVinInAppButton() {
        basePageSteps.onCardPage().vinReport().button("Купить в приложении за 499\u00a0₽")
                .should(hasAttribute("href", startsWith("https://sb76.adj.st/history/")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по ссылке «никогда не отправляйте предоплату»")
    public void shouldClickFraudUrl() {
        basePageSteps.onCardPage().button("никогда не отправляйте предоплату")
                .should(hasAttribute("href",
                        "https://mag.auto.ru/article/how-to-call/?from=card_predoplata"));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().button("никогда не отправляйте предоплату"));
        urlSteps.shouldSeeCertainNumberOfTabs(2);
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

    @Test
    @DisplayName("Клик по кнопке «Написать»")
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    public void shouldClickSendMessageButton() {
        basePageSteps.onCardPage().floatingContacts().sendMessageButton().should(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/")
                .path(SALE_ID).path("/")
                .addParam("openChat", "true")
                .ignoreParam("_gl")
                .fragment("open-chat/" + SALE_ID + "/")
                .shouldNotSeeDiff();
    }
}
