package ru.yandex.realty.compare;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.test.api.realty.offer.create.userid.Offer;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.COMPARISON;
import static ru.yandex.realty.model.offer.ParkingType.SEPARATE;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by kopitsa on 25.07.17.
 */
@DisplayName("Работа чекбокса «Показать только отличия»")
@Feature(COMPARISON)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ComparisonPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Test
    @Owner(KOPITSA)
    @DisplayName("Показать только отличия")
    public void shouldSeeOnlyMainFields() {
        apiSteps.createVos2Account(account, OWNER);
        Offer offerWithAllFields = getDefaultOffer(APARTMENT_SELL).withCommon(getDefaultOffer(APARTMENT_SELL)
                .getCommon().withHaggle(true)).withBuildingDescription(getDefaultOffer(APARTMENT_SELL)
                .getBuildingDescription().withParkingType("SEPARATE"));
        List<String> ids = offerBuildingSteps.addNewOffer(account).withBody(offerWithAllFields).count(2)
                .withSearcherWait().create().getIds();
        urlSteps.testing().path(Pages.COMPARISON).queryParam("id", ids.get(0)).queryParam("id", ids.get(1))
                .queryParam("type", "SELL").queryParam("category", "APARTMENT").open();

        shouldSeeAllFilterRows();
        basePageSteps.onComparisonPage().comparisionTable().selectCheckBox("Показывать только отличия");
        shouldNotSeeSecondaryFilterRows();
    }

    @Step("Проверяем, что отображаются все строки списка основных и вторичных фильтров")
    private void shouldSeeAllFilterRows() {
        basePageSteps.onComparisonPage().savedItemsTable().mainRowList().forEach(row -> row.should(isDisplayed()));
        basePageSteps.onComparisonPage().savedItemsTable().secondaryRowList().forEach(row -> row.should(isDisplayed()));
    }

    @Step("Проверяем, что не отображаются строки из списка вторичных фильтров")
    private void shouldNotSeeSecondaryFilterRows() {
        basePageSteps.onComparisonPage().savedItemsTable().mainRowList().forEach(row -> row.should(isDisplayed()));
        basePageSteps.onComparisonPage().savedItemsTable().secondaryRowList().forEach(row -> row.should(not(isDisplayed())));
    }
}
