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
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.realty.consts.OfferAdd.RENT;
import static ru.yandex.realty.consts.OfferAdd.RENT_A_DAY;
import static ru.yandex.realty.consts.OfferByRegion.Region.SUPER_HIGH;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.page.ManagementNewPage.REDACT;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

@DisplayName("Личный кабинет. Редактирование оффера")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PermanentOfferDealTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;
    @Parameterized.Parameter
    public String buttonName;
    @Inject
    private ApiSteps api;
    @Inject
    private OfferAddSteps offerAddSteps;
    @Inject
    private UrlSteps urlSteps;
    @Inject
    private Account account;
    @Inject
    private OfferBuildingSteps offerBuildingSteps;
    @Inject
    private ManagementSteps managementSteps;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {RENT},
                {RENT_A_DAY}
        });
    }

    @Before
    public void openManagementPage() {
        api.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(SUPER_HIGH))
                        .withCreateTime(reformatOfferCreateDate(now().minusDays(1)))).withInactive()
                .create();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("При форматировании объявления нельзя менять тип и форму недвижимости")
    public void shouldNotChangeSaveOffer() {
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.moveCursor(managementSteps.onManagementNewPage().offer(FIRST));
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().link(REDACT).click();
        offerAddSteps.onOfferAddPage().dealType().button(buttonName)
                .should(isDisabled());
    }
}
