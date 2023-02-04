package ru.auto.tests.cabinet.backonsale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.BACK_ON_SALE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExample;
import static ru.auto.tests.desktop.mock.MockComeback.comebackRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.COMEBACK;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_BACK_ON_SALE_PLACEHOLDER;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кабинет дилера. Снова в продаже. Объявление в листинге")
@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.BACK_ON_SALE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class ListingItemTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/OfferCarsPhones"),
                stub().withPostMatches(COMEBACK)
                        .withRequestBody(getJsonObject(comebackRequest()))
                        .withResponseBody(comebackExample().getBody())
        ).create();

        cookieSteps.setCookieForBaseDomain(IS_SHOWING_BACK_ON_SALE_PLACEHOLDER, "1");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение активного объявления")
    public void shouldSeeActiveSale() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).should(hasText("Audi A5 I (8T), 2011\n" +
                "125 000 км 2.0 AMT (211 л.с.) 4WD\nкупе полный\nWAUZZZ8T8BA077208 7 записей в отчёте\nЧастник\n" +
                "Москва, 30 марта 2019\n750 000 \u20BD\nПродавался с пробегом\n3 года с момента продажи\n" +
                "Вы предыдущий продавец\n+7 ●●● ●●● ●● ●●"));
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).soldBadge().should(not(isDisplayed()));
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).hover();
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).button("3\u00a0года с момента продажи").hover();
        basePageSteps.onCabinetOnSaleAgainPage().activePopup().should(hasText("Снят с продажи 14.09.2019"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение неактивного объявления")
    public void shouldSeeInactiveSale() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(1).should(hasText("Продан\n" +
                "Mazda 3 I (BK) Рестайлинг, 2007\n179 000 км 1.6 AT (105 л.с.)\nседан передний\n" +
                "JMZBK12Z581696377 —\nЧастник\nМосква, 19 октября 2019\nПродавался с пробегом\n" +
                "4 года с момента продажи\nВы предыдущий продавец"));
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(1).soldBadge().should(hasText("Продан"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Обслуживался на сервисе")
    public void shouldSeeMaintenanceSale() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(2).should(hasText("Audi A5 I (8T), 2011\n" +
                "125 000 км 2.0 AMT (211 л.с.) 4WD\nкупе полный\nWAUZZZ8T8BA077208 7 записей в отчёте\n" +
                "Частник\nМосква, 30 марта 2019\n750 000 ₽\nОбслуживался на сервисе\nОбслуживался 3 года назад\n" +
                "+7 ●●● ●●● ●● ●●"));
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(2).hover();
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(2).button("Обслуживался 3\u00a0года назад").hover();
        basePageSteps.onCabinetOnSaleAgainPage().activePopup().waitUntil(hasText("Снят с продажи 14.09.2019\n" +
                "Последний раз обслуживался на сервисе 30.03.2019"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Оценивался")
    public void shouldSeeEstimateSale() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(3).info().should(hasText("Audi A5 I (8T), 2011\n" +
                "125 000 км 2.0 AMT (211 л.с.) 4WD\nкупе полный\nWAUZZZ8T8BA077208 7 записей в отчёте\nЧастник\n" +
                "Москва, 30 марта 2019"));
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(3).price().should(hasText("750 000 ₽"));
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(3).additionalInfo()
                .should(hasText(matchesPattern("Оценивался\nОценивался \\d+ (год|года|лет) назад")));
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(3).hover();
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(3).buttonContains("Оценивался").hover();
        basePageSteps.onCabinetOnSaleAgainPage().activePopup().waitUntil(hasText("Оценка стоимости была произведена " +
                "13.04.2019"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Продавался не на Авто.ру")
    public void shouldSeeExternalSale() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(4).should(hasText("Продан\n" +
                "Volkswagen Jetta VI Рестайлинг, 2017\n66 300 км 1.6 MT (110 л.с.)\nседан передний\nXW8ZZZ16ZHN900632 —\n" +
                "Частник\nСанкт-Петербург, 23 мая 2020\nПродавался не на Авто.ру\n3 года с момента продажи\nВы предыдущий " +
                "продавец"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Продавался с пробегом")
    public void shouldSeeUsedSale() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(5).should(hasText("Продан\nBMW 3 серия 320i " +
                "V (E90/E91/E92/E93) Рестайлинг, 2012\n212 000 км 2.0 AT (156 л.с.)\nседан задний\n" +
                "X4XPG98470E945572 —\nЧастник\nСанкт-Петербург, 22 мая 2020\nПродавался с пробегом\n" +
                "3 года с момента продажи\nВы предыдущий продавец"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Продавался новый")
    public void shouldSeeNewSale() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(6).should(hasText("Продан\nBMW 3 серия " +
                "320i V (E90/E91/E92/E93) Рестайлинг, 2012\n212 000 км 2.0 AT (156 л.с.)\nседан задний\n" +
                "X4XPG98470E945572 —\nЧастник\nСанкт-Петербург, 22 мая 2020\nПродавался новый\n" +
                "3 года с момента продажи\nВы предыдущий продавец"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по фото объявления")
    public void shouldClickSaleImage() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).image().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/audi/a5/1076842087-f1e84/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по ссылке «Продавался ...»")
    public void shouldClickSaleTitle() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).button("Продавался с пробегом")
                .waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/audi/a5/1076842087-f1e84/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Переход по ссылке просмотра истории автомобиля")
    public void shouldClickHistoryUrl() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).button("7 записей в отчёте")
                .waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(HISTORY).path("/WAUZZZ8T8BA077208/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Просмотр телефона")
    public void shouldClickPhoneButton() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).phone().click();
        basePageSteps.onCabinetOnSaleAgainPage().listing().getItem(0).phone().should(hasText("+7 921 001-35-93"));
    }

}
