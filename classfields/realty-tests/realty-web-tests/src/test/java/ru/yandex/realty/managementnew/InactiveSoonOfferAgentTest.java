package ru.yandex.realty.managementnew;


import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.OfferByRegion;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.time.LocalDateTime;
import java.util.Collection;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.AGENT;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

@DisplayName("Проверяем истекающие объявления")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class InactiveSoonOfferAgentTest {

    private static final LocalDateTime DEFAULT_DAYS_TO_DEACTIVATE = now().minusDays(90);

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
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public LocalDateTime date;

    @Parameterized.Parameter(1)
    public String deactivateTime;

    @Parameterized.Parameters(name = "{index} видим что объявление истекает {1}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {DEFAULT_DAYS_TO_DEACTIVATE.plusMinutes(18), "через \\d{2} минут"},
                {DEFAULT_DAYS_TO_DEACTIVATE.plusHours(5), "через \\d час"},
                {DEFAULT_DAYS_TO_DEACTIVATE.plusHours(23), "завтра"},
                {DEFAULT_DAYS_TO_DEACTIVATE.plusDays(2), "через 2 дня"}
        });
    }

    @Before
    public void openManagementPage() {
        api.createVos2Account(account, AGENT);
        String deactivateRemain = reformatOfferCreateDate(date);
        offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_SELL)
                .withLocation(getLocationForRegion(OfferByRegion.Region.LOW))
                .withCreateTime(deactivateRemain).withUpdateTime(deactivateRemain)).create();
        urlSteps.testing().path(MANAGEMENT_NEW);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeDeactivateSoonMessage() {
        urlSteps.open();
        basePageSteps.onManagementNewPage().agencyOffer(FIRST).message()
                .should(hasText(findPattern("Срок размещения истекает " + deactivateTime)));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeDeactivateSoonMessageAgent() {
        urlSteps.open();
        compareSteps.getElementScreenshot(basePageSteps.onManagementNewPage().agencyOffer(FIRST));
        urlSteps.setProductionHost().open();
        compareSteps.getElementScreenshot(basePageSteps.onManagementNewPage().agencyOffer(FIRST));
    }
}
