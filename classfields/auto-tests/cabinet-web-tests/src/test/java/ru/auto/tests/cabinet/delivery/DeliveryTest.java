package ru.auto.tests.cabinet.delivery;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Доставка")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class DeliveryTest {

    private static final String CITY_1 = "Химки";
    private static final String SHORT_ADDRESS_1 = "Химки, Овражная 24к12";
    private static final String LONG_ADDRESS_1 =
            "Россия, Московская область, Химки, микрорайон Сходня, Овражная улица, 24к12";
    private static final String SHORT_ADDRESS_2 = "Новосибирск, Чигорина, 12";
    private static final String LONG_ADDRESS_2 =
            "Россия, Новосибирск, Кировский район, Северо-Чемской жилмассив, улица Чигорина, 12";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/OfferCarsProductsPriceRegionNovosibirsk"),
                stub("cabinet/OfferCarsProductsPriceRegionKhimki"),
                stub("cabinet/OfferCarsDeliveryPut"),
                stub("cabinet/OfferCarsDeliveryDelete")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).addParam(STATUS, "active").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа доставки")
    public void shouldSeeDeliveryPopup() {
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().click();

        steps.onCabinetOffersPage().deliveryPopup().should(hasText("Доставка автомобилей в регионы\nОбъявление " +
                "будет отображаться в поисковой выдаче на auto.ru в выбранном городе. За каждую точку доставки " +
                "дополнительно будет списываться сумма за размещение и подключенные услуги согласно тарифам " +
                "данного региона.\nВведите город и адрес"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сохранение адреса")
    public void shouldSaveAddress() {
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().click();
        selectAddress(SHORT_ADDRESS_1, LONG_ADDRESS_1);
        steps.onCabinetOffersPage().deliveryPopup().button("Сохранить").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText("Данные успешно сохранены"));
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().waitUntil(hasText("Химки"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сохранение адреса")
    public void shouldSaveTwoAddress() {
        mockRule.setStubs(stub("cabinet/OfferCarsDeliveryPutTwoAddress")).update();

        steps.onCabinetOffersPage().snippet(0).addCitiesButton().click();
        selectAddress(SHORT_ADDRESS_1, LONG_ADDRESS_1);
        selectAddress(SHORT_ADDRESS_2, LONG_ADDRESS_2);
        steps.onCabinetOffersPage().deliveryPopup().button("Сохранить").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText("Данные успешно сохранены"));
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().waitUntil(hasText("Новосибирск и ещё 1"));
    }

    @Test
    @Category({Regression.class,})
    @Owner(DSVICHIHIN)
    @DisplayName("Список услуг по адресу")
    public void shouldSeeAddressServices() {
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().click();
        selectAddress(SHORT_ADDRESS_1, LONG_ADDRESS_1);
        steps.onCabinetOffersPage().deliveryPopup().getAddress(0).arrow().click();
        steps.onCabinetOffersPage().deliveryPopup().getAddress(0).services()
                .waitUntil(hasText("Размещение объявления\n200 \u20BD / в сутки\nСтикеры\n40 \u20BD / в сутки"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление и отмена удаления адреса")
    public void shouldDeleteAndUndeleteAddress() {
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().click();
        selectAddress(SHORT_ADDRESS_1, LONG_ADDRESS_1);
        steps.onCabinetOffersPage().deliveryPopup().getAddress(0).deleteButton().click();
        steps.onCabinetOffersPage().deliveryPopup().getAddress(0).undoDeleteButton().waitUntil(isDisplayed()).click();
        steps.onCabinetOffersPage().deliveryPopup().getAddress(0).undoDeleteButton().waitUntil(not(isDisplayed()));
        steps.onCabinetOffersPage().deliveryPopup().getAddress(0)
                .waitUntil(hasText(format("%s\n240 \u20BD / в сутки\n%s", CITY_1, LONG_ADDRESS_1)));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление всех адресов")
    public void shouldDeleteAddresses() {
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().click();
        selectAddress(SHORT_ADDRESS_1, LONG_ADDRESS_1);
        steps.onCabinetOffersPage().deliveryPopup().button("Удалить все города").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).waitUntil(hasText("Данные успешно сохранены"));
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ввод неполного адреса")
    public void shouldSelectIncorrectAddress() {
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().click();
        selectAddress("Химки", "Россия, Московская область, Химки");
        steps.onCabinetOffersPage().deliveryPopup().errorMessage().waitUntil(hasText("Пожалуйста, уточните адрес"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке на тарифы")
    public void shouldClickTariffsUrl() {
        steps.onCabinetOffersPage().snippet(0).addCitiesButton().click();
        steps.onCabinetOffersPage().deliveryPopup().url("тарифам").waitUntil(isDisplayed()).click();
        urlSteps.fromUri("https://auto.ru/dealer/#cost").shouldNotSeeDiff();
    }

    @Step("Выбираем адрес {longAddress}")
    private void selectAddress(String shortAddress, String longAddress) {
        steps.onCabinetOffersPage().deliveryPopup().input("Введите город и адрес", shortAddress);
        steps.onCabinetOffersPage().deliveryPopup().geoSuggest().waitUntil(isDisplayed());
        steps.onCabinetOffersPage().deliveryPopup().geoSuggest().region(longAddress).waitUntil(isDisplayed()).click();
    }
}
