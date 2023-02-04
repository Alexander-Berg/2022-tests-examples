package ru.yandex.realty.compare;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.test.api.realty.OfferType;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.test.api.realty.OfferType.APARTMENT_RENT;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.auto.test.api.realty.OfferType.COMMERCIAL_RENT;
import static ru.auto.test.api.realty.OfferType.COMMERCIAL_SELL;
import static ru.auto.test.api.realty.OfferType.HOUSE_RENT;
import static ru.auto.test.api.realty.OfferType.HOUSE_SELL;
import static ru.auto.test.api.realty.OfferType.LOT_SELL;
import static ru.auto.test.api.realty.OfferType.ROOM_RENT;
import static ru.auto.test.api.realty.OfferType.ROOM_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.COMPARISON;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Поля страницы сравнения")
@Feature(COMPARISON)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ComparisonPageScreenshotsTest {

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
    private CompareSteps compareSteps;

    @Inject
    private Account account;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Parameterized.Parameter
    public OfferType offerType;

    @Parameterized.Parameter(1)
    public String dealType;

    @Parameterized.Parameter(2)
    public String realtyType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {APARTMENT_SELL, "SELL", "APARTMENT"},
                {APARTMENT_RENT, "RENT", "APARTMENT"},
                {ROOM_SELL, "SELL", "ROOMS"},
                {ROOM_RENT, "RENT", "ROOMS"},
                {HOUSE_SELL, "SELL", "HOUSE"},
                {HOUSE_RENT, "RENT", "HOUSE"},
                {LOT_SELL, "SELL", "LOT"},
                {COMMERCIAL_SELL, "SELL", "COMMERCIAL"},
                {COMMERCIAL_RENT, "RENT", "COMMERCIAL"}
        });
    }


    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(KURAU)
    @DisplayName("Проверяем поля на странице сравнения офферов")
    public void shouldCheckComparisonFields() {
        apiSteps.createVos2Account(account, OWNER);
        String offerId = offerBuildingSteps.addNewOffer(account).withType(offerType).withSearcherWait().create()
                .getId();

        urlSteps.testing().path(Pages.COMPARISON).queryParam("id", offerId)
                .queryParam("type", dealType).queryParam("category", realtyType).open();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(basePageSteps.onComparisonPage()
                .comparisionTable().waitUntil(isDisplayed()));

        urlSteps.production().path(Pages.COMPARISON).queryParam("id", offerId)
                .queryParam("type", dealType).queryParam("category", realtyType).open();
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(basePageSteps.onComparisonPage()
                .comparisionTable().waitUntil(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
