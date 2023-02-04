package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.test.api.realty.offer.create.userid.Price;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL_WITHOUT_PHOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.page.ManagementNewPage.REDACT;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Редактирование оффера со страницы личного кабинета")
@Link("https://st.yandex-team.ru/VERTISTEST-1490")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class EditOfferTest {

    private static final String RUB_SYMBOL = "₽";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void openManagementPage() {
        apiSteps.createVos2Account(account, OWNER);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим урл редактирования оффера")
    public void shouldSeeEditOfferUrl() {
        String offerId = offerBuildingSteps.addNewOffer(account).withInactive().create().getId();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().link(REDACT).click();
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(offerId).path("/").ignoreParam("from")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «добавить фото» для оффера без фото")
    public void shouldSeeAddPhotoMessage() {
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL_WITHOUT_PHOTO).create();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().addPhotoButton()
                .should(hasText(containsString("Добавить фото")));
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().link(REDACT).click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем кнопку добавления фото, должны перейти в форму редактирования")
    public void shouldSeePassToEditOfferForm() {
        String offerId = offerBuildingSteps.addNewOffer(account).create().getId();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().addPhotoButton().waitUntil(isDisplayed())
                .click();
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(offerId).path("/")
                .queryParam("scroll", "photo").shouldNotDiffWithWebDriverUrl();
    }

    @Ignore("как банить офферы?")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Жмем кнопку добавления фото. Забаненный оффер должны перейти в форму редактирования")
    public void shouldSeePassToEditBannedOfferForm() {
        String offerId = offerBuildingSteps.addNewOffer(account).withBanned().create().getId();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().addPhotoButton().waitUntil(isDisplayed())
                .click();
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(offerId).path("/").queryParam("from", "new-lk")
                .queryParam("scroll", "photo").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Нельзя менять цену оффера на «0»")
    public void shouldNotChangeOfferToZeroPrice() {
        long initialPrice = 100L;
        Price price = getDefaultOffer(APARTMENT_SELL).getPrice().withValue(initialPrice);
        offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_SELL).withPrice(price)).
                withInactive().create().getId();

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .waitUntil(hasValue(valueOf(initialPrice)));
        managementSteps.clearInputByBackSpace(() ->
                managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input());
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().sendKeys("0");
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().waitUntil(hasValue("0"));
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().sendKeys(Keys.ENTER);
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .should(hasValue(valueOf(initialPrice)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем цену на оффер")
    public void shouldChangeOfferPrice() {
        long initialPrice = 550L;
        long secondPrice = 11000L;
        String initialPriceByMeter = "10 ₽ за м²";
        String secondPriceByMeter = "200 ₽ за м²";
        Price price = getDefaultOffer(APARTMENT_SELL).getPrice().withValue(initialPrice);
        offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_SELL).withPrice(price)).
                withInactive().create().getId();

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().currency().should(hasText(RUB_SYMBOL));
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().byMeter()
                .waitUntil(hasText(initialPriceByMeter));
        managementSteps.clearInputByBackSpace(() ->
                managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input());
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .sendKeys(valueOf(secondPrice) + Keys.ENTER);
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().byMeter()
                .waitUntil(hasText(secondPriceByMeter));
        apiSteps.waitOfferPrice(account.getId(), secondPrice);
    }
}
