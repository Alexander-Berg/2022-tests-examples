package ru.yandex.realty.card;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.test.api.realty.OfferType;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.OfferPageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Проверка карточки оффера со стороны продавца")
@Feature(OFFERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferFieldsApartmentTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private OfferPageSteps offerPageSteps;

    @Parameterized.Parameter
    public OfferType offerType;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {APARTMENT_SELL},
        });
    }

    @Before
    public void createAccount() {
        apiSteps.createVos2Account(account, OWNER);
        String offerId = offerBuildingSteps.addNewOffer(account).withType(offerType).create().getId();
        offerPageSteps.disableAd();
        compareSteps.resize(1920, 10000);
        urlSteps.testing().path(Pages.OFFER).path(offerId).open();
        offerPageSteps.refreshUntil(() -> offerPageSteps.onAuthorOfferPage().phones(), isDisplayed(), 55);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем пак скриншотов")
    public void shouldSeeBreadcrumbs() {
        Screenshot testing = getElementsScreenshot();
        urlSteps.setProductionHost().open();
        Screenshot production = getElementsScreenshot();
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    Screenshot getElementsScreenshot() {
        return compareSteps.takePackScreenshots(asList(
                offerPageSteps.onOfferCardPage().breadCrumbs(),
                offerPageSteps.onOfferCardPage().galleryOpener(),
                offerPageSteps.onOfferCardPage().shortcutsContainer(),
                offerPageSteps.onOfferCardPage().offerCardHighlights(),
                offerPageSteps.onOfferCardPage().offerCardFeatures(),
                offerPageSteps.onOfferCardPage().offerCardSummary(),
                offerPageSteps.onOfferCardPage().descriptionBlock(),
                offerPageSteps.onOfferCardPage().offerCardLocation()));
    }
}
