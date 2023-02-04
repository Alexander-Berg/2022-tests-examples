package ru.yandex.realty.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.awaitility.core.ThrowingRunnable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.test.api.realty.offer.create.userid.Offer;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.time.LocalDateTime.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.not;
import static ru.auto.test.api.realty.OfferType.COMMERCIAL_RENT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE;
import static ru.yandex.realty.consts.OfferAdd.EXTRA;
import static ru.yandex.realty.consts.OfferAdd.FORM_OF_TAXATION;
import static ru.yandex.realty.consts.OfferAdd.SUBRENT;
import static ru.yandex.realty.consts.OfferByRegion.Region.LOW;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.model.offer.DealStatus.SALE_OF_LEASE_RIGHTS;
import static ru.yandex.realty.model.offer.TaxationForm.USN;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.RISING;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

/**
 * Created by ivanvan on 28.07.17.
 */
@DisplayName("Форма добавления объявления. Блок цены")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PriceFieldTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void openManagementAddPage() {
        Offer body = OfferBuildingSteps.getDefaultOffer(COMMERCIAL_RENT).withLocation(getLocationForRegion(LOW));
        api.createVos2Account(account, OWNER);
        String id = offerBuildingSteps.addNewOffer(account)
                .withBody(body.withCreateTime(reformatOfferCreateDate(now().minusDays(1)))).withSearcherWait()
                .create()
                .getId();
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(id).open();
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(SUBRENT);
    }

    @Test
    @DisplayName("Чекбокс «Электроэнергия включена»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeElectricity() {
        offerAddSteps.onOfferAddPage().priceField().featureField(EXTRA).selectCheckBox("Включена электроэнергия");
        cancelServiceAndPublish();

        waitUntilSee(() -> Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasElectricityIncluded(true));
    }

    @Test
    @DisplayName("Проверяем поле «Предоплата»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeePrepay() {
        long prepay = Utils.getRandomShortLong();
        offerAddSteps.onOfferAddPage().priceField().prePayment().sendKeys(String.valueOf(prepay));
        cancelServiceAndPublish();
        waitUntilSee(() -> Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasPrepayment(prepay));
    }

    @Test
    @DisplayName("Проверяем поле «Комиссия агента»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeAgentPercent() {
        String value = String.valueOf(Utils.getRandomShortInt());

        offerAddSteps.onOfferAddPage().priceField().agentFee().sendKeys(value);
        cancelServiceAndPublish();

        waitUntilSee(() -> Assertions.assertThat(api.getOfferInfo(account).getSpecific())
                .hasAgentFee(Double.valueOf(value)));
    }

    @Test
    @DisplayName("Проверяем поле «Обеспечительный платеж»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeSafetyPay() {
        long value = Utils.getRandomShortLong();

        offerAddSteps.onOfferAddPage().priceField().securityPayment().sendKeys(String.valueOf(value));
        cancelServiceAndPublish();

        waitUntilSee(() -> Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasSecurityPayment(value));
    }

    @Test
    @DisplayName("Проверяем поле «Форма налогообложения»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeFormOfTaxation() {
        offerAddSteps.onOfferAddPage().priceField().featureField(FORM_OF_TAXATION).selectButton("УСН");
        cancelServiceAndPublish();

        waitUntilSee(() -> Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasTaxationForm("USN"));
    }

    @Test
    @DisplayName("Проверяем поле «Продажа права аренды»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeDealType() {
        offerAddSteps.onOfferAddPage().priceField().featureField(DEAL_TYPE).selectButton("Продажа права аренды");
        cancelServiceAndPublish();

        waitUntilSee(() -> Assertions.assertThat(api.getOfferInfo(account).getSpecific())
                .hasDealType("SALE_OF_LEASE_RIGHTS"));
    }

    @Step("Отменяем услугу и сохраняем изменения")
    private void cancelServiceAndPublish() {
        offerAddSteps.waitSaveOnBackend();
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.onOfferAddPage().publishTrap().sellTab(NORMAL_SALE).click();
        offerAddSteps.onOfferAddPage().publishTrap().deSelectPaySelector(RISING);
        offerAddSteps.onOfferAddPage().publishTrap().payButton().click();
        offerAddSteps.onOfferAddPage().continueButtonOnTrapPage().clickWhile(not(isDisplayed()));
        offerAddSteps.waitPublish();
    }

    @Step("Ждем пока")
    public void waitUntilSee(ThrowingRunnable assertion) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).pollInterval(1000, MILLISECONDS)
                .atMost(40000, MILLISECONDS).ignoreExceptions().untilAsserted(assertion);
    }
}
