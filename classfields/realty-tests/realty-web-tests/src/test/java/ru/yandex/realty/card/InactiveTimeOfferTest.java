package ru.yandex.realty.card;


import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.yandex.realty.adaptor.SearcherAdaptor;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static ru.auto.test.api.realty.OfferType.APARTMENT_RENT;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.auto.test.api.realty.OfferType.COMMERCIAL_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

@DisplayName("Проверяем снятые с публикации объявления")
@Issue("")
@Feature(RealtyFeatures.OFFERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class InactiveTimeOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private SearcherAdaptor searcherAdaptor;

    @Parameterized.Parameter
    public String hoursAgo;

    @Parameterized.Parameter(1)
    public String text;

    @Parameterized.Parameter(2)
    public OfferType offerType;

    @Parameterized.Parameters(name = "{index} - {0} час(а) назад")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"1", "1", APARTMENT_SELL},
                {"2", "2", APARTMENT_RENT},
                {"5", "5", COMMERCIAL_SELL},
                {"23", "вчера", APARTMENT_SELL},
                {"24", "вчера", APARTMENT_RENT},
                {"48", String.valueOf(now().minusHours(48).getDayOfMonth()), COMMERCIAL_SELL},
        });
    }

    @Before
    public void openManagementPage() {
        api.createVos2AccountWithoutLogin(account, OWNER);
        String id = offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(offerType)
                .withCreateTime(reformatOfferCreateDate(now().minusDays(20)))
                .withUpdateTime(reformatOfferCreateDate(now().minusHours(Long.valueOf(hoursAgo)))))
                .withInactive().create().getId();
        searcherAdaptor.waitSearcher(id);
        urlSteps.testing().path(OFFER).path(id).open();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeInactiveOffer() {
        basePageSteps.onOfferCardPage().offerCardSummary().tag("объявление снято или\u00a0устарело").should(isDisplayed());
    }
}
