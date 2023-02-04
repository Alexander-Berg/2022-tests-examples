package ru.auto.tests.desktop.auction;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.ALEXANDERREX;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.C2B_AUCTION;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Форма добавления авто в аукцион")
@Epic(AutoruFeatures.AUCTION)
@Feature(AutoruFeatures.AUCTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AuctionFormTest {

    static private final String MILEAGE_MORE_THAN_AUCTION_VALIDATION = "300000";
    static private final String MILEAGE_FOR_AUCTION = "30000";

    private String mileage;

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

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/UserModerator"),
                stub("desktop/SearchCarsBreadcrumbsAudi"),
                stub("desktop/auction/ReferenceCatalogCarsSuggest"),
                stub("desktop/auction/SessionAuthModeratorAuction"),
                stub("desktop/auction/SearchCarsBreadcrumbsAuction"),
                stub("desktop/auction/AuctionApplicationCreateByAm"),
                stub("desktop/auction/AuctionApplicationCreateByAmSkipValidations"),
                stub("desktop/auction/AuctionApplicationCreateByAmValidation"),
                stub("desktop/ReferenceCatalogCarsSuggestAudiA3"),
                stub("desktop/auction/ReferenceCatalogCarsSuggestAuctionAudiA32018"),
                stub("desktop/auction/ReferenceCatalogCarsSuggestAuctionAudiA320785010"),
                stub("desktop/auction/ReferenceCatalogCarsSuggestAuctionAudiA3Engine"),
                stub("desktop/auction/ReferenceCatalogCarsSuggestAuctionAudiA3Drive"),
                stub("desktop/auction/ReferenceCatalogCarsSuggestAuctionAudiA3Transmission"),
                stub("desktop/auction/ReferenceCatalogCarsSuggestAuctionAudiA3Modification")
        ).create();

        urlSteps.testing().path(C2B_AUCTION).path(ADD).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEXANDERREX)
    @DisplayName("Добавление авто в аукцион с подходящими характеристиками")
    public void shouldSeeSubmitAuctionForm() {
        mileage = MILEAGE_FOR_AUCTION;
        basePageSteps.onAuctionPage().button("Заполнить форму").click();
        fillAuctionAmForm();

        basePageSteps.onAuctionPage().auctionCheckboxPreOffers().click();
        basePageSteps.onAuctionPage().button("Создать заявку").click();

        basePageSteps.onAuctionPage().notificationBlock("Заявка с id 1234 успешно создана").should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEXANDERREX)
    @DisplayName("Добавление авто в аукцион без ограничений")
    public void shouldSeeSubmitAuctionFormWithNoLimits() {
        mileage = MILEAGE_MORE_THAN_AUCTION_VALIDATION;
        basePageSteps.onAuctionPage().button("Заполнить форму").click();
        fillAuctionAmForm();

        basePageSteps.onAuctionPage().auctionCheckboxPreOffers().click();
        basePageSteps.onAuctionPage().auctionCheckboxNoLimits().click();
        basePageSteps.onAuctionPage().button("Создать заявку").click();

        basePageSteps.onAuctionPage().notificationBlock("Заявка с id 1234 успешно создана").should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEXANDERREX)
    @DisplayName("Проверка характеристик авто при добавлении в аукцион")
    public void shouldSeeValidationErrorOnAuctionForm() {
        mileage = MILEAGE_MORE_THAN_AUCTION_VALIDATION;
        basePageSteps.onAuctionPage().button("Заполнить форму").click();
        fillAuctionAmForm();

        basePageSteps.onAuctionPage().auctionCheckboxPreOffers().click();
        basePageSteps.onAuctionPage().button("Создать заявку").click();

        basePageSteps.onAuctionPage().errorNotificationBlock().should(hasText("Не удалось создать заявку из-за следующих ошибок валидации:\n" +
                "Поле: mileage\nСтатус: INVALID\nПравило: <= 200000\nТекущее значение: Some(300000)\nПоле: vin\nСтатус: INVALID\n" +
                "Правило: should be unique across active applications\nТекущее значение: WAUKJ"));
    }

    @Step("Заполнение формы аукциона")
    private void fillAuctionAmForm() {
        basePageSteps.onAuctionPage().input("Имя продавца", "Test Test");
        basePageSteps.onAuctionPage().input("Номер телефона", "9771234567");
        basePageSteps.onAuctionPage().input("Госномер", "У555РА55");
        basePageSteps.onAuctionPage().input("VIN", "WAUKJBFM0AA121212");
        basePageSteps.onAuctionPage().markBlock().radioButton("Audi").click();
        basePageSteps.onAuctionPage().modelBlock().radioButton("A3").click();
        basePageSteps.onAuctionPage().radioButton("2018").click();
        basePageSteps.onAuctionPage().radioButton("Седан").waitUntil(isDisplayed()).click();
        basePageSteps.onAuctionPage().radioButton("Бензин").waitUntil(isDisplayed()).click();
        basePageSteps.onAuctionPage().radioButton("Передний").waitUntil(isDisplayed()).click();
        basePageSteps.onAuctionPage().radioButton("Робот").waitUntil(isDisplayed()).click();
        basePageSteps.onAuctionPage().radioButton("1.0 AMT (115\u00a0л.с.) 2016 - 2020").waitUntil(isDisplayed()).click();
        basePageSteps.onAuctionPage().radioButton("Слева").waitUntil(isDisplayed()).click();
        basePageSteps.onAuctionPage().color("FAFBFB").click();
        basePageSteps.onAuctionPage().input("Пробег, км", mileage);
        basePageSteps.onAuctionPage().radioButton("Оригинал").click();
        basePageSteps.onAuctionPage().radioButton("1").click();
        basePageSteps.onAuctionPage().input("Желаемая стоимость продажи", "1500000");
        basePageSteps.onAuctionPage().input("Место осмотра", "Москва");
        basePageSteps.onAuctionPage().input("Даты осмотра (DD-MM-YYYY,DD-MM-YYYY)", "01-06-2022,01-07-2022");
        basePageSteps.onAuctionPage().input("Время осмотра", "13:00-18:00");
        basePageSteps.onAuctionPage().input("ID запроса в AMOCRM", "1234567890");
    }

}
