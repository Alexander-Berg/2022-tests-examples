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
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное б/у объявление")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class AmpSaleTruckUsedTest {

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
                "desktop/OfferTrucksUsedUser",
                "desktop/ReferenceCatalogTrucksDictionariesV1Equipment").post();

        urlSteps.testing().path(AMP).path(TRUCK).path(USED).path(SALE).path("/zil/5301/")
                .path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение комментария полностью / частично")
    @Owner(NATAGOLOVKINA)
    public void shouldExpandAndCollapseSellerComment() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sellerComment().showAllButton("Показать полностью"));
        basePageSteps.onCardPage().sellerComment().waitUntil(hasText("Комментарий продавца\nКомментарий продавца\n" +
                "<script>alert(5)</script> Грузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\n" +
                "Грузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\nГрузовик\n" +
                "Грузовик\nГрузовик\nГрузовик\nГрузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик " +
                "Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик Грузовик\nСкрыть подробности"));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sellerComment().showAllButton("Скрыть подробности"));
        basePageSteps.onCardPage().sellerComment().waitUntil(hasText("Комментарий продавца\nКомментарий продавца\n" +
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
    @Owner(NATAGOLOVKINA)
    public void shouldClickOption() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().complectation().option("Безопасность"));
        basePageSteps.onCardPage().complectation()
                .waitUntil(hasText("Комплектация\nБезопасность\n1\nАнтиблокировочная система (ABS)\nУправление\n1\n" +
                        "Защита от угона\n2\nЭкстерьер\n1\nКомфорт\n3\nОбзор\n3"));
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
        mockRule.with("desktop/OfferTrucksPhones").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().floatingContacts().button("Позвонить +7 916 039-84-27");
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов (настоящие)")
    public void shouldSeeRealPhones() {
        mockRule.with("desktop/OfferTrucksPhonesRedirectFalse").update();

        basePageSteps.onCardPage().floatingContacts().callButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().floatingContacts().button("Позвонить +7 916 039-84-30");
    }

    @Test
    @DisplayName("Клик по кнопке «Написать»")
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    public void shouldClickSendMessageButton() {
        basePageSteps.onCardPage().floatingContacts().sendMessageButton().should(isDisplayed()).click();
        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path("/zil/5301/")
                .path(SALE_ID).path("/")
                .addParam("openChat", "true")
                .ignoreParam("_gl")
                .fragment("open-chat/" + SALE_ID + "/")
                .shouldNotSeeDiff();
    }
}
