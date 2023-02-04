package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.test.api.realty.offer.userid.offerid.responses.OfferInfo;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PromocodesSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPremium;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPromotion;
import static ru.yandex.realty.page.ManagementNewPage.PUBLISH;

@Tag(JURICS)
@DisplayName("Юрик. Массовые покупки васов")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class JuricOfferServicesBatchTest {

    private static final int OFFERS_COUNT = 2;

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

    @Inject
    private Vos2Adaptor vos2Adaptor;

    @Inject
    private PromocodesSteps promocodesSteps;

    @Parameterized.Parameter
    public String serviceType;

    @Parameterized.Parameter(1)
    public Matcher<OfferInfo> serviceMatcher;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Продвинуть", hasPromotion()},
                {"Премиум", hasPremium()},

        });
    }

    @Before
    public void before() {
        managementSteps.setWindowSize(1200, 1600);
        apiSteps.createRealty3JuridicalAccount(account);
        promocodesSteps.use2000Promo(2);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL).count(OFFERS_COUNT).create();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на услугу, проверяем что ко всем офферам применена")
    public void shouldApplyServiceAllOffers() {
        managementSteps.onManagementNewPage().headerAgentOffers().selectAllChecbox().should(isDisplayed()).click();
        managementSteps.onManagementNewPage().offersControlPanel().buttonWithClickIf(PUBLISH).clickIf(isDisplayed());
        managementSteps.onManagementNewPage().offersControlPanel().button(serviceType).click();
        vos2Adaptor.getUserOffers(account.getId()).getOffers()
                .forEach(offer -> vos2Adaptor.waitOffer(account.getId(), offer.getId(), serviceMatcher));
    }
}
