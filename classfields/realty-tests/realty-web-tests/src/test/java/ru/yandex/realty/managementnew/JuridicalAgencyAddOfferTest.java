package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static ru.yandex.realty.consts.OfferAdd.PHONE;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_PLACEMENT;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Tag(JURICS)
@DisplayName("Агентство. Скриншоты.")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class JuridicalAgencyAddOfferTest {

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
    private OfferAddSteps offerAddSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
        managementSteps.clearInputByBackSpace(() ->
                offerAddSteps.onOfferAddPage().contactInfo().featureField(PHONE).input());
        String phone = Utils.getRandomPhone();
        offerAddSteps.onOfferAddPage().contactInfo().input(PHONE, removeStart(phone, "7"));
        offerAddSteps.waitSaveOnBackend();
        offerAddSteps.onOfferAddPage().button(NORMAL_PLACEMENT).click();
        offerAddSteps.waitPublish();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сравниваем скрины добавленого оффера")
    public void shouldSeeOffer() {
        basePageSteps.resize(1920, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onManagementNewPage().agencyOffer(FIRST));
        urlSteps.setProductionHost().open();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onManagementNewPage().agencyOffer(FIRST));
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
